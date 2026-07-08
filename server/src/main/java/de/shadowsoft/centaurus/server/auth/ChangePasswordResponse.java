package de.shadowsoft.centaurus.server.auth;

import java.time.Instant;

public record ChangePasswordResponse(
    boolean passwordChangeRequired,
    Instant changedAt,
    long revokedSessionCount
) {
}
