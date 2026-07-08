package de.shadowsoft.centaurus.server.statuscheck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusCheckConfigurationRepository extends JpaRepository<StatusCheckConfiguration, UUID> {

    List<StatusCheckConfiguration> findByMachineIdOrderBySortOrderAscLabelAsc(UUID machineId);

    List<StatusCheckConfiguration> findByScriptDefinitionAgentIdOrderBySortOrderAscLabelAsc(UUID agentId);

    Optional<StatusCheckConfiguration> findByIdAndMachineId(UUID id, UUID machineId);

    void deleteByMachineId(UUID machineId);
}
