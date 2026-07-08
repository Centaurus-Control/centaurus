# Centaurus Security Notes

This document captures project-level security decisions that affect server, Web UI, and Agent implementation.

## Deployment Assumption

Centaurus is primarily designed for private LAN deployments.

Typical environments may use:

- self-signed server certificates
- a private local root CA
- local hostnames such as `centaurus.local`
- mobile browsers that do not have the private root CA installed

## Browser TLS Limitation

Browser-based Web UIs cannot disable TLS certificate validation from JavaScript.

The browser validates the certificate before application code can make trusted API calls. APIs such as `fetch()` and browser WebSocket do not provide an application-level option to ignore certificate errors or to upload a custom root CA for browser TLS trust.

Therefore:

- The Web UI must not offer a fake "disable certificate validation" option.
- The Web UI must not claim that uploading a root CA inside the app makes browser TLS trust it.
- Browser TLS trust is controlled by the browser and operating system.

Users can still accept browser certificate warnings where the browser allows it, or they can install the private root CA into the operating system or browser trust store.

## Preferred Web Architecture

The Web UI and REST API should be served from the same origin.

Recommended:

```text
https://centaurus.local:8443/        Web UI
https://centaurus.local:8443/api/... REST API
```

Avoid:

```text
https://web-ui.local:5173  ->  https://centaurus.local:8443/api
```

Same-origin deployment avoids:

- CORS complexity
- third-party cookie behavior
- cross-origin mobile browser differences
- multiple certificate exceptions for UI and API

## Server TLS Modes

Centaurus should support these server deployment modes:

### HTTPS With Provided Certificate

The user provides a certificate and private key or Java keystore.

This is the preferred mode when users already have a local CA or a real certificate.

### HTTPS With Self-Signed Certificate

The server uses a self-signed certificate or a certificate issued by a locally generated CA.

Browser users may need to accept the certificate warning or install the local CA.

### Explicit HTTP LAN Mode

The server runs as plain HTTP.

This mode is intended only for trusted private LANs. It should be clearly marked as less secure in logs and UI.

When HTTP LAN mode is used:

- refresh cookies must not use the `Secure` attribute
- browser Secure Context features may not be available
- users should understand that LAN attackers can observe or modify traffic

## Refresh Cookie Secure Mode

The refresh cookie `Secure` attribute is controlled by:

```text
CENTAURUS_AUTH_REFRESH_COOKIE_SECURE_MODE
```

Supported values:

```text
AUTO
ALWAYS
NEVER
```

Behavior:

- `AUTO`: Set `Secure` when the request is HTTPS or `X-Forwarded-Proto: https`.
- `ALWAYS`: Always set `Secure`.
- `NEVER`: Never set `Secure`.

Default:

```text
AUTO
```

Use `AUTO` for normal server deployments. Use `NEVER` only for explicit HTTP LAN mode.

## Agent TLS Trust

The Java Agent is not limited like a browser and should support explicit TLS trust configuration.

Agent identity authentication currently uses Ed25519 keys:

- The Agent generates its own Ed25519 keypair during enrollment.
- The server stores the Agent public key in `agent_identity_keys`.
- The server has an active Ed25519 identity key in `server_identity_keys`.
- Enrollment bundles include the server public key and key ID.
- WebSocket sessions use challenge-response authentication before runtime messages are accepted.

## Agent Command Security

Script execution is intentionally Agent-authoritative:

- The server stores only script IDs, labels, parameter schemas, and result schemas.
- The server does not receive local script paths, shell commands, working directories, or local environment configuration.
- The Agent validates script IDs, parameter names, parameter types, enum values, and local cooldown rules before execution.
- Runtime commands are accepted only over an authenticated Agent WebSocket session.

`VIEWER` users may read machine state and command history. `OPERATOR` and `ADMIN` users may create runtime commands such as script execution, stats refresh, and Wake-on-LAN.

Wake-on-LAN is currently implemented as an Agent-side command. In later multi-Agent deployments, server-side relay selection should prefer Agents that explicitly report `WOL_RELAY`.

## Agent Local UI Security

The Agent local UI is protected by a local Agent UI session cookie.

Login flow:

1. User enters Centaurus Server URL and server credentials in the Agent UI.
2. Agent calls the server `/api/auth/login`.
3. Agent verifies `/api/me` and requires role `ADMIN`.
4. Agent creates a local `HttpOnly` session cookie.

The Agent does not store the server password. The local session is in-memory and expires automatically.

The Agent status endpoint remains public for local diagnostics. Enrollment and script configuration endpoints require the local Agent UI session.

Planned Agent config:

```yaml
server:
  url: "https://centaurus.local:8443"

tls:
  validationMode: "SYSTEM"
  caCertificatePath: null
```

Agent local UI defaults:

```yaml
server:
  address: "127.0.0.1"
  port: 8787

centaurus:
  agent:
    localUi:
      remoteAccessEnabled: false
```

For headless LAN systems, remote access must be enabled deliberately:

```yaml
server:
  address: "0.0.0.0"
  port: 8787

centaurus:
  agent:
    localUi:
      remoteAccessEnabled: true
```

Future hardening for remote Agent UI:

- local Agent UI authentication
- optional HTTPS
- allowed network ranges
- clear warning when bound to non-loopback interfaces

Supported validation modes:

```text
SYSTEM
CUSTOM_CA
INSECURE
```

### SYSTEM

Use the JVM/system trust store.

### CUSTOM_CA

Trust certificates issued by a configured private root CA.

### INSECURE

Disable TLS certificate validation.

This is acceptable only for explicit local development or trusted LAN testing. The Agent must log a warning when this mode is active.

## Recommended User Experience

For a home-lab style deployment, Centaurus should guide the user through one of these choices:

1. Use HTTP LAN mode.
2. Use HTTPS and accept the browser warning.
3. Use HTTPS and install a generated/local root CA.
4. Use HTTPS with a user-provided certificate.

The Web UI should explain browser trust limitations plainly and should not imply that browser TLS can be bypassed from inside the application.
