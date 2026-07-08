package de.shadowsoft.centaurus.server.command;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandRepository extends JpaRepository<Command, UUID> {

    Optional<Command> findByCommandId(UUID commandId);

    List<Command> findByHiddenFromUiFalseOrderByCreatedAtDesc();

    List<Command> findByMachineIdAndHiddenFromUiFalseOrderByCreatedAtDesc(UUID machineId);

    List<Command> findByAgentIdAndHiddenFromUiFalseOrderByCreatedAtDesc(UUID agentId);

    void deleteByMachineIdOrAgentId(UUID machineId, UUID agentId);
}
