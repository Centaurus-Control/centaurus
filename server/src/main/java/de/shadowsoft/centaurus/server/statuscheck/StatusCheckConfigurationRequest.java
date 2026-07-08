package de.shadowsoft.centaurus.server.statuscheck;

import java.util.Map;
import java.util.UUID;

public record StatusCheckConfigurationRequest(
    UUID scriptDefinitionId,
    String label,
    Boolean enabled,
    Integer intervalSeconds,
    Integer sortOrder,
    Map<String, Object> parameters
) {
}
