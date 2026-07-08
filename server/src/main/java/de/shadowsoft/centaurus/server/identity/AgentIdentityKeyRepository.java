package de.shadowsoft.centaurus.server.identity;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentIdentityKeyRepository extends JpaRepository<AgentIdentityKey, UUID> {

    Optional<AgentIdentityKey> findByAgentIdAndKeyIdAndActiveTrue(UUID agentId, String keyId);

    List<AgentIdentityKey> findByAgentIdAndActiveTrue(UUID agentId);

    void deleteByAgentId(UUID agentId);
}
