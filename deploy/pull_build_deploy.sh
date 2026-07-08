#!/usr/bin/env bash
set -Eeuo pipefail

# ==========================================================
# Script location
# ==========================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ENV_FILE="$SCRIPT_DIR/.build.env"

# ==========================================================
# Helper functions
# ==========================================================

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

find_server_jar() {
  local artifact_dir="$1"
  local jars=()

  [[ -d "$artifact_dir" ]] || fail "Gradle artifact directory not found: $artifact_dir"

  mapfile -t jars < <(
    find "$artifact_dir" \
      -maxdepth 1 \
      -type f \
      -name "*.jar" \
      ! -name "*-plain.jar" \
      ! -name "*-sources.jar" \
      ! -name "*-javadoc.jar" \
      | sort
  )

  if [[ "${#jars[@]}" -eq 0 ]]; then
    mapfile -t jars < <(
      find "$artifact_dir" \
        -maxdepth 1 \
        -type f \
        -name "*.jar" \
        ! -name "*-sources.jar" \
        ! -name "*-javadoc.jar" \
        | sort
    )
  fi

  [[ "${#jars[@]}" -gt 0 ]] || fail "No JAR file found in: $artifact_dir"

  if [[ "${#jars[@]}" -gt 1 ]]; then
    echo "Found multiple possible JAR files:"
    printf ' - %s\n' "${jars[@]}"
    fail "More than one possible JAR file found. Please make the build output unambiguous."
  fi

  echo "${jars[0]}"
}

build_docker_image() {
  local context_dir="$1"
  local dockerfile_path="$2"
  local image_name="$3"
  local image_tag="$4"
  local commit_short="$5"
  local extra_build_args_text="${6:-}"

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

# ==========================================================
# Load environment file
# ==========================================================

if [[ -f "$ENV_FILE" ]]; then
  log "Loading environment file: $ENV_FILE"
  # shellcheck source=/dev/null
  source "$ENV_FILE"
else
  log "No environment file found at: $ENV_FILE"
fi

# ==========================================================
# Configuration
# ==========================================================

REPO_URL="${REPO_URL:-}"
BRANCH="${BRANCH:-main}"

OUTPUT_DIR="$(to_absolute_path "${OUTPUT_DIR:-./build-output}")"

PULL_BASE_IMAGES="${PULL_BASE_IMAGES:-true}"
DOCKER_NO_CACHE="${DOCKER_NO_CACHE:-false}"
ADD_COMMIT_TAG="${ADD_COMMIT_TAG:-true}"
PRUNE_DANGLING_IMAGES="${PRUNE_DANGLING_IMAGES:-false}"

SERVER_ENABLED="${SERVER_ENABLED:-true}"
SERVER_PROJECT_DIR="${SERVER_PROJECT_DIR:-server}"
SERVER_JAVA_HOME="${SERVER_JAVA_HOME:-}"
SERVER_GRADLE_TASKS="${SERVER_GRADLE_TASKS:-clean build}"
SERVER_DOCKERFILE_PATH="${SERVER_DOCKERFILE_PATH:-Dockerfile}"
SERVER_DOCKER_JAR_NAME="${SERVER_DOCKER_JAR_NAME:-app.jar}"
SERVER_DOCKER_BUILD_ARGS="${SERVER_DOCKER_BUILD_ARGS:-}"
SERVER_IMAGE_NAME="${SERVER_IMAGE_NAME:-centaurus-server}"
SERVER_IMAGE_TAG="${SERVER_IMAGE_TAG:-latest}"

WEBUI_ENABLED="${WEBUI_ENABLED:-true}"
WEBUI_PROJECT_DIR="${WEBUI_PROJECT_DIR:-ui}"
WEBUI_DOCKERFILE_PATH="${WEBUI_DOCKERFILE_PATH:-Dockerfile}"
WEBUI_DOCKER_BUILD_ARGS="${WEBUI_DOCKER_BUILD_ARGS:-}"
WEBUI_IMAGE_NAME="${WEBUI_IMAGE_NAME:-centaurus-ui}"
WEBUI_IMAGE_TAG="${WEBUI_IMAGE_TAG:-latest}"

COMPOSE_ENABLED="${COMPOSE_ENABLED:-false}"
COMPOSE_PROJECT_DIR="${COMPOSE_PROJECT_DIR:-}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-.env}"
COMPOSE_DEPENDENCY_SERVICES="${COMPOSE_DEPENDENCY_SERVICES:-}"
COMPOSE_RECREATE_SERVICES="${COMPOSE_RECREATE_SERVICES:-}"
COMPOSE_WAIT_TIMEOUT_SECONDS="${COMPOSE_WAIT_TIMEOUT_SECONDS:-120}"

# ==========================================================
# Pre-flight checks
# ==========================================================

require_command git
require_command docker
require_command find
require_command date
require_command mktemp
require_command sed
require_command sleep

[[ -n "$REPO_URL" ]] || fail "REPO_URL is not configured. Please set it in $ENV_FILE."

if is_true "$SERVER_ENABLED"; then
  if [[ -n "$SERVER_JAVA_HOME" ]]; then
    [[ -d "$SERVER_JAVA_HOME" ]] || fail "SERVER_JAVA_HOME does not exist: $SERVER_JAVA_HOME"
    [[ -x "$SERVER_JAVA_HOME/bin/java" ]] || fail "Java executable not found: $SERVER_JAVA_HOME/bin/java"

    export JAVA_HOME="$SERVER_JAVA_HOME"
    export PATH="$SERVER_JAVA_HOME/bin:$PATH"

    log "Using SERVER_JAVA_HOME: $SERVER_JAVA_HOME"
  else
    log "SERVER_JAVA_HOME is not set. Using Java from PATH."
  fi

  require_command java

  log "Java version:"
  java -version
fi

if is_true "$COMPOSE_ENABLED"; then
  [[ -n "$COMPOSE_PROJECT_DIR" ]] || fail "COMPOSE_PROJECT_DIR is not configured."
  [[ -d "$COMPOSE_PROJECT_DIR" ]] || fail "COMPOSE_PROJECT_DIR does not exist: $COMPOSE_PROJECT_DIR"
fi

# ==========================================================
# Temporary workspace
# ==========================================================

WORK_DIR="$(mktemp -d)"
REPO_DIR="$WORK_DIR/repo"

cleanup() {
  rm -rf "$WORK_DIR"
}

trap cleanup EXIT

# ==========================================================
# Prepare output directory
# ==========================================================

clear_directory_contents "$OUTPUT_DIR"

# ==========================================================
# Git checkout
# ==========================================================

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

# ==========================================================
# Build server image
# ==========================================================

if is_true "$SERVER_ENABLED"; then
  SERVER_BUILD_DIR="$REPO_DIR/$SERVER_PROJECT_DIR"
  SERVER_CONTEXT_DIR="$OUTPUT_DIR/server-context"
  SERVER_BUILD_INFO="$OUTPUT_DIR/server-build-info.txt"
  SERVER_DOCKERFILE_SOURCE="$(resolve_path_relative_to "$SERVER_BUILD_DIR" "$SERVER_DOCKERFILE_PATH")"

  [[ -d "$SERVER_BUILD_DIR" ]] || fail "Server project directory does not exist: $SERVER_PROJECT_DIR"
  [[ -f "$SERVER_DOCKERFILE_SOURCE" ]] || fail "Server Dockerfile not found: $SERVER_DOCKERFILE_SOURCE"

  if [[ -f "$SERVER_BUILD_DIR/gradlew" ]]; then
    chmod +x "$SERVER_BUILD_DIR/gradlew"

    SERVER_WRAPPER_JAR="$SERVER_BUILD_DIR/gradle/wrapper/gradle-wrapper.jar"
    [[ -f "$SERVER_WRAPPER_JAR" ]] || fail "Gradle wrapper JAR is missing: $SERVER_WRAPPER_JAR"

    SERVER_GRADLE_CMD=("$SERVER_BUILD_DIR/gradlew" "--no-daemon")
  elif [[ -f "$REPO_DIR/gradlew" ]]; then
    chmod +x "$REPO_DIR/gradlew"

    SERVER_WRAPPER_JAR="$REPO_DIR/gradle/wrapper/gradle-wrapper.jar"
    [[ -f "$SERVER_WRAPPER_JAR" ]] || fail "Gradle wrapper JAR is missing: $SERVER_WRAPPER_JAR"

    SERVER_GRADLE_CMD=("$REPO_DIR/gradlew" "--no-daemon")
  else
    require_command gradle
    SERVER_GRADLE_CMD=("gradle" "--no-daemon")
  fi

  cd "$SERVER_BUILD_DIR"

  log "Starting server Gradle build in: $SERVER_BUILD_DIR"
  log "Server Gradle tasks: $SERVER_GRADLE_TASKS"

  read -r -a SERVER_GRADLE_ARGS <<< "$SERVER_GRADLE_TASKS"

  "${SERVER_GRADLE_CMD[@]}" "${SERVER_GRADLE_ARGS[@]}"

  SERVER_SOURCE_JAR="$(find_server_jar "$SERVER_BUILD_DIR/build/libs")"

  clear_directory_contents "$SERVER_CONTEXT_DIR"

  log "Preparing server Docker context..."
  cp "$SERVER_SOURCE_JAR" "$SERVER_CONTEXT_DIR/$SERVER_DOCKER_JAR_NAME"
  cp "$SERVER_DOCKERFILE_SOURCE" "$SERVER_CONTEXT_DIR/Dockerfile"

  log "Server source JAR: $SERVER_SOURCE_JAR"
  log "Server Dockerfile source: $SERVER_DOCKERFILE_SOURCE"
  log "Server Docker context: $SERVER_CONTEXT_DIR"
  log "Server Dockerfile preview:"
  sed -n '1,120p' "$SERVER_CONTEXT_DIR/Dockerfile"

  build_docker_image \
    "$SERVER_CONTEXT_DIR" \
    "$SERVER_CONTEXT_DIR/Dockerfile" \
    "$SERVER_IMAGE_NAME" \
    "$SERVER_IMAGE_TAG" \
    "$COMMIT_SHORT" \
    "$SERVER_DOCKER_BUILD_ARGS"

  cat > "$SERVER_BUILD_INFO" <<EOF
repo=$REMOTE_URL
branch=$BRANCH
commit=$COMMIT_HASH
commit_short=$COMMIT_SHORT
server_project_dir=$SERVER_PROJECT_DIR
server_gradle_tasks=$SERVER_GRADLE_TASKS
server_source_jar=$SERVER_SOURCE_JAR
server_dockerfile_source=$SERVER_DOCKERFILE_SOURCE
server_docker_context=$SERVER_CONTEXT_DIR
server_docker_jar=$SERVER_CONTEXT_DIR/$SERVER_DOCKER_JAR_NAME
server_image=$SERVER_IMAGE_NAME:$SERVER_IMAGE_TAG
server_commit_image=$SERVER_IMAGE_NAME:$COMMIT_SHORT
built_at=$BUILD_TIMESTAMP
EOF

  log "Server image build completed."
fi

# ==========================================================
# Build web UI image
# ==========================================================

if is_true "$WEBUI_ENABLED"; then
  WEBUI_BUILD_DIR="$REPO_DIR/$WEBUI_PROJECT_DIR"
  WEBUI_BUILD_INFO="$OUTPUT_DIR/webui-build-info.txt"
  WEBUI_DOCKERFILE_SOURCE="$(resolve_path_relative_to "$WEBUI_BUILD_DIR" "$WEBUI_DOCKERFILE_PATH")"

  [[ -d "$WEBUI_BUILD_DIR" ]] || fail "Web UI project directory does not exist: $WEBUI_PROJECT_DIR"
  [[ -f "$WEBUI_BUILD_DIR/package.json" ]] || fail "package.json not found in: $WEBUI_BUILD_DIR"
  [[ -f "$WEBUI_DOCKERFILE_SOURCE" ]] || fail "Web UI Dockerfile not found: $WEBUI_DOCKERFILE_SOURCE"

  log "Web UI Docker build directory: $WEBUI_BUILD_DIR"
  log "Web UI Dockerfile source: $WEBUI_DOCKERFILE_SOURCE"
  log "Web UI Dockerfile preview:"
  sed -n '1,120p' "$WEBUI_DOCKERFILE_SOURCE"

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

# ==========================================================
# Docker compose deployment
# ==========================================================

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
  )

  if [[ -n "$COMPOSE_ENV_FILE" ]]; then
    COMPOSE_ENV_FILE_PATH="$COMPOSE_ENV_FILE"

    if [[ "$COMPOSE_ENV_FILE_PATH" != /* ]]; then
      COMPOSE_ENV_FILE_PATH="$COMPOSE_PROJECT_DIR/$COMPOSE_ENV_FILE_PATH"
    fi

    [[ -f "$COMPOSE_ENV_FILE_PATH" ]] || fail "Compose env file not found: $COMPOSE_ENV_FILE_PATH"

    COMPOSE_CMD+=(
      --env-file "$COMPOSE_ENV_FILE_PATH"
    )
  fi

  log "Docker compose project directory: $COMPOSE_PROJECT_DIR"
  log "Docker compose file: $COMPOSE_FILE_PATH"

  if [[ -n "${COMPOSE_ENV_FILE_PATH:-}" ]]; then
    log "Docker compose env file: $COMPOSE_ENV_FILE_PATH"
  fi

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

# ==========================================================
# Cleanup dangling images
# ==========================================================

if is_true "$PRUNE_DANGLING_IMAGES"; then
  log "Pruning dangling Docker images..."
  docker image prune -f
fi

# ==========================================================
# Overall build info
# ==========================================================

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