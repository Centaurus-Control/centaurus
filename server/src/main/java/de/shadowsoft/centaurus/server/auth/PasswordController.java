package de.shadowsoft.centaurus.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PasswordController {

    private final PasswordChangeService passwordChangeService;
    private final RefreshCookieService refreshCookieService;

    public PasswordController(
        PasswordChangeService passwordChangeService,
        RefreshCookieService refreshCookieService
    ) {
        this.passwordChangeService = passwordChangeService;
        this.refreshCookieService = refreshCookieService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody ChangePasswordRequest request,
        HttpServletRequest servletRequest
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ChangePasswordResponse response = passwordChangeService.changePassword(
            userId,
            request.currentPassword(),
            request.newPassword()
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookieService.createExpiredRefreshCookie(servletRequest).toString())
            .body(response);
    }
}
