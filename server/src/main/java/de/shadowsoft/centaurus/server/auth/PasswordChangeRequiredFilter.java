package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.user.User;
import de.shadowsoft.centaurus.server.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public PasswordChangeRequiredFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (isAllowedDuringPasswordChange(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findByIdAndDeletedFalse(userId).orElse(null);
        if (user == null || !user.isPasswordChangeRequired()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
            {"type":"about:blank","title":"Password change required","status":403,"detail":"The current user must change the password before using this endpoint."}
            """);
    }

    private boolean isAllowedDuringPasswordChange(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/me")
            || path.equals("/api/auth/change-password")
            || path.startsWith("/api/auth/")
            || path.startsWith("/actuator/");
    }
}
