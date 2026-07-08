package de.shadowsoft.centaurus.server.statuscheck;

import java.time.Instant;
import java.util.UUID;

public record StatusCheckConfigurationResponse(
    UUID id,
    UUID machineId,
    UUID scriptDefinitionId,
    UUID agentId,
    UUID scriptId,
    String label,
    boolean enabled,
    int intervalSeconds,
    int sortOrder,
    String parametersJson,
    Instant createdAt,
    Instant updatedAt
) {

    public static StatusCheckConfigurationResponse from(StatusCheckConfiguration configuration) {
        return new StatusCheckConfigurationResponse(
            configuration.getId(),
            configuration.getMachine().getId(),
            configuration.getScriptDefinition().getId(),
            configuration.getScriptDefinition().getAgent().getId(),
            configuration.getScriptDefinition().getScriptId(),
            configuration.getLabel(),
            configuration.isEnabled(),
            configuration.getIntervalSeconds(),
            configuration.getSortOrder(),
            configuration.getParametersJson(),
            configuration.getCreatedAt(),
            configuration.getUpdatedAt()
        );
    }
}
