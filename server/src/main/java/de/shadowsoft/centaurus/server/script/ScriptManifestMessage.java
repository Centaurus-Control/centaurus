package de.shadowsoft.centaurus.server.script;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScriptManifestMessage(
    long manifestVersion,
    String manifestHash,
    List<ScriptEntry> scripts
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScriptEntry(
        UUID id,
        String label,
        String description,
        JsonNode parameters,
        JsonNode resultSchema
    ) {
    }
}
