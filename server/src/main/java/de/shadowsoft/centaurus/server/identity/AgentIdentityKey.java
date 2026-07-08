package de.shadowsoft.centaurus.server.identity;

import de.shadowsoft.centaurus.server.agent.Agent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "agent_identity_keys",
    uniqueConstraints = @UniqueConstraint(name = "uk_agent_identity_key_id", columnNames = {"agent_id", "key_id"})
)
public class AgentIdentityKey {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "key_id", nullable = false)
    private String keyId;

    @Column(name = "public_key", nullable = false)
    private String publicKey;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected AgentIdentityKey() {
    }

    public AgentIdentityKey(Agent agent, String keyId, String publicKey) {
        this.id = UUID.randomUUID();
        this.agent = agent;
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public Agent getAgent() {
        return agent;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void markUsed(Instant usedAt) {
        this.lastUsedAt = usedAt;
    }

    public void revoke(Instant revokedAt) {
        this.active = false;
        this.revokedAt = revokedAt;
    }
}
