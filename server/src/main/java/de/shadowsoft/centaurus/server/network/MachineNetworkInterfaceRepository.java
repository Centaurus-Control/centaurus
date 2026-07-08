package de.shadowsoft.centaurus.server.network;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineNetworkInterfaceRepository extends JpaRepository<MachineNetworkInterface, UUID> {

    List<MachineNetworkInterface> findByMachineId(UUID machineId);

    List<MachineNetworkInterface> findByAgentId(UUID agentId);

    void deleteByAgentId(UUID agentId);

    void deleteByMachineId(UUID machineId);
}
