package de.shadowsoft.centaurus.server.agent;

import de.shadowsoft.centaurus.server.machine.Machine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, unique = true)
    private Machine machine;

    @Column(name = "installation_id", nullable = false, unique = true)
    private UUID installationId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String hostname;

    @Column(name = "agent_version", nullable = false)
    private String agentVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentStatus status;

    @Column(name = "last_connected_at")
    private Instant lastConnectedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Agent() {
    }

    public Agent(Machine machine, UUID installationId, String displayName, String hostname, String agentVersion) {
        this.id = UUID.randomUUID();
        this.machine = machine;
        this.installationId = installationId;
        this.displayName = displayName;
        this.hostname = hostname;
        this.agentVersion = agentVersion;
        this.status = AgentStatus.REGISTERED;
        this.deleted = false;
    }

    public UUID getId() {
        return id;
    }

    public Machine getMachine() {
        return machine;
    }

    public UUID getInstallationId() {
        return installationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHostname() {
        return hostname;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public Instant getLastConnectedAt() {
        return lastConnectedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void markConnected(Instant connectedAt) {
        this.status = AgentStatus.ONLINE;
        this.lastConnectedAt = connectedAt;
        this.lastSeenAt = connectedAt;
    }

    public void markSeen(Instant seenAt) {
        this.lastSeenAt = seenAt;
    }

    public void changeStatus(AgentStatus status) {
        this.status = status;
    }

    public void updateRuntimeAttributes(String hostname, String agentVersion) {
        this.hostname = hostname;
        this.agentVersion = agentVersion;
    }

    public void markDeleted(Instant deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
        this.status = AgentStatus.REVOKED;
    }

    public void reactivate(String displayName, String hostname, String agentVersion) {
        this.deleted = false;
        this.deletedAt = null;
        this.displayName = displayName;
        this.hostname = hostname;
        this.agentVersion = agentVersion;
        this.status = AgentStatus.REGISTERED;
        this.lastConnectedAt = null;
        this.lastSeenAt = null;
    }
}
