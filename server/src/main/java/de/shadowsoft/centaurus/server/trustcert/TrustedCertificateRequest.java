package de.shadowsoft.centaurus.server.trustcert;

public record TrustedCertificateRequest(
    String alias,
    String displayName,
    String certificatePem,
    Boolean enabled
) {
}
