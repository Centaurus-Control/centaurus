package de.shadowsoft.centaurus.server.auth;

import java.time.Instant;

public record AuthTokenResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt
) {
}
