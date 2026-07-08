# Centaurus API

This document describes the current server API contract for clients such as the Web UI.

The API is in early development. While the project is in Initial Development mode, breaking changes are allowed and the database may be recreated from scratch.

## Base URL

Local HTTP development default:

```text
http://localhost:8080
```

Recommended LAN deployment shape:

```text
https://centaurus.local:8443
```

The Web UI and API should be served from the same origin:

```text
https://centaurus.local:8443/        Web UI
https://centaurus.local:8443/api/... REST API
```

This avoids cross-origin cookies, CORS, and mobile browser edge cases.

## Authentication Model

Centaurus uses short-lived JWT access tokens and long-lived refresh tokens.

- Access tokens are returned in JSON responses.
- API calls use the access token as a Bearer token.
- Refresh tokens are opaque random tokens stored in an `HttpOnly` cookie.
- Refresh tokens are stored only as hashes on the server.
- Refresh rotates the refresh token.
- Logout revokes the current refresh session and clears the cookie.

### Authorization Header

```http
Authorization: Bearer <accessToken>
```

### Refresh Cookie

Default cookie name:

```text
centaurus_refresh_token
```

Cookie properties:

```text
HttpOnly
SameSite=Lax
Path=/api/auth
```

The `Secure` cookie attribute is controlled by `CENTAURUS_AUTH_REFRESH_COOKIE_SECURE_MODE`:

```text
AUTO    Set Secure for HTTPS requests, omit it for HTTP requests.
ALWAYS  Always set Secure.
NEVER   Never set Secure.
```

Default:

```text
AUTO
```

Use `AUTO` for normal deployments. Use `NEVER` only for explicit HTTP LAN mode.

## Auth Endpoints

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "username": "admin",
  "password": "temporary-or-user-password"
}
```

Response:

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresAt": "2026-06-27T16:05:36.780826318Z"
}
```

Side effect:

```text
Set-Cookie: centaurus_refresh_token=...
```

### Refresh Access Token

```http
POST /api/auth/refresh
Cookie: centaurus_refresh_token=...
```

Response:

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresAt": "2026-06-27T16:20:36.780826318Z"
}
```

Side effect:

```text
Set-Cookie: centaurus_refresh_token=...
```

The refresh token is rotated on every successful refresh.

### Logout

```http
POST /api/auth/logout
Cookie: centaurus_refresh_token=...
```

Response:

```http
204 No Content
```

Side effect:

```text
Set-Cookie: centaurus_refresh_token=; Max-Age=0
```

### Change Password

```http
POST /api/auth/change-password
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "currentPassword": "current-password",
  "newPassword": "new-password-with-at-least-12-characters"
}
```

Response:

```json
{
  "passwordChangeRequired": false,
  "changedAt": "2026-06-27T16:45:00.000000000Z",
  "revokedSessionCount": 1
}
```

Side effect:

```text
Set-Cookie: centaurus_refresh_token=; Max-Age=0
```

After a successful password change:

- `passwordChangeRequired` is cleared.
- All active refresh sessions for the user are revoked.
- The current refresh cookie is cleared.
- The UI should discard the in-memory access token and route to login.

Password policy:

- Minimum length: 12 characters.
- New password must differ from the current password.
- New password must not equal the username, case-insensitive.

## User Endpoints

## Roles

Supported user roles:

```text
ADMIN
OPERATOR
VIEWER
```

Role intent:

- `ADMIN`: User management, security-relevant configuration, machine onboarding.
- `OPERATOR`: Wake machines, run scripts, shut down machines.
- `VIEWER`: View machines and stats, but cannot send commands or wake machines.

### Current User

```http
GET /api/me
Authorization: Bearer <accessToken>
```

Response:

```json
{
  "id": "3623ae24-8f38-48f3-9d29-7d2d1725a492",
  "username": "admin",
  "role": "ADMIN",
  "passwordChangeRequired": true
}
```

If `passwordChangeRequired` is `true`, the UI should route the user to the password change flow before allowing normal application actions.

If `passwordChangeRequired` is `true`, normal protected API endpoints return:

```http
403 Forbidden
```

Problem detail:

```json
{
  "type": "about:blank",
  "title": "Password change required",
  "status": 403,
  "detail": "The current user must change the password before using this endpoint."
}
```

The following endpoints remain usable while a password change is required:

- `GET /api/me`
- `POST /api/auth/change-password`
- `POST /api/auth/logout`
- `POST /api/auth/refresh`

## Admin User Management

All endpoints in this section require:

```http
Authorization: Bearer <adminAccessToken>
```

Only users with role `ADMIN` may use these endpoints.

### List Users

```http
GET /api/admin/users
```

Response:

```json
[
  {
    "id": "3623ae24-8f38-48f3-9d29-7d2d1725a492",
    "username": "admin",
    "role": "ADMIN",
    "passwordChangeRequired": false,
    "createdAt": "2026-06-27T16:45:00.000000000Z",
    "updatedAt": "2026-06-27T16:45:00.000000000Z"
  }
]
```

Soft-deleted users are not returned.

### Create User

```http
POST /api/admin/users
Content-Type: application/json
```

Request:

```json
{
  "username": "operator",
  "role": "OPERATOR"
}
```

Response:

```json
{
  "user": {
    "id": "a83ed0e0-7f4d-4553-ae64-b6fbf25cc406",
    "username": "operator",
    "role": "OPERATOR",
    "passwordChangeRequired": true,
    "createdAt": "2026-06-27T16:45:00.000000000Z",
    "updatedAt": "2026-06-27T16:45:00.000000000Z"
  },
  "temporaryPassword": "generated-temporary-password"
}
```

The temporary password is returned only once. The created user must change it after first login.

### Update User Role

```http
PUT /api/admin/users/{userId}/role
Content-Type: application/json
```

Request:

```json
{
  "role": "VIEWER"
}
```

Response:

```json
{
  "id": "a83ed0e0-7f4d-4553-ae64-b6fbf25cc406",
  "username": "operator",
  "role": "VIEWER",
  "passwordChangeRequired": false,
  "createdAt": "2026-06-27T16:45:00.000000000Z",
  "updatedAt": "2026-06-27T16:50:00.000000000Z"
}
```

Changing a user's role revokes that user's active refresh sessions.

The last active admin cannot be demoted.

### Reset User Password

```http
POST /api/admin/users/{userId}/reset-password
```

Response:

```json
{
  "user": {
    "id": "a83ed0e0-7f4d-4553-ae64-b6fbf25cc406",
    "username": "operator",
    "role": "OPERATOR",
    "passwordChangeRequired": true,
    "createdAt": "2026-06-27T16:45:00.000000000Z",
    "updatedAt": "2026-06-27T16:55:00.000000000Z"
  },
  "temporaryPassword": "generated-temporary-password"
}
```

Resetting a user's password revokes that user's active refresh sessions. The temporary password is returned only once.

### Delete User

```http
DELETE /api/admin/users/{userId}
```

Response:

```http
204 No Content
```

Deleting a user is a soft delete. It revokes the user's active refresh sessions.

The last active admin cannot be deleted.

## Admin Enrollment Token Management

All endpoints in this section require role `ADMIN`.

### List Enrollment Tokens

```http
GET /api/admin/enrollment-tokens
Authorization: Bearer <adminAccessToken>
```

Response:

```json
[
  {
    "id": "e6b26a47-422c-4e28-9961-469f85a97d86",
    "suggestedName": "Debian Server",
    "expiresAt": "2026-06-27T17:45:00Z",
    "usedAt": null,
    "usedByAgentId": null,
    "createdAt": "2026-06-27T16:45:00Z"
  }
]
```

### Create Enrollment Token

```http
POST /api/admin/enrollment-tokens
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

Request:

```json
{
  "suggestedName": "Debian Server",
  "expiresIn": "PT1H"
}
```

Response:

```json
{
  "token": {
    "id": "e6b26a47-422c-4e28-9961-469f85a97d86",
    "suggestedName": "Debian Server",
    "expiresAt": "2026-06-27T17:45:00Z",
    "usedAt": null,
    "usedByAgentId": null,
    "createdAt": "2026-06-27T16:45:00Z"
  },
  "enrollmentBundle": "raenroll:..."
}
```

The enrollment bundle contains the plaintext enrollment token and is returned only once.

Decoded bundle shape:

```json
{
  "v": 1,
  "serverUrl": "http://localhost:8080",
  "wsUrl": "ws://localhost:8080/agent/ws",
  "enrollmentToken": "raet_...",
  "serverPublicKey": "base64-ed25519-public-key",
  "serverKeyId": "srvkey_...",
  "suggestedName": "Debian Server",
  "expiresAt": "2026-06-27T17:45:00Z"
}
```

## Agent Enrollment API

### Enroll Agent

```http
POST /api/agent/enroll
Content-Type: application/json
```

This endpoint is public. The enrollment token is the credential.

Request:

```json
{
  "enrollmentToken": "raet_...",
  "installationId": "6f3d91be-31f0-4e61-9a50-4ea0cf46f7ef",
  "agentPublicKey": "base64-ed25519-public-key",
  "agentKeyId": "agtkey_01",
  "agentVersion": "0.1.0-SNAPSHOT",
  "hostname": "debian-server",
  "displayName": "Debian Server",
  "capabilities": [
    "STATS",
    "SCRIPT_EXECUTION"
  ]
}
```

Response:

```json
{
  "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
  "machineId": "a75e9ccb-21d0-4765-b9d8-fb2997621daa",
  "wsUrl": "ws://localhost:8080/agent/ws",
  "heartbeatIntervalSeconds": 30,
  "statsIntervalSeconds": 30
}
```

Successful enrollment creates a machine, an agent, an active agent identity key, agent capabilities, and marks the enrollment token as used.

## Agent WebSocket API

Agents connect outbound to:

```text
ws://localhost:8080/agent/ws
```

The WebSocket endpoint is public at HTTP level. Authentication happens inside the WebSocket protocol with the enrolled Agent identity key.

### Server Challenge

Immediately after the socket opens, the server sends:

```json
{
  "type": "SERVER_CHALLENGE",
  "serverNonce": "base64-random",
  "serverKeyId": "srvkey_...",
  "sessionId": "base64-random"
}
```

### Agent Auth

The Agent signs:

```text
agentId + "\n" + serverNonce + "\n" + agentNonce + "\n" + sessionId + "\n" + timestamp
```

Message:

```json
{
  "type": "AGENT_AUTH",
  "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
  "agentKeyId": "agtkey_...",
  "agentNonce": "base64-random",
  "timestamp": "2026-06-27T17:45:00Z",
  "signature": "base64-ed25519-signature"
}
```

On success, the server marks the Agent and Machine as `ONLINE`.

### Server Auth

The server signs:

```text
agentNonce + "\n" + serverNonce + "\n" + sessionId + "\n" + timestamp
```

Message:

```json
{
  "type": "SERVER_AUTH",
  "serverKeyId": "srvkey_...",
  "timestamp": "2026-06-27T17:45:00Z",
  "signature": "base64-ed25519-signature"
}
```

The Agent verifies this with the `serverPublicKey` stored during enrollment.

### Agent Hello

After mutual authentication, the Agent sends:

```json
{
  "type": "AGENT_HELLO",
  "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
  "agentVersion": "0.1.0-SNAPSHOT",
  "hostname": "debian-server",
  "capabilities": [
    "STATS",
    "SCRIPT_EXECUTION"
  ],
  "sentAt": "2026-06-27T17:45:00Z"
}
```

The server uses `AGENT_HELLO` to refresh Agent runtime attributes such as `hostname` and `agentVersion`.
If the hostname changed since enrollment, the Machine and Agent `hostname` fields are updated. The Machine `displayName` is not changed.

Server response:

```json
{
  "type": "AGENT_HELLO_ACK",
  "receivedAt": "2026-06-27T17:45:00Z"
}
```

### Heartbeat

Agent message:

```json
{
  "type": "HEARTBEAT",
  "sentAt": "2026-06-27T17:45:30Z"
}
```

Server response:

```json
{
  "type": "HEARTBEAT_ACK",
  "receivedAt": "2026-06-27T17:45:30Z"
}
```

Heartbeats update `agents.last_seen_at` and `machines.last_seen_at`.

### Script Manifest

Agent message:

```json
{
  "type": "SCRIPT_MANIFEST",
  "manifestVersion": 3,
  "manifestHash": "sha256:...",
  "scripts": [
    {
      "id": "a0000000-b111-2222-c333-444444444444",
      "label": "Backup",
      "description": "Runs the local backup script",
      "parameters": {
        "dryRun": {
          "type": "boolean",
          "required": false,
          "default": true
        }
      },
      "resultSchema": {
        "status": {
          "type": "enum",
          "required": true,
          "allowedValues": ["SUCCESS", "FAILED"]
        }
      }
    }
  ]
}
```

The server stores the manifest and active script definitions. Local command paths are never sent.

### Stats Snapshot

Agent message:

```json
{
  "type": "STATS_SNAPSHOT",
  "cpuLoad": 0.24,
  "memoryUsedPercent": 61.3,
  "uptimeSeconds": 3600,
  "sampledAt": "2026-06-27T17:45:30Z"
}
```

### Network Interfaces

Agent message:

```json
{
  "type": "NETWORK_INTERFACES",
  "sampledAt": "2026-06-27T17:45:30Z",
  "interfaces": [
    {
      "interfaceName": "eth0",
      "displayName": "eth0",
      "macAddress": "00:11:22:33:44:55",
      "ipAddress": "192.168.1.50",
      "prefixLength": 24,
      "family": "IPV4",
      "up": true,
      "loopback": false,
      "virtual": false,
      "wireless": false,
      "defaultRoute": false,
      "wolCandidate": true
    }
  ]
}
```

### Runtime Commands

Server to Agent:

```json
{
  "type": "EXECUTE_SCRIPT",
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09",
  "scriptId": "a0000000-b111-2222-c333-444444444444",
  "parameters": {
    "dryRun": true
  }
}
```

```json
{
  "type": "SEND_WOL",
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09",
  "macAddress": "00:11:22:33:44:55",
  "broadcastAddress": "255.255.255.255",
  "port": 9
}
```

```json
{
  "type": "REFRESH_STATS",
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09"
}
```

```json
{
  "type": "REFRESH_SCRIPT_MANIFEST",
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09"
}
```

Agent responses:

```json
{
  "type": "COMMAND_ACCEPTED",
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09",
  "acceptedAt": "2026-06-27T17:45:31Z"
}
```

```json
{
  "type": "COMMAND_REJECTED",
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09",
  "reason": "INVALID_PARAMETER_VALUE",
  "message": "target has an unsupported value"
}
```

```json
{
  "type": "COMMAND_FINISHED",
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09",
  "scriptId": "a0000000-b111-2222-c333-444444444444",
  "status": "SUCCESS",
  "startedAt": "2026-06-27T17:45:31Z",
  "finishedAt": "2026-06-27T17:45:34Z",
  "durationMs": 3000,
  "result": {
    "exitCode": 0,
    "stdout": "done",
    "stderr": "",
    "status": "SUCCESS"
  },
  "error": {}
}
```

## Agent Local UI API

The Java Agent exposes a small local web UI/API.

Default:

```text
http://127.0.0.1:8787
```

For headless systems, the bind address can be changed explicitly.

Most Agent-local mutating endpoints require Agent UI authentication. The Agent UI login validates a Centaurus Server `ADMIN` user and then creates a local Agent UI session cookie.

### Agent UI Login

```http
POST /api/agent/ui/login
Content-Type: application/json
```

Request:

```json
{
  "serverUrl": "http://localhost:8080",
  "username": "admin",
  "password": "admin"
}
```

`serverUrl` is required before enrollment. After enrollment, the Agent can use the stored server URL.

Response:

```json
{
  "authenticated": true,
  "username": "admin",
  "role": "ADMIN",
  "expiresAt": "2026-06-28T16:47:07.308154579Z"
}
```

Side effect:

```text
Set-Cookie: centaurus_agent_ui_session=...
```

### Agent UI Session

```http
GET /api/agent/ui/session
```

### Agent UI Logout

```http
POST /api/agent/ui/logout
```

### Agent Local Status

```http
GET /api/agent/status
```

Response:

```json
{
  "enrolled": true,
  "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
  "machineId": "a75e9ccb-21d0-4765-b9d8-fb2997621daa",
  "serverUrl": "http://localhost:8080",
  "wsUrl": "ws://localhost:8080/agent/ws",
  "configPath": "./agent/agent-data/config.yml",
  "remoteAccessEnabled": false,
  "serverConnected": true,
  "serverAuthenticated": true
}
```

### Enroll Local Agent

```http
POST /api/agent/enroll
Content-Type: application/json
Cookie: centaurus_agent_ui_session=...
```

Request:

```json
{
  "enrollmentBundle": "raenroll:...",
  "displayName": "Debian Server"
}
```

### List Local Scripts

```http
GET /api/agent/scripts
Cookie: centaurus_agent_ui_session=...
```

### Save Local Script

```http
POST /api/agent/scripts
Content-Type: application/json
Cookie: centaurus_agent_ui_session=...
```

Request shape matches the Agent YAML script object documented in `AGENT_CONFIG.md`.
Arguments can be configured with `argumentMappings`. The list order is the process argument order:

```json
{
  "id": "a0000000-b111-2222-c333-444444444444",
  "label": "Backup",
  "command": "/opt/centaurus/scripts/backup.sh",
  "workingDirectory": "/opt/centaurus/scripts",
  "argumentMappings": [
    {"type": "NAMED_PARAMETER", "name": "--target", "parameterName": "target"},
    {"type": "FLAG_PARAMETER", "name": "--dry-run", "parameterName": "dryRun"}
  ],
  "parameters": {
    "target": {"type": "string", "required": true},
    "dryRun": {"type": "bool", "required": false, "default": true}
  }
}
```

### Browse Agent Files

```http
GET /api/agent/files?path=/opt/centaurus/scripts
Cookie: centaurus_agent_ui_session=...
```

Lists files and directories from the Agent host filesystem for local script configuration.
This endpoint is only available after Agent UI authentication.

Response:

```json
{
  "path": "/opt/centaurus/scripts",
  "parentPath": "/opt/centaurus",
  "entries": [
    {
      "name": "backup.sh",
      "path": "/opt/centaurus/scripts/backup.sh",
      "directory": false,
      "executable": true
    }
  ]
}
```

### Delete Local Script

```http
DELETE /api/agent/scripts/{scriptId}
Cookie: centaurus_agent_ui_session=...
```

### Publish Local Script Manifest

```http
POST /api/agent/scripts/publish-manifest
Cookie: centaurus_agent_ui_session=...
```

Response:

```json
{
  "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
  "machineId": "a75e9ccb-21d0-4765-b9d8-fb2997621daa",
  "serverUrl": "http://localhost:8080",
  "wsUrl": "ws://localhost:8080/agent/ws"
}
```

## Machine Registry API

All endpoints in this section require an authenticated user. `VIEWER`, `OPERATOR`, and `ADMIN` may read machine state.

### List Machines

```http
GET /api/machines
Authorization: Bearer <accessToken>
```

Each machine has at most one active Agent. The `agent` field is `null` only before an Agent is attached.

Response:

```json
[
  {
    "id": "a75e9ccb-21d0-4765-b9d8-fb2997621daa",
    "displayName": "Debian Server",
    "hostname": "debian-server",
    "status": "ONLINE",
    "lastSeenAt": "2026-06-27T17:45:30Z",
    "wolEnabled": false,
    "primaryWolInterfaceId": null,
    "agent": {
      "id": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
      "installationId": "6f3d91be-31f0-4e61-9a50-4ea0cf46f7ef",
      "displayName": "Debian Server",
      "hostname": "debian-server",
      "agentVersion": "0.1.0-SNAPSHOT",
      "status": "ONLINE",
      "lastConnectedAt": "2026-06-27T17:45:00Z",
      "lastSeenAt": "2026-06-27T17:45:30Z",
      "capabilities": [
        "STATS",
        "SCRIPT_EXECUTION"
      ],
      "createdAt": "2026-06-27T17:44:00Z",
      "updatedAt": "2026-06-27T17:45:30Z"
    },
    "createdAt": "2026-06-27T17:44:00Z",
    "updatedAt": "2026-06-27T17:45:30Z"
  }
]
```

### Get Machine

```http
GET /api/machines/{machineId}
Authorization: Bearer <accessToken>
```

Response shape is the same as one item from `GET /api/machines`.

### Rename Machine

Admin only.

```http
PUT /api/admin/machines/{machineId}/rename
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "displayName": "Office Workstation"
}
```

Response shape is the same as one item from `GET /api/machines`.
This changes the user-visible Machine display name only. The hostname remains managed by the Agent.

### List Machine Scripts

```http
GET /api/machines/{machineId}/scripts
Authorization: Bearer <accessToken>
```

Returns active script definitions reported by the Agent. Script paths and shell commands are not exposed.

### Latest Machine Stats

```http
GET /api/machines/{machineId}/stats/latest
Authorization: Bearer <accessToken>
```

Response:

```json
{
  "machineId": "a75e9ccb-21d0-4765-b9d8-fb2997621daa",
  "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
  "cpuLoad": 0.24,
  "memoryUsedPercent": 61.3,
  "uptimeSeconds": 3600,
  "updatedAt": "2026-06-27T17:45:30Z"
}
```

### Latest Machine Status Checks

```http
GET /api/machines/{machineId}/status-checks/latest
Authorization: Bearer <accessToken>
```

Returns the latest result of each centrally configured status check for the machine.

Response:

```json
[
  {
    "id": "8db3d6b6-c005-49f8-9ac8-f99244274a63",
    "machineId": "a75e9ccb-21d0-4765-b9d8-fb2997621daa",
    "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
    "checkId": "b0000000-c111-2222-d333-444444444444",
    "label": "Service",
    "healthy": true,
    "exitCode": 0,
    "stdout": "true",
    "stderr": "",
    "error": null,
    "sortOrder": 10,
    "checkedAt": "2026-06-27T17:45:30Z",
    "updatedAt": "2026-06-27T17:45:30Z"
  }
]
```

### List Status Check Configurations

```http
GET /api/machines/{machineId}/status-check-configurations
Authorization: Bearer <accessToken>
```

Returns the central status check assignments for the machine.

### Create Status Check Configuration

```http
POST /api/admin/machines/{machineId}/status-check-configurations
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "scriptDefinitionId": "7a4503f5-c6c0-47b1-99d4-b0d6ee3a2738",
  "label": "Service",
  "enabled": true,
  "intervalSeconds": 10,
  "sortOrder": 10,
  "parameters": {}
}
```

### Update Status Check Configuration

```http
PUT /api/admin/machines/{machineId}/status-check-configurations/{configurationId}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request shape is the same as create.

### Delete Status Check Configuration

```http
DELETE /api/admin/machines/{machineId}/status-check-configurations/{configurationId}
Authorization: Bearer <accessToken>
```

Response:

```http
200 OK
```

### List Machine Network Interfaces

```http
GET /api/machines/{machineId}/network-interfaces
Authorization: Bearer <accessToken>
```

Returns the latest network interface snapshot reported by the Agent.

### Configure Wake-on-LAN

Admin only.

```http
PUT /api/machines/{machineId}/wake-on-lan
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Enable Wake-on-LAN and set the primary WOL interface:

```json
{
  "enabled": true,
  "primaryWolInterfaceId": "e352ae39-4de7-4816-94bc-8820f425e171"
}
```

Disable Wake-on-LAN:

```json
{
  "enabled": false,
  "primaryWolInterfaceId": null
}
```

When enabled, `primaryWolInterfaceId` is required. The interface must belong to the selected machine and must have a MAC address.
The selected interface identifies the target MAC address to wake. The Agent that sends the Wake-on-LAN packet does not have to be the Agent of the target machine.
The Server automatically selects a currently connected Agent as WOL relay, preferring another machine over the target machine.

### List Machine Commands

```http
GET /api/machines/{machineId}/commands
Authorization: Bearer <accessToken>
```

Returns command history for the machine.

### List Machine Functions

```http
GET /api/machines/{machineId}/functions
Authorization: Bearer <accessToken>
```

Every machine has three fixed functions:

- `WOL`: built-in Wake-on-LAN function.
- `REBOOT`: can be assigned to a stored script configuration.
- `SHUTDOWN`: can be assigned to a stored script configuration.

Response:

```json
[
  {
    "type": "WOL",
    "enabled": true,
    "scriptConfiguration": null
  },
  {
    "type": "REBOOT",
    "enabled": true,
    "scriptConfiguration": {
      "id": "c7cf8f06-4df4-4d4d-8bb7-d0fd1ba2eb9f",
      "machineId": "a75e9ccb-21d0-4765-b9d8-fb2997621daa",
      "scriptDefinitionId": "b9ff947a-4819-45ab-9350-a5df881e9866",
      "agentId": "f72aa64d-c4f7-412e-963d-3e1d28403ef7",
      "scriptId": "a0000000-b111-2222-c333-444444444444",
      "label": "Reboot",
      "enabled": true,
      "sortOrder": 10,
      "parametersJson": "{}"
    }
  }
]
```

### List Script Configurations

```http
GET /api/machines/{machineId}/script-configurations
Authorization: Bearer <accessToken>
```

Returns the stored button configurations for a machine. These configurations are intended for the Web UI button matrix.

### Create Script Configuration

Admin only.

```http
POST /api/admin/machines/{machineId}/script-configurations
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "scriptDefinitionId": "b9ff947a-4819-45ab-9350-a5df881e9866",
  "label": "Backup NAS",
  "enabled": true,
  "sortOrder": 20,
  "parameters": {
    "target": "nas",
    "dryRun": false
  }
}
```

`scriptDefinitionId` must belong to the selected machine. The server stores only the script call and fixed parameter values, never the Agent-local command path.

### Update Script Configuration

Admin only.

```http
PUT /api/admin/machines/{machineId}/script-configurations/{configurationId}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request shape is the same as create.

### Delete Script Configuration

Admin only.

```http
DELETE /api/admin/machines/{machineId}/script-configurations/{configurationId}
Authorization: Bearer <accessToken>
```

### Assign Fixed Machine Function

Admin only. `REBOOT` and `SHUTDOWN` can be assigned to a stored script configuration. `WOL` is built in and cannot be assigned to a script.

```http
PUT /api/admin/machines/{machineId}/functions/{functionType}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "scriptConfigurationId": "c7cf8f06-4df4-4d4d-8bb7-d0fd1ba2eb9f"
}
```

Use `null` to clear an assignment:

```json
{
  "scriptConfigurationId": null
}
```

### Execute Script Configuration

`ADMIN` and `OPERATOR` may execute enabled script configurations.

```http
POST /api/machines/{machineId}/script-configurations/{configurationId}/execute
Authorization: Bearer <accessToken>
```

The stored parameter values are sent to the Agent as an `EXECUTE_SCRIPT` command.

### Execute Fixed Machine Function

`ADMIN` and `OPERATOR` may execute fixed machine functions.

```http
POST /api/machines/{machineId}/functions/{functionType}/execute
Authorization: Bearer <accessToken>
```

Behavior:

- `WOL`: sends the built-in Wake-on-LAN command using the machine's configured primary WOL interface.
- `REBOOT`: executes the assigned script configuration.
- `SHUTDOWN`: executes the assigned script configuration.

## Command API

`ADMIN` and `OPERATOR` may create commands. `VIEWER` may read command history.

### List Commands

```http
GET /api/commands
Authorization: Bearer <accessToken>
```

### Execute Script

```http
POST /api/machines/{machineId}/commands/execute-script
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "scriptId": "a0000000-b111-2222-c333-444444444444",
  "parameters": {
    "dryRun": true
  }
}
```

Response:

```json
{
  "commandId": "5a7c7c66-8898-42f5-8a42-49ae8bb4fd09",
  "commandType": "EXECUTE_SCRIPT",
  "status": "SENT"
}
```

The full response includes command timestamps, payload JSON, result JSON, and error JSON.

### Send Wake-on-LAN

```http
POST /api/machines/{machineId}/commands/wake-on-lan
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "macAddress": "00:11:22:33:44:55",
  "broadcastAddress": "255.255.255.255",
  "port": 9
}
```

The command targets `{machineId}`, but it may be dispatched to another connected Agent as WOL relay.
This allows waking a powered-off machine whose own Agent is not connected.

### Refresh Stats

```http
POST /api/machines/{machineId}/commands/refresh-stats
Authorization: Bearer <accessToken>
```

### Refresh Script Manifest

```http
POST /api/machines/{machineId}/commands/refresh-script-manifest
Authorization: Bearer <accessToken>
```

## Audit Endpoints

Audit events are persisted for security-relevant user and system actions.

Current audited actions include:

- Logins, failed logins, token refresh, logout, password changes.
- User creation, role changes, password resets, and user deletion.
- Enrollment bundle creation and Agent registration attempts.
- Agent online/offline status changes.
- Script Button Configuration creation, update, deletion, assignment, and execution.
- Fixed machine function assignment and execution.
- Command dispatch and final command result.
- Wake-on-LAN configuration changes.

Expected operational states, such as an Agent being offline, are not logged as application errors.
If a user-triggered action cannot be completed because an Agent is unavailable, it may still be recorded as an audit failure.

### Recent Audit Events

```http
GET /api/admin/audit-events
Authorization: Bearer <accessToken>
```

Required role:

```text
ADMIN
```

Response:

```json
[
  {
    "id": "1d3bd84e-67d9-4484-a103-a8e45d79d790",
    "createdAt": "2026-06-29T11:54:12.810Z",
    "action": "SCRIPT_BUTTON_EXECUTED",
    "result": "SUCCESS",
    "userId": "7520afd1-fe95-41c3-8637-e41964583bfc",
    "username": "admin",
    "targetType": "SCRIPT_BUTTON_CONFIGURATION",
    "targetId": "f0f622d2-5b0a-4c2b-a8f0-a8f5061b5706",
    "targetLabel": "Restart service",
    "detailsJson": "{\"machineId\":\"...\",\"commandId\":\"...\"}"
  }
]
```

The endpoint returns the latest 200 events, newest first.

## Server Endpoints

### Server Status

```http
GET /api/server/status
Authorization: Bearer <accessToken>
```

Response:

```json
{
  "application": "centaurus-server",
  "status": "UP",
  "timestamp": "2026-06-27T13:04:02.303715253Z",
  "machineCount": 0,
  "agentCount": 0,
  "commandCount": 0
}
```

## Actuator

### Health

```http
GET /actuator/health
```

This endpoint is public.

## Bootstrap Admin

On server startup, the server checks whether a non-deleted admin user exists.

Default username:

```text
admin
```

If the admin user does not exist:

- The server creates it with role `ADMIN`.
- If `CENTAURUS_BOOTSTRAP_ADMIN_PASSWORD` is set, that password is used and `passwordChangeRequired = false`.
- Otherwise, the server generates a temporary password, logs it once, and sets `passwordChangeRequired = true`.

## Error Format

Authentication failures return RFC 9457-style problem details.

Example:

```json
{
  "type": "about:blank",
  "title": "Authentication failed",
  "status": 401,
  "detail": "Invalid username or password"
}
```

## Planned Next API Areas

Not implemented yet:

- Command timeout handling
- Full historical stats series
- Server-side UI for script forms
- WOL relay selection across Agents on other machines
