package de.shadowsoft.centaurus.agent.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentRuntimeMessenger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRuntimeMessenger.class);

    private final ObjectMapper objectMapper;
    private volatile WebSocket webSocket;

    public AgentRuntimeMessenger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void bind(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void clear(WebSocket webSocket) {
        if (this.webSocket == webSocket) {
            this.webSocket = null;
        }
    }

    public boolean isConnected() {
        return webSocket != null;
    }

    public void send(Object payload) {
        WebSocket socket = webSocket;
        if (socket == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (this) {
                if (webSocket == socket) {
                    socket.sendText(json, true).join();
                }
            }
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Could not encode WebSocket message", exception);
        } catch (CompletionException | IllegalStateException exception) {
            LOGGER.warn("Could not send WebSocket message", exception);
        }
    }
}
