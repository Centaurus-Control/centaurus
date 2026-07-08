package de.shadowsoft.centaurus.server.network;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.machine.Machine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_network_interfaces")
public class MachineNetworkInterface {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "interface_name", nullable = false)
    private String interfaceName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "mac_address", length = 50)
    private String macAddress;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "prefix_length")
    private Integer prefixLength;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IpAddressFamily family;

    @Column(nullable = false)
    private boolean up;

    @Column(nullable = false)
    private boolean loopback;

    @Column(nullable = false)
    private boolean virtual;

    @Column(nullable = false)
    private boolean wireless;

    @Column(name = "default_route", nullable = false)
    private boolean defaultRoute;

    @Column(name = "wol_candidate", nullable = false)
    private boolean wolCandidate;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected MachineNetworkInterface() {
    }

    public MachineNetworkInterface(
        Machine machine,
        Agent agent,
        String interfaceName,
        IpAddressFamily family,
        Instant lastSeenAt
    ) {
        this.id = UUID.randomUUID();
        this.machine = machine;
        this.agent = agent;
        this.interfaceName = interfaceName;
        this.family = family;
        this.lastSeenAt = lastSeenAt;
    }

    public UUID getId() {
        return id;
    }

    public Machine getMachine() {
        return machine;
    }

    public Agent getAgent() {
        return agent;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getPrefixLength() {
        return prefixLength;
    }

    public IpAddressFamily getFamily() {
        return family;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isLoopback() {
        return loopback;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public boolean isWireless() {
        return wireless;
    }

    public boolean isDefaultRoute() {
        return defaultRoute;
    }

    public boolean isWolCandidate() {
        return wolCandidate;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void updateDetails(
        String displayName,
        String macAddress,
        String ipAddress,
        Integer prefixLength,
        boolean up,
        boolean loopback,
        boolean virtual,
        boolean wireless,
        boolean defaultRoute,
        boolean wolCandidate,
        Instant lastSeenAt
    ) {
        this.displayName = displayName;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.prefixLength = prefixLength;
        this.up = up;
        this.loopback = loopback;
        this.virtual = virtual;
        this.wireless = wireless;
        this.defaultRoute = defaultRoute;
        this.wolCandidate = wolCandidate;
        this.lastSeenAt = lastSeenAt;
    }
}
