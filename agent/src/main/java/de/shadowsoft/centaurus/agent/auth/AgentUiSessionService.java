package de.shadowsoft.centaurus.agent.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AgentUiSessionService {

    public static final String COOKIE_NAME = "centaurus_agent_ui_session";

    private static final Duration SESSION_TTL = Duration.ofHours(8);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, AgentUiSession> sessions = new ConcurrentHashMap<>();

    public AgentUiSession create(String username, String role) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AgentUiSession session = new AgentUiSession(token, username, role, Instant.now().plus(SESSION_TTL));
        sessions.put(token, session);
        return session;
    }

    public Optional<AgentUiSession> find(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        AgentUiSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public record AgentUiSession(
        String token,
        String username,
        String role,
        Instant expiresAt
    ) {
    }
}
