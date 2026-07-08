package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.audit.AuditResult;
import de.shadowsoft.centaurus.server.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RefreshCookieService refreshCookieService;
    private final AuditService auditService;

    public AuthController(
        AuthenticationService authenticationService,
        RefreshCookieService refreshCookieService,
        AuditService auditService
    ) {
        this.authenticationService = authenticationService;
        this.refreshCookieService = refreshCookieService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        try {
            AuthSessionTokens tokens = authenticationService.login(
                request.username(),
                request.password(),
                servletRequest.getHeader(HttpHeaders.USER_AGENT),
                servletRequest.getRemoteAddr()
            );
            auditService.record(
                "LOGIN",
                AuditResult.SUCCESS,
                tokens.user(),
                request.username(),
                "USER",
                tokens.user().getId(),
                tokens.user().getUsername(),
                AuditService.details(
                    "ipAddress", servletRequest.getRemoteAddr(),
                    "userAgent", servletRequest.getHeader(HttpHeaders.USER_AGENT)
                )
            );
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshCookie(tokens.refreshToken(), servletRequest).toString())
                .body(tokens.accessToken());
        } catch (BadCredentialsException exception) {
            auditService.record(
                "LOGIN",
                AuditResult.FAILURE,
                null,
                request.username(),
                "USER",
                null,
                request.username(),
                AuditService.details(
                    "reason", "bad_credentials",
                    "ipAddress", servletRequest.getRemoteAddr(),
                    "userAgent", servletRequest.getHeader(HttpHeaders.USER_AGENT)
                )
            );
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(
        @CookieValue(name = "${centaurus.auth.refresh-cookie-name}", required = false) String refreshToken,
        HttpServletRequest servletRequest
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Missing refresh token");
        }
        AuthSessionTokens tokens = authenticationService.refresh(refreshToken);
        auditService.record(
            "TOKEN_REFRESH",
            AuditResult.SUCCESS,
            tokens.user(),
            tokens.user().getUsername(),
            "USER",
            tokens.user().getId(),
            tokens.user().getUsername(),
            AuditService.details("ipAddress", servletRequest.getRemoteAddr())
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookieService.createRefreshCookie(tokens.refreshToken(), servletRequest).toString())
            .body(tokens.accessToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = "${centaurus.auth.refresh-cookie-name}", required = false) String refreshToken,
        HttpServletRequest servletRequest
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authenticationService.logout(refreshToken);
        }
        auditService.recordSuccess("LOGOUT", null, "USER", null, null, AuditService.details("ipAddress", servletRequest.getRemoteAddr()));
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshCookieService.createExpiredRefreshCookie(servletRequest).toString())
            .build();
    }
}
