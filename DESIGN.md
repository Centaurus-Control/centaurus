# Centaurus Design

This document describes the current intended system design for Centaurus.
It is the product and architecture reference for implementation work.

## Scope

Centaurus is a private LAN remote administration system for trusted machines.
It is not a corporate multi-tenant platform and does not aim to provide arbitrary remote shell access.

The system consists of:

- Central Server
- Web UI
- Remote Agent

## Core Principles

- Agents connect outbound to the Server.
- The Server does not connect directly to random machines.
- The Server orchestrates actions, but the Agent is authoritative for local execution.
- A Machine has exactly one active Agent.
- An Agent belongs to exactly one Machine.
- Reinstalling or re-enrolling a machine should replace the active Agent conceptually, not create multiple active Agents for one Machine.
- Scripts are owned and configured only by the Agent.
- The Server never receives Agent-local script paths, shell commands, working directories, or environment details.
- The Server receives only script manifests: script UUID, label, description, parameter schema, and result schema.
- The Server may store reusable Script Button Configurations for a machine.
- Every machine has three fixed functions: `WOL`, `REBOOT`, `SHUTDOWN`.
- `WOL` is built in and is not a script.
- `REBOOT` and `SHUTDOWN` are assigned to stored Script Button Configurations.

## Components

### Server

Responsibilities:

- User authentication and roles.
- Admin bootstrap.
- Agent enrollment.
- Agent registry.
- Machine registry.
- Agent WebSocket endpoint.
- Command orchestration.
- Latest stats storage.
- Network interface storage.
- Script manifest storage.
- Script Button Configuration storage.
- Fixed machine function assignment.
- Command history.
- Security audit event storage.

Roles:

- `ADMIN`: user management, enrollment, security-relevant config, machine onboarding, script configuration management.
- `OPERATOR`: execute machine functions and enabled Script Button Configurations.
- `VIEWER`: read-only access to machines, stats, and command history.

### Agent

Responsibilities:

- Runs on a managed machine.
- Connects outbound to the Server.
- Authenticates WebSocket sessions with Agent identity keys.
- Sends heartbeat.
- Sends latest stats.
- Sends network interface snapshots.
- Owns local script configuration.
- Publishes script manifests.
- Executes only locally configured scripts.
- Executes built-in Wake-on-LAN command where supported.
- Provides a small local Agent UI for enrollment and local script configuration.

### Web UI

Responsibilities:

- Login and password change.
- User management for admins.
- Enrollment token management for admins.
- Machine list.
- Machine detail.
- Latest stats display.
- Network interface display.
- Fixed machine function buttons: `WOL`, `REBOOT`, `SHUTDOWN`.
- Script Button Configuration button matrix.
- Admin UI for creating/editing Script Button Configurations.
- Command history and command result display.
- Admin audit event display.

Machine detail layout:

- The first action category is `Power Cycle`.
- `Power Cycle` always contains the fixed buttons `WOL`, `REBOOT`, and `SHUTDOWN`.
- `WOL` is enabled only when the user may execute commands, Wake-on-LAN is configured, and the machine is not currently online.
- `WOL` targets the selected machine's configured MAC address, but the sending Agent may be any currently connected Agent in the LAN.
- Automatic WOL relay selection prefers a connected Agent on another machine over the target machine itself.
- `REBOOT` and `SHUTDOWN` are enabled only when the user may execute commands, the machine is online, and the function is assigned to an enabled Script Button Configuration.
- The regular machine view does not show raw script definitions or ad-hoc parameter entry.
- The regular machine view shows only enabled Script Button Configurations as a button matrix.
- Script Button Configurations assigned to `REBOOT` or `SHUTDOWN` are not repeated in the regular button matrix.
- The regular machine view prioritizes actions over telemetry: `Power Cycle`, Script Button Configuration matrix, then stats.
- Machine configuration is an admin-only sub-view of the selected machine.
- Admins configure Wake-on-LAN in the machine configuration sub-view by selecting the primary network interface with a MAC address or disabling WOL.
- Wake-on-LAN configuration is shown as its own section, separate from Script Button Configuration management.
- Admins manage the button matrix from the machine detail view by selecting a reported script definition, setting a button label, sort order, enabled flag, and fixed parameter values.
- Admins assign `REBOOT` and `SHUTDOWN` to existing Script Button Configurations. `WOL` is built in and is not assignable.
- Agent details are shown in the admin-only machine configuration sub-view, not in the regular machine action view.

## Script Model

Scripts are configured only on the Agent.

Agent-local script config includes:

- `id`
- `label`
- `description`
- `command`
- `workingDirectory`
- `timeoutSeconds`
- `parameters`
- `argumentMappings`
- `resultSchema`
- `spamProtection`

The `command` field is an executable path on the Agent host. It is selected through the Agent-local file browser.
The `workingDirectory` defaults to the command parent directory when selected through the Agent UI.

Parameter types currently used by the Agent UI:

- `string`
- `bool`
- `int`
- `string[]`
- `int[]`

Argument mapping types:

- `FIXED`: append one literal argument.
- `PARAMETER`: append a server parameter value as a positional argument.
- `NAMED_PARAMETER`: append `name` and then the server parameter value.
- `FLAG_PARAMETER`: append `name` only when the mapped boolean parameter is `true`.

Example:

```yaml
argumentMappings:
  - type: NAMED_PARAMETER
    name: "--target"
    parameterName: target
  - type: FLAG_PARAMETER
    name: "--dry-run"
    parameterName: dryRun
```

## Server-Side Script Button Configurations

The Server stores reusable machine-local script calls as Script Button Configurations.

A configuration contains:

- Machine ID
- Script Definition ID
- Label
- Enabled flag
- Sort order
- Fixed parameter values

The Server does not store local script paths or shell commands.

Example API payload:

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

The Web UI should show enabled configurations as a button matrix.
Executing a configuration creates a normal `EXECUTE_SCRIPT` command.

## Fixed Machine Functions

Each machine has exactly three fixed user-facing functions:

- `WOL`
- `REBOOT`
- `SHUTDOWN`

`WOL` uses the machine's configured primary Wake-on-LAN interface as target MAC source.
The WOL command may be dispatched through another connected Agent because the target machine's own Agent is usually offline when Wake-on-LAN is needed.

`REBOOT` and `SHUTDOWN` are assigned to Script Button Configurations.
This keeps reboot/shutdown implementation local to the Agent machine while giving the Server and Web UI stable, predictable machine actions.

## Current API Areas

Implemented or intended for version 2.0:

- Auth API
- User management API
- Enrollment token API
- Agent enrollment API
- Agent WebSocket protocol
- Machine registry API
- Machine latest stats API
- Machine network interfaces API
- Machine scripts API
- Command API
- Script Button Configuration API
- Fixed Machine Function API
- Agent-local UI API
- Agent-local file browser API
- Agent-local script configuration API

## Version 2.0 Implementation Focus

Version 2.0 should provide an end-to-end usable LAN workflow:

- Start Server locally.
- Start Agent locally or on another LAN machine.
- Enroll Agent.
- See machine in Web UI.
- See latest stats and network interfaces.
- Configure Agent-local scripts.
- Publish script manifest.
- Create Server-side Script Button Configurations.
- Assign `REBOOT` and `SHUTDOWN`.
- Execute `WOL`, `REBOOT`, `SHUTDOWN`, and configured buttons.
- See command history and results.

## Remaining 2.0 Work

High priority:

- Web UI machine list and machine detail.
- Web UI latest stats and network interface display.
- Web UI Script Button Configuration admin screen.
- Web UI button matrix for enabled configurations.
- Web UI fixed function buttons.
- Command result/history UI.
- Command timeout handling on the Server.
- Docker/dev packaging.
- Setup documentation.

Medium priority:

- Better validation of stored Script Button Configuration parameters against script parameter schema.
- Better WOL primary interface configuration in the Web UI.
- Cleaner empty/error/loading states in the Web UI.
- End-to-end dev script covering enrollment, manifest publication, button config creation, and execution.

Deferred:

- Historical stats series.
- Charts and metric history.
- Explicit/admin-selectable WOL relay policy across Agents on other machines.
- WOL result tracking beyond command success/failure.
- Remote file transfer.
- Remote shell.
- Complex RBAC.
- mTLS.
- Per-message signatures.

## Transport Security Direction

For early local development and trusted LAN testing, HTTP/WS mode is acceptable.

The design should remain compatible with HTTPS/WSS and explicit Agent TLS trust configuration.
Browser-based Web UI cannot disable certificate validation from application code.
Custom local root certificates must be installed into the browser or operating system trust store if strict browser TLS trust is required.

## Audit Model

Security-relevant actions are persisted as audit events and also emitted as structured `AUDIT` log lines.

Audit events include:

- Action name and result.
- User ID and username when the action was initiated by an authenticated user.
- Target type, target ID, and target label where applicable.
- Small structured details as JSON.

Audit logging must not store raw passwords, refresh tokens, enrollment token plaintext, script stdout/stderr, or full local script command definitions.

Expected operational states are not application errors. An Agent going offline is a normal audit event. A user-triggered command that cannot be dispatched may be an audit failure, but should not be logged as an error solely because the Agent is offline.

## Data Model Summary

Important entities:

- `users`
- `machines`
- `agents`
- `agent_identity_keys`
- `server_identity_keys`
- `enrollment_tokens`
- `agent_capabilities`
- `machine_network_interfaces`
- `script_manifests`
- `script_definitions`
- `script_button_configurations`
- `machine_function_assignments`
- `commands`
- `audit_events`
- `machine_stats_latest`

Stats history is intentionally deferred.
