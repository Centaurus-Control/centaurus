package de.shadowsoft.centaurus.server.trustcert;

import java.util.List;

public record TrustedCertificateBundleResponse(
    List<PublicTrustedCertificateResponse> certificates
) {
}
