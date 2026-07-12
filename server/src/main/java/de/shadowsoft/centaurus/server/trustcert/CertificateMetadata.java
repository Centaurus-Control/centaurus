package de.shadowsoft.centaurus.server.trustcert;

import java.time.Instant;

record CertificateMetadata(
    String subjectDn,
    String issuerDn,
    String serialNumber,
    Instant notBefore,
    Instant notAfter,
    String sha256Fingerprint
) {
}
