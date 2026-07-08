package de.shadowsoft.centaurus.server.status;

import java.time.Instant;

public record ServerStatusResponse(
    String application,
    String status,
    Instant timestamp,
    long machineCount,
    long agentCount,
    long commandCount
) {
}
