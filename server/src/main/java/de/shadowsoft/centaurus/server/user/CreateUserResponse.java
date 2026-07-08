package de.shadowsoft.centaurus.server.user;

public record CreateUserResponse(
    UserResponse user,
    String temporaryPassword
) {
}
