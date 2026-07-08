package de.shadowsoft.centaurus.server.statuscheck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineStatusCheckLatestRepository extends JpaRepository<MachineStatusCheckLatest, UUID> {

    List<MachineStatusCheckLatest> findByMachineIdOrderBySortOrderAscLabelAsc(UUID machineId);

    Optional<MachineStatusCheckLatest> findByMachineIdAndCheckId(UUID machineId, UUID checkId);

    void deleteByMachineId(UUID machineId);
}
