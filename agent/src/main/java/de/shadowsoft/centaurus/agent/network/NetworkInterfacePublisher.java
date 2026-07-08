package de.shadowsoft.centaurus.agent.network;

import de.shadowsoft.centaurus.agent.connection.AgentRuntimeMessenger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NetworkInterfacePublisher {

    private final AgentRuntimeMessenger messenger;

    public NetworkInterfacePublisher(AgentRuntimeMessenger messenger) {
        this.messenger = messenger;
    }

    public void publish() {
        if (!messenger.isConnected()) {
            return;
        }
        messenger.send(Map.of(
            "type", "NETWORK_INTERFACES",
            "sampledAt", Instant.now(),
            "interfaces", interfaces()
        ));
    }

    private List<Map<String, Object>> interfaces() {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    if (!(address.getAddress() instanceof Inet4Address) && !(address.getAddress() instanceof Inet6Address)) {
                        continue;
                    }
                    Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("interfaceName", networkInterface.getName());
                    entry.put("displayName", networkInterface.getDisplayName() == null ? networkInterface.getName() : networkInterface.getDisplayName());
                    entry.put("macAddress", macAddress(networkInterface));
                    entry.put("ipAddress", address.getAddress().getHostAddress());
                    entry.put("prefixLength", (int) address.getNetworkPrefixLength());
                    entry.put("family", address.getAddress() instanceof Inet4Address ? "IPV4" : "IPV6");
                    entry.put("up", networkInterface.isUp());
                    entry.put("loopback", networkInterface.isLoopback());
                    entry.put("virtual", networkInterface.isVirtual());
                    entry.put("wireless", false);
                    entry.put("defaultRoute", false);
                    entry.put("wolCandidate", !networkInterface.isLoopback() && !networkInterface.isVirtual() && networkInterface.getHardwareAddress() != null);
                    result.add(entry);
                }
            }
            return result;
        } catch (SocketException exception) {
            return List.of();
        }
    }

    private String macAddress(NetworkInterface networkInterface) throws SocketException {
        byte[] hardwareAddress = networkInterface.getHardwareAddress();
        if (hardwareAddress == null) {
            return "";
        }
        return HexFormat.ofDelimiter(":").formatHex(hardwareAddress);
    }
}
