# Centaurus

Centaurus is a self-hosted control and monitoring system for machines and agents.

The project consists of:

- `server`: Spring Boot backend with REST APIs, WebSocket agent communication, authentication, PostgreSQL persistence, and Flyway migrations.
- `webui`: React/Vite frontend served through Nginx.
- `agent`: machine-side agent components.
- `deploy`: Docker-based deployment bundle for building and running the server and web UI.

## Deployment

The deployment pipeline is designed so an operator only needs the packaged `deploy/` directory on a Docker host. The application source is checked out by `deploy.sh` during deployment.

### Requirements

- Git
- Docker
- Docker Compose plugin (`docker compose`)
- Network access to this repository and container registries

### Recommended: Use The GitHub Release Bundle

Download the deployment bundle from the GitHub release section:

```text
centaurus-deploy-<version>.tar.gz
```

Copy it to the target host and extract it:

```bash
tar -xzf centaurus-deploy-<version>.tar.gz
cd deploy
```

This is the intended deployment path. The target host does not need a full working copy of the repository.

### Maintainers: Create The Deployment Bundle

From a tagged commit in the repository root:

```bash
./package-deploy.sh
```

This creates:

```text
dist/centaurus-deploy-<tag>.tar.gz
```

The packaging script fails if the working tree is not clean or if `HEAD` is not exactly tagged.

### Configure The Target Host

Create local environment files:

```bash
cp .env.example .env
cp compose/.env.example compose/.env
```

Edit both files before deployment:

- `.env` controls repository checkout, branch/tag, Docker build options, image names, and Compose paths.
- `compose/.env` controls runtime ports, database credentials, application secrets, and public enrollment URLs.

The deployment script refuses to continue while required runtime values are missing or still contain `change-me` placeholders.

### Run Deployment

```bash
./deploy.sh
```

The script clones the configured repository, builds the server and web UI Docker images, starts Compose dependencies, waits for healthchecks, and recreates the application containers.

### SSL Termination

SSL termination and external reverse proxy configuration are intentionally outside the deployment pipeline. A reverse proxy can forward:

- `/` to the WebUI port
- `/api/` to the server port
- `/agent/ws` to the server port with WebSocket upgrade headers
- `/actuator/` to the server port, if health endpoints should be exposed through the proxy

For HTTPS deployments, configure the runtime environment with public HTTPS/WSS URLs, for example:

```env
CENTAURUS_ENROLLMENT_SERVER_URL=https://centaurus.example.com
CENTAURUS_ENROLLMENT_WS_URL=wss://centaurus.example.com/agent/ws
CENTAURUS_AUTH_REFRESH_COOKIE_SECURE_MODE=AUTO
```

## License

Centaurus is licensed under the GNU General Public License version 2. See [LICENSE](LICENSE).

GPLv2 allows use, commercial use, modification, and redistribution. When distributing modified versions or derivative works, GPLv2's copyleft terms apply, including preserving the license and providing corresponding source code under GPLv2.
