package de.shadowsoft.centaurus.server.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.user.User;
import de.shadowsoft.centaurus.server.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditService(
        AuditEventRepository auditEventRepository,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditEventResponse> listRecentEvents() {
        return auditEventRepository.findTop200ByOrderByCreatedAtDesc()
            .stream()
            .map(AuditEventResponse::from)
            .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        String action,
        AuditResult result,
        User user,
        String username,
        String targetType,
        UUID targetId,
        String targetLabel,
        Map<String, ?> details
    ) {
        try {
            String detailsJson = details == null || details.isEmpty() ? null : objectMapper.writeValueAsString(details);
            AuditEvent event = new AuditEvent(
                action,
                result,
                user,
                username != null ? username : user == null ? null : user.getUsername(),
                targetType,
                targetId,
                targetLabel,
                detailsJson
            );
            auditEventRepository.save(event);
            AUDIT_LOGGER.info(
                "action={} result={} userId={} username={} targetType={} targetId={} targetLabel={} details={}",
                action,
                result,
                user == null ? null : user.getId(),
                event.getUsername(),
                targetType,
                targetId,
                targetLabel,
                detailsJson
            );
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Could not serialize audit details for action {}", action, exception);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not persist audit event for action {}", action, exception);
        }
    }

    public void recordSuccess(
        String action,
        Authentication authentication,
        String targetType,
        UUID targetId,
        String targetLabel,
        Map<String, ?> details
    ) {
        record(action, AuditResult.SUCCESS, user(authentication), username(authentication), targetType, targetId, targetLabel, details);
    }

    public void recordFailure(
        String action,
        Authentication authentication,
        String targetType,
        UUID targetId,
        String targetLabel,
        Map<String, ?> details
    ) {
        record(action, AuditResult.FAILURE, user(authentication), username(authentication), targetType, targetId, targetLabel, details);
    }

    public User user(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        try {
            return userRepository.findByIdAndDeletedFalse(UUID.fromString(jwt.getSubject())).orElse(null);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public String username(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        String username = jwt.getClaimAsString("username");
        return username == null || username.isBlank() ? null : username;
    }

    public static Map<String, Object> details(Object... values) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            if (values[index] != null && values[index + 1] != null) {
                details.put(values[index].toString(), values[index + 1]);
            }
        }
        return details;
    }
}
