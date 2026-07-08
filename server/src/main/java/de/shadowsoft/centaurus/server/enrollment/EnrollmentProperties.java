package de.shadowsoft.centaurus.server.enrollment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "centaurus.enrollment")
public class EnrollmentProperties {

    private Duration tokenTtl = Duration.ofHours(1);
    private String serverUrl = "http://localhost:8080";
    private String wsUrl = "ws://localhost:8080/agent/ws";

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }
}
