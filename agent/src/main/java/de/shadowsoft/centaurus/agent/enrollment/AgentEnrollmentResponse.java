package de.shadowsoft.centaurus.agent.enrollment;

import java.util.UUID;

public record AgentEnrollmentResponse(
    UUID agentId,
    UUID machineId,
    String wsUrl,
    int heartbeatIntervalSeconds,
    int statsIntervalSeconds
) {
}
