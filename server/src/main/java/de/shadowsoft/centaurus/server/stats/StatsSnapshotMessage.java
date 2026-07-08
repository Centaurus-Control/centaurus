package de.shadowsoft.centaurus.server.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatsSnapshotMessage(
    double cpuLoad,
    double memoryUsedPercent,
    long uptimeSeconds,
    Instant sampledAt
) {
}
