package de.shadowsoft.centaurus.server.auth;

public record RefreshToken(
    String token,
    String hash
) {
}
