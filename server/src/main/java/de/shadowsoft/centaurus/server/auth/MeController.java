package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.user.User;
import de.shadowsoft.centaurus.server.user.UserRepository;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow();
        return new AuthenticatedUserResponse(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.isPasswordChangeRequired()
        );
    }
}
