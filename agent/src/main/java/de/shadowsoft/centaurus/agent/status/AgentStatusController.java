package de.shadowsoft.centaurus.agent.status;

import de.shadowsoft.centaurus.agent.connection.AgentConnectionService;
import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import de.shadowsoft.centaurus.agent.config.AgentProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentStatusController {

    private final AgentConfigStore configStore;
    private final AgentProperties agentProperties;
    private final AgentConnectionService agentConnectionService;

    public AgentStatusController(
        AgentConfigStore configStore,
        AgentProperties agentProperties,
        AgentConnectionService agentConnectionService
    ) {
        this.configStore = configStore;
        this.agentProperties = agentProperties;
        this.agentConnectionService = agentConnectionService;
    }

    @GetMapping("/status")
    public AgentStatusResponse getStatus() {
        AgentConfig config = configStore.load();
        return new AgentStatusResponse(
            config.getAgentId() != null,
            config.getAgentId(),
            config.getMachineId(),
            config.getServerUrl(),
            config.getWsUrl(),
            agentProperties.getConfigPath(),
            agentProperties.getLocalUi().isRemoteAccessEnabled(),
            agentConnectionService.isConnected(),
            agentConnectionService.isAuthenticated()
        );
    }
}
