package de.shadowsoft.centaurus.server.script;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScriptManifestIngestionService {

    private final AgentRepository agentRepository;
    private final ScriptManifestRepository scriptManifestRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ObjectMapper objectMapper;

    public ScriptManifestIngestionService(
        AgentRepository agentRepository,
        ScriptManifestRepository scriptManifestRepository,
        ScriptDefinitionRepository scriptDefinitionRepository,
        ObjectMapper objectMapper
    ) {
        this.agentRepository = agentRepository;
        this.scriptManifestRepository = scriptManifestRepository;
        this.scriptDefinitionRepository = scriptDefinitionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ingest(UUID agentId, ScriptManifestMessage message) {
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown agent"));
        scriptManifestRepository.save(new ScriptManifest(agent, message.manifestVersion(), message.manifestHash()));
        Set<UUID> activeScriptIds = new HashSet<>();
        if (message.scripts() != null) {
            for (ScriptManifestMessage.ScriptEntry script : message.scripts()) {
                activeScriptIds.add(script.id());
                String parameters = toJson(script.parameters());
                String resultSchema = toJson(script.resultSchema());
                scriptDefinitionRepository.findByAgentIdAndScriptId(agentId, script.id())
                    .ifPresentOrElse(
                        existing -> existing.updateFromManifest(
                            script.label(),
                            script.description(),
                            message.manifestVersion(),
                            parameters,
                            resultSchema
                        ),
                        () -> {
                            ScriptDefinition definition = new ScriptDefinition(
                                agent,
                                script.id(),
                                script.label(),
                                message.manifestVersion(),
                                parameters,
                                resultSchema
                            );
                            definition.updateFromManifest(
                                script.label(),
                                script.description(),
                                message.manifestVersion(),
                                parameters,
                                resultSchema
                            );
                            scriptDefinitionRepository.save(definition);
                        }
                    );
            }
        }
        scriptDefinitionRepository.findByAgentIdAndActiveTrue(agentId)
            .stream()
            .filter(definition -> !activeScriptIds.contains(definition.getScriptId()))
            .forEach(ScriptDefinition::deactivate);
    }

    private String toJson(Object value) {
        try {
            if (value == null) {
                return "{}";
            }
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize script manifest JSON", exception);
        }
    }
}
