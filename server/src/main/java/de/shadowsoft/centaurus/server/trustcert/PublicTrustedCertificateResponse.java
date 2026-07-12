package de.shadowsoft.centaurus.server.trustcert;

public record PublicTrustedCertificateResponse(
    String alias,
    String certificatePem,
    String sha256Fingerprint
) {

    public static PublicTrustedCertificateResponse from(TrustedCertificate certificate) {
        return new PublicTrustedCertificateResponse(
            certificate.getAlias(),
            certificate.getCertificatePem(),
            certificate.getSha256Fingerprint()
        );
    }
}
