#!/usr/bin/env bash
set -Eeuo pipefail

SERVICE_NAME="centaurus-agent"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

download_to_file() {
  local url="$1"
  local target="$2"

  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --show-error --silent --output "${target}" "${url}"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet --output-document="${target}" "${url}"
  else
    fail "curl or wget is required to download trusted certificates."
  fi
}

import_trusted_certificates() {
  local certificates_url="${CENTAURUS_AGENT_TRUSTED_CERTIFICATES_URL:-}"
  local java_home="${JAVA_HOME:-${INSTALL_DIR}/runtime}"
  local keytool="${java_home}/bin/keytool"
  local cacerts="${java_home}/lib/security/cacerts"
  local tmp_dir
  local bundle_file
  local imported_count=0

  if [[ -z "${certificates_url}" ]]; then
    echo "No CENTAURUS_AGENT_TRUSTED_CERTIFICATES_URL configured; skipping trusted certificate import."
    return
  fi

  [[ -x "${keytool}" ]] || fail "keytool not found or not executable: ${keytool}"
  [[ -f "${cacerts}" ]] || fail "Java truststore not found: ${cacerts}"
  command -v base64 >/dev/null 2>&1 || fail "base64 is required to import trusted certificates."

  tmp_dir="$(mktemp -d)"
  bundle_file="${tmp_dir}/trusted-certificates.txt"

  echo "Downloading trusted certificates from ${certificates_url}..."
  download_to_file "${certificates_url}" "${bundle_file}"

  while IFS=$'\t' read -r alias certificate_base64; do
    if [[ -z "${alias}" || "${alias}" == \#* ]]; then
      continue
    fi
    if [[ -z "${certificate_base64}" ]]; then
      fail "Invalid trusted certificate bundle line for alias: ${alias}"
    fi

    local certificate_file="${tmp_dir}/${alias}.pem"
    printf '%s' "${certificate_base64}" | base64 --decode > "${certificate_file}" || fail "Could not decode certificate for alias: ${alias}"

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

    imported_count=$((imported_count + 1))
  done < "${bundle_file}"

  rm -rf "${tmp_dir}"
  echo "Imported ${imported_count} trusted certificate(s)."
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
  sed \
    -e "s|{{INSTALL_DIR}}|${INSTALL_DIR}|g" \
    "${INSTALL_DIR}/.env.example" > "${INSTALL_DIR}/.env"
  chmod 0644 "${INSTALL_DIR}/.env"
else
  echo "Keeping existing ${INSTALL_DIR}/.env"
fi

ENV_TRUSTED_CERTIFICATES_URL="${CENTAURUS_AGENT_TRUSTED_CERTIFICATES_URL:-}"

set -a
# shellcheck disable=SC1091
source "${INSTALL_DIR}/.env"
set +a

if [[ -n "${ENV_TRUSTED_CERTIFICATES_URL}" ]]; then
  CENTAURUS_AGENT_TRUSTED_CERTIFICATES_URL="${ENV_TRUSTED_CERTIFICATES_URL}"
fi

import_trusted_certificates

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
