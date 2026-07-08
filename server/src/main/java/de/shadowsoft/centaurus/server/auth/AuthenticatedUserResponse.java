package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.user.UserRole;
import java.util.UUID;

public record AuthenticatedUserResponse(
    UUID id,
    String username,
    UserRole role,
    boolean passwordChangeRequired
) {
}
