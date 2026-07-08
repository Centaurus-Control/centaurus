package de.shadowsoft.centaurus.server.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkInterfaceSnapshotMessage(
    Instant sampledAt,
    List<NetworkInterfaceEntry> interfaces
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NetworkInterfaceEntry(
        String interfaceName,
        String displayName,
        String macAddress,
        String ipAddress,
        Integer prefixLength,
        IpAddressFamily family,
        boolean up,
        boolean loopback,
        boolean virtual,
        boolean wireless,
        boolean defaultRoute,
        boolean wolCandidate
    ) {
    }
}
