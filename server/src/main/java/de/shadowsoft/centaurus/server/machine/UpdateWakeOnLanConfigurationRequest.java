package de.shadowsoft.centaurus.server.machine;

import java.util.UUID;

public record UpdateWakeOnLanConfigurationRequest(
    boolean enabled,
    UUID primaryWolInterfaceId
) {
}
