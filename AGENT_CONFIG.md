# Centaurus Agent Config

The Agent stores its runtime config as YAML.

Default path:

```text
./agent/agent-data/config.yml
```

Override:

```text
CENTAURUS_AGENT_CONFIG_PATH=/path/to/config.yml
```

## Enrollment Fields

These fields are written by the local Agent enrollment flow:

```yaml
installationId: "6f3d91be-31f0-4e61-9a50-4ea0cf46f7ef"
agentId: "f72aa64d-c4f7-412e-963d-3e1d28403ef7"
machineId: "a75e9ccb-21d0-4765-b9d8-fb2997621daa"
serverUrl: "http://localhost:8080"
wsUrl: "ws://localhost:8080/agent/ws"
serverPublicKey: "base64-ed25519-public-key"
serverKeyId: "srvkey_..."
agentPrivateKey: "base64-ed25519-private-key"
agentPublicKey: "base64-ed25519-public-key"
agentKeyId: "agtkey_..."
heartbeatIntervalSeconds: 30
statsIntervalSeconds: 30
```

## Local Scripts

Scripts are configured only on the Agent. The server receives only script metadata and schemas, never paths or shell commands.

Scripts can be edited in the local Agent UI:

```text
http://127.0.0.1:8787/
```

The Agent UI requires a local session. Login validates a Centaurus Server `ADMIN` user against the configured server and stores only a local Agent UI session cookie.

The `command` field is selected through the local Agent file browser. It should point to an executable file on the Agent host. When a command is selected, the UI fills `workingDirectory` with the command parent directory if the field is empty.

Arguments are configured through `argumentMappings`. This keeps the command itself a plain executable path and avoids shell command lines. The argument list is evaluated from top to bottom.

Supported argument mapping types:

- `FIXED`: appends one literal argument.
- `PARAMETER`: appends a server parameter value as positional argument.
- `NAMED_PARAMETER`: appends `name` and then the server parameter value, for example `--target nas`.
- `FLAG_PARAMETER`: appends `name` only when the mapped boolean parameter is `true`, for example `--dry-run`.

Example:

```yaml
scripts:
  - id: "a0000000-b111-2222-c333-444444444444"
    label: "Backup"
    description: "Runs the local backup script"
    command: "/opt/centaurus/scripts/backup.sh"
    workingDirectory: "/opt/centaurus/scripts"
    timeoutSeconds: 900

    argumentMappings:
      - type: NAMED_PARAMETER
        name: "--target"
        parameterName: target
      - type: FLAG_PARAMETER
        name: "--dry-run"
        parameterName: dryRun

    parameters:
      dryRun:
        type: bool
        required: false
        default: true
      target:
        type: string
        required: true

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
      enabled: true
      cooldownSeconds: 10
```

Parameters are passed to the process as environment variables:

```text
CENTAURUS_PARAM_DRYRUN=true
CENTAURUS_PARAM_TARGET=nas
```

The supported Agent UI parameter types are:

```text
string
bool
int
string[]
int[]
```

## Status Checks

Status checks are configured centrally in the Centaurus Server UI. The Agent only defines its available scripts through the normal `scripts` manifest.

The server assigns one of the Agent-reported scripts as a status check, including:

- Label shown in the Server UI.
- Execution interval.
- Sort order.
- Static parameter values.

The Agent receives these assignments over the authenticated WebSocket connection, executes the referenced local script by its script ID, and reports only the latest result back to the server.

The check result can be returned as stdout:

```text
true
false
1
0
```

If stdout does not contain one of these values, the Agent falls back to the process exit code: `0` is healthy and `1` is unhealthy. Any other result is reported as undefined and shown as a gray dot in the Server UI.

## Local Dev Harness

The repository contains a development script that registers the current machine as a local Agent and runs a sample script command:

```text
dev/register-local-agent.sh
```

Prerequisites:

- Server is running on `http://localhost:8080`.
- Agent is running on `http://127.0.0.1:8787`.
- Bootstrap credentials are `admin` / `admin`, or overridden via environment variables.

The script writes a local Agent config to:

```text
agent/agent-data/config.yml
```

It registers this test script:

```text
agent/scripts/dev-echo.sh
```

The Agent UI itself is protected. The dev harness logs in to the Agent UI by validating the configured admin credentials against the Centaurus Server and then uses the local Agent UI session cookie for enrollment.
