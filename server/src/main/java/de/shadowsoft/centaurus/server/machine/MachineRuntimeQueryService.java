package de.shadowsoft.centaurus.server.machine;

import de.shadowsoft.centaurus.server.agent.AgentRepository;
import de.shadowsoft.centaurus.server.command.CommandRepository;
import de.shadowsoft.centaurus.server.command.CommandResponse;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterfaceRepository;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterfaceResponse;
import de.shadowsoft.centaurus.server.script.ScriptDefinitionRepository;
import de.shadowsoft.centaurus.server.script.ScriptDefinitionResponse;
import de.shadowsoft.centaurus.server.stats.MachineStatsLatestRepository;
import de.shadowsoft.centaurus.server.stats.MachineStatsLatestResponse;
import de.shadowsoft.centaurus.server.statuscheck.MachineStatusCheckLatestRepository;
import de.shadowsoft.centaurus.server.statuscheck.MachineStatusCheckLatestResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineRuntimeQueryService {

    private final MachineRepository machineRepository;
    private final AgentRepository agentRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final MachineStatsLatestRepository machineStatsLatestRepository;
    private final MachineStatusCheckLatestRepository machineStatusCheckLatestRepository;
    private final MachineNetworkInterfaceRepository machineNetworkInterfaceRepository;
    private final CommandRepository commandRepository;

    public MachineRuntimeQueryService(
        MachineRepository machineRepository,
        AgentRepository agentRepository,
        ScriptDefinitionRepository scriptDefinitionRepository,
        MachineStatsLatestRepository machineStatsLatestRepository,
        MachineStatusCheckLatestRepository machineStatusCheckLatestRepository,
        MachineNetworkInterfaceRepository machineNetworkInterfaceRepository,
        CommandRepository commandRepository
    ) {
        this.machineRepository = machineRepository;
        this.agentRepository = agentRepository;
        this.scriptDefinitionRepository = scriptDefinitionRepository;
        this.machineStatsLatestRepository = machineStatsLatestRepository;
        this.machineStatusCheckLatestRepository = machineStatusCheckLatestRepository;
        this.machineNetworkInterfaceRepository = machineNetworkInterfaceRepository;
        this.commandRepository = commandRepository;
    }

    @Transactional(readOnly = true)
    public List<ScriptDefinitionResponse> listScripts(UUID machineId) {
        ensureMachineExists(machineId);
        return agentRepository.findByMachineIdAndDeletedFalse(machineId)
            .map(agent -> scriptDefinitionRepository.findByAgentIdAndActiveTrue(agent.getId()))
            .orElse(List.of())
            .stream()
            .map(ScriptDefinitionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public MachineStatsLatestResponse latestStats(UUID machineId) {
        ensureMachineExists(machineId);
        return machineStatsLatestRepository.findById(machineId)
            .map(MachineStatsLatestResponse::from)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MachineStatusCheckLatestResponse> listLatestStatusChecks(UUID machineId) {
        ensureMachineExists(machineId);
        return machineStatusCheckLatestRepository.findByMachineIdOrderBySortOrderAscLabelAsc(machineId)
            .stream()
            .map(MachineStatusCheckLatestResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MachineNetworkInterfaceResponse> listNetworkInterfaces(UUID machineId) {
        ensureMachineExists(machineId);
        return machineNetworkInterfaceRepository.findByMachineId(machineId)
            .stream()
            .map(MachineNetworkInterfaceResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CommandResponse> listCommands(UUID machineId) {
        ensureMachineExists(machineId);
        return commandRepository.findByMachineIdAndHiddenFromUiFalseOrderByCreatedAtDesc(machineId)
            .stream()
            .map(CommandResponse::from)
            .toList();
    }

    private void ensureMachineExists(UUID machineId) {
        if (!machineRepository.existsByIdAndDeletedFalse(machineId)) {
            throw new MachineNotFoundException("Machine not found");
        }
    }
}
