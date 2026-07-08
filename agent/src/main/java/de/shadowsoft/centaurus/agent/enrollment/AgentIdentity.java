package de.shadowsoft.centaurus.agent.enrollment;

public record AgentIdentity(
    String keyId,
    String publicKey,
    String privateKey
) {
}
