package de.shadowsoft.centaurus.server.scriptconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.command.CommandDispatchService;
import de.shadowsoft.centaurus.server.command.CommandResponse;
import de.shadowsoft.centaurus.server.command.ExecuteScriptRequest;
import de.shadowsoft.centaurus.server.command.SendWakeOnLanRequest;
import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.machine.MachineNotFoundException;
import de.shadowsoft.centaurus.server.machine.MachineRepository;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionAssignment;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionAssignmentRepository;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionAssignmentRequest;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionResponse;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionType;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterface;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterfaceRepository;
import de.shadowsoft.centaurus.server.script.ScriptDefinition;
import de.shadowsoft.centaurus.server.script.ScriptDefinitionRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ScriptButtonConfigurationService {

    private static final TypeReference<Map<String, Object>> PARAMETER_MAP_TYPE = new TypeReference<>() {
    };

    private final MachineRepository machineRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptButtonConfigurationRepository scriptButtonConfigurationRepository;
    private final MachineFunctionAssignmentRepository machineFunctionAssignmentRepository;
    private final MachineNetworkInterfaceRepository machineNetworkInterfaceRepository;
    private final CommandDispatchService commandDispatchService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ScriptButtonConfigurationService(
        MachineRepository machineRepository,
        ScriptDefinitionRepository scriptDefinitionRepository,
        ScriptButtonConfigurationRepository scriptButtonConfigurationRepository,
        MachineFunctionAssignmentRepository machineFunctionAssignmentRepository,
        MachineNetworkInterfaceRepository machineNetworkInterfaceRepository,
        CommandDispatchService commandDispatchService,
        ObjectMapper objectMapper,
        AuditService auditService
    ) {
        this.machineRepository = machineRepository;
        this.scriptDefinitionRepository = scriptDefinitionRepository;
        this.scriptButtonConfigurationRepository = scriptButtonConfigurationRepository;
        this.machineFunctionAssignmentRepository = machineFunctionAssignmentRepository;
        this.machineNetworkInterfaceRepository = machineNetworkInterfaceRepository;
        this.commandDispatchService = commandDispatchService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ScriptButtonConfigurationResponse> listConfigurations(UUID machineId) {
        ensureMachineExists(machineId);
        return scriptButtonConfigurationRepository.findByMachineIdOrderBySortOrderAscLabelAsc(machineId)
            .stream()
            .map(ScriptButtonConfigurationResponse::from)
            .toList();
    }

    @Transactional
    public ScriptButtonConfigurationResponse createConfiguration(
        UUID machineId,
        ScriptButtonConfigurationRequest request,
        Authentication authentication
    ) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        ScriptDefinition scriptDefinition = resolveScriptDefinition(machineId, request.scriptDefinitionId());
        ScriptButtonConfiguration configuration = new ScriptButtonConfiguration(
            machine,
            scriptDefinition,
            label(request.label(), scriptDefinition),
            Boolean.TRUE.equals(request.enabled()),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            parametersJson(request.parameters())
        );
        ScriptButtonConfiguration saved = scriptButtonConfigurationRepository.save(configuration);
        auditService.recordSuccess(
            "SCRIPT_BUTTON_CREATED",
            authentication,
            "SCRIPT_BUTTON_CONFIGURATION",
            saved.getId(),
            saved.getLabel(),
            AuditService.details("machineId", machine.getId(), "scriptDefinitionId", scriptDefinition.getId(), "enabled", saved.isEnabled())
        );
        return ScriptButtonConfigurationResponse.from(saved);
    }

    @Transactional
    public ScriptButtonConfigurationResponse updateConfiguration(
        UUID machineId,
        UUID configurationId,
        ScriptButtonConfigurationRequest request,
        Authentication authentication
    ) {
        ScriptButtonConfiguration configuration = resolveConfiguration(machineId, configurationId);
        ScriptDefinition scriptDefinition = resolveScriptDefinition(machineId, request.scriptDefinitionId());
        configuration.update(
            scriptDefinition,
            label(request.label(), scriptDefinition),
            Boolean.TRUE.equals(request.enabled()),
            request.sortOrder() == null ? configuration.getSortOrder() : request.sortOrder(),
            parametersJson(request.parameters())
        );
        auditService.recordSuccess(
            "SCRIPT_BUTTON_UPDATED",
            authentication,
            "SCRIPT_BUTTON_CONFIGURATION",
            configuration.getId(),
            configuration.getLabel(),
            AuditService.details(
                "machineId", machineId,
                "scriptDefinitionId", scriptDefinition.getId(),
                "enabled", configuration.isEnabled(),
                "sortOrder", configuration.getSortOrder()
            )
        );
        return ScriptButtonConfigurationResponse.from(configuration);
    }

    @Transactional
    public void deleteConfiguration(UUID machineId, UUID configurationId, Authentication authentication) {
        ScriptButtonConfiguration configuration = resolveConfiguration(machineId, configurationId);
        auditService.recordSuccess(
            "SCRIPT_BUTTON_DELETED",
            authentication,
            "SCRIPT_BUTTON_CONFIGURATION",
            configuration.getId(),
            configuration.getLabel(),
            AuditService.details("machineId", machineId, "scriptDefinitionId", configuration.getScriptDefinition().getId())
        );
        scriptButtonConfigurationRepository.delete(configuration);
    }

    @Transactional(readOnly = true)
    public List<MachineFunctionResponse> listFunctions(UUID machineId) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        Map<MachineFunctionType, MachineFunctionAssignment> assignments = new EnumMap<>(MachineFunctionType.class);
        for (MachineFunctionAssignment assignment : machineFunctionAssignmentRepository.findByMachineId(machineId)) {
            assignments.put(assignment.getFunctionType(), assignment);
        }
        return List.of(
            new MachineFunctionResponse(MachineFunctionType.WOL, machine.isWolEnabled(), null),
            functionResponse(MachineFunctionType.REBOOT, assignments.get(MachineFunctionType.REBOOT)),
            functionResponse(MachineFunctionType.SHUTDOWN, assignments.get(MachineFunctionType.SHUTDOWN))
        );
    }

    @Transactional
    public MachineFunctionResponse assignFunction(
        UUID machineId,
        MachineFunctionType functionType,
        MachineFunctionAssignmentRequest request,
        Authentication authentication
    ) {
        if (functionType == MachineFunctionType.WOL) {
            throw new ScriptConfigurationException("WOL is a built-in function and cannot be assigned to a script");
        }
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        ScriptButtonConfiguration configuration = request.scriptConfigurationId() == null
            ? null
            : resolveConfiguration(machineId, request.scriptConfigurationId());
        MachineFunctionAssignment assignment = machineFunctionAssignmentRepository
            .findByMachineIdAndFunctionType(machineId, functionType)
            .orElseGet(() -> machineFunctionAssignmentRepository.save(new MachineFunctionAssignment(machine, functionType, null)));
        assignment.assign(configuration);
        auditService.recordSuccess(
            "MACHINE_FUNCTION_ASSIGNED",
            authentication,
            "MACHINE",
            machine.getId(),
            machine.getDisplayName(),
            AuditService.details(
                "functionType", functionType,
                "scriptConfigurationId", configuration == null ? null : configuration.getId(),
                "scriptConfigurationLabel", configuration == null ? null : configuration.getLabel()
            )
        );
        return functionResponse(functionType, assignment);
    }

    @Transactional
    public CommandResponse executeConfiguration(UUID machineId, UUID configurationId, Authentication authentication) {
        ScriptButtonConfiguration configuration = resolveConfiguration(machineId, configurationId);
        if (!configuration.isEnabled()) {
            throw new ScriptConfigurationException("Script configuration is disabled");
        }
        CommandResponse response = executeScriptConfiguration(machineId, configuration, authentication);
        auditService.recordSuccess(
            "SCRIPT_BUTTON_EXECUTED",
            authentication,
            "SCRIPT_BUTTON_CONFIGURATION",
            configuration.getId(),
            configuration.getLabel(),
            AuditService.details("machineId", machineId, "commandId", response.commandId())
        );
        return response;
    }

    @Transactional
    public CommandResponse executeFunction(
        UUID machineId,
        MachineFunctionType functionType,
        Authentication authentication
    ) {
        if (functionType == MachineFunctionType.WOL) {
            CommandResponse response = executeWakeOnLan(machineId, authentication);
            auditService.recordSuccess(
                "MACHINE_FUNCTION_EXECUTED",
                authentication,
                "MACHINE",
                machineId,
                null,
                AuditService.details("functionType", functionType, "commandId", response.commandId())
            );
            return response;
        }
        MachineFunctionAssignment assignment = machineFunctionAssignmentRepository
            .findByMachineIdAndFunctionType(machineId, functionType)
            .orElseThrow(() -> new ScriptConfigurationException(functionType + " is not assigned to a script configuration"));
        ScriptButtonConfiguration configuration = assignment.getScriptConfiguration();
        if (configuration == null) {
            throw new ScriptConfigurationException(functionType + " is not assigned to a script configuration");
        }
        if (!configuration.isEnabled()) {
            throw new ScriptConfigurationException(functionType + " script configuration is disabled");
        }
        CommandResponse response = executeScriptConfiguration(machineId, configuration, authentication);
        auditService.recordSuccess(
            "MACHINE_FUNCTION_EXECUTED",
            authentication,
            "MACHINE",
            machineId,
            null,
            AuditService.details(
                "functionType", functionType,
                "scriptConfigurationId", configuration.getId(),
                "scriptConfigurationLabel", configuration.getLabel(),
                "commandId", response.commandId()
            )
        );
        return response;
    }

    private CommandResponse executeScriptConfiguration(
        UUID machineId,
        ScriptButtonConfiguration configuration,
        Authentication authentication
    ) {
        return commandDispatchService.executeScriptOnAgent(
            machineId,
            configuration.getScriptDefinition().getAgent().getId(),
            new ExecuteScriptRequest(
                configuration.getScriptDefinition().getScriptId(),
                parameters(configuration.getParametersJson())
            ),
            authentication
        );
    }

    private CommandResponse executeWakeOnLan(UUID machineId, Authentication authentication) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        if (!machine.isWolEnabled() || machine.getPrimaryWolInterfaceId() == null) {
            throw new ScriptConfigurationException("Wake-on-LAN is not configured for this machine");
        }
        MachineNetworkInterface networkInterface = machineNetworkInterfaceRepository.findById(machine.getPrimaryWolInterfaceId())
            .filter(candidate -> candidate.getMachine().getId().equals(machineId))
            .orElseThrow(() -> new ScriptConfigurationException("Configured Wake-on-LAN interface was not found"));
        if (!StringUtils.hasText(networkInterface.getMacAddress())) {
            throw new ScriptConfigurationException("Configured Wake-on-LAN interface has no MAC address");
        }
        return commandDispatchService.sendWakeOnLan(
            machineId,
            new SendWakeOnLanRequest(networkInterface.getMacAddress(), null, null),
            authentication
        );
    }

    private MachineFunctionResponse functionResponse(
        MachineFunctionType functionType,
        MachineFunctionAssignment assignment
    ) {
        ScriptButtonConfiguration configuration = assignment == null ? null : assignment.getScriptConfiguration();
        return new MachineFunctionResponse(
            functionType,
            configuration != null && configuration.isEnabled(),
            configuration == null ? null : ScriptButtonConfigurationResponse.from(configuration)
        );
    }

    private ScriptButtonConfiguration resolveConfiguration(UUID machineId, UUID configurationId) {
        return scriptButtonConfigurationRepository.findByIdAndMachineId(configurationId, machineId)
            .orElseThrow(() -> new ScriptConfigurationException("Script configuration not found"));
    }

    private ScriptDefinition resolveScriptDefinition(UUID machineId, UUID scriptDefinitionId) {
        if (scriptDefinitionId == null) {
            throw new ScriptConfigurationException("scriptDefinitionId is required");
        }
        ScriptDefinition scriptDefinition = scriptDefinitionRepository.findById(scriptDefinitionId)
            .filter(ScriptDefinition::isActive)
            .orElseThrow(() -> new ScriptConfigurationException("Script definition is not active"));
        if (!scriptDefinition.getAgent().getMachine().getId().equals(machineId)) {
            throw new ScriptConfigurationException("Script definition does not belong to the selected machine");
        }
        return scriptDefinition;
    }

    private void ensureMachineExists(UUID machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new MachineNotFoundException("Machine not found");
        }
    }

    private String label(String requestedLabel, ScriptDefinition scriptDefinition) {
        if (StringUtils.hasText(requestedLabel)) {
            return requestedLabel.trim();
        }
        return scriptDefinition.getLabel();
    }

    private String parametersJson(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters == null ? Map.of() : parameters);
        } catch (JsonProcessingException exception) {
            throw new ScriptConfigurationException("Could not serialize parameters", exception);
        }
    }

    private Map<String, Object> parameters(String parametersJson) {
        try {
            return objectMapper.readValue(parametersJson, PARAMETER_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new ScriptConfigurationException("Could not parse stored parameters", exception);
        }
    }
}
