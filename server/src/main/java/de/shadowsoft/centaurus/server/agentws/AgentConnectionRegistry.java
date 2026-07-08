package de.shadowsoft.centaurus.server.agentws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AgentConnectionRegistry {

    private final ObjectMapper objectMapper;
    private final Map<UUID, WebSocketSession> sessionsByAgentId = new ConcurrentHashMap<>();

    public AgentConnectionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(UUID agentId, WebSocketSession session) {
        sessionsByAgentId.put(agentId, session);
    }

    public void unregister(UUID agentId, WebSocketSession session) {
        sessionsByAgentId.computeIfPresent(agentId, (ignored, currentSession) -> {
            if (currentSession.getId().equals(session.getId())) {
                return null;
            }
            return currentSession;
        });
    }

    public boolean isConnected(UUID agentId) {
        WebSocketSession session = sessionsByAgentId.get(agentId);
        return session != null && session.isOpen();
    }

    public void disconnect(UUID agentId) {
        WebSocketSession session = sessionsByAgentId.remove(agentId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close();
        } catch (IOException ignored) {
            // The registration is already removed; transport cleanup is best effort.
        }
    }

    public void send(UUID agentId, Object payload) {
        WebSocketSession session = sessionsByAgentId.get(agentId);
        if (session == null || !session.isOpen()) {
            throw new AgentNotConnectedException("Agent is not connected");
        }
        try {
            synchronized (session) {
                if (!session.isOpen()) {
                    throw new AgentNotConnectedException("Agent is not connected");
                }
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (IOException exception) {
            throw new AgentNotConnectedException("Could not send message to agent", exception);
        }
    }
}
