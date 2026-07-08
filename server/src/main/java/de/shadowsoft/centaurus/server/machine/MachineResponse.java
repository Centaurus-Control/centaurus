package de.shadowsoft.centaurus.server.machine;

import java.time.Instant;
import java.util.UUID;

public record MachineResponse(
    UUID id,
    String displayName,
    String hostname,
    MachineStatus status,
    Instant lastSeenAt,
    boolean wolEnabled,
    UUID primaryWolInterfaceId,
    AgentSummaryResponse agent,
    Instant createdAt,
    Instant updatedAt
) {

    public static MachineResponse from(Machine machine, AgentSummaryResponse agent) {
        return new MachineResponse(
            machine.getId(),
            machine.getDisplayName(),
            machine.getHostname(),
            machine.getStatus(),
            machine.getLastSeenAt(),
            machine.isWolEnabled(),
            machine.getPrimaryWolInterfaceId(),
            agent,
            machine.getCreatedAt(),
            machine.getUpdatedAt()
        );
    }
}
