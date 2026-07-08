package de.shadowsoft.centaurus.server.enrollment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.identity.ServerIdentityKey;
import de.shadowsoft.centaurus.server.identity.ServerIdentityService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentManagementService {

    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final EnrollmentTokenSecretService tokenSecretService;
    private final EnrollmentProperties enrollmentProperties;
    private final ServerIdentityService serverIdentityService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public EnrollmentManagementService(
        EnrollmentTokenRepository enrollmentTokenRepository,
        EnrollmentTokenSecretService tokenSecretService,
        EnrollmentProperties enrollmentProperties,
        ServerIdentityService serverIdentityService,
        ObjectMapper objectMapper,
        AuditService auditService
    ) {
        this.enrollmentTokenRepository = enrollmentTokenRepository;
        this.tokenSecretService = tokenSecretService;
        this.enrollmentProperties = enrollmentProperties;
        this.serverIdentityService = serverIdentityService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentTokenResponse> listTokens() {
        return enrollmentTokenRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(EnrollmentTokenResponse::from)
            .toList();
    }

    @Transactional
    public CreateEnrollmentTokenResponse createToken(CreateEnrollmentTokenRequest request, Authentication authentication) {
        EnrollmentTokenSecret secret = tokenSecretService.createToken();
        Instant expiresAt = Instant.now().plus(resolveTtl(request));
        EnrollmentToken token = new EnrollmentToken(secret.hash(), request.suggestedName(), expiresAt);
        enrollmentTokenRepository.saveAndFlush(token);
        ServerIdentityKey serverIdentityKey = serverIdentityService.activeKey();
        EnrollmentBundlePayload payload = new EnrollmentBundlePayload(
            1,
            enrollmentProperties.getServerUrl(),
            enrollmentProperties.getWsUrl(),
            secret.token(),
            serverIdentityKey.getPublicKey(),
            serverIdentityKey.getKeyId(),
            request.suggestedName(),
            expiresAt
        );
        auditService.recordSuccess(
            "ENROLLMENT_BUNDLE_CREATED",
            authentication,
            "ENROLLMENT_TOKEN",
            token.getId(),
            token.getSuggestedName(),
            AuditService.details("expiresAt", expiresAt, "suggestedName", request.suggestedName())
        );
        return new CreateEnrollmentTokenResponse(EnrollmentTokenResponse.from(token), encodeBundle(payload));
    }

    private Duration resolveTtl(CreateEnrollmentTokenRequest request) {
        if (request.expiresIn() == null) {
            return enrollmentProperties.getTokenTtl();
        }
        return request.expiresIn();
    }

    private String encodeBundle(EnrollmentBundlePayload payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            return "raenroll:" + Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode enrollment bundle", exception);
        }
    }
}
