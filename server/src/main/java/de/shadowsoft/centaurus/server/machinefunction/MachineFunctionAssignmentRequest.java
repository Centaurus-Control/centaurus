package de.shadowsoft.centaurus.server.machinefunction;

import java.util.UUID;

public record MachineFunctionAssignmentRequest(
    UUID scriptConfigurationId
) {
}
