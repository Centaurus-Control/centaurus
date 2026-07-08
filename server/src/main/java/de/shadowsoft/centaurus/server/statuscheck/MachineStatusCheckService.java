package de.shadowsoft.centaurus.server.statuscheck;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MachineStatusCheckService {

    private static final int OUTPUT_LIMIT = 2048;
    private static final int ERROR_LIMIT = 100;

    private final AgentRepository agentRepository;
    private final MachineStatusCheckLatestRepository repository;

    public MachineStatusCheckService(AgentRepository agentRepository, MachineStatusCheckLatestRepository repository) {
        this.agentRepository = agentRepository;
        this.repository = repository;
    }

    @Transactional
    public void updateLatest(UUID agentId, StatusSnapshotMessage message) {
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown agent"));
        Instant updatedAt = message.sampledAt() == null ? Instant.now() : message.sampledAt();
        List<StatusSnapshotMessage.StatusCheckResult> statuses = message.statuses() == null ? List.of() : message.statuses();
        for (StatusSnapshotMessage.StatusCheckResult status : statuses) {
            if (status.id() == null || !StringUtils.hasText(status.label())) {
                continue;
            }
            Instant checkedAt = status.checkedAt() == null ? updatedAt : status.checkedAt();
            repository.findByMachineIdAndCheckId(agent.getMachine().getId(), status.id())
                .ifPresentOrElse(
                    existing -> existing.update(
                        agent,
                        status.label(),
                        status.healthy(),
                        status.exitCode(),
                        truncate(status.stdout(), OUTPUT_LIMIT),
                        truncate(status.stderr(), OUTPUT_LIMIT),
                        truncate(status.error(), ERROR_LIMIT),
                        status.sortOrder(),
                        checkedAt,
                        updatedAt
                    ),
                    () -> repository.save(new MachineStatusCheckLatest(
                        agent.getMachine(),
                        agent,
                        status.id(),
                        status.label(),
                        status.healthy(),
                        status.exitCode(),
                        truncate(status.stdout(), OUTPUT_LIMIT),
                        truncate(status.stderr(), OUTPUT_LIMIT),
                        truncate(status.error(), ERROR_LIMIT),
                        status.sortOrder(),
                        checkedAt,
                        updatedAt
                    ))
                );
        }
    }

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
