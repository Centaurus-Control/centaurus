package de.shadowsoft.centaurus.server.user;

import de.shadowsoft.centaurus.server.audit.AuditService;
import de.shadowsoft.centaurus.server.auth.BootstrapPasswordGenerator;
import de.shadowsoft.centaurus.server.auth.UserSession;
import de.shadowsoft.centaurus.server.auth.UserSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapPasswordGenerator passwordGenerator;
    private final AuditService auditService;

    public UserManagementService(
        UserRepository userRepository,
        UserSessionRepository userSessionRepository,
        PasswordEncoder passwordEncoder,
        BootstrapPasswordGenerator passwordGenerator,
        AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findByDeletedFalseOrderByUsernameAsc()
            .stream()
            .map(UserResponse::from)
            .toList();
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request, Authentication authentication) {
        validateUsername(request.username());
        UserRole role = requireRole(request.role());
        if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
            throw new UserManagementException("Username is already in use");
        }

        String temporaryPassword = passwordGenerator.generatePassword();
        User user = new User(request.username(), passwordEncoder.encode(temporaryPassword), role);
        user.requirePasswordChange();
        userRepository.saveAndFlush(user);
        auditService.recordSuccess(
            "USER_CREATED",
            authentication,
            "USER",
            user.getId(),
            user.getUsername(),
            AuditService.details("role", user.getRole())
        );
        return new CreateUserResponse(UserResponse.from(user), temporaryPassword);
    }

    @Transactional
    public UserResponse updateRole(UUID userId, UpdateUserRoleRequest request, Authentication authentication) {
        User user = findActiveUser(userId);
        UserRole newRole = requireRole(request.role());
        UserRole previousRole = user.getRole();
        if (user.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN && isLastAdmin()) {
            throw new UserManagementException("The last admin user cannot be demoted");
        }
        user.changeRole(newRole);
        revokeUserSessions(user, Instant.now());
        userRepository.flush();
        auditService.recordSuccess(
            "USER_ROLE_CHANGED",
            authentication,
            "USER",
            user.getId(),
            user.getUsername(),
            AuditService.details("previousRole", previousRole, "newRole", newRole)
        );
        return UserResponse.from(user);
    }

    @Transactional
    public ResetUserPasswordResponse resetPassword(UUID userId, Authentication authentication) {
        User user = findActiveUser(userId);
        String temporaryPassword = passwordGenerator.generatePassword();
        user.resetPasswordHash(passwordEncoder.encode(temporaryPassword));
        revokeUserSessions(user, Instant.now());
        userRepository.flush();
        auditService.recordSuccess(
            "USER_PASSWORD_RESET",
            authentication,
            "USER",
            user.getId(),
            user.getUsername(),
            AuditService.details("passwordChangeRequired", user.isPasswordChangeRequired())
        );
        return new ResetUserPasswordResponse(UserResponse.from(user), temporaryPassword);
    }

    @Transactional
    public void deleteUser(UUID userId, Authentication authentication) {
        User user = findActiveUser(userId);
        if (user.getRole() == UserRole.ADMIN && isLastAdmin()) {
            throw new UserManagementException("The last admin user cannot be deleted");
        }
        user.markDeleted();
        revokeUserSessions(user, Instant.now());
        auditService.recordSuccess(
            "USER_DELETED",
            authentication,
            "USER",
            user.getId(),
            user.getUsername(),
            AuditService.details("role", user.getRole())
        );
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new UserManagementException("User not found"));
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new UserManagementException("Username is required");
        }
    }

    private UserRole requireRole(UserRole role) {
        if (role == null) {
            throw new UserManagementException("Role is required");
        }
        return role;
    }

    private void revokeUserSessions(User user, Instant revokedAt) {
        for (UserSession session : userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            session.revoke(revokedAt);
        }
    }

    private boolean isLastAdmin() {
        return userRepository.countByRoleAndDeletedFalse(UserRole.ADMIN) <= 1;
    }
}
