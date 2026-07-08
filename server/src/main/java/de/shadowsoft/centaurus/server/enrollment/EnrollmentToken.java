package de.shadowsoft.centaurus.server.enrollment;

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
@Table(name = "enrollment_tokens")
public class EnrollmentToken {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "suggested_name")
    private String suggestedName;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_agent_id")
    private Agent usedByAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EnrollmentToken() {
    }

    public EnrollmentToken(String tokenHash, String suggestedName, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tokenHash = tokenHash;
        this.suggestedName = suggestedName;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getSuggestedName() {
        return suggestedName;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Agent getUsedByAgent() {
        return usedByAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(Agent agent, Instant usedAt) {
        this.usedByAgent = agent;
        this.usedAt = usedAt;
    }

    public void detachUsedByAgent() {
        this.usedByAgent = null;
    }
}
