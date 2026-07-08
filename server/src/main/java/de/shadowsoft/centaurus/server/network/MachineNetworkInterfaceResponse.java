package de.shadowsoft.centaurus.server.network;

import java.time.Instant;
import java.util.UUID;

public record MachineNetworkInterfaceResponse(
    UUID id,
    UUID machineId,
    UUID agentId,
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
    boolean wolCandidate,
    Instant lastSeenAt
) {

    public static MachineNetworkInterfaceResponse from(MachineNetworkInterface networkInterface) {
        return new MachineNetworkInterfaceResponse(
            networkInterface.getId(),
            networkInterface.getMachine().getId(),
            networkInterface.getAgent().getId(),
            networkInterface.getInterfaceName(),
            networkInterface.getDisplayName(),
            networkInterface.getMacAddress(),
            networkInterface.getIpAddress(),
            networkInterface.getPrefixLength(),
            networkInterface.getFamily(),
            networkInterface.isUp(),
            networkInterface.isLoopback(),
            networkInterface.isVirtual(),
            networkInterface.isWireless(),
            networkInterface.isDefaultRoute(),
            networkInterface.isWolCandidate(),
            networkInterface.getLastSeenAt()
        );
    }
}
