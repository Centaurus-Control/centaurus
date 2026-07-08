package de.shadowsoft.centaurus.server.machinefunction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineFunctionAssignmentRepository extends JpaRepository<MachineFunctionAssignment, UUID> {

    List<MachineFunctionAssignment> findByMachineId(UUID machineId);

    Optional<MachineFunctionAssignment> findByMachineIdAndFunctionType(UUID machineId, MachineFunctionType functionType);

    void deleteByMachineId(UUID machineId);
}
