#!/usr/bin/env bash
set -Eeuo pipefail

DOCKER_APT_KEYRING="/etc/apt/keyrings/docker.asc"
DOCKER_APT_SOURCE="/etc/apt/sources.list.d/docker.list"
ADD_DEPLOY_USER_TO_DOCKER_GROUP="${ADD_DEPLOY_USER_TO_DOCKER_GROUP:-false}"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_root() {
  [[ "${EUID}" -eq 0 ]] || fail "This setup script must be run as root. Use: sudo ./basic_setup.sh"
}

require_debian() {
  [[ -r /etc/os-release ]] || fail "Cannot detect OS: /etc/os-release is missing"

  # shellcheck source=/dev/null
  source /etc/os-release

  [[ "${ID:-}" == "debian" ]] || fail "This setup script is intended for Debian. Detected ID=${ID:-unknown}"
  [[ -n "${VERSION_CODENAME:-}" ]] || fail "Cannot detect Debian codename from /etc/os-release"
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

apt_package_installed() {
  dpkg-query -W -f='${Status}' "$1" 2>/dev/null | grep -q "install ok installed"
}

install_apt_prerequisites() {
  local packages=(
    ca-certificates
    curl
    gnupg
  )

  log "Installing apt prerequisites..."
  apt-get update
  apt-get install -y "${packages[@]}"
}

configure_docker_apt_repository() {
  local arch

  arch="$(dpkg --print-architecture)"

  log "Configuring Docker apt repository for Debian ${VERSION_CODENAME}/${arch}..."

  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/debian/gpg -o "${DOCKER_APT_KEYRING}"
  chmod a+r "${DOCKER_APT_KEYRING}"

  cat > "${DOCKER_APT_SOURCE}" <<EOF
deb [arch=${arch} signed-by=${DOCKER_APT_KEYRING}] https://download.docker.com/linux/debian ${VERSION_CODENAME} stable
EOF

  apt-get update
}

remove_conflicting_docker_packages() {
  local conflicting_packages=(
    docker.io
    docker-doc
    docker-compose
    podman-docker
    containerd
    runc
  )
  local installed_conflicts=()
  local package_name

  for package_name in "${conflicting_packages[@]}"; do
    if apt_package_installed "${package_name}"; then
      installed_conflicts+=("${package_name}")
    fi
  done

  if (( ${#installed_conflicts[@]} == 0 )); then
    return
  fi

  log "Removing conflicting Docker packages: ${installed_conflicts[*]}"
  apt-get remove -y "${installed_conflicts[@]}"
}

docker_engine_ready() {
  command_exists docker && docker version >/dev/null 2>&1
}

docker_compose_ready() {
  command_exists docker && docker compose version >/dev/null 2>&1
}

install_docker_packages() {
  local packages=(
    docker-ce
    docker-ce-cli
    containerd.io
    docker-buildx-plugin
    docker-compose-plugin
  )

  if docker_engine_ready && docker_compose_ready; then
    log "Docker Engine and Docker Compose plugin are already available."
    return
  fi

  remove_conflicting_docker_packages
  install_apt_prerequisites
  configure_docker_apt_repository

  log "Installing Docker Engine, Buildx, and Compose plugin..."
  apt-get install -y "${packages[@]}"
}

install_deploy_packages() {
  local packages=(
    git
    findutils
  )

  log "Installing Centaurus deploy prerequisites..."
  apt-get install -y "${packages[@]}"
}

enable_docker_service() {
  if ! command_exists systemctl; then
    log "systemctl is not available; skipping Docker service enable/start."
    return
  fi

  log "Enabling and starting Docker service..."
  systemctl enable docker
  systemctl start docker
}

configure_deploy_user() {
  local deploy_user="${SUDO_USER:-}"

  if [[ "${ADD_DEPLOY_USER_TO_DOCKER_GROUP}" != "true" ]]; then
    return
  fi

  if [[ -z "${deploy_user}" || "${deploy_user}" == "root" ]]; then
    log "No non-root sudo user detected; skipping docker group membership."
    return
  fi

  log "Adding ${deploy_user} to docker group..."
  usermod -aG docker "${deploy_user}"
  log "User ${deploy_user} must log out and back in before running docker without sudo."
}

verify_setup() {
  command_exists git || fail "git is not available after setup"
  command_exists docker || fail "docker is not available after setup"

  docker version >/dev/null 2>&1 || fail "Docker Engine is installed but not usable"
  docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin is installed but not usable"

  log "Docker version: $(docker --version)"
  log "Docker Compose version: $(docker compose version --short)"
  log "Git version: $(git --version)"
}

main() {
  require_root
  require_debian

  install_apt_prerequisites
  install_docker_packages
  install_deploy_packages
  enable_docker_service
  configure_deploy_user
  verify_setup

  log "Basic Debian setup completed. You can now configure deploy/.env and deploy/compose/.env, then run deploy.sh."
}

main "$@"
