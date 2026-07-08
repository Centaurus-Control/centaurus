package de.shadowsoft.centaurus.agent.script;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import de.shadowsoft.centaurus.agent.config.AgentScriptConfig;
import de.shadowsoft.centaurus.agent.connection.AgentRuntimeMessenger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ScriptManifestPublisher {

    private final AgentConfigStore configStore;
    private final AgentRuntimeMessenger messenger;
    private final ObjectMapper objectMapper;

    public ScriptManifestPublisher(
        AgentConfigStore configStore,
        AgentRuntimeMessenger messenger,
        ObjectMapper objectMapper
    ) {
        this.configStore = configStore;
        this.messenger = messenger;
        this.objectMapper = objectMapper;
    }

    public void publish() {
        AgentConfig config = configStore.load();
        List<Map<String, Object>> scripts = config.getScripts()
            .stream()
            .filter(script -> script.getId() != null && script.getLabel() != null && script.getCommand() != null)
            .map(this::toManifestEntry)
            .toList();
        String manifestHash = manifestHash(scripts);
        messenger.send(Map.of(
            "type", "SCRIPT_MANIFEST",
            "manifestVersion", Instant.now().getEpochSecond(),
            "manifestHash", manifestHash,
            "scripts", scripts
        ));
    }

    private Map<String, Object> toManifestEntry(AgentScriptConfig script) {
        return new java.util.LinkedHashMap<>(Map.of(
            "id", script.getId(),
            "label", script.getLabel(),
            "description", script.getDescription() == null ? "" : script.getDescription(),
            "parameters", script.getParameters(),
            "resultSchema", script.getResultSchema()
        ));
    }

    private String manifestHash(Object manifest) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(manifest);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not hash script manifest", exception);
        }
    }
}
