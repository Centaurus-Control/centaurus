package de.shadowsoft.centaurus.agent.auth;

public record AgentUiLoginRequest(
    String serverUrl,
    String username,
    String password
) {
}
