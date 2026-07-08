package de.shadowsoft.centaurus.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "centaurus.agent")
public class AgentProperties {

    private String configPath = "./agent/agent-data/config.yml";
    private String version = "0.1.0-SNAPSHOT";
    private Connection connection = new Connection();
    private LocalUi localUi = new LocalUi();

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalUi getLocalUi() {
        return localUi;
    }

    public void setLocalUi(LocalUi localUi) {
        this.localUi = localUi;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public static class Connection {

        private boolean autoConnect = true;
        private int reconnectDelaySeconds = 10;

        public boolean isAutoConnect() {
            return autoConnect;
        }

        public void setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
        }

        public int getReconnectDelaySeconds() {
            return reconnectDelaySeconds;
        }

        public void setReconnectDelaySeconds(int reconnectDelaySeconds) {
            this.reconnectDelaySeconds = reconnectDelaySeconds;
        }
    }

    public static class LocalUi {

        private boolean remoteAccessEnabled;

        public boolean isRemoteAccessEnabled() {
            return remoteAccessEnabled;
        }

        public void setRemoteAccessEnabled(boolean remoteAccessEnabled) {
            this.remoteAccessEnabled = remoteAccessEnabled;
        }
    }
}
