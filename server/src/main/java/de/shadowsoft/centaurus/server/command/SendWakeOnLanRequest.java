package de.shadowsoft.centaurus.server.command;

public record SendWakeOnLanRequest(
    String macAddress,
    String broadcastAddress,
    Integer port
) {
}
