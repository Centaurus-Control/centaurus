package de.shadowsoft.centaurus.server.auth;

public record ChangePasswordRequest(
    String currentPassword,
    String newPassword
) {
}
