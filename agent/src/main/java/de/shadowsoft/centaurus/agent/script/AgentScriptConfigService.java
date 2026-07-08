package de.shadowsoft.centaurus.agent.script;

import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import de.shadowsoft.centaurus.agent.config.AgentScriptConfig;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentScriptConfigService {

    private final AgentConfigStore configStore;
    private final ScriptManifestPublisher manifestPublisher;

    public AgentScriptConfigService(AgentConfigStore configStore, ScriptManifestPublisher manifestPublisher) {
        this.configStore = configStore;
        this.manifestPublisher = manifestPublisher;
    }

    public List<AgentScriptConfig> listScripts() {
        return configStore.load().getScripts()
            .stream()
            .sorted(Comparator.comparing(script -> script.getLabel() == null ? "" : script.getLabel()))
            .toList();
    }

    public AgentScriptConfig saveScript(AgentScriptConfig script) {
        validate(script);
        AgentConfig config = configStore.load();
        if (script.getId() == null) {
            script.setId(UUID.randomUUID());
        }
        List<AgentScriptConfig> scripts = new java.util.ArrayList<>(config.getScripts());
        scripts.removeIf(existing -> script.getId().equals(existing.getId()));
        scripts.add(script);
        config.setScripts(scripts);
        configStore.save(config);
        publishIfEnrolled(config);
        return script;
    }

    public void deleteScript(UUID scriptId) {
        AgentConfig config = configStore.load();
        List<AgentScriptConfig> scripts = new java.util.ArrayList<>(config.getScripts());
        scripts.removeIf(existing -> scriptId.equals(existing.getId()));
        config.setScripts(scripts);
        configStore.save(config);
        publishIfEnrolled(config);
    }

    public void publishManifest() {
        manifestPublisher.publish();
    }

    private void publishIfEnrolled(AgentConfig config) {
        if (config.getAgentId() != null) {
            manifestPublisher.publish();
        }
    }

    private void validate(AgentScriptConfig script) {
        if (!StringUtils.hasText(script.getLabel())) {
            throw new IllegalArgumentException("label is required");
        }
        if (!StringUtils.hasText(script.getCommand())) {
            throw new IllegalArgumentException("command is required");
        }
        if (script.getTimeoutSeconds() <= 0) {
            script.setTimeoutSeconds(900);
        }
    }
}
