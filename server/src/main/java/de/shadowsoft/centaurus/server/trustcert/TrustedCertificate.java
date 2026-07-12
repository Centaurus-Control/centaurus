package de.shadowsoft.centaurus.server.trustcert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "trusted_certificates")
public class TrustedCertificate {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String alias;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "certificate_pem", nullable = false, columnDefinition = "text")
    private String certificatePem;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "subject_dn", nullable = false, columnDefinition = "text")
    private String subjectDn;

    @Column(name = "issuer_dn", nullable = false, columnDefinition = "text")
    private String issuerDn;

    @Column(name = "serial_number", nullable = false)
    private String serialNumber;

    @Column(name = "not_before", nullable = false)
    private Instant notBefore;

    @Column(name = "not_after", nullable = false)
    private Instant notAfter;

    @Column(name = "sha256_fingerprint", nullable = false)
    private String sha256Fingerprint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TrustedCertificate() {
    }

    public TrustedCertificate(
        String alias,
        String displayName,
        String certificatePem,
        boolean enabled,
        CertificateMetadata metadata
    ) {
        this.id = UUID.randomUUID();
        update(alias, displayName, certificatePem, enabled, metadata);
    }

    public UUID getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getNotAfter() {
        return notAfter;
    }

    public String getSha256Fingerprint() {
        return sha256Fingerprint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
        String alias,
        String displayName,
        String certificatePem,
        boolean enabled,
        CertificateMetadata metadata
    ) {
        this.alias = alias;
        this.displayName = displayName;
        this.certificatePem = certificatePem;
        this.enabled = enabled;
        this.subjectDn = metadata.subjectDn();
        this.issuerDn = metadata.issuerDn();
        this.serialNumber = metadata.serialNumber();
        this.notBefore = metadata.notBefore();
        this.notAfter = metadata.notAfter();
        this.sha256Fingerprint = metadata.sha256Fingerprint();
    }
}
