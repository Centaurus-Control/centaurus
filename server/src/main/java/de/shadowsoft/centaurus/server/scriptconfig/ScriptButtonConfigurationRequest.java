package de.shadowsoft.centaurus.server.scriptconfig;

import java.util.Map;
import java.util.UUID;

public record ScriptButtonConfigurationRequest(
    UUID scriptDefinitionId,
    String label,
    Boolean enabled,
    Integer sortOrder,
    Map<String, Object> parameters
) {
}
