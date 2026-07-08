package de.shadowsoft.centaurus.server.agent;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

    Optional<Agent> findByInstallationId(UUID installationId);

    Optional<Agent> findByMachineId(UUID machineId);

    Optional<Agent> findByMachineIdAndDeletedFalse(UUID machineId);

    long countByDeletedFalse();
}
