package de.shadowsoft.centaurus.agent.auth;

import java.time.Instant;

public record AgentUiSessionResponse(
    boolean authenticated,
    String username,
    String role,
    Instant expiresAt
) {
}
