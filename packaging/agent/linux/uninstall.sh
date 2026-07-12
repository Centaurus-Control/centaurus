#!/usr/bin/env bash
set -Eeuo pipefail

SERVICE_NAME="centaurus-agent"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

if [[ "${EUID}" -ne 0 ]]; then
  echo "ERROR: This uninstaller must be run as root. Use: sudo ./uninstall.sh" >&2
  exit 1
fi

if command -v systemctl >/dev/null 2>&1; then
  systemctl stop "${SERVICE_NAME}.service" >/dev/null 2>&1 || true
  systemctl disable "${SERVICE_NAME}.service" >/dev/null 2>&1 || true
fi

rm -f "${SERVICE_FILE}"

if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload
fi

echo "Centaurus Agent service removed. Installation files were left in place."
