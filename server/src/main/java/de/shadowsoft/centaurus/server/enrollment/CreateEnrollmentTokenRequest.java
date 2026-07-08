package de.shadowsoft.centaurus.server.enrollment;

import java.time.Duration;

public record CreateEnrollmentTokenRequest(
    String suggestedName,
    Duration expiresIn
) {
}
