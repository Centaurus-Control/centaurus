package de.shadowsoft.centaurus.server.scriptconfig;

import java.time.Instant;
import java.util.UUID;

public record ScriptButtonConfigurationResponse(
    UUID id,
    UUID machineId,
    UUID scriptDefinitionId,
    UUID agentId,
    UUID scriptId,
    String label,
    boolean enabled,
    int sortOrder,
    String parametersJson,
    Instant createdAt,
    Instant updatedAt
) {

    public static ScriptButtonConfigurationResponse from(ScriptButtonConfiguration configuration) {
        return new ScriptButtonConfigurationResponse(
            configuration.getId(),
            configuration.getMachine().getId(),
            configuration.getScriptDefinition().getId(),
            configuration.getScriptDefinition().getAgent().getId(),
            configuration.getScriptDefinition().getScriptId(),
            configuration.getLabel(),
            configuration.isEnabled(),
            configuration.getSortOrder(),
            configuration.getParametersJson(),
            configuration.getCreatedAt(),
            configuration.getUpdatedAt()
        );
    }
}
