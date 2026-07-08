package de.shadowsoft.centaurus.server.stats;

import java.time.Instant;
import java.util.UUID;

public record MachineStatsLatestResponse(
    UUID machineId,
    UUID agentId,
    double cpuLoad,
    double memoryUsedPercent,
    long uptimeSeconds,
    Instant updatedAt
) {

    public static MachineStatsLatestResponse from(MachineStatsLatest stats) {
        return new MachineStatsLatestResponse(
            stats.getMachineId(),
            stats.getAgent().getId(),
            stats.getCpuLoad(),
            stats.getMemoryUsedPercent(),
            stats.getUptimeSeconds(),
            stats.getUpdatedAt()
        );
    }
}
