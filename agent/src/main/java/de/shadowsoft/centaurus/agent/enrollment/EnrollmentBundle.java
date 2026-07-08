package de.shadowsoft.centaurus.agent.enrollment;

import java.time.Instant;

public record EnrollmentBundle(
    int v,
    String serverUrl,
    String wsUrl,
    String enrollmentToken,
    String serverPublicKey,
    String serverKeyId,
    String suggestedName,
    Instant expiresAt
) {
}
