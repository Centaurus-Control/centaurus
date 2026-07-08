package de.shadowsoft.centaurus.server.script;

import de.shadowsoft.centaurus.server.agent.Agent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "script_manifests")
public class ScriptManifest {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "manifest_version", nullable = false)
    private long manifestVersion;

    @Column(name = "manifest_hash", nullable = false)
    private String manifestHash;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected ScriptManifest() {
    }

    public ScriptManifest(Agent agent, long manifestVersion, String manifestHash) {
        this.id = UUID.randomUUID();
        this.agent = agent;
        this.manifestVersion = manifestVersion;
        this.manifestHash = manifestHash;
    }

    public UUID getId() {
        return id;
    }

    public Agent getAgent() {
        return agent;
    }

    public long getManifestVersion() {
        return manifestVersion;
    }

    public String getManifestHash() {
        return manifestHash;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
