package de.shadowsoft.centaurus.server.machine;

import java.time.Instant;
import java.util.UUID;

public record MachineStatusChangedEvent(
    UUID machineId,
    MachineStatus status,
    Instant changedAt
) {
}
