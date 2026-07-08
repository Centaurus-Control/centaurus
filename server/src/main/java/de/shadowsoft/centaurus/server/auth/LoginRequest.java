package de.shadowsoft.centaurus.server.auth;

public record LoginRequest(
    String username,
    String password
) {
}
