package de.shadowsoft.centaurus.server.enrollment;

import java.util.UUID;

public record AgentEnrollmentResponse(
    UUID agentId,
    UUID machineId,
    String wsUrl,
    int heartbeatIntervalSeconds,
    int statsIntervalSeconds
) {
}
