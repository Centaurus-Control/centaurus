package de.shadowsoft.centaurus.server.trustcert;

import de.shadowsoft.centaurus.server.audit.AuditService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrustedCertificateService {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,127}");
    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    private final TrustedCertificateRepository trustedCertificateRepository;
    private final AuditService auditService;

    public TrustedCertificateService(
        TrustedCertificateRepository trustedCertificateRepository,
        AuditService auditService
    ) {
        this.trustedCertificateRepository = trustedCertificateRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TrustedCertificateResponse> listCertificates() {
        return trustedCertificateRepository.findAllByOrderByAliasAsc()
            .stream()
            .map(TrustedCertificateResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public TrustedCertificateResponse getCertificate(UUID certificateId) {
        return TrustedCertificateResponse.from(resolve(certificateId));
    }

    @Transactional
    public TrustedCertificateResponse createCertificate(
        TrustedCertificateRequest request,
        Authentication authentication
    ) {
        String alias = normalizeAlias(request.alias());
        if (trustedCertificateRepository.existsByAlias(alias)) {
            throw new TrustedCertificateException("Certificate alias already exists");
        }

        String certificatePem = normalizeCertificatePem(request.certificatePem());
        CertificateMetadata metadata = metadata(certificatePem);
        TrustedCertificate certificate = trustedCertificateRepository.save(new TrustedCertificate(
            alias,
            displayName(request.displayName(), alias),
            certificatePem,
            Boolean.TRUE.equals(request.enabled()),
            metadata
        ));
        auditService.recordSuccess(
            "TRUSTED_CERTIFICATE_CREATED",
            authentication,
            "TRUSTED_CERTIFICATE",
            certificate.getId(),
            certificate.getAlias(),
            AuditService.details("enabled", certificate.isEnabled(), "sha256Fingerprint", certificate.getSha256Fingerprint())
        );
        return TrustedCertificateResponse.from(certificate);
    }

    @Transactional
    public TrustedCertificateResponse updateCertificate(
        UUID certificateId,
        TrustedCertificateRequest request,
        Authentication authentication
    ) {
        TrustedCertificate certificate = resolve(certificateId);
        String alias = normalizeAlias(request.alias());
        if (trustedCertificateRepository.existsByAliasAndIdNot(alias, certificateId)) {
            throw new TrustedCertificateException("Certificate alias already exists");
        }

        String certificatePem = normalizeCertificatePem(request.certificatePem());
        CertificateMetadata metadata = metadata(certificatePem);
        certificate.update(
            alias,
            displayName(request.displayName(), alias),
            certificatePem,
            Boolean.TRUE.equals(request.enabled()),
            metadata
        );
        auditService.recordSuccess(
            "TRUSTED_CERTIFICATE_UPDATED",
            authentication,
            "TRUSTED_CERTIFICATE",
            certificate.getId(),
            certificate.getAlias(),
            AuditService.details("enabled", certificate.isEnabled(), "sha256Fingerprint", certificate.getSha256Fingerprint())
        );
        return TrustedCertificateResponse.from(certificate);
    }

    @Transactional
    public void deleteCertificate(UUID certificateId, Authentication authentication) {
        TrustedCertificate certificate = resolve(certificateId);
        auditService.recordSuccess(
            "TRUSTED_CERTIFICATE_DELETED",
            authentication,
            "TRUSTED_CERTIFICATE",
            certificate.getId(),
            certificate.getAlias(),
            AuditService.details("sha256Fingerprint", certificate.getSha256Fingerprint())
        );
        trustedCertificateRepository.delete(certificate);
    }

    @Transactional(readOnly = true)
    public TrustedCertificateBundleResponse publicEnabledBundle() {
        List<PublicTrustedCertificateResponse> certificates = trustedCertificateRepository.findByEnabledTrueOrderByAliasAsc()
            .stream()
            .map(PublicTrustedCertificateResponse::from)
            .toList();
        return new TrustedCertificateBundleResponse(certificates);
    }

    @Transactional(readOnly = true)
    public String publicKeytoolBundle() {
        StringBuilder bundle = new StringBuilder("# centaurus-trusted-certificates-v1\n");
        trustedCertificateRepository.findByEnabledTrueOrderByAliasAsc()
            .forEach(certificate -> bundle
                .append(certificate.getAlias())
                .append('\t')
                .append(Base64.getEncoder().encodeToString(certificate.getCertificatePem().getBytes(StandardCharsets.US_ASCII)))
                .append('\n'));
        return bundle.toString();
    }

    private TrustedCertificate resolve(UUID certificateId) {
        return trustedCertificateRepository.findById(certificateId)
            .orElseThrow(() -> new TrustedCertificateException("Trusted certificate not found"));
    }

    private String normalizeAlias(String alias) {
        if (!StringUtils.hasText(alias)) {
            throw new TrustedCertificateException("Certificate alias is required");
        }
        String normalized = alias.trim().toLowerCase(Locale.ROOT);
        if (!ALIAS_PATTERN.matcher(normalized).matches()) {
            throw new TrustedCertificateException("Certificate alias must match [a-z0-9][a-z0-9._-]{0,127}");
        }
        return normalized;
    }

    private String displayName(String displayName, String alias) {
        if (!StringUtils.hasText(displayName)) {
            return alias;
        }
        String normalized = displayName.trim();
        if (normalized.length() > 255) {
            throw new TrustedCertificateException("Display name must not exceed 255 characters");
        }
        return normalized;
    }

    private String normalizeCertificatePem(String certificatePem) {
        if (!StringUtils.hasText(certificatePem)) {
            throw new TrustedCertificateException("Certificate PEM is required");
        }
        String normalized = certificatePem.trim().replace("\r\n", "\n").replace('\r', '\n');
        int beginIndex = normalized.indexOf(BEGIN_CERTIFICATE);
        int endIndex = normalized.indexOf(END_CERTIFICATE);
        if (beginIndex < 0 || endIndex < beginIndex) {
            throw new TrustedCertificateException("Certificate must be PEM encoded");
        }
        int endOffset = endIndex + END_CERTIFICATE.length();
        if (normalized.indexOf(BEGIN_CERTIFICATE, beginIndex + BEGIN_CERTIFICATE.length()) >= 0) {
            throw new TrustedCertificateException("Only one certificate per entry is supported");
        }
        String pem = normalized.substring(beginIndex, endOffset);
        String body = pem
            .replace(BEGIN_CERTIFICATE, "")
            .replace(END_CERTIFICATE, "")
            .replaceAll("\\s", "");
        if (body.isBlank()) {
            throw new TrustedCertificateException("Certificate PEM body is empty");
        }
        try {
            Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException exception) {
            throw new TrustedCertificateException("Certificate PEM body is not valid Base64", exception);
        }
        return BEGIN_CERTIFICATE + "\n" + body.replaceAll("(.{64})", "$1\n").trim() + "\n" + END_CERTIFICATE + "\n";
    }

    private CertificateMetadata metadata(String certificatePem) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
                new ByteArrayInputStream(certificatePem.getBytes(StandardCharsets.US_ASCII))
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String fingerprint = HexFormat.ofDelimiter(":")
                .withUpperCase()
                .formatHex(digest.digest(certificate.getEncoded()));
            return new CertificateMetadata(
                certificate.getSubjectX500Principal().getName(),
                certificate.getIssuerX500Principal().getName(),
                certificate.getSerialNumber().toString(16).toUpperCase(Locale.ROOT),
                Instant.ofEpochMilli(certificate.getNotBefore().getTime()),
                Instant.ofEpochMilli(certificate.getNotAfter().getTime()),
                fingerprint
            );
        } catch (Exception exception) {
            throw new TrustedCertificateException("Certificate is not a valid X.509 certificate", exception);
        }
    }
}
