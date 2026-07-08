package de.shadowsoft.centaurus.server.stats;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineStatsService {

    private final AgentRepository agentRepository;
    private final MachineStatsLatestRepository machineStatsLatestRepository;

    public MachineStatsService(
        AgentRepository agentRepository,
        MachineStatsLatestRepository machineStatsLatestRepository
    ) {
        this.agentRepository = agentRepository;
        this.machineStatsLatestRepository = machineStatsLatestRepository;
    }

    @Transactional
    public void updateLatest(UUID agentId, StatsSnapshotMessage message) {
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown agent"));
        Instant sampledAt = message.sampledAt() == null ? Instant.now() : message.sampledAt();
        machineStatsLatestRepository.findById(agent.getMachine().getId())
            .ifPresentOrElse(
                existing -> existing.update(
                    agent,
                    message.cpuLoad(),
                    message.memoryUsedPercent(),
                    message.uptimeSeconds(),
                    sampledAt
                ),
                () -> machineStatsLatestRepository.save(new MachineStatsLatest(
                    agent.getMachine(),
                    agent,
                    message.cpuLoad(),
                    message.memoryUsedPercent(),
                    message.uptimeSeconds(),
                    sampledAt
                ))
            );
    }
}
