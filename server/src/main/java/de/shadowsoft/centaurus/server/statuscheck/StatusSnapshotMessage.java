package de.shadowsoft.centaurus.server.statuscheck;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusSnapshotMessage(
    List<StatusCheckResult> statuses,
    Instant sampledAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusCheckResult(
        UUID id,
        String label,
        Boolean healthy,
        Integer exitCode,
        String stdout,
        String stderr,
        String error,
        int sortOrder,
        Instant checkedAt
    ) {
    }
}
