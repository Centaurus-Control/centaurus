package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.user.User;

public record AuthSessionTokens(
    AuthTokenResponse accessToken,
    String refreshToken,
    User user
) {
}
