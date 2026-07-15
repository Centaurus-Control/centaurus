package de.shadowsoft.centaurus.agent.enrollment;

import de.shadowsoft.centaurus.agent.connection.AgentConnectionService;
import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import de.shadowsoft.centaurus.agent.config.AgentProperties;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentEnrollmentService {

    private final EnrollmentBundleParser bundleParser;
    private final AgentIdentityService identityService;
    private final AgentEnrollmentClient enrollmentClient;
    private final AgentConfigStore configStore;
    private final AgentProperties agentProperties;
    private final AgentConnectionService agentConnectionService;

    public AgentEnrollmentService(
        EnrollmentBundleParser bundleParser,
        AgentIdentityService identityService,
        AgentEnrollmentClient enrollmentClient,
        AgentConfigStore configStore,
        AgentProperties agentProperties,
        AgentConnectionService agentConnectionService
    ) {
        this.bundleParser = bundleParser;
        this.identityService = identityService;
        this.enrollmentClient = enrollmentClient;
        this.configStore = configStore;
        this.agentProperties = agentProperties;
        this.agentConnectionService = agentConnectionService;
    }

    public EnrollAgentResponse enroll(EnrollAgentRequest request) {
        AgentConfig existingConfig = configStore.load();
        if (existingConfig.getAgentId() != null) {
            throw new EnrollmentException("Agent is already enrolled");
        }

        EnrollmentBundle bundle = bundleParser.parse(request.enrollmentBundle());
        AgentIdentity identity = identityService.generateIdentity();
        UUID installationId = existingConfig.getInstallationId() == null
            ? UUID.randomUUID()
            : existingConfig.getInstallationId();
        String hostname = hostname();
        String displayName = resolveDisplayName(request, bundle, hostname);

        AgentEnrollmentRequest enrollmentRequest = new AgentEnrollmentRequest(
            bundle.enrollmentToken(),
            installationId,
            identity.publicKey(),
            identity.keyId(),
            agentProperties.getVersion(),
            hostname,
            displayName,
            List.of(AgentCapabilityType.STATS, AgentCapabilityType.SCRIPT_EXECUTION)
        );
        AgentEnrollmentResponse response = enrollmentClient.enroll(bundle.serverUrl(), enrollmentRequest);

        AgentConfig config = new AgentConfig();
        config.setInstallationId(installationId);
        config.setAgentId(response.agentId());
        config.setMachineId(response.machineId());
        config.setServerUrl(bundle.serverUrl());
        config.setWsUrl(response.wsUrl());
        config.setServerPublicKey(bundle.serverPublicKey());
        config.setServerKeyId(bundle.serverKeyId());
        config.setAgentPrivateKey(identity.privateKey());
        config.setAgentPublicKey(identity.publicKey());
        config.setAgentKeyId(identity.keyId());
        config.setHeartbeatIntervalSeconds(response.heartbeatIntervalSeconds());
        config.setStatsIntervalSeconds(response.statsIntervalSeconds());
        config.setScripts(existingConfig.getScripts());
        configStore.save(config);
        agentConnectionService.connectNow();

        return new EnrollAgentResponse(response.agentId(), response.machineId(), bundle.serverUrl(), response.wsUrl());
    }

    private String resolveDisplayName(EnrollAgentRequest request, EnrollmentBundle bundle, String hostname) {
        if (StringUtils.hasText(request.displayName())) {
            return request.displayName();
        }
        if (StringUtils.hasText(bundle.suggestedName())) {
            return bundle.suggestedName();
        }
        return hostname;
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "unknown-host";
        }
    }
}
