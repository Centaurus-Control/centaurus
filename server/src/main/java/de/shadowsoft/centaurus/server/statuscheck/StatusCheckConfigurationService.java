package de.shadowsoft.centaurus.server.statuscheck;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.agentws.AgentConnectionRegistry;
import de.shadowsoft.centaurus.server.agentws.AgentNotConnectedException;
import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.machine.MachineNotFoundException;
import de.shadowsoft.centaurus.server.machine.MachineRepository;
import de.shadowsoft.centaurus.server.script.ScriptDefinition;
import de.shadowsoft.centaurus.server.script.ScriptDefinitionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StatusCheckConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCheckConfigurationService.class);
    private static final TypeReference<Map<String, Object>> PARAMETER_MAP_TYPE = new TypeReference<>() {
    };

    private final MachineRepository machineRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final StatusCheckConfigurationRepository repository;
    private final AgentConnectionRegistry agentConnectionRegistry;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public StatusCheckConfigurationService(
        MachineRepository machineRepository,
        ScriptDefinitionRepository scriptDefinitionRepository,
        StatusCheckConfigurationRepository repository,
        AgentConnectionRegistry agentConnectionRegistry,
        ObjectMapper objectMapper,
        AuditService auditService
    ) {
        this.machineRepository = machineRepository;
        this.scriptDefinitionRepository = scriptDefinitionRepository;
        this.repository = repository;
        this.agentConnectionRegistry = agentConnectionRegistry;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StatusCheckConfigurationResponse> listConfigurations(UUID machineId) {
        ensureMachineExists(machineId);
        return repository.findByMachineIdOrderBySortOrderAscLabelAsc(machineId)
            .stream()
            .map(StatusCheckConfigurationResponse::from)
            .toList();
    }

    @Transactional
    public StatusCheckConfigurationResponse createConfiguration(
        UUID machineId,
        StatusCheckConfigurationRequest request,
        Authentication authentication
    ) {
        Machine machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        ScriptDefinition scriptDefinition = resolveScriptDefinition(machineId, request.scriptDefinitionId());
        StatusCheckConfiguration configuration = new StatusCheckConfiguration(
            machine,
            scriptDefinition,
            label(request.label(), scriptDefinition),
            Boolean.TRUE.equals(request.enabled()),
            intervalSeconds(request.intervalSeconds()),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            parametersJson(request.parameters())
        );
        StatusCheckConfiguration saved = repository.save(configuration);
        auditService.recordSuccess(
            "STATUS_CHECK_CREATED",
            authentication,
            "STATUS_CHECK_CONFIGURATION",
            saved.getId(),
            saved.getLabel(),
            AuditService.details("machineId", machineId, "scriptDefinitionId", scriptDefinition.getId(), "enabled", saved.isEnabled())
        );
        publishAssignments(scriptDefinition.getAgent().getId());
        return StatusCheckConfigurationResponse.from(saved);
    }

    @Transactional
    public StatusCheckConfigurationResponse updateConfiguration(
        UUID machineId,
        UUID configurationId,
        StatusCheckConfigurationRequest request,
        Authentication authentication
    ) {
        StatusCheckConfiguration configuration = resolveConfiguration(machineId, configurationId);
        UUID previousAgentId = configuration.getScriptDefinition().getAgent().getId();
        ScriptDefinition scriptDefinition = resolveScriptDefinition(machineId, request.scriptDefinitionId());
        configuration.update(
            scriptDefinition,
            label(request.label(), scriptDefinition),
            Boolean.TRUE.equals(request.enabled()),
            intervalSeconds(request.intervalSeconds()),
            request.sortOrder() == null ? configuration.getSortOrder() : request.sortOrder(),
            parametersJson(request.parameters())
        );
        auditService.recordSuccess(
            "STATUS_CHECK_UPDATED",
            authentication,
            "STATUS_CHECK_CONFIGURATION",
            configuration.getId(),
            configuration.getLabel(),
            AuditService.details("machineId", machineId, "scriptDefinitionId", scriptDefinition.getId(), "enabled", configuration.isEnabled())
        );
        publishAssignments(previousAgentId);
        publishAssignments(scriptDefinition.getAgent().getId());
        return StatusCheckConfigurationResponse.from(configuration);
    }

    @Transactional
    public void deleteConfiguration(UUID machineId, UUID configurationId, Authentication authentication) {
        StatusCheckConfiguration configuration = resolveConfiguration(machineId, configurationId);
        UUID agentId = configuration.getScriptDefinition().getAgent().getId();
        auditService.recordSuccess(
            "STATUS_CHECK_DELETED",
            authentication,
            "STATUS_CHECK_CONFIGURATION",
            configuration.getId(),
            configuration.getLabel(),
            AuditService.details("machineId", machineId, "scriptDefinitionId", configuration.getScriptDefinition().getId())
        );
        repository.delete(configuration);
        repository.flush();
        publishAssignments(agentId);
    }

    @Transactional(readOnly = true)
    public void publishAssignments(UUID agentId) {
        if (!agentConnectionRegistry.isConnected(agentId)) {
            return;
        }
        try {
            agentConnectionRegistry.send(agentId, Map.of(
                "type", "STATUS_CHECK_CONFIG",
                "checks", repository.findByScriptDefinitionAgentIdOrderBySortOrderAscLabelAsc(agentId)
                    .stream()
                    .map(this::assignmentPayload)
                    .toList()
            ));
        } catch (AgentNotConnectedException exception) {
            LOGGER.debug("Could not publish status check assignments to disconnected agent {}", agentId);
        }
    }

    private Map<String, Object> assignmentPayload(StatusCheckConfiguration configuration) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("configurationId", configuration.getId());
        payload.put("scriptId", configuration.getScriptDefinition().getScriptId());
        payload.put("label", configuration.getLabel());
        payload.put("enabled", configuration.isEnabled());
        payload.put("intervalSeconds", configuration.getIntervalSeconds());
        payload.put("sortOrder", configuration.getSortOrder());
        payload.put("parameters", parameters(configuration.getParametersJson()));
        return payload;
    }

    private StatusCheckConfiguration resolveConfiguration(UUID machineId, UUID configurationId) {
        return repository.findByIdAndMachineId(configurationId, machineId)
            .orElseThrow(() -> new IllegalArgumentException("Status check configuration not found"));
    }

    private ScriptDefinition resolveScriptDefinition(UUID machineId, UUID scriptDefinitionId) {
        if (scriptDefinitionId == null) {
            throw new IllegalArgumentException("scriptDefinitionId is required");
        }
        ScriptDefinition scriptDefinition = scriptDefinitionRepository.findById(scriptDefinitionId)
            .filter(ScriptDefinition::isActive)
            .orElseThrow(() -> new IllegalArgumentException("Script definition is not active"));
        if (!scriptDefinition.getAgent().getMachine().getId().equals(machineId)) {
            throw new IllegalArgumentException("Script definition does not belong to the selected machine");
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

    private int intervalSeconds(Integer value) {
        return value == null || value < 1 ? 30 : value;
    }

    private String parametersJson(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters == null ? Map.of() : parameters);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize parameters", exception);
        }
    }

    private Map<String, Object> parameters(String parametersJson) {
        try {
            return objectMapper.readValue(parametersJson, PARAMETER_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not parse stored parameters", exception);
        }
    }
}
