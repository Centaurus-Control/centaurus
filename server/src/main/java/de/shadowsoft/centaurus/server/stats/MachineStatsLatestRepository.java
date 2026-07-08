package de.shadowsoft.centaurus.server.stats;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineStatsLatestRepository extends JpaRepository<MachineStatsLatest, UUID> {

    void deleteByMachineId(UUID machineId);
}
