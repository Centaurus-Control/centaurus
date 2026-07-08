package de.shadowsoft.centaurus.server.script;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptDefinitionRepository extends JpaRepository<ScriptDefinition, UUID> {

    List<ScriptDefinition> findByAgentIdAndActiveTrue(UUID agentId);

    Optional<ScriptDefinition> findByAgentIdAndScriptIdAndActiveTrue(UUID agentId, UUID scriptId);

    Optional<ScriptDefinition> findByAgentIdAndScriptId(UUID agentId, UUID scriptId);

    void deleteByAgentId(UUID agentId);
}
