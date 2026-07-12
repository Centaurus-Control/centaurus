package de.shadowsoft.centaurus.server.trustcert;

import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrustedCertificateController {

    private final TrustedCertificateService trustedCertificateService;

    public TrustedCertificateController(TrustedCertificateService trustedCertificateService) {
        this.trustedCertificateService = trustedCertificateService;
    }

    @GetMapping("/api/admin/trusted-certificates")
    public List<TrustedCertificateResponse> listCertificates() {
        return trustedCertificateService.listCertificates();
    }

    @GetMapping("/api/admin/trusted-certificates/{certificateId}")
    public TrustedCertificateResponse getCertificate(@PathVariable UUID certificateId) {
        return trustedCertificateService.getCertificate(certificateId);
    }

    @PostMapping("/api/admin/trusted-certificates")
    public TrustedCertificateResponse createCertificate(
        @RequestBody TrustedCertificateRequest request,
        Authentication authentication
    ) {
        return trustedCertificateService.createCertificate(request, authentication);
    }

    @PutMapping("/api/admin/trusted-certificates/{certificateId}")
    public TrustedCertificateResponse updateCertificate(
        @PathVariable UUID certificateId,
        @RequestBody TrustedCertificateRequest request,
        Authentication authentication
    ) {
        return trustedCertificateService.updateCertificate(certificateId, request, authentication);
    }

    @DeleteMapping("/api/admin/trusted-certificates/{certificateId}")
    public ResponseEntity<Void> deleteCertificate(@PathVariable UUID certificateId, Authentication authentication) {
        trustedCertificateService.deleteCertificate(certificateId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/agent/trusted-certificates")
    public TrustedCertificateBundleResponse publicEnabledBundle() {
        return trustedCertificateService.publicEnabledBundle();
    }

    @GetMapping(value = "/api/agent/trusted-certificates/keytool-bundle", produces = MediaType.TEXT_PLAIN_VALUE)
    public String publicKeytoolBundle() {
        return trustedCertificateService.publicKeytoolBundle();
    }
}
