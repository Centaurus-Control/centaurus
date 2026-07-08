package de.shadowsoft.centaurus.server.enrollment;

import de.shadowsoft.centaurus.server.audit.AuditResult;
import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentCapability;
import de.shadowsoft.centaurus.server.agent.AgentCapabilityRepository;
import de.shadowsoft.centaurus.server.identity.AgentIdentityKey;
import de.shadowsoft.centaurus.server.identity.AgentIdentityKeyRepository;
import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.machine.MachineRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentEnrollmentService {

    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final int STATS_INTERVAL_SECONDS = 30;

    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final EnrollmentTokenSecretService tokenSecretService;
    private final MachineRepository machineRepository;
    private final de.shadowsoft.centaurus.server.agent.AgentRepository agentRepository;
    private final AgentIdentityKeyRepository agentIdentityKeyRepository;
    private final AgentCapabilityRepository agentCapabilityRepository;
    private final EnrollmentProperties enrollmentProperties;
    private final AuditService auditService;

    public AgentEnrollmentService(
        EnrollmentTokenRepository enrollmentTokenRepository,
        EnrollmentTokenSecretService tokenSecretService,
        MachineRepository machineRepository,
        de.shadowsoft.centaurus.server.agent.AgentRepository agentRepository,
        AgentIdentityKeyRepository agentIdentityKeyRepository,
        AgentCapabilityRepository agentCapabilityRepository,
        EnrollmentProperties enrollmentProperties,
        AuditService auditService
    ) {
        this.enrollmentTokenRepository = enrollmentTokenRepository;
        this.tokenSecretService = tokenSecretService;
        this.machineRepository = machineRepository;
        this.agentRepository = agentRepository;
        this.agentIdentityKeyRepository = agentIdentityKeyRepository;
        this.agentCapabilityRepository = agentCapabilityRepository;
        this.enrollmentProperties = enrollmentProperties;
        this.auditService = auditService;
    }

    @Transactional
    public AgentEnrollmentResponse enroll(AgentEnrollmentRequest request) {
        validateRequest(request);
        Instant now = Instant.now();
        EnrollmentToken token = enrollmentTokenRepository.findByTokenHash(tokenSecretService.hash(request.enrollmentToken()))
            .orElseThrow(() -> {
                auditService.record(
                    "AGENT_REGISTRATION",
                    AuditResult.FAILURE,
                    null,
                    null,
                    "AGENT",
                    null,
                    request.hostname(),
                    AuditService.details("reason", "invalid_token", "hostname", request.hostname(), "agentVersion", request.agentVersion())
                );
                return new AgentEnrollmentException("Invalid enrollment token");
            });
        if (token.isUsed()) {
            auditService.record(
                "AGENT_REGISTRATION",
                AuditResult.FAILURE,
                null,
                null,
                "ENROLLMENT_TOKEN",
                token.getId(),
                token.getSuggestedName(),
                AuditService.details("reason", "token_used", "hostname", request.hostname(), "agentVersion", request.agentVersion())
            );
            throw new AgentEnrollmentException("Enrollment token was already used");
        }
        if (token.isExpired(now)) {
            auditService.record(
                "AGENT_REGISTRATION",
                AuditResult.FAILURE,
                null,
                null,
                "ENROLLMENT_TOKEN",
                token.getId(),
                token.getSuggestedName(),
                AuditService.details("reason", "token_expired", "hostname", request.hostname(), "agentVersion", request.agentVersion())
            );
            throw new AgentEnrollmentException("Enrollment token is expired");
        }

        String displayName = StringUtils.hasText(request.displayName())
            ? request.displayName()
            : token.getSuggestedName();
        if (!StringUtils.hasText(displayName)) {
            displayName = request.hostname();
        }
        String resolvedDisplayName = displayName;

        Agent agent = agentRepository.findByInstallationId(request.installationId())
            .map(existingAgent -> reactivateDeletedAgent(existingAgent, resolvedDisplayName, request))
            .orElseGet(() -> createAgent(resolvedDisplayName, request));
        Machine machine = agent.getMachine();
        agentIdentityKeyRepository.save(new AgentIdentityKey(agent, request.agentKeyId(), request.agentPublicKey()));
        agentCapabilityRepository.deleteByAgentId(agent.getId());
        if (request.capabilities() != null) {
            for (var capability : request.capabilities()) {
                agentCapabilityRepository.save(new AgentCapability(agent, capability, true));
            }
        }
        token.markUsed(agent, now);
        auditService.record(
            "AGENT_REGISTRATION",
            AuditResult.SUCCESS,
            null,
            null,
            "AGENT",
            agent.getId(),
            agent.getDisplayName(),
            AuditService.details(
                "machineId", machine.getId(),
                "machineName", machine.getDisplayName(),
                "hostname", request.hostname(),
                "agentVersion", request.agentVersion(),
                "enrollmentTokenId", token.getId()
            )
        );

        return new AgentEnrollmentResponse(
            agent.getId(),
            machine.getId(),
            enrollmentProperties.getWsUrl(),
            HEARTBEAT_INTERVAL_SECONDS,
            STATS_INTERVAL_SECONDS
        );
    }

    private Agent createAgent(String displayName, AgentEnrollmentRequest request) {
        Machine machine = machineRepository.save(new Machine(displayName, request.hostname()));
        return agentRepository.save(new Agent(
            machine,
            request.installationId(),
            displayName,
            request.hostname(),
            request.agentVersion()
        ));
    }

    private Agent reactivateDeletedAgent(Agent agent, String displayName, AgentEnrollmentRequest request) {
        if (!agent.isDeleted()) {
            throw new AgentEnrollmentException("Agent installation is already registered");
        }
        Machine machine = agent.getMachine();
        machine.reactivate(displayName, request.hostname());
        agent.reactivate(displayName, request.hostname(), request.agentVersion());
        return agent;
    }

    private void validateRequest(AgentEnrollmentRequest request) {
        if (!StringUtils.hasText(request.enrollmentToken())) {
            throw new AgentEnrollmentException("Enrollment token is required");
        }
        if (request.installationId() == null) {
            throw new AgentEnrollmentException("Installation ID is required");
        }
        if (!StringUtils.hasText(request.agentPublicKey())) {
            throw new AgentEnrollmentException("Agent public key is required");
        }
        if (!StringUtils.hasText(request.agentKeyId())) {
            throw new AgentEnrollmentException("Agent key ID is required");
        }
        if (!StringUtils.hasText(request.agentVersion())) {
            throw new AgentEnrollmentException("Agent version is required");
        }
        if (!StringUtils.hasText(request.hostname())) {
            throw new AgentEnrollmentException("Hostname is required");
        }
    }
}
