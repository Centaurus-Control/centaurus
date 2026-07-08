package de.shadowsoft.centaurus.server.user;

public record CreateUserRequest(
    String username,
    UserRole role
) {
}
