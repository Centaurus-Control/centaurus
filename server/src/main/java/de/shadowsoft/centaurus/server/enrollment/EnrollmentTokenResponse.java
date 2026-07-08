package de.shadowsoft.centaurus.server.enrollment;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentTokenResponse(
    UUID id,
    String suggestedName,
    Instant expiresAt,
    Instant usedAt,
    UUID usedByAgentId,
    Instant createdAt
) {

    public static EnrollmentTokenResponse from(EnrollmentToken token) {
        UUID usedByAgentId = token.getUsedByAgent() == null ? null : token.getUsedByAgent().getId();
        return new EnrollmentTokenResponse(
            token.getId(),
            token.getSuggestedName(),
            token.getExpiresAt(),
            token.getUsedAt(),
            usedByAgentId,
            token.getCreatedAt()
        );
    }
}
