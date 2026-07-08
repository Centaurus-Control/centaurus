package de.shadowsoft.centaurus.server.machine;

import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentCapabilityRepository;
import de.shadowsoft.centaurus.server.agent.AgentCapabilityType;
import de.shadowsoft.centaurus.server.agent.AgentNotFoundException;
import de.shadowsoft.centaurus.server.agent.AgentRepository;
import de.shadowsoft.centaurus.server.agentws.AgentConnectionRegistry;
import de.shadowsoft.centaurus.server.identity.AgentIdentityKey;
import de.shadowsoft.centaurus.server.identity.AgentIdentityKeyRepository;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterface;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterfaceRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MachineManagementService {

    private final MachineRepository machineRepository;
    private final AgentRepository agentRepository;
    private final AgentCapabilityRepository agentCapabilityRepository;
    private final MachineNetworkInterfaceRepository machineNetworkInterfaceRepository;
    private final AgentIdentityKeyRepository agentIdentityKeyRepository;
    private final AgentConnectionRegistry agentConnectionRegistry;
    private final AuditService auditService;

    public MachineManagementService(
        MachineRepository machineRepository,
        AgentRepository agentRepository,
        AgentCapabilityRepository agentCapabilityRepository,
        MachineNetworkInterfaceRepository machineNetworkInterfaceRepository,
        AgentIdentityKeyRepository agentIdentityKeyRepository,
        AgentConnectionRegistry agentConnectionRegistry,
        AuditService auditService
    ) {
        this.machineRepository = machineRepository;
        this.agentRepository = agentRepository;
        this.agentCapabilityRepository = agentCapabilityRepository;
        this.machineNetworkInterfaceRepository = machineNetworkInterfaceRepository;
        this.agentIdentityKeyRepository = agentIdentityKeyRepository;
        this.agentConnectionRegistry = agentConnectionRegistry;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> listMachines() {
        return machineRepository.findByDeletedFalse()
            .stream()
            .sorted(Comparator.comparing(Machine::getCreatedAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public MachineResponse getMachine(UUID machineId) {
        Machine machine = machineRepository.findById(machineId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        return toResponse(machine);
    }

    @Transactional
    public MachineResponse renameMachine(UUID machineId, RenameMachineRequest request, Authentication authentication) {
        if (request == null) {
            throw new MachineManagementException("Display name is required");
        }
        String displayName = request.displayName() == null ? "" : request.displayName().trim();
        if (!StringUtils.hasText(displayName)) {
            throw new MachineManagementException("Display name is required");
        }
        if (displayName.length() > 255) {
            throw new MachineManagementException("Display name must not exceed 255 characters");
        }

        Machine machine = machineRepository.findById(machineId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        String previousDisplayName = machine.getDisplayName();
        machine.rename(displayName);
        auditService.recordSuccess(
            "MACHINE_RENAMED",
            authentication,
            "MACHINE",
            machine.getId(),
            machine.getDisplayName(),
            AuditService.details("previousDisplayName", previousDisplayName, "displayName", displayName)
        );
        return toResponse(machine);
    }

    @Transactional
    public MachineResponse updateWakeOnLanConfiguration(
        UUID machineId,
        UpdateWakeOnLanConfigurationRequest request,
        Authentication authentication
    ) {
        Machine machine = machineRepository.findById(machineId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        UUID primaryWolInterfaceId = request.enabled() ? requireWakeOnLanInterface(machineId, request.primaryWolInterfaceId()) : null;
        machine.configureWakeOnLan(request.enabled(), primaryWolInterfaceId);
        auditService.recordSuccess(
            "MACHINE_WOL_CONFIGURATION_CHANGED",
            authentication,
            "MACHINE",
            machine.getId(),
            machine.getDisplayName(),
            AuditService.details("enabled", request.enabled(), "primaryWolInterfaceId", primaryWolInterfaceId)
        );
        return toResponse(machine);
    }

    @Transactional
    public void removeMachine(UUID machineId, Authentication authentication) {
        Machine machine = machineRepository.findById(machineId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new MachineNotFoundException("Machine not found"));
        Agent agent = agentRepository.findByMachineId(machineId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new MachineManagementException("Machine has no registered agent"));
        removeAgent(machine, agent, authentication);
    }

    @Transactional
    public void removeAgent(UUID agentId, Authentication authentication) {
        Agent agent = agentRepository.findById(agentId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new AgentNotFoundException("Agent not found"));
        removeAgent(agent.getMachine(), agent, authentication);
    }

    private void removeAgent(Machine machine, Agent agent, Authentication authentication) {
        UUID machineId = machine.getId();
        UUID agentId = agent.getId();
        Instant now = Instant.now();

        agentConnectionRegistry.disconnect(agentId);
        for (AgentIdentityKey identityKey : agentIdentityKeyRepository.findByAgentIdAndActiveTrue(agentId)) {
            identityKey.revoke(now);
        }
        agent.markDeleted(now);
        machine.markDeleted(now);

        auditService.recordSuccess(
            "AGENT_REMOVED",
            authentication,
            "AGENT",
            agentId,
            agent.getDisplayName(),
            AuditService.details(
                "machineId", machineId,
                "machineName", machine.getDisplayName(),
                "installationId", agent.getInstallationId(),
                "hostname", machine.getHostname()
            )
        );
    }

    private UUID requireWakeOnLanInterface(UUID machineId, UUID primaryWolInterfaceId) {
        if (primaryWolInterfaceId == null) {
            throw new MachineManagementException("primaryWolInterfaceId is required when Wake-on-LAN is enabled");
        }
        MachineNetworkInterface networkInterface = machineNetworkInterfaceRepository.findById(primaryWolInterfaceId)
            .filter(candidate -> candidate.getMachine().getId().equals(machineId))
            .orElseThrow(() -> new MachineManagementException("Wake-on-LAN interface does not belong to the selected machine"));
        if (networkInterface.getMacAddress() == null || networkInterface.getMacAddress().isBlank()) {
            throw new MachineManagementException("Wake-on-LAN interface has no MAC address");
        }
        return networkInterface.getId();
    }

    private MachineResponse toResponse(Machine machine) {
        AgentSummaryResponse agent = agentRepository.findByMachineIdAndDeletedFalse(machine.getId())
            .map(this::toAgentResponse)
            .orElse(null);
        return MachineResponse.from(machine, agent);
    }

    private AgentSummaryResponse toAgentResponse(Agent agent) {
        List<AgentCapabilityType> capabilities = agentCapabilityRepository.findByAgentId(agent.getId())
            .stream()
            .filter(capability -> capability.isEnabled())
            .map(capability -> capability.getCapability())
            .sorted()
            .toList();
        return AgentSummaryResponse.from(agent, capabilities);
    }
}
