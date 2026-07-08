package de.shadowsoft.centaurus.server.machine;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentCapabilityType;
import de.shadowsoft.centaurus.server.agent.AgentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentSummaryResponse(
    UUID id,
    UUID installationId,
    String displayName,
    String hostname,
    String agentVersion,
    AgentStatus status,
    Instant lastConnectedAt,
    Instant lastSeenAt,
    List<AgentCapabilityType> capabilities,
    Instant createdAt,
    Instant updatedAt
) {

    public static AgentSummaryResponse from(Agent agent, List<AgentCapabilityType> capabilities) {
        return new AgentSummaryResponse(
            agent.getId(),
            agent.getInstallationId(),
            agent.getDisplayName(),
            agent.getHostname(),
            agent.getAgentVersion(),
            agent.getStatus(),
            agent.getLastConnectedAt(),
            agent.getLastSeenAt(),
            capabilities,
            agent.getCreatedAt(),
            agent.getUpdatedAt()
        );
    }
}
