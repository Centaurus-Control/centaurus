package de.shadowsoft.centaurus.server.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    Instant createdAt,
    String action,
    AuditResult result,
    UUID userId,
    String username,
    String targetType,
    UUID targetId,
    String targetLabel,
    String detailsJson
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
            event.getId(),
            event.getCreatedAt(),
            event.getAction(),
            event.getResult(),
            event.getUser() == null ? null : event.getUser().getId(),
            event.getUsername(),
            event.getTargetType(),
            event.getTargetId(),
            event.getTargetLabel(),
            event.getDetailsJson()
        );
    }
}
