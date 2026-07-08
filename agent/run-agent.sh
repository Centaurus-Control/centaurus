#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${CENTAURUS_AGENT_HOME:-$SCRIPT_DIR}"
JAR_PATH="${CENTAURUS_AGENT_JAR:-}"
JAVA_HOME="${JAVA_HOME:-}"
JAVA_BIN="${JAVA_BIN:-}"

if [[ -f "$APP_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$APP_DIR/.env"
  set +a
fi

if [[ -z "$JAR_PATH" ]]; then
  JAR_PATH="$(find "$APP_DIR" -maxdepth 2 -type f -name 'centaurus-agent-*.jar' ! -name '*-plain.jar' | sort | tail -n 1)"
fi

if [[ -z "$JAR_PATH" || ! -f "$JAR_PATH" ]]; then
  echo "Centaurus Agent jar not found. Set CENTAURUS_AGENT_JAR or place centaurus-agent-*.jar in $APP_DIR." >&2
  exit 1
fi

export CENTAURUS_AGENT_CONFIG_PATH="${CENTAURUS_AGENT_CONFIG_PATH:-$APP_DIR/agent-data/config.yml}"
export CENTAURUS_AGENT_UI_BIND_ADDRESS="${CENTAURUS_AGENT_UI_BIND_ADDRESS:-127.0.0.1}"
export CENTAURUS_AGENT_UI_PORT="${CENTAURUS_AGENT_UI_PORT:-8787}"
export CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED="${CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED:-false}"
export CENTAURUS_AGENT_AUTO_CONNECT="${CENTAURUS_AGENT_AUTO_CONNECT:-true}"
export CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS="${CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS:-10}"
export CENTAURUS_AGENT_LOG_PATH="${CENTAURUS_AGENT_LOG_PATH:-$APP_DIR/logs/centaurus-agent.log}"

mkdir -p "$(dirname "$CENTAURUS_AGENT_CONFIG_PATH")"
mkdir -p "$(dirname "$CENTAURUS_AGENT_LOG_PATH")"

if [[ -z "$JAVA_BIN" ]]; then
  if [[ -n "$JAVA_HOME" ]]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
  else
    JAVA_BIN="java"
  fi
fi

exec "$JAVA_BIN" ${JAVA_OPTS:-} -jar "$JAR_PATH"
