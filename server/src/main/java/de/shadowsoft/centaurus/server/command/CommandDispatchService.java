package de.shadowsoft.centaurus.server.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.audit.AuditResult;
import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentRepository;
import de.shadowsoft.centaurus.server.agent.AgentStatus;
import de.shadowsoft.centaurus.server.agentws.AgentConnectionRegistry;
import de.shadowsoft.centaurus.server.agentws.AgentNotConnectedException;
import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.machine.MachineNotFoundException;
import de.shadowsoft.centaurus.server.machine.MachineRepository;
import de.shadowsoft.centaurus.server.script.ScriptDefinitionRepository;
import de.shadowsoft.centaurus.server.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CommandDispatchService {

    private final CommandRepository commandRepository;
    private final MachineRepository machineRepository;
    private final AgentRepository agentRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final AgentConnectionRegistry agentConnectionRegistry;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public CommandDispatchService(
        CommandRepository commandRepository,
        MachineRepository machineRepository,
        AgentRepository agentRepository,
        ScriptDefinitionRepository scriptDefinitionRepository,
        AgentConnectionRegistry agentConnectionRegistry,
        ObjectMapper objectMapper,
        AuditService auditService
    ) {
        this.commandRepository = commandRepository;
        this.machineRepository = machineRepository;
        this.agentRepository = agentRepository;
        this.scriptDefinitionRepository = scriptDefinitionRepository;
        this.agentConnectionRegistry = agentConnectionRegistry;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CommandResponse> listCommands() {
        return commandRepository.findByHiddenFromUiFalseOrderByCreatedAtDesc()
            .stream()
            .map(CommandResponse::from)
            .toList();
    }

    @Transactional
    public CommandResponse executeScript(UUID machineId, ExecuteScriptRequest request, Authentication authentication) {
        if (request.scriptId() == null) {
            throw new CommandDispatchException("scriptId is required");
        }
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        Agent agent = resolveOnlineAgent(machineId);
        scriptDefinitionRepository.findByAgentIdAndScriptIdAndActiveTrue(agent.getId(), request.scriptId())
            .orElseThrow(() -> new CommandDispatchException("Script is not available for the selected machine"));
        Map<String, Object> payload = Map.of(
            "type", "EXECUTE_SCRIPT",
            "scriptId", request.scriptId(),
            "parameters", request.parameters() == null ? Map.of() : request.parameters()
        );
        return createAndSend(machine, agent, CommandType.EXECUTE_SCRIPT, auditService.user(authentication), payload);
    }

    @Transactional
    public CommandResponse executeScriptOnAgent(
        UUID machineId,
        UUID agentId,
        ExecuteScriptRequest request,
        Authentication authentication
    ) {
        if (request.scriptId() == null) {
            throw new CommandDispatchException("scriptId is required");
        }
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        Agent agent = resolveConnectedAgent(machineId, agentId);
        scriptDefinitionRepository.findByAgentIdAndScriptIdAndActiveTrue(agent.getId(), request.scriptId())
            .orElseThrow(() -> new CommandDispatchException("Script is not available for the selected machine"));
        Map<String, Object> payload = Map.of(
            "type", "EXECUTE_SCRIPT",
            "scriptId", request.scriptId(),
            "parameters", request.parameters() == null ? Map.of() : request.parameters()
        );
        return createAndSend(machine, agent, CommandType.EXECUTE_SCRIPT, auditService.user(authentication), payload);
    }

    @Transactional
    public CommandResponse sendWakeOnLan(UUID machineId, SendWakeOnLanRequest request, Authentication authentication) {
        if (!StringUtils.hasText(request.macAddress())) {
            throw new CommandDispatchException("macAddress is required");
        }
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        Agent agent = resolveWakeOnLanRelayAgent(machineId);
        Map<String, Object> payload = Map.of(
            "type", "SEND_WOL",
            "macAddress", request.macAddress(),
            "broadcastAddress", request.broadcastAddress() == null ? "255.255.255.255" : request.broadcastAddress(),
            "port", request.port() == null ? 9 : request.port()
        );
        return createAndSend(machine, agent, CommandType.SEND_WOL, auditService.user(authentication), payload);
    }

    @Transactional
    public CommandResponse refreshStats(UUID machineId, Authentication authentication) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        Agent agent = resolveOnlineAgent(machineId);
        return createAndSend(machine, agent, CommandType.REFRESH_STATS, auditService.user(authentication), Map.of("type", "REFRESH_STATS"));
    }

    @Transactional
    public CommandResponse refreshScriptManifest(UUID machineId, Authentication authentication) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        Agent agent = resolveOnlineAgent(machineId);
        return createAndSend(machine, agent, CommandType.REFRESH_SCRIPT_MANIFEST, auditService.user(authentication), Map.of("type", "REFRESH_SCRIPT_MANIFEST"));
    }

    private CommandResponse createAndSend(
        Machine machine,
        Agent agent,
        CommandType commandType,
        User user,
        Map<String, Object> payload
    ) {
        try {
            Command command = new Command(machine, agent, commandType, user, objectMapper.writeValueAsString(payload));
            commandRepository.saveAndFlush(command);
            Map<String, Object> outbound = new java.util.LinkedHashMap<>(payload);
            outbound.put("commandId", command.getCommandId());
            command.markSent(Instant.now());
            commandRepository.saveAndFlush(command);
            agentConnectionRegistry.send(agent.getId(), outbound);
            auditService.record(
                "COMMAND_DISPATCHED",
                AuditResult.SUCCESS,
                user,
                user == null ? null : user.getUsername(),
                "COMMAND",
                command.getId(),
                command.getCommandId().toString(),
                AuditService.details(
                    "commandId", command.getCommandId(),
                    "commandType", commandType,
                    "machineId", machine.getId(),
                    "machineName", machine.getDisplayName(),
                    "agentId", agent.getId()
                )
            );
            return CommandResponse.from(command);
        } catch (JsonProcessingException exception) {
            throw new CommandDispatchException("Could not serialize command payload", exception);
        } catch (AgentNotConnectedException exception) {
            auditService.record(
                "COMMAND_DISPATCHED",
                AuditResult.FAILURE,
                user,
                user == null ? null : user.getUsername(),
                "AGENT",
                agent.getId(),
                agent.getDisplayName(),
                AuditService.details(
                    "reason", "agent_not_connected",
                    "commandType", commandType,
                    "machineId", machine.getId(),
                    "machineName", machine.getDisplayName()
                )
            );
            throw new CommandDispatchException(exception.getMessage(), exception);
        }
    }

    private Agent resolveOnlineAgent(UUID machineId) {
        return agentRepository.findByMachineId(machineId)
            .filter(agent -> agent.getStatus() == AgentStatus.ONLINE)
            .filter(agent -> agentConnectionRegistry.isConnected(agent.getId()))
            .orElseThrow(() -> new CommandDispatchException("No connected agent is available for this machine"));
    }

    private Agent resolveWakeOnLanRelayAgent(UUID targetMachineId) {
        return agentRepository.findAll()
            .stream()
            .filter(agent -> agent.getStatus() == AgentStatus.ONLINE)
            .filter(agent -> agentConnectionRegistry.isConnected(agent.getId()))
            .sorted(java.util.Comparator.comparing(agent -> agent.getMachine().getId().equals(targetMachineId)))
            .findFirst()
            .orElseThrow(() -> new CommandDispatchException("No connected Wake-on-LAN relay agent is available"));
    }

    private Agent resolveConnectedAgent(UUID machineId, UUID agentId) {
        return agentRepository.findById(agentId)
            .filter(agent -> agent.getMachine().getId().equals(machineId))
            .filter(agent -> agent.getStatus() == AgentStatus.ONLINE)
            .filter(agent -> agentConnectionRegistry.isConnected(agent.getId()))
            .orElseThrow(() -> new CommandDispatchException("The selected script agent is not connected"));
    }

}
