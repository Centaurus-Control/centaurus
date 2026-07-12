package de.shadowsoft.centaurus.server.trustcert;

import java.time.Instant;
import java.util.UUID;

public record TrustedCertificateResponse(
    UUID id,
    String alias,
    String displayName,
    String certificatePem,
    boolean enabled,
    String subjectDn,
    String issuerDn,
    String serialNumber,
    Instant notBefore,
    Instant notAfter,
    String sha256Fingerprint,
    Instant createdAt,
    Instant updatedAt
) {

    public static TrustedCertificateResponse from(TrustedCertificate certificate) {
        return new TrustedCertificateResponse(
            certificate.getId(),
            certificate.getAlias(),
            certificate.getDisplayName(),
            certificate.getCertificatePem(),
            certificate.isEnabled(),
            certificate.getSubjectDn(),
            certificate.getIssuerDn(),
            certificate.getSerialNumber(),
            certificate.getNotBefore(),
            certificate.getNotAfter(),
            certificate.getSha256Fingerprint(),
            certificate.getCreatedAt(),
            certificate.getUpdatedAt()
        );
    }
}
