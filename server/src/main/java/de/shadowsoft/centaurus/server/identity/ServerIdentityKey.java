package de.shadowsoft.centaurus.server.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "server_identity_keys")
public class ServerIdentityKey {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "key_id", nullable = false, unique = true)
    private String keyId;

    @Column(name = "public_key", nullable = false)
    private String publicKey;

    @Column(name = "private_key_reference", nullable = false)
    private String privateKeyReference;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ServerIdentityKey() {
    }

    public ServerIdentityKey(String keyId, String publicKey, String privateKeyReference) {
        this.id = UUID.randomUUID();
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.privateKeyReference = privateKeyReference;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKeyReference() {
        return privateKeyReference;
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

    public void revoke(Instant revokedAt) {
        this.active = false;
        this.revokedAt = revokedAt;
    }
}
