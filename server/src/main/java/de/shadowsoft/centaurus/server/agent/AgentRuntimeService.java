package de.shadowsoft.centaurus.server.agent;

import de.shadowsoft.centaurus.server.audit.AuditResult;
import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.identity.AgentIdentityKey;
import de.shadowsoft.centaurus.server.identity.AgentIdentityKeyRepository;
import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.machine.MachineStatusChangedEvent;
import de.shadowsoft.centaurus.server.machine.MachineRepository;
import de.shadowsoft.centaurus.server.machine.MachineStatus;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentRuntimeService {

    private final AgentRepository agentRepository;
    private final AgentIdentityKeyRepository agentIdentityKeyRepository;
    private final MachineRepository machineRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public AgentRuntimeService(
        AgentRepository agentRepository,
        AgentIdentityKeyRepository agentIdentityKeyRepository,
        MachineRepository machineRepository,
        AuditService auditService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.agentRepository = agentRepository;
        this.agentIdentityKeyRepository = agentIdentityKeyRepository;
        this.machineRepository = machineRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Agent authenticate(UUID agentId, String agentKeyId, String signedPayload, String signatureValue) {
        AgentIdentityKey identityKey = agentIdentityKeyRepository.findByAgentIdAndKeyIdAndActiveTrue(agentId, agentKeyId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown active agent identity key"));
        if (!verify(identityKey.getPublicKey(), signedPayload, signatureValue)) {
            throw new IllegalArgumentException("Invalid agent signature");
        }
        Instant now = Instant.now();
        identityKey.markUsed(now);
        Agent agent = identityKey.getAgent();
        Machine machine = agent.getMachine();
        if (agent.isDeleted() || machine.isDeleted()) {
            throw new IllegalArgumentException("Agent was removed from the server");
        }
        boolean wasOnline = agent.getStatus() == AgentStatus.ONLINE;
        MachineStatus previousMachineStatus = machine.getStatus();
        agent.markConnected(now);
        machine.changeStatus(MachineStatus.ONLINE);
        machine.markSeen(now);
        if (!wasOnline) {
            auditService.record(
                "AGENT_ONLINE",
                AuditResult.SUCCESS,
                null,
                null,
                "AGENT",
                agent.getId(),
                agent.getDisplayName(),
                AuditService.details("machineId", machine.getId(), "machineName", machine.getDisplayName())
            );
        }
        publishMachineStatusChange(machine, previousMachineStatus, now);
        return agent;
    }

    @Transactional
    public void markHello(UUID agentId, String hostname, String agentVersion) {
        markSeen(agentId, hostname, agentVersion);
    }

    @Transactional
    public void markHeartbeat(UUID agentId) {
        markSeen(agentId, null, null);
    }

    @Transactional
    public void markDisconnected(UUID agentId) {
        agentRepository.findById(agentId).filter(agent -> !agent.isDeleted()).ifPresent(agent -> {
            boolean wasOnline = agent.getStatus() == AgentStatus.ONLINE;
            agent.changeStatus(AgentStatus.OFFLINE);
            Machine machine = agent.getMachine();
            MachineStatus previousMachineStatus = machine.getStatus();
            machine.changeStatus(MachineStatus.OFFLINE);
            if (wasOnline) {
                auditService.record(
                    "AGENT_OFFLINE",
                    AuditResult.SUCCESS,
                    null,
                    null,
                    "AGENT",
                    agent.getId(),
                    agent.getDisplayName(),
                    AuditService.details("machineId", machine.getId(), "machineName", machine.getDisplayName())
                );
            }
            publishMachineStatusChange(machine, previousMachineStatus, Instant.now());
        });
    }

    private void markSeen(UUID agentId, String hostname, String agentVersion) {
        Agent agent = agentRepository.findById(agentId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Unknown agent"));
        Instant now = Instant.now();
        agent.markSeen(now);
        Machine machine = agent.getMachine();
        if (machine.isDeleted()) {
            throw new IllegalArgumentException("Unknown agent");
        }
        MachineStatus previousMachineStatus = machine.getStatus();
        machine.markSeen(now);
        updateRuntimeAttributes(agent, machine, hostname, agentVersion);
        if (agent.getStatus() != AgentStatus.ONLINE) {
            agent.changeStatus(AgentStatus.ONLINE);
            auditService.record(
                "AGENT_ONLINE",
                AuditResult.SUCCESS,
                null,
                null,
                "AGENT",
                agent.getId(),
                agent.getDisplayName(),
                AuditService.details("machineId", machine.getId(), "machineName", machine.getDisplayName())
            );
        }
        if (machine.getStatus() != MachineStatus.ONLINE) {
            machine.changeStatus(MachineStatus.ONLINE);
        }
        publishMachineStatusChange(machine, previousMachineStatus, now);
    }

    private void updateRuntimeAttributes(Agent agent, Machine machine, String hostname, String agentVersion) {
        String resolvedHostname = StringUtils.hasText(hostname) ? hostname.trim() : agent.getHostname();
        String resolvedAgentVersion = StringUtils.hasText(agentVersion) ? agentVersion.trim() : agent.getAgentVersion();
        if (!resolvedHostname.equals(agent.getHostname()) || !resolvedAgentVersion.equals(agent.getAgentVersion())) {
            agent.updateRuntimeAttributes(resolvedHostname, resolvedAgentVersion);
        }
        if (!resolvedHostname.equals(machine.getHostname())) {
            machine.updateHostname(resolvedHostname);
        }
    }

    private void publishMachineStatusChange(Machine machine, MachineStatus previousStatus, Instant changedAt) {
        if (machine.getStatus() != previousStatus) {
            eventPublisher.publishEvent(new MachineStatusChangedEvent(machine.getId(), machine.getStatus(), changedAt));
        }
    }

    private boolean verify(String publicKeyValue, String payload, String signatureValue) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyValue);
            PublicKey publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureValue));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }
}
