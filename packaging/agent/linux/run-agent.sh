#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd -P)"
APP_DIR="${CENTAURUS_AGENT_HOME:-$SCRIPT_DIR}"

if [[ -f "${APP_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${APP_DIR}/.env"
  set +a
fi

JAR_PATH="${CENTAURUS_AGENT_JAR:-${APP_DIR}/centaurus-agent.jar}"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "Centaurus Agent jar not found: ${JAR_PATH}" >&2
  exit 1
fi

export CENTAURUS_AGENT_CONFIG_PATH="${CENTAURUS_AGENT_CONFIG_PATH:-${APP_DIR}/agent-data/config.yml}"
export CENTAURUS_AGENT_UI_BIND_ADDRESS="${CENTAURUS_AGENT_UI_BIND_ADDRESS:-127.0.0.1}"
export CENTAURUS_AGENT_UI_PORT="${CENTAURUS_AGENT_UI_PORT:-8787}"
export CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED="${CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED:-false}"
export CENTAURUS_AGENT_AUTO_CONNECT="${CENTAURUS_AGENT_AUTO_CONNECT:-true}"
export CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS="${CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS:-10}"
export CENTAURUS_AGENT_LOG_PATH="${CENTAURUS_AGENT_LOG_PATH:-${APP_DIR}/logs/centaurus-agent.log}"

mkdir -p "$(dirname "${CENTAURUS_AGENT_CONFIG_PATH}")" "$(dirname "${CENTAURUS_AGENT_LOG_PATH}")"

if [[ -n "${JAVA_BIN:-}" ]]; then
  JAVA_EXECUTABLE="${JAVA_BIN}"
elif [[ -n "${JAVA_HOME:-}" ]]; then
  JAVA_EXECUTABLE="${JAVA_HOME}/bin/java"
else
  JAVA_EXECUTABLE="${APP_DIR}/runtime/bin/java"
fi

if [[ ! -x "${JAVA_EXECUTABLE}" ]]; then
  echo "Java executable not found or not executable: ${JAVA_EXECUTABLE}" >&2
  exit 1
fi

exec "${JAVA_EXECUTABLE}" ${JAVA_OPTS:-} -jar "${JAR_PATH}"
