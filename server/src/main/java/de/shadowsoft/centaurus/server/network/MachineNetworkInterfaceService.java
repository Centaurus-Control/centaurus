package de.shadowsoft.centaurus.server.network;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineNetworkInterfaceService {

    private final AgentRepository agentRepository;
    private final MachineNetworkInterfaceRepository machineNetworkInterfaceRepository;

    public MachineNetworkInterfaceService(
        AgentRepository agentRepository,
        MachineNetworkInterfaceRepository machineNetworkInterfaceRepository
    ) {
        this.agentRepository = agentRepository;
        this.machineNetworkInterfaceRepository = machineNetworkInterfaceRepository;
    }

    @Transactional
    public void replaceForAgent(UUID agentId, NetworkInterfaceSnapshotMessage message) {
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown agent"));
        Instant sampledAt = message.sampledAt() == null ? Instant.now() : message.sampledAt();
        Map<InterfaceKey, MachineNetworkInterface> existingInterfaces = new HashMap<>();
        for (MachineNetworkInterface networkInterface : machineNetworkInterfaceRepository.findByAgentId(agentId)) {
            existingInterfaces.put(key(networkInterface), networkInterface);
        }
        Set<MachineNetworkInterface> seenInterfaces = new HashSet<>();
        if (message.interfaces() == null) {
            return;
        }
        for (NetworkInterfaceSnapshotMessage.NetworkInterfaceEntry entry : message.interfaces()) {
            InterfaceKey key = new InterfaceKey(entry.interfaceName(), entry.family(), entry.macAddress(), entry.ipAddress());
            MachineNetworkInterface networkInterface = existingInterfaces.getOrDefault(
                key,
                new MachineNetworkInterface(agent.getMachine(), agent, entry.interfaceName(), entry.family(), sampledAt)
            );
            networkInterface.updateDetails(
                entry.displayName(),
                entry.macAddress(),
                entry.ipAddress(),
                entry.prefixLength(),
                entry.up(),
                entry.loopback(),
                entry.virtual(),
                entry.wireless(),
                entry.defaultRoute(),
                entry.wolCandidate(),
                sampledAt
            );
            machineNetworkInterfaceRepository.save(networkInterface);
            seenInterfaces.add(networkInterface);
        }
        for (MachineNetworkInterface networkInterface : existingInterfaces.values()) {
            if (!seenInterfaces.contains(networkInterface)) {
                machineNetworkInterfaceRepository.delete(networkInterface);
            }
        }
    }

    private InterfaceKey key(MachineNetworkInterface networkInterface) {
        return new InterfaceKey(
            networkInterface.getInterfaceName(),
            networkInterface.getFamily(),
            networkInterface.getMacAddress(),
            networkInterface.getIpAddress()
        );
    }

    private record InterfaceKey(
        String interfaceName,
        IpAddressFamily family,
        String macAddress,
        String ipAddress
    ) {
        private InterfaceKey {
            interfaceName = Objects.toString(interfaceName, "");
            macAddress = Objects.toString(macAddress, "");
            ipAddress = Objects.toString(ipAddress, "");
        }
    }
}
