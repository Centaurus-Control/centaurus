#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

is_true() {
  [[ "${1:-false}" == "true" ]]
}

to_absolute_path() {
  local input_path="$1"

  if [[ "$input_path" == /* ]]; then
    echo "$input_path"
  else
    echo "$SCRIPT_DIR/$input_path"
  fi
}

resolve_path_relative_to() {
  local base_dir="$1"
  local input_path="$2"

  if [[ "$input_path" == /* ]]; then
    echo "$input_path"
  else
    echo "$base_dir/$input_path"
  fi
}

clear_directory_contents() {
  local target_dir="$1"

  [[ -n "$target_dir" ]] || fail "Target directory must not be empty"
  [[ "$target_dir" != "/" ]] || fail "Refusing to clear root directory"
  [[ "$target_dir" != "$SCRIPT_DIR" ]] || fail "Refusing to clear script directory: $SCRIPT_DIR"

  mkdir -p "$target_dir"

  log "Clearing directory: $target_dir"

  find "$target_dir" \
    -mindepth 1 \
    -maxdepth 1 \
    -exec rm -rf -- {} +
}

env_file_value() {
  local file_path="$1"
  local key="$2"
  local line
  local value

  line="$(grep -E "^[[:space:]]*${key}=" "$file_path" | tail -n 1 || true)"
  [[ -n "$line" ]] || return 1

  value="${line#*=}"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"

  if [[ "$value" == \"*\" && "$value" == *\" ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "$value" == \'*\' && "$value" == *\' ]]; then
    value="${value:1:${#value}-2}"
  fi

  printf '%s' "$value"
}

require_env_file_key() {
  local file_path="$1"
  local key="$2"
  local value

  value="$(env_file_value "$file_path" "$key" || true)"
  [[ -n "$value" ]] || fail "Required runtime env value is missing or empty: $key in $file_path"
}

reject_placeholder_env_value() {
  local file_path="$1"
  local key="$2"
  local value

  value="$(env_file_value "$file_path" "$key" || true)"

  case "$value" in
    ""|change-me|change-me-*|*-change-me|*change-me*)
      fail "Runtime env value still contains a placeholder: $key in $file_path"
      ;;
  esac
}

build_docker_image() {
  local context_dir="$1"
  local dockerfile_path="$2"
  local image_name="$3"
  local image_tag="$4"
  local commit_short="$5"
  local extra_build_args_text="${6:-}"
  local server_gradle_tasks_arg="${7:-}"

  local docker_tag_args=(
    "-t" "$image_name:$image_tag"
  )

  if is_true "$ADD_COMMIT_TAG"; then
    docker_tag_args+=(
      "-t" "$image_name:$commit_short"
    )
  fi

  local docker_build_args=()

  if is_true "$PULL_BASE_IMAGES"; then
    docker_build_args+=("--pull")
  fi

  if is_true "$DOCKER_NO_CACHE"; then
    docker_build_args+=("--no-cache")
  fi

  docker_build_args+=(
    "--label" "org.opencontainers.image.source=$REMOTE_URL"
    "--label" "org.opencontainers.image.revision=$COMMIT_HASH"
    "--label" "org.opencontainers.image.created=$BUILD_TIMESTAMP"
  )

  if [[ -n "$extra_build_args_text" ]]; then
    read -r -a extra_build_args <<< "$extra_build_args_text"
    docker_build_args+=("${extra_build_args[@]}")
  fi

  if [[ -n "$server_gradle_tasks_arg" ]]; then
    docker_build_args+=(
      "--build-arg" "SERVER_GRADLE_TASKS=$server_gradle_tasks_arg"
    )
  fi

  log "Docker build context: $context_dir"
  log "Dockerfile path: $dockerfile_path"
  log "Docker image: $image_name:$image_tag"

  if is_true "$ADD_COMMIT_TAG"; then
    log "Docker commit image: $image_name:$commit_short"
  fi

  docker build \
    "${docker_build_args[@]}" \
    "${docker_tag_args[@]}" \
    -f "$dockerfile_path" \
    "$context_dir"
}

wait_for_compose_service_health() {
  local service_name="$1"
  local timeout_seconds="$2"
  local start_time
  local container_id
  local health_status
  local elapsed_seconds

  start_time="$(date +%s)"

  log "Waiting for compose service health: $service_name"

  while true; do
    container_id="$("${COMPOSE_CMD[@]}" ps -q "$service_name" || true)"

    if [[ -n "$container_id" ]]; then
      health_status="$(docker inspect \
        --format '{{ if .State.Health }}{{ .State.Health.Status }}{{ else }}none{{ end }}' \
        "$container_id" 2>/dev/null || true)"

      if [[ "$health_status" == "healthy" ]]; then
        log "Compose service is healthy: $service_name"
        return 0
      fi

      if [[ "$health_status" == "none" ]]; then
        log "Compose service has no healthcheck, continuing: $service_name"
        return 0
      fi

      log "Compose service health status: $service_name=$health_status"
    else
      log "Compose service container not found yet: $service_name"
    fi

    elapsed_seconds="$(( $(date +%s) - start_time ))"

    if (( elapsed_seconds >= timeout_seconds )); then
      fail "Timed out waiting for compose service health: $service_name"
    fi

    sleep 3
  done
}

validate_runtime_env() {
  local compose_env_file_path="$1"
  local expected_server_image="$2"
  local expected_webui_image="$3"

  [[ -f "$compose_env_file_path" ]] || fail "Compose env file not found: $compose_env_file_path. Copy .env.example to .env and configure it first."

  local required_keys=(
    CENTAURUS_SERVER_IMAGE
    CENTAURUS_WEBUI_IMAGE
    CENTAURUS_SERVER_HTTP_PORT
    CENTAURUS_WEBUI_HTTP_PORT
    POSTGRES_DB
    POSTGRES_USER
    POSTGRES_PASSWORD
    CENTAURUS_AUTH_JWT_SECRET
    CENTAURUS_BOOTSTRAP_ADMIN_USERNAME
    CENTAURUS_BOOTSTRAP_ADMIN_PASSWORD
    CENTAURUS_ENROLLMENT_SERVER_URL
    CENTAURUS_ENROLLMENT_WS_URL
  )

  local placeholder_keys=(
    POSTGRES_PASSWORD
    CENTAURUS_AUTH_JWT_SECRET
    CENTAURUS_BOOTSTRAP_ADMIN_PASSWORD
  )

  for key in "${required_keys[@]}"; do
    require_env_file_key "$compose_env_file_path" "$key"
  done

  for key in "${placeholder_keys[@]}"; do
    reject_placeholder_env_value "$compose_env_file_path" "$key"
  done

  if is_true "$SERVER_ENABLED"; then
    local configured_server_image
    configured_server_image="$(env_file_value "$compose_env_file_path" CENTAURUS_SERVER_IMAGE || true)"
    [[ "$configured_server_image" == "$expected_server_image" ]] || fail "CENTAURUS_SERVER_IMAGE must match the image built by deploy.sh. Expected '$expected_server_image', got '$configured_server_image'."
  fi

  if is_true "$WEBUI_ENABLED"; then
    local configured_webui_image
    configured_webui_image="$(env_file_value "$compose_env_file_path" CENTAURUS_WEBUI_IMAGE || true)"
    [[ "$configured_webui_image" == "$expected_webui_image" ]] || fail "CENTAURUS_WEBUI_IMAGE must match the image built by deploy.sh. Expected '$expected_webui_image', got '$configured_webui_image'."
  fi
}

[[ -f "$ENV_FILE" ]] || fail "Deployment env file not found: $ENV_FILE. Copy .env.example to .env and configure it first."

log "Loading deployment env file: $ENV_FILE"
# shellcheck source=/dev/null
source "$ENV_FILE"

REPO_URL="${REPO_URL:-}"
BRANCH="${BRANCH:-main}"

OUTPUT_DIR="$(to_absolute_path "${OUTPUT_DIR:-./build-output}")"

PULL_BASE_IMAGES="${PULL_BASE_IMAGES:-true}"
DOCKER_NO_CACHE="${DOCKER_NO_CACHE:-false}"
ADD_COMMIT_TAG="${ADD_COMMIT_TAG:-true}"
PRUNE_DANGLING_IMAGES="${PRUNE_DANGLING_IMAGES:-false}"

SERVER_ENABLED="${SERVER_ENABLED:-true}"
SERVER_PROJECT_DIR="${SERVER_PROJECT_DIR:-server}"
SERVER_DOCKERFILE_PATH="${SERVER_DOCKERFILE_PATH:-deploy/Dockerfile}"
SERVER_DOCKER_BUILD_ARGS="${SERVER_DOCKER_BUILD_ARGS:-}"
SERVER_GRADLE_TASKS="${SERVER_GRADLE_TASKS:-clean build}"
SERVER_IMAGE_NAME="${SERVER_IMAGE_NAME:-centaurus-server}"
SERVER_IMAGE_TAG="${SERVER_IMAGE_TAG:-latest}"

WEBUI_ENABLED="${WEBUI_ENABLED:-true}"
WEBUI_PROJECT_DIR="${WEBUI_PROJECT_DIR:-webui}"
WEBUI_DOCKERFILE_PATH="${WEBUI_DOCKERFILE_PATH:-deploy/Dockerfile}"
WEBUI_DOCKER_BUILD_ARGS="${WEBUI_DOCKER_BUILD_ARGS:-}"
WEBUI_IMAGE_NAME="${WEBUI_IMAGE_NAME:-centaurus-webui}"
WEBUI_IMAGE_TAG="${WEBUI_IMAGE_TAG:-latest}"

COMPOSE_ENABLED="${COMPOSE_ENABLED:-true}"
COMPOSE_PROJECT_DIR="$(to_absolute_path "${COMPOSE_PROJECT_DIR:-./compose}")"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-.env}"
COMPOSE_DEPENDENCY_SERVICES="${COMPOSE_DEPENDENCY_SERVICES:-postgres}"
COMPOSE_RECREATE_SERVICES="${COMPOSE_RECREATE_SERVICES:-server webui}"
COMPOSE_WAIT_TIMEOUT_SECONDS="${COMPOSE_WAIT_TIMEOUT_SECONDS:-120}"

require_command git
require_command docker
require_command find
require_command date
require_command mktemp
require_command sleep

docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin is not available. Expected: docker compose"

[[ -n "$REPO_URL" ]] || fail "REPO_URL is not configured in $ENV_FILE."

if is_true "$COMPOSE_ENABLED"; then
  [[ -d "$COMPOSE_PROJECT_DIR" ]] || fail "COMPOSE_PROJECT_DIR does not exist: $COMPOSE_PROJECT_DIR"

  COMPOSE_ENV_FILE_PATH="$COMPOSE_ENV_FILE"

  if [[ "$COMPOSE_ENV_FILE_PATH" != /* ]]; then
    COMPOSE_ENV_FILE_PATH="$COMPOSE_PROJECT_DIR/$COMPOSE_ENV_FILE_PATH"
  fi

  validate_runtime_env \
    "$COMPOSE_ENV_FILE_PATH" \
    "$SERVER_IMAGE_NAME:$SERVER_IMAGE_TAG" \
    "$WEBUI_IMAGE_NAME:$WEBUI_IMAGE_TAG"
fi

WORK_DIR="$(mktemp -d)"
REPO_DIR="$WORK_DIR/repo"

cleanup() {
  rm -rf "$WORK_DIR"
}

trap cleanup EXIT

clear_directory_contents "$OUTPUT_DIR"

log "Checking out branch '$BRANCH' from '$REPO_URL'..."

git clone \
  --depth 1 \
  --single-branch \
  --branch "$BRANCH" \
  "$REPO_URL" \
  "$REPO_DIR"

cd "$REPO_DIR"

COMMIT_HASH="$(git rev-parse HEAD)"
COMMIT_SHORT="$(git rev-parse --short HEAD)"
REMOTE_URL="$(git remote get-url origin)"
BUILD_TIMESTAMP="$(date -Iseconds)"

log "Checked out commit: $COMMIT_HASH"

if is_true "$SERVER_ENABLED"; then
  SERVER_BUILD_DIR="$REPO_DIR/$SERVER_PROJECT_DIR"
  SERVER_BUILD_INFO="$OUTPUT_DIR/server-build-info.txt"
  SERVER_DOCKERFILE_SOURCE="$(resolve_path_relative_to "$SERVER_BUILD_DIR" "$SERVER_DOCKERFILE_PATH")"

  [[ -d "$SERVER_BUILD_DIR" ]] || fail "Server project directory does not exist: $SERVER_PROJECT_DIR"
  [[ -f "$SERVER_DOCKERFILE_SOURCE" ]] || fail "Server Dockerfile not found: $SERVER_DOCKERFILE_SOURCE"

  build_docker_image \
    "$SERVER_BUILD_DIR" \
    "$SERVER_DOCKERFILE_SOURCE" \
    "$SERVER_IMAGE_NAME" \
    "$SERVER_IMAGE_TAG" \
    "$COMMIT_SHORT" \
    "$SERVER_DOCKER_BUILD_ARGS" \
    "$SERVER_GRADLE_TASKS"

  cat > "$SERVER_BUILD_INFO" <<EOF
repo=$REMOTE_URL
branch=$BRANCH
commit=$COMMIT_HASH
commit_short=$COMMIT_SHORT
server_project_dir=$SERVER_PROJECT_DIR
server_dockerfile_source=$SERVER_DOCKERFILE_SOURCE
server_gradle_tasks=$SERVER_GRADLE_TASKS
server_image=$SERVER_IMAGE_NAME:$SERVER_IMAGE_TAG
server_commit_image=$SERVER_IMAGE_NAME:$COMMIT_SHORT
built_at=$BUILD_TIMESTAMP
EOF

  log "Server image build completed."
fi

if is_true "$WEBUI_ENABLED"; then
  WEBUI_BUILD_DIR="$REPO_DIR/$WEBUI_PROJECT_DIR"
  WEBUI_BUILD_INFO="$OUTPUT_DIR/webui-build-info.txt"
  WEBUI_DOCKERFILE_SOURCE="$(resolve_path_relative_to "$WEBUI_BUILD_DIR" "$WEBUI_DOCKERFILE_PATH")"

  [[ -d "$WEBUI_BUILD_DIR" ]] || fail "Web UI project directory does not exist: $WEBUI_PROJECT_DIR"
  [[ -f "$WEBUI_BUILD_DIR/package.json" ]] || fail "package.json not found in: $WEBUI_BUILD_DIR"
  [[ -f "$WEBUI_DOCKERFILE_SOURCE" ]] || fail "Web UI Dockerfile not found: $WEBUI_DOCKERFILE_SOURCE"

  build_docker_image \
    "$WEBUI_BUILD_DIR" \
    "$WEBUI_DOCKERFILE_SOURCE" \
    "$WEBUI_IMAGE_NAME" \
    "$WEBUI_IMAGE_TAG" \
    "$COMMIT_SHORT" \
    "$WEBUI_DOCKER_BUILD_ARGS"

  cat > "$WEBUI_BUILD_INFO" <<EOF
repo=$REMOTE_URL
branch=$BRANCH
commit=$COMMIT_HASH
commit_short=$COMMIT_SHORT
webui_project_dir=$WEBUI_PROJECT_DIR
webui_dockerfile_source=$WEBUI_DOCKERFILE_SOURCE
webui_image=$WEBUI_IMAGE_NAME:$WEBUI_IMAGE_TAG
webui_commit_image=$WEBUI_IMAGE_NAME:$COMMIT_SHORT
webui_docker_build_args=$WEBUI_DOCKER_BUILD_ARGS
built_at=$BUILD_TIMESTAMP
EOF

  log "Web UI image build completed."
fi

if is_true "$COMPOSE_ENABLED"; then
  COMPOSE_FILE_PATH="$COMPOSE_FILE"

  if [[ "$COMPOSE_FILE_PATH" != /* ]]; then
    COMPOSE_FILE_PATH="$COMPOSE_PROJECT_DIR/$COMPOSE_FILE_PATH"
  fi

  [[ -f "$COMPOSE_FILE_PATH" ]] || fail "Compose file not found: $COMPOSE_FILE_PATH"

  COMPOSE_CMD=(
    docker compose
    --project-directory "$COMPOSE_PROJECT_DIR"
    -f "$COMPOSE_FILE_PATH"
    --env-file "$COMPOSE_ENV_FILE_PATH"
  )

  log "Docker compose project directory: $COMPOSE_PROJECT_DIR"
  log "Docker compose file: $COMPOSE_FILE_PATH"
  log "Docker compose env file: $COMPOSE_ENV_FILE_PATH"

  if [[ -n "$COMPOSE_DEPENDENCY_SERVICES" ]]; then
    read -r -a DEPENDENCY_SERVICES <<< "$COMPOSE_DEPENDENCY_SERVICES"

    log "Ensuring compose dependency services are running: $COMPOSE_DEPENDENCY_SERVICES"

    "${COMPOSE_CMD[@]}" up \
      -d \
      "${DEPENDENCY_SERVICES[@]}"

    for service_name in "${DEPENDENCY_SERVICES[@]}"; do
      wait_for_compose_service_health "$service_name" "$COMPOSE_WAIT_TIMEOUT_SECONDS"
    done
  fi

  if [[ -n "$COMPOSE_RECREATE_SERVICES" ]]; then
    read -r -a RECREATE_SERVICES <<< "$COMPOSE_RECREATE_SERVICES"

    log "Recreating compose services: $COMPOSE_RECREATE_SERVICES"

    "${COMPOSE_CMD[@]}" up \
      -d \
      --force-recreate \
      --no-deps \
      "${RECREATE_SERVICES[@]}"
  else
    log "Recreating full compose stack..."

    "${COMPOSE_CMD[@]}" up \
      -d \
      --force-recreate \
      --remove-orphans
  fi

  log "Docker compose deployment completed."
fi

if is_true "$PRUNE_DANGLING_IMAGES"; then
  log "Pruning dangling Docker images..."
  docker image prune -f
fi

BUILD_INFO="$OUTPUT_DIR/build-info.txt"

cat > "$BUILD_INFO" <<EOF
repo=$REMOTE_URL
branch=$BRANCH
commit=$COMMIT_HASH
commit_short=$COMMIT_SHORT
server_enabled=$SERVER_ENABLED
server_image=$SERVER_IMAGE_NAME:$SERVER_IMAGE_TAG
server_commit_image=$SERVER_IMAGE_NAME:$COMMIT_SHORT
webui_enabled=$WEBUI_ENABLED
webui_image=$WEBUI_IMAGE_NAME:$WEBUI_IMAGE_TAG
webui_commit_image=$WEBUI_IMAGE_NAME:$COMMIT_SHORT
compose_enabled=$COMPOSE_ENABLED
compose_project_dir=$COMPOSE_PROJECT_DIR
compose_file=$COMPOSE_FILE
compose_env_file=$COMPOSE_ENV_FILE
compose_dependency_services=$COMPOSE_DEPENDENCY_SERVICES
compose_recreate_services=$COMPOSE_RECREATE_SERVICES
built_at=$BUILD_TIMESTAMP
EOF

log "Pipeline completed successfully."
log "Build info: $BUILD_INFO"

if is_true "$SERVER_ENABLED"; then
  log "Server image: $SERVER_IMAGE_NAME:$SERVER_IMAGE_TAG"

  if is_true "$ADD_COMMIT_TAG"; then
    log "Server commit image: $SERVER_IMAGE_NAME:$COMMIT_SHORT"
  fi
fi

if is_true "$WEBUI_ENABLED"; then
  log "Web UI image: $WEBUI_IMAGE_NAME:$WEBUI_IMAGE_TAG"

  if is_true "$ADD_COMMIT_TAG"; then
    log "Web UI commit image: $WEBUI_IMAGE_NAME:$COMMIT_SHORT"
  fi
fi
