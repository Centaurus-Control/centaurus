# Centaurus Deployment

This directory is the deployment bundle for building and running Centaurus on a Docker host.

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

The deployment script refuses to continue while required runtime values are missing or still contain `change-me` placeholders.

## Run

```bash
./deploy.sh
```

The script clones the configured branch, builds the server and web UI Docker images, starts Compose dependencies, waits for dependency healthchecks, and recreates the application containers.
