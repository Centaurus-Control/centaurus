# Centaurus Agent Packaging

The Agent build creates self-contained Linux Agent packages for GitHub Releases.

Build:

```bash
cd agent
./gradlew build
```

Output:

```text
dist/agent/
  centaurus-agent-linux-amd64-<version>.tar.gz
  centaurus-agent-linux-amd64-<version>.tar.gz.sha256
  centaurus-agent-linux-arm64-<version>.tar.gz
  centaurus-agent-linux-arm64-<version>.tar.gz.sha256
```

Each archive contains:

```text
centaurus-agent/
  centaurus-agent.jar
  install.sh
  uninstall.sh
  run-agent.sh
  .env.example
  runtime/
  agent-data/
  logs/
```

The runtime is Eclipse Temurin 21 for the target architecture.

Install on the target host:

```bash
sudo mkdir -p /opt/centaurus-agent
sudo tar -xzf centaurus-agent-linux-amd64-<version>.tar.gz -C /opt/centaurus-agent --strip-components=1
cd /opt/centaurus-agent
sudo ./install.sh
```

Use the `arm64` archive for 64-bit Raspberry Pi OS, Debian, or Ubuntu on AArch64.

The installer assumes the archive has already been unpacked to the final installation directory. It writes a `centaurus-agent` systemd service, runs it as `root`, and starts it immediately.
