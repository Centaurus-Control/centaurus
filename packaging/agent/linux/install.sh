#!/usr/bin/env bash
set -Eeuo pipefail

SERVICE_NAME="centaurus-agent"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

import_certificate_file() {
  local alias="$1"
  local certificate_file="$2"
  local java_home="${JAVA_HOME:-${INSTALL_DIR}/runtime}"
  local keytool="${java_home}/bin/keytool"
  local cacerts="${java_home}/lib/security/cacerts"

  [[ -x "${keytool}" ]] || fail "keytool not found or not executable: ${keytool}"
  [[ -f "${cacerts}" ]] || fail "Java truststore not found: ${cacerts}"

  if "${keytool}" -list \
      -keystore "${cacerts}" \
      -storepass "${KEYSTORE_PASSWORD}" \
      -alias "${alias}" >/dev/null 2>&1; then

    "${keytool}" -delete \
      -keystore "${cacerts}" \
      -storepass "${KEYSTORE_PASSWORD}" \
      -alias "${alias}"
  fi

  "${keytool}" -importcert \
    -trustcacerts \
    -noprompt \
    -keystore "${cacerts}" \
    -storepass "${KEYSTORE_PASSWORD}" \
    -alias "${alias}" \
    -file "${certificate_file}"
}

split_pem_bundle() {
  local source_file="$1"
  local target_dir="$2"

  awk -v target_dir="${target_dir}" '
    /-----BEGIN CERTIFICATE-----/ {
      in_cert = 1
      count++
      file = sprintf("%s/certificate-%03d.pem", target_dir, count)
    }
    in_cert {
      print > file
    }
    /-----END CERTIFICATE-----/ {
      in_cert = 0
      close(file)
    }
    END {
      if (in_cert) {
        exit 2
      }
      if (count < 1) {
        exit 3
      }
    }
  ' "${source_file}"
}

resolve_install_dir_placeholders() {
  local target_file="$1"
  local escaped_install_dir="${INSTALL_DIR}"

  escaped_install_dir="${escaped_install_dir//\\/\\\\}"
  escaped_install_dir="${escaped_install_dir//&/\\&}"
  escaped_install_dir="${escaped_install_dir//|/\\|}"

  sed -i -e "s|{{INSTALL_DIR}}|${escaped_install_dir}|g" "${target_file}"

  if grep -q "{{INSTALL_DIR}}" "${target_file}"; then
    fail "Unresolved {{INSTALL_DIR}} placeholder in ${target_file}"
  fi
}

import_local_trusted_certificates() {
  local certificates_file="${CENTAURUS_AGENT_TRUSTED_CERTIFICATES_FILE:-}"
  local tmp_dir
  local index=0

  if [[ -z "${certificates_file}" ]]; then
    return
  fi

  [[ -f "${certificates_file}" ]] || fail "Trusted certificate file not found: ${certificates_file}"
  command -v awk >/dev/null 2>&1 || fail "awk is required to import local trusted certificates."

  tmp_dir="$(mktemp -d)"
  if ! split_pem_bundle "${certificates_file}" "${tmp_dir}"; then
    rm -rf "${tmp_dir}"
    fail "Trusted certificate file must contain one or more PEM certificates."
  fi

  for certificate_file in "${tmp_dir}"/certificate-*.pem; do
    index=$((index + 1))
    import_certificate_file "centaurus-bootstrap-${index}" "${certificate_file}"
  done

  rm -rf "${tmp_dir}"
  echo "Imported ${index} local trusted certificate(s)."
}

if [[ "${EUID}" -ne 0 ]]; then
  fail "This installer must be run as root. Use: sudo ./install.sh"
fi

INSTALL_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd -P)"
PACKAGE_ARCH="{{PACKAGE_ARCH}}"
AGENT_VERSION="{{AGENT_VERSION}}"

case "$(uname -m)" in
  x86_64|amd64) HOST_ARCH="amd64" ;;
  aarch64|arm64) HOST_ARCH="arm64" ;;
  *) fail "Unsupported CPU architecture: $(uname -m). Supported: amd64, arm64." ;;
esac

if [[ "${HOST_ARCH}" != "${PACKAGE_ARCH}" ]]; then
  fail "This package is for ${PACKAGE_ARCH}, but this host is ${HOST_ARCH}."
fi

if ! command -v systemctl >/dev/null 2>&1 || [[ ! -d /run/systemd/system ]]; then
  fail "systemd is required to install Centaurus Agent as a service."
fi

[[ -f "${INSTALL_DIR}/centaurus-agent.jar" ]] || fail "Missing ${INSTALL_DIR}/centaurus-agent.jar"
[[ -x "${INSTALL_DIR}/runtime/bin/java" ]] || fail "Missing executable Temurin runtime: ${INSTALL_DIR}/runtime/bin/java"
[[ -f "${INSTALL_DIR}/run-agent.sh" ]] || fail "Missing ${INSTALL_DIR}/run-agent.sh"

mkdir -p "${INSTALL_DIR}/agent-data" "${INSTALL_DIR}/logs"
chmod +x "${INSTALL_DIR}/run-agent.sh"

if [[ ! -f "${INSTALL_DIR}/.env" ]]; then
  cp "${INSTALL_DIR}/.env.example" "${INSTALL_DIR}/.env"
  chmod 0644 "${INSTALL_DIR}/.env"
else
  echo "Keeping existing ${INSTALL_DIR}/.env"
fi

resolve_install_dir_placeholders "${INSTALL_DIR}/.env"

ENV_TRUSTED_CERTIFICATES_FILE="${CENTAURUS_AGENT_TRUSTED_CERTIFICATES_FILE:-}"

set -a
# shellcheck disable=SC1091
source "${INSTALL_DIR}/.env"
set +a

if [[ -n "${ENV_TRUSTED_CERTIFICATES_FILE}" ]]; then
  CENTAURUS_AGENT_TRUSTED_CERTIFICATES_FILE="${ENV_TRUSTED_CERTIFICATES_FILE}"
fi

import_local_trusted_certificates

cat > "${SERVICE_FILE}" <<EOF
[Unit]
Description=Centaurus Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=${INSTALL_DIR}
EnvironmentFile=${INSTALL_DIR}/.env
ExecStart=${INSTALL_DIR}/run-agent.sh
Restart=always
RestartSec=10
SuccessExitStatus=143
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF

chmod 0644 "${SERVICE_FILE}"

systemctl daemon-reload
systemctl enable "${SERVICE_NAME}.service"
systemctl restart "${SERVICE_NAME}.service"

echo "Centaurus Agent ${AGENT_VERSION} installed successfully."
systemctl --no-pager --full status "${SERVICE_NAME}.service" || true
