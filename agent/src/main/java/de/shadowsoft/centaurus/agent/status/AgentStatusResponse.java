package de.shadowsoft.centaurus.agent.status;

import java.util.UUID;

public record AgentStatusResponse(
    boolean enrolled,
    UUID agentId,
    UUID machineId,
    String serverUrl,
    String wsUrl,
    String configPath,
    boolean remoteAccessEnabled,
    boolean serverConnected,
    boolean serverAuthenticated
) {
}
