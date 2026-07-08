package de.shadowsoft.centaurus.server.agent;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentCapabilityRepository extends JpaRepository<AgentCapability, UUID> {

    List<AgentCapability> findByAgentId(UUID agentId);

    void deleteByAgentId(UUID agentId);
}
