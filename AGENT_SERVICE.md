# Centaurus Agent Debian Service

The Agent should run directly on the host, not in Docker.
It needs host access for local scripts, Wake-on-LAN, stats, reboot, and shutdown.

## Suggested Filesystem Layout

```text
/opt/centaurus-agent/
  centaurus-agent-2.0.0-SNAPSHOT.jar
  run-agent.sh
  .env
  agent-data/
    config.yml
  logs/
    centaurus-agent.log
```

## Install Files

Build the Agent jar on the development machine:

```bash
cd agent
./gradlew bootJar
```

Copy these files to the target Debian machine:

```bash
sudo mkdir -p /opt/centaurus-agent/agent-data /opt/centaurus-agent/logs
sudo cp centaurus-agent-2.0.0-SNAPSHOT.jar /opt/centaurus-agent/
sudo cp run-agent.sh /opt/centaurus-agent/
sudo chmod +x /opt/centaurus-agent/run-agent.sh
```

Create `/opt/centaurus-agent/.env`:

```bash
sudo nano /opt/centaurus-agent/.env
```

Example:

```text
CENTAURUS_AGENT_UI_BIND_ADDRESS=0.0.0.0
CENTAURUS_AGENT_UI_PORT=8787
CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED=true
CENTAURUS_AGENT_CONFIG_PATH=/opt/centaurus-agent/agent-data/config.yml
CENTAURUS_AGENT_LOG_PATH=/opt/centaurus-agent/logs/centaurus-agent.log
CENTAURUS_AGENT_VERSION=2.0.0
CENTAURUS_AGENT_AUTO_CONNECT=true
CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS=10
JAVA_HOME=/usr/lib/jvm/temurin-21-jre
JAVA_OPTS="-Xms128m -Xmx256m"
```

Use `127.0.0.1` instead of `0.0.0.0` if the Agent UI should only be reachable locally.

If you do not want to use `JAVA_HOME`, you can set an explicit binary instead:

```text
JAVA_BIN=/opt/jdk-21/bin/java
```

## Create Service User

For first tests, running as `root` is simplest because reboot and shutdown need elevated permissions.
For a stricter setup, create a dedicated user and grant only the required sudo permissions later.

```bash
sudo useradd --system --home /opt/centaurus-agent --shell /usr/sbin/nologin centaurus-agent
sudo chown -R centaurus-agent:centaurus-agent /opt/centaurus-agent
```

If you need host reboot/shutdown from scripts, either run the service as `root` or configure sudo for the specific script commands.

## systemd Unit

Create `/etc/systemd/system/centaurus-agent.service`:

```ini
[Unit]
Description=Centaurus Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=centaurus-agent
Group=centaurus-agent
WorkingDirectory=/opt/centaurus-agent
ExecStart=/opt/centaurus-agent/run-agent.sh
Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

If you intentionally want the Agent to run as root for reboot/shutdown testing, remove or comment:

```ini
User=centaurus-agent
Group=centaurus-agent
```

## Enable And Start

```bash
sudo systemctl daemon-reload
sudo systemctl enable centaurus-agent
sudo systemctl start centaurus-agent
```

Check status:

```bash
sudo systemctl status centaurus-agent
```

Follow logs:

```bash
journalctl -u centaurus-agent -f
```

The Agent also writes its own rolling logfile:

```bash
tail -f /opt/centaurus-agent/logs/centaurus-agent.log
```

Rotated logfiles are stored next to it as compressed `.gz` files.

## Agent UI

If the service uses:

```text
CENTAURUS_AGENT_UI_BIND_ADDRESS=0.0.0.0
CENTAURUS_AGENT_UI_PORT=8787
```

open:

```text
http://<agent-host>:8787/
```

Login with a Centaurus Server `ADMIN` user, then enroll the Agent using an enrollment bundle from the Server UI.

## Windows Start Script

For Windows hosts, use:

```text
run-agent.bat
```

Place it next to the Agent jar and optional `.env` file. It supports the same relevant variables:

```text
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21
JAVA_BIN=C:\Tools\jdk-21\bin\java.exe
JAVA_OPTS=-Xms128m -Xmx256m
CENTAURUS_AGENT_CONFIG_PATH=C:\Centaurus\agent-data\config.yml
CENTAURUS_AGENT_LOG_PATH=C:\Centaurus\logs\centaurus-agent.log
```
