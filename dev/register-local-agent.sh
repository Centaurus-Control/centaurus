#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_URL="${CENTAURUS_DEV_SERVER_URL:-http://localhost:8080}"
AGENT_URL="${CENTAURUS_DEV_AGENT_URL:-http://127.0.0.1:8787}"
ADMIN_USERNAME="${CENTAURUS_DEV_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${CENTAURUS_DEV_ADMIN_PASSWORD:-admin}"
AGENT_CONFIG_PATH="${CENTAURUS_DEV_AGENT_CONFIG_PATH:-$ROOT_DIR/agent/agent-data/config.yml}"
SCRIPT_PATH="$ROOT_DIR/agent/scripts/dev-echo.sh"
AGENT_COOKIE_JAR="$(mktemp)"
trap 'rm -f "$AGENT_COOKIE_JAR"' EXIT

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

json_get() {
  python3 -c 'import json,sys; data=json.load(sys.stdin); cur=data
for part in sys.argv[1].split("."):
    cur = cur[int(part)] if isinstance(cur, list) else cur.get(part)
print("" if cur is None else cur)' "$1"
}

http_json() {
  local method="$1"
  local url="$2"
  local payload="${3:-}"
  shift 3 || true
  if [[ -n "$payload" ]]; then
    curl -fsS -X "$method" "$url" -H "Content-Type: application/json" "$@" --data "$payload"
  else
    curl -fsS -X "$method" "$url" "$@"
  fi
}

require_command curl
require_command python3

mkdir -p "$(dirname "$AGENT_CONFIG_PATH")"
chmod +x "$SCRIPT_PATH"

if [[ -f "$AGENT_CONFIG_PATH" ]] && grep -q '^agentId:' "$AGENT_CONFIG_PATH"; then
  echo "Agent config already contains an enrollment at $AGENT_CONFIG_PATH"
  echo "Delete it first if you want to re-enroll against a fresh DB."
  exit 1
fi

cat > "$AGENT_CONFIG_PATH" <<EOF
scripts:
  - id: "11111111-1111-4111-8111-111111111111"
    label: "Dev Echo"
    description: "Simple local script for Centaurus development checks"
    command: "$SCRIPT_PATH"
    workingDirectory: "$ROOT_DIR"
    timeoutSeconds: 30
    argumentMappings:
      - type: NAMED_PARAMETER
        name: "--target"
        parameterName: target
      - type: FLAG_PARAMETER
        name: "--dry-run"
        parameterName: dryRun
      - type: NAMED_PARAMETER
        name: "--message"
        parameterName: message
    parameters:
      target:
        type: enum
        required: true
        allowedValues:
          - local
          - ci
      dryRun:
        type: boolean
        required: false
        default: true
      message:
        type: string
        required: false
        default: "hello from Centaurus"
    resultSchema:
      status:
        type: enum
        required: true
        allowedValues:
          - SUCCESS
          - FAILED
      exitCode:
        type: integer
        required: true
    spamProtection:
      enabled: false
      cooldownSeconds: 1
EOF

echo "Checking server at $SERVER_URL"
curl -fsS "$SERVER_URL/actuator/health" >/dev/null

echo "Checking local agent at $AGENT_URL"
curl -fsS "$AGENT_URL/api/agent/status" >/dev/null

echo "Logging in as $ADMIN_USERNAME"
login_response="$(http_json POST "$SERVER_URL/api/auth/login" "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")"
access_token="$(printf '%s' "$login_response" | json_get accessToken)"

echo "Logging in to local agent UI"
http_json POST "$AGENT_URL/api/agent/ui/login" "{\"serverUrl\":\"$SERVER_URL\",\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}" -c "$AGENT_COOKIE_JAR" >/dev/null

echo "Creating enrollment token"
enrollment_response="$(http_json POST "$SERVER_URL/api/admin/enrollment-tokens" '{"suggestedName":"Local Dev PC","expiresIn":"PT1H"}' -H "Authorization: Bearer $access_token")"
enrollment_bundle="$(printf '%s' "$enrollment_response" | json_get enrollmentBundle)"

echo "Enrolling local agent"
http_json POST "$AGENT_URL/api/agent/enroll" "{\"enrollmentBundle\":\"$enrollment_bundle\",\"displayName\":\"Local Dev PC\"}" -b "$AGENT_COOKIE_JAR" >/dev/null

echo "Waiting for agent WebSocket connection and script manifest"
machine_id=""
script_count="0"
for _ in {1..30}; do
  machines_response="$(http_json GET "$SERVER_URL/api/machines" "" -H "Authorization: Bearer $access_token")"
  machine_id="$(printf '%s' "$machines_response" | python3 -c 'import json,sys
data=json.load(sys.stdin)
online=[m for m in data if m.get("displayName")=="Local Dev PC"]
print("" if not online else online[0]["id"])')"
  if [[ -n "$machine_id" ]]; then
    scripts_response="$(http_json GET "$SERVER_URL/api/machines/$machine_id/scripts" "" -H "Authorization: Bearer $access_token")"
    script_count="$(printf '%s' "$scripts_response" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))')"
    if [[ "$script_count" != "0" ]]; then
      break
    fi
  fi
  sleep 1
done

if [[ -z "$machine_id" ]]; then
  echo "Enrollment succeeded, but machine did not appear in server API." >&2
  exit 1
fi

if [[ "$script_count" == "0" ]]; then
  echo "Enrollment succeeded, but the agent did not connect and publish its script manifest in time." >&2
  exit 1
fi

echo "Machine ID: $machine_id"
echo "Triggering Dev Echo script"
command_response="$(http_json POST "$SERVER_URL/api/machines/$machine_id/commands/execute-script" '{"scriptId":"11111111-1111-4111-8111-111111111111","parameters":{"target":"local","dryRun":true,"message":"registered by dev harness"}}' -H "Authorization: Bearer $access_token")"
command_id="$(printf '%s' "$command_response" | json_get commandId)"
echo "Command ID: $command_id"

echo "Waiting for command result"
for _ in {1..30}; do
  commands_response="$(http_json GET "$SERVER_URL/api/machines/$machine_id/commands" "" -H "Authorization: Bearer $access_token")"
  status="$(printf '%s' "$commands_response" | python3 -c 'import json,sys
data=json.load(sys.stdin)
cmd=next((c for c in data if c.get("commandId")=="'"$command_id"'"), None)
print("" if cmd is None else cmd.get("status",""))')"
  if [[ "$status" == "FINISHED" || "$status" == "FAILED" || "$status" == "REJECTED" ]]; then
    echo "$commands_response" | python3 -m json.tool
    exit 0
  fi
  sleep 1
done

echo "Command did not finish in time." >&2
exit 1
