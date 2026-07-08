package de.shadowsoft.centaurus.agent.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/ui")
public class AgentUiAuthController {

    private final AgentUiAuthenticationService authenticationService;
    private final AgentUiSessionService sessionService;

    public AgentUiAuthController(
        AgentUiAuthenticationService authenticationService,
        AgentUiSessionService sessionService
    ) {
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public AgentUiSessionResponse login(@RequestBody AgentUiLoginRequest request, HttpServletResponse response) {
        AgentUiSessionService.AgentUiSession session = authenticationService.login(request);
        ResponseCookie cookie = ResponseCookie.from(AgentUiSessionService.COOKIE_NAME, session.token())
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(java.time.Duration.between(java.time.Instant.now(), session.expiresAt()))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return toResponse(session);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.revoke(cookieValue(request));
        ResponseCookie cookie = ResponseCookie.from(AgentUiSessionService.COOKIE_NAME, "")
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @GetMapping("/session")
    public AgentUiSessionResponse session(HttpServletRequest request) {
        return sessionService.find(cookieValue(request))
            .map(this::toResponse)
            .orElseGet(() -> new AgentUiSessionResponse(false, null, null, null));
    }

    private AgentUiSessionResponse toResponse(AgentUiSessionService.AgentUiSession session) {
        return new AgentUiSessionResponse(true, session.username(), session.role(), session.expiresAt());
    }

    private String cookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AgentUiSessionService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
