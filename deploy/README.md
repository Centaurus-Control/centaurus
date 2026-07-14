# Centaurus Deployment

This directory is the deployment bundle for building and running Centaurus on a Docker host.

The recommended path is to download `centaurus-deploy-<version>.tar.gz` from the GitHub release section, extract it on the target host, configure the environment files, and run `deploy.sh`. A full repository checkout is not required on the target host.

## Requirements

- Git
- Docker
- Docker Compose plugin (`docker compose`)
- Network access to the configured Git repository and base image registries

## Setup

```bash
cp .env.example .env
cp compose/.env.example compose/.env
```

Edit both files before running the deployment:

- `.env` controls repository checkout, Docker build options, image names, and Compose paths.
- `compose/.env` controls runtime ports, database credentials, application secrets, and public enrollment URLs.

The default repository URL uses public HTTPS access and does not require a GitHub login while the repository is public. SSH URLs can still be used when the target host has an appropriate deploy key configured.

The deployment script refuses to continue while required runtime values are missing or still contain `change-me` placeholders.

By default, the runtime Compose file binds the Centaurus Server, Web UI, and PostgreSQL ports to `127.0.0.1`.
This allows an existing host-level reverse proxy such as Nginx, Apache, or Caddy to terminate TLS and forward to the local ports without exposing the application containers directly on the LAN.
For direct LAN testing, set the corresponding `*_BIND_ADDRESS` values in `compose/.env` to `0.0.0.0`.

## Run

```bash
./deploy.sh
```

The script clones the configured branch, builds the server and web UI Docker images, starts Compose dependencies, waits for dependency healthchecks, and recreates the application containers.

## Release Bundle

Maintainers can create this bundle from a tagged commit in the repository root with:

```bash
./package-deploy.sh
```

The script only packages a clean working tree when `HEAD` is exactly tagged. The archive is written to `dist/` and contains this `deploy/` directory without local `.env` files or build output.
