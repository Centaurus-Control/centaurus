package de.shadowsoft.centaurus.server.user;

public record ResetUserPasswordResponse(
    UserResponse user,
    String temporaryPassword
) {
}
