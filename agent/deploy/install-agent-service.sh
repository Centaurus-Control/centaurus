#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="centaurus-agent"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

WORKING_DIR="/opt/centaurus"
ENV_FILE="${WORKING_DIR}/.env"
EXEC_FILE="${WORKING_DIR}/run-agent.sh"

JAVA_HOME="${WORKING_DIR}/java"
KEYTOOL="${JAVA_HOME}/bin/keytool"
JAVA_CACERTS="${JAVA_HOME}/lib/security/cacerts"

ROOT_CA_FILE="${WORKING_DIR}/root-ca.crt"

CA_ALIAS="centaurus-root-ca"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Error: This script must be run as root."
  echo "Run it with: sudo $0"
  exit 1
fi

install_root_ca() {
  if [[ ! -f "${ROOT_CA_FILE}" ]]; then
    echo "No root CA found at ${ROOT_CA_FILE}; skipping Java truststore import."
    return
  fi

  echo "Installing Centaurus root CA into Java truststore..."

  if [[ ! -x "${KEYTOOL}" ]]; then
    echo "Error: keytool not found or not executable: ${KEYTOOL}"
    exit 1
  fi

  if [[ ! -f "${JAVA_CACERTS}" ]]; then
    echo "Error: Java truststore not found: ${JAVA_CACERTS}"
    exit 1
  fi

  if "${KEYTOOL}" -list \
      -keystore "${JAVA_CACERTS}" \
      -storepass "${KEYSTORE_PASSWORD}" \
      -alias "${CA_ALIAS}" >/dev/null 2>&1; then

    echo "Existing CA alias found. Replacing alias: ${CA_ALIAS}"

    "${KEYTOOL}" -delete \
      -keystore "${JAVA_CACERTS}" \
      -storepass "${KEYSTORE_PASSWORD}" \
      -alias "${CA_ALIAS}"
  fi

  "${KEYTOOL}" -importcert \
    -trustcacerts \
    -noprompt \
    -keystore "${JAVA_CACERTS}" \
    -storepass "${KEYSTORE_PASSWORD}" \
    -alias "${CA_ALIAS}" \
    -file "${ROOT_CA_FILE}"

  echo "Root CA installed successfully."
}

install_service() {
  echo "Installing ${SERVICE_NAME} systemd service..."

  if [[ ! -d "${WORKING_DIR}" ]]; then
    echo "Warning: Working directory does not exist: ${WORKING_DIR}"
  fi

  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "Warning: Environment file does not exist: ${ENV_FILE}"
  fi

  if [[ ! -f "${EXEC_FILE}" ]]; then
    echo "Warning: Agent start script does not exist: ${EXEC_FILE}"
  else
    chmod +x "${EXEC_FILE}"
  fi

  cat > "${SERVICE_FILE}" <<'EOF'
[Unit]
Description=Centaurus Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=/opt/centaurus
EnvironmentFile=/opt/centaurus/.env
Environment=JAVA_HOME=/opt/centaurus/java
ExecStart=/opt/centaurus/run-agent.sh

Restart=always
RestartSec=10
SuccessExitStatus=143

# For first LAN tests, root is the simplest option because reboot/shutdown scripts
# and WOL may need elevated host permissions.
# Later, switch to a dedicated user and sudo rules for specific commands.
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF

  chmod 644 "${SERVICE_FILE}"

  echo "Reloading systemd..."
  systemctl daemon-reload

  echo "Enabling ${SERVICE_NAME}..."
  systemctl enable "${SERVICE_NAME}.service"

  echo "Starting ${SERVICE_NAME}..."
  systemctl restart "${SERVICE_NAME}.service"

  echo
  echo "Service installed successfully."
  echo
  echo "Status:"
  systemctl --no-pager --full status "${SERVICE_NAME}.service" || true
}

install_root_ca
install_service
