package de.shadowsoft.centaurus.server.enrollment;

import java.time.Instant;

public record EnrollmentBundlePayload(
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
