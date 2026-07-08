package de.shadowsoft.centaurus.server.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshCookieService {

    private final AuthProperties authProperties;

    public RefreshCookieService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public ResponseCookie createRefreshCookie(String refreshToken, HttpServletRequest request) {
        return baseCookie(refreshToken, request)
            .maxAge(authProperties.getRefreshTokenTtl())
            .build();
    }

    public ResponseCookie createExpiredRefreshCookie(HttpServletRequest request) {
        return baseCookie("", request)
            .maxAge(Duration.ZERO)
            .build();
    }

    public String extractRefreshToken(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value, HttpServletRequest request) {
        return ResponseCookie.from(authProperties.getRefreshCookieName(), value)
            .httpOnly(true)
            .secure(shouldUseSecureCookie(request))
            .sameSite("Lax")
            .path("/api/auth");
    }

    private boolean shouldUseSecureCookie(HttpServletRequest request) {
        return switch (authProperties.getRefreshCookieSecureMode()) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        };
    }
}
