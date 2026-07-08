package de.shadowsoft.centaurus.server.script;

import java.time.Instant;
import java.util.UUID;

public record ScriptDefinitionResponse(
    UUID id,
    UUID agentId,
    UUID scriptId,
    String label,
    String description,
    long manifestVersion,
    String parameterSchemaJson,
    String resultSchemaJson,
    Instant updatedAt
) {

    public static ScriptDefinitionResponse from(ScriptDefinition definition) {
        return new ScriptDefinitionResponse(
            definition.getId(),
            definition.getAgent().getId(),
            definition.getScriptId(),
            definition.getLabel(),
            definition.getDescription(),
            definition.getManifestVersion(),
            definition.getParameterSchemaJson(),
            definition.getResultSchemaJson(),
            definition.getUpdatedAt()
        );
    }
}
