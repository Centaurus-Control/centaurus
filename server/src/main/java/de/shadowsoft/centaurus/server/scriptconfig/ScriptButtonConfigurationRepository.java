package de.shadowsoft.centaurus.server.scriptconfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptButtonConfigurationRepository extends JpaRepository<ScriptButtonConfiguration, UUID> {

    List<ScriptButtonConfiguration> findByMachineIdOrderBySortOrderAscLabelAsc(UUID machineId);

    Optional<ScriptButtonConfiguration> findByIdAndMachineId(UUID id, UUID machineId);

    void deleteByMachineId(UUID machineId);
}
