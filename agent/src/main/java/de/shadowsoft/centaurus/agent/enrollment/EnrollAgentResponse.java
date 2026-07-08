package de.shadowsoft.centaurus.agent.enrollment;

import java.util.UUID;

public record EnrollAgentResponse(
    UUID agentId,
    UUID machineId,
    String serverUrl,
    String wsUrl
) {
}
