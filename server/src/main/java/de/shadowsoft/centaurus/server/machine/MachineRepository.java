package de.shadowsoft.centaurus.server.machine;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, UUID> {

    List<Machine> findByDeletedFalse();

    boolean existsByIdAndDeletedFalse(UUID id);

    long countByDeletedFalse();
}
