#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/opt/centaurus"
JAVA_DIR="${APP_DIR}/java"
JDK_VERSION="21"
ADOPTIUM_VENDOR="eclipse"
AGENT_JAR_NAME="centaurus-agent.jar"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Error: This script must be run as root."
  echo "Run it with: sudo $0"
  exit 1
fi

detect_arch() {
  case "$(uname -m)" in
    x86_64|amd64) echo "x64" ;;
    aarch64|arm64) echo "aarch64" ;;
    armv7l|armv7*) echo "arm" ;;
    ppc64le) echo "ppc64le" ;;
    s390x) echo "s390x" ;;
    *)
      echo "Error: Unsupported CPU architecture: $(uname -m)" >&2
      exit 1
      ;;
  esac
}

download_file() {
  local url="$1"
  local target="$2"

  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --show-error --silent --output "${target}" "${url}"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet --output-document="${target}" "${url}"
  else
    echo "Error: curl or wget is required to download Temurin JDK ${JDK_VERSION}." >&2
    exit 1
  fi
}

install_temurin_jdk() {
  local arch
  local download_url
  local archive
  local tmp_dir

  arch="$(detect_arch)"
  download_url="https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/ga/linux/${arch}/jdk/hotspot/normal/${ADOPTIUM_VENDOR}?project=jdk"
  tmp_dir="$(mktemp -d)"
  archive="${tmp_dir}/temurin-${JDK_VERSION}.tar.gz"

  echo "Downloading Eclipse Temurin JDK ${JDK_VERSION} for linux/${arch}..."
  if ! download_file "${download_url}" "${archive}"; then
    rm -rf "${tmp_dir}"
    exit 1
  fi

  echo "Installing Java into ${JAVA_DIR}..."
  rm -rf "${JAVA_DIR}"
  mkdir -p "${JAVA_DIR}"
  if ! tar -xzf "${archive}" --strip-components=1 -C "${JAVA_DIR}"; then
    rm -rf "${tmp_dir}"
    exit 1
  fi
  rm -rf "${tmp_dir}"

  if [[ ! -x "${JAVA_DIR}/bin/java" || ! -x "${JAVA_DIR}/bin/keytool" ]]; then
    echo "Error: Extracted JDK is incomplete: ${JAVA_DIR}" >&2
    exit 1
  fi
}

install_agent_files() {
  local jar="${SCRIPT_DIR}/${AGENT_JAR_NAME}"

  if [[ ! -f "${jar}" ]]; then
    echo "Error: Agent jar not found: ${jar}" >&2
    echo "Place ${AGENT_JAR_NAME} next to $(basename "$0") before running this script." >&2
    exit 1
  fi

  if [[ ! -f "${SCRIPT_DIR}/run-agent.sh" ]]; then
    echo "Error: run-agent.sh not found next to $(basename "$0")." >&2
    exit 1
  fi

  if [[ ! -f "${SCRIPT_DIR}/install-agent-service.sh" ]]; then
    echo "Error: install-agent-service.sh not found next to $(basename "$0")." >&2
    exit 1
  fi

  echo "Installing Centaurus Agent files into ${APP_DIR}..."
  mkdir -p "${APP_DIR}/agent-data" "${APP_DIR}/logs"

  find "${APP_DIR}" -maxdepth 1 -type f \( -name 'centaurus-agent.jar' -o -name 'centaurus-agent-*.jar' \) -delete
  install -m 0644 "${jar}" "${APP_DIR}/${AGENT_JAR_NAME}"
  install -m 0755 "${SCRIPT_DIR}/run-agent.sh" "${APP_DIR}/run-agent.sh"
  install -m 0755 "${SCRIPT_DIR}/install-agent-service.sh" "${APP_DIR}/install-agent-service.sh"

  if [[ -f "${SCRIPT_DIR}/root-ca.crt" ]]; then
    install -m 0644 "${SCRIPT_DIR}/root-ca.crt" "${APP_DIR}/root-ca.crt"
  fi

  if [[ ! -f "${APP_DIR}/.env" ]]; then
    cat > "${APP_DIR}/.env" <<EOF
CENTAURUS_AGENT_UI_BIND_ADDRESS=127.0.0.1
CENTAURUS_AGENT_UI_PORT=8787
CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED=false
CENTAURUS_AGENT_CONFIG_PATH=${APP_DIR}/agent-data/config.yml
CENTAURUS_AGENT_LOG_PATH=${APP_DIR}/logs/centaurus-agent.log
CENTAURUS_AGENT_AUTO_CONNECT=true
CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS=10
JAVA_HOME=${JAVA_DIR}
JAVA_OPTS="-Xms128m -Xmx256m"
EOF
    chmod 0644 "${APP_DIR}/.env"
  else
    echo "Keeping existing ${APP_DIR}/.env."
  fi
}

install_temurin_jdk
install_agent_files

echo "Running service installer..."
"${APP_DIR}/install-agent-service.sh"
