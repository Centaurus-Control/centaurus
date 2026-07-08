package de.shadowsoft.centaurus.server.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    UserRole role,
    boolean passwordChangeRequired,
    Instant createdAt,
    Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.isPasswordChangeRequired(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
