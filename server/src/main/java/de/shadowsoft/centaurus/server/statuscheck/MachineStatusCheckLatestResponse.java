package de.shadowsoft.centaurus.server.statuscheck;

import java.time.Instant;
import java.util.UUID;

public record MachineStatusCheckLatestResponse(
    UUID id,
    UUID machineId,
    UUID agentId,
    UUID checkId,
    String label,
    Boolean healthy,
    Integer exitCode,
    String stdout,
    String stderr,
    String error,
    int sortOrder,
    Instant checkedAt,
    Instant updatedAt
) {

    public static MachineStatusCheckLatestResponse from(MachineStatusCheckLatest statusCheck) {
        return new MachineStatusCheckLatestResponse(
            statusCheck.getId(),
            statusCheck.getMachine().getId(),
            statusCheck.getAgent().getId(),
            statusCheck.getCheckId(),
            statusCheck.getLabel(),
            statusCheck.getHealthy(),
            statusCheck.getExitCode(),
            statusCheck.getStdout(),
            statusCheck.getStderr(),
            statusCheck.getError(),
            statusCheck.getSortOrder(),
            statusCheck.getCheckedAt(),
            statusCheck.getUpdatedAt()
        );
    }
}
