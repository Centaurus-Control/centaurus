package de.shadowsoft.centaurus.server.script;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptManifestRepository extends JpaRepository<ScriptManifest, UUID> {

    List<ScriptManifest> findByAgentIdOrderByManifestVersionDesc(UUID agentId);

    void deleteByAgentId(UUID agentId);
}
