package de.shadowsoft.centaurus.agent.enrollment;

import java.util.List;
import java.util.UUID;

public record AgentEnrollmentRequest(
    String enrollmentToken,
    UUID installationId,
    String agentPublicKey,
    String agentKeyId,
    String agentVersion,
    String hostname,
    String displayName,
    List<AgentCapabilityType> capabilities
) {
}
