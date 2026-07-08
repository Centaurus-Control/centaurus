package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.audit.AuditResult;
import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.user.User;
import de.shadowsoft.centaurus.server.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public PasswordChangeService(
        UserRepository userRepository,
        UserSessionRepository userSessionRepository,
        PasswordEncoder passwordEncoder,
        AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public ChangePasswordResponse changePassword(
        UUID userId,
        String currentPassword,
        String newPassword
    ) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow();
        validateCurrentPassword(user, currentPassword);
        validateNewPassword(user, currentPassword, newPassword);

        Instant changedAt = Instant.now();
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        long revokedSessionCount = revokeSessions(user, changedAt);
        auditService.record(
            "PASSWORD_CHANGED",
            AuditResult.SUCCESS,
            user,
            user.getUsername(),
            "USER",
            user.getId(),
            user.getUsername(),
            AuditService.details("revokedSessionCount", revokedSessionCount)
        );

        return new ChangePasswordResponse(
            user.isPasswordChangeRequired(),
            changedAt,
            revokedSessionCount
        );
    }

    private void validateCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid current password");
        }
    }

    private void validateNewPassword(User user, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new PasswordPolicyException("New password must have at least 12 characters");
        }
        if (newPassword.equals(currentPassword)) {
            throw new PasswordPolicyException("New password must be different from the current password");
        }
        if (newPassword.equalsIgnoreCase(user.getUsername())) {
            throw new PasswordPolicyException("New password must not match the username");
        }
    }

    private long revokeSessions(User user, Instant revokedAt) {
        long revokedSessionCount = 0;
        for (UserSession session : userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            session.revoke(revokedAt);
            revokedSessionCount++;
        }
        return revokedSessionCount;
    }
}
