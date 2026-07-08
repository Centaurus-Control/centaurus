package de.shadowsoft.centaurus.agent.config;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class AgentConfig {

    private UUID installationId;
    private UUID agentId;
    private UUID machineId;
    private String serverUrl;
    private String wsUrl;
    private String serverPublicKey;
    private String serverKeyId;
    private String agentPrivateKey;
    private String agentPublicKey;
    private String agentKeyId;
    private int heartbeatIntervalSeconds;
    private int statsIntervalSeconds;
    private List<AgentScriptConfig> scripts = new ArrayList<>();

    public UUID getInstallationId() {
        return installationId;
    }

    public void setInstallationId(UUID installationId) {
        this.installationId = installationId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
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

    public String getServerPublicKey() {
        return serverPublicKey;
    }

    public void setServerPublicKey(String serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
    }

    public String getServerKeyId() {
        return serverKeyId;
    }

    public void setServerKeyId(String serverKeyId) {
        this.serverKeyId = serverKeyId;
    }

    public String getAgentPrivateKey() {
        return agentPrivateKey;
    }

    public void setAgentPrivateKey(String agentPrivateKey) {
        this.agentPrivateKey = agentPrivateKey;
    }

    public String getAgentPublicKey() {
        return agentPublicKey;
    }

    public void setAgentPublicKey(String agentPublicKey) {
        this.agentPublicKey = agentPublicKey;
    }

    public String getAgentKeyId() {
        return agentKeyId;
    }

    public void setAgentKeyId(String agentKeyId) {
        this.agentKeyId = agentKeyId;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public int getStatsIntervalSeconds() {
        return statsIntervalSeconds;
    }

    public void setStatsIntervalSeconds(int statsIntervalSeconds) {
        this.statsIntervalSeconds = statsIntervalSeconds;
    }

    public List<AgentScriptConfig> getScripts() {
        return scripts;
    }

    public void setScripts(List<AgentScriptConfig> scripts) {
        this.scripts = scripts == null ? new ArrayList<>() : scripts;
    }

}
