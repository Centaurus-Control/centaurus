package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.user.User;
import de.shadowsoft.centaurus.server.user.UserRepository;
import java.time.Instant;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;

    public AuthenticationService(
        UserRepository userRepository,
        UserSessionRepository userSessionRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService,
        RefreshTokenService refreshTokenService,
        AuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authProperties = authProperties;
    }

    @Transactional
    public AuthSessionTokens login(String username, String password, String userAgent, String ipAddress) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return createSessionTokens(user, userAgent, ipAddress);
    }

    @Transactional
    public AuthSessionTokens refresh(String refreshToken) {
        Instant now = Instant.now();
        String refreshTokenHash = refreshTokenService.hash(refreshToken);
        UserSession session = userSessionRepository.findByRefreshTokenHash(refreshTokenHash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!session.isActive(now)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (session.getUser().isDeleted()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        RefreshToken rotatedRefreshToken = refreshTokenService.createToken();
        session.rotate(rotatedRefreshToken.hash(), now.plus(authProperties.getRefreshTokenTtl()), now);
        AuthTokenResponse accessToken = jwtTokenService.createAccessToken(session.getUser());
        return new AuthSessionTokens(accessToken, rotatedRefreshToken.token(), session.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        String refreshTokenHash = refreshTokenService.hash(refreshToken);
        userSessionRepository.findByRefreshTokenHash(refreshTokenHash)
            .ifPresent(session -> session.revoke(Instant.now()));
    }

    private AuthSessionTokens createSessionTokens(User user, String userAgent, String ipAddress) {
        RefreshToken refreshToken = refreshTokenService.createToken();
        Instant expiresAt = Instant.now().plus(authProperties.getRefreshTokenTtl());
        UserSession session = new UserSession(user, refreshToken.hash(), expiresAt, userAgent, ipAddress);
        userSessionRepository.save(session);
        AuthTokenResponse accessToken = jwtTokenService.createAccessToken(user);
        return new AuthSessionTokens(accessToken, refreshToken.token(), user);
    }
}
