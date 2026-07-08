package de.shadowsoft.centaurus.server.machinefunction;

import de.shadowsoft.centaurus.server.scriptconfig.ScriptButtonConfigurationResponse;

public record MachineFunctionResponse(
    MachineFunctionType type,
    boolean enabled,
    ScriptButtonConfigurationResponse scriptConfiguration
) {
}
