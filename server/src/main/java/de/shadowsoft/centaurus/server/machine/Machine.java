package de.shadowsoft.centaurus.server.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String hostname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MachineStatus status;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "primary_wol_interface_id")
    private UUID primaryWolInterfaceId;

    @Column(name = "wol_enabled", nullable = false)
    private boolean wolEnabled;

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

    protected Machine() {
    }

    public Machine(String displayName, String hostname) {
        this.id = UUID.randomUUID();
        this.displayName = displayName;
        this.hostname = hostname;
        this.status = MachineStatus.UNKNOWN;
        this.wolEnabled = false;
        this.deleted = false;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHostname() {
        return hostname;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public UUID getPrimaryWolInterfaceId() {
        return primaryWolInterfaceId;
    }

    public boolean isWolEnabled() {
        return wolEnabled;
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

    public void rename(String displayName) {
        this.displayName = displayName;
    }

    public void updateHostname(String hostname) {
        this.hostname = hostname;
    }

    public void markSeen(Instant seenAt) {
        this.lastSeenAt = seenAt;
    }

    public void changeStatus(MachineStatus status) {
        this.status = status;
    }

    public void configureWakeOnLan(boolean wolEnabled, UUID primaryWolInterfaceId) {
        this.wolEnabled = wolEnabled;
        this.primaryWolInterfaceId = primaryWolInterfaceId;
    }

    public void markDeleted(Instant deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
        this.status = MachineStatus.OFFLINE;
    }

    public void reactivate(String displayName, String hostname) {
        this.deleted = false;
        this.deletedAt = null;
        this.displayName = displayName;
        this.hostname = hostname;
        this.status = MachineStatus.UNKNOWN;
        this.lastSeenAt = null;
        this.primaryWolInterfaceId = null;
        this.wolEnabled = false;
    }
}
