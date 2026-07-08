package de.shadowsoft.centaurus.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class AgentConfigStore {

    private final AgentProperties agentProperties;
    private final ObjectMapper yamlMapper;

    public AgentConfigStore(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public AgentConfig load() {
        Path configPath = Path.of(agentProperties.getConfigPath());
        if (!Files.exists(configPath)) {
            return new AgentConfig();
        }
        try {
            return yamlMapper.readValue(configPath.toFile(), AgentConfig.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load agent config from " + configPath, exception);
        }
    }

    public void save(AgentConfig config) {
        Path configPath = Path.of(agentProperties.getConfigPath());
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            yamlMapper.writeValue(configPath.toFile(), config);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save agent config to " + configPath, exception);
        }
    }
}
