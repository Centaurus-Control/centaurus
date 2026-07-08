package de.shadowsoft.centaurus.server.command;

import java.util.Map;
import java.util.UUID;

public record ExecuteScriptRequest(
    UUID scriptId,
    Map<String, Object> parameters
) {
}
