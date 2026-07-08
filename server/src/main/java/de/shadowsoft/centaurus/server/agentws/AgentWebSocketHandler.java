package de.shadowsoft.centaurus.server.agentws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.agent.AgentRuntimeService;
import de.shadowsoft.centaurus.server.command.AgentCommandResultService;
import de.shadowsoft.centaurus.server.identity.ServerIdentityKey;
import de.shadowsoft.centaurus.server.identity.ServerIdentityService;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterfaceService;
import de.shadowsoft.centaurus.server.network.NetworkInterfaceSnapshotMessage;
import de.shadowsoft.centaurus.server.script.ScriptManifestIngestionService;
import de.shadowsoft.centaurus.server.script.ScriptManifestMessage;
import de.shadowsoft.centaurus.server.stats.MachineStatsService;
import de.shadowsoft.centaurus.server.stats.StatsSnapshotMessage;
import de.shadowsoft.centaurus.server.statuscheck.MachineStatusCheckService;
import de.shadowsoft.centaurus.server.statuscheck.StatusCheckConfigurationService;
import de.shadowsoft.centaurus.server.statuscheck.StatusSnapshotMessage;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final AgentRuntimeService agentRuntimeService;
    private final ServerIdentityService serverIdentityService;
    private final AgentConnectionRegistry agentConnectionRegistry;
    private final ScriptManifestIngestionService scriptManifestIngestionService;
    private final MachineStatsService machineStatsService;
    private final MachineStatusCheckService machineStatusCheckService;
    private final StatusCheckConfigurationService statusCheckConfigurationService;
    private final MachineNetworkInterfaceService machineNetworkInterfaceService;
    private final AgentCommandResultService agentCommandResultService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, ConnectionState> connections = new ConcurrentHashMap<>();

    public AgentWebSocketHandler(
        ObjectMapper objectMapper,
        AgentRuntimeService agentRuntimeService,
        ServerIdentityService serverIdentityService,
        AgentConnectionRegistry agentConnectionRegistry,
        ScriptManifestIngestionService scriptManifestIngestionService,
        MachineStatsService machineStatsService,
        MachineStatusCheckService machineStatusCheckService,
        StatusCheckConfigurationService statusCheckConfigurationService,
        MachineNetworkInterfaceService machineNetworkInterfaceService,
        AgentCommandResultService agentCommandResultService
    ) {
        this.objectMapper = objectMapper;
        this.agentRuntimeService = agentRuntimeService;
        this.serverIdentityService = serverIdentityService;
        this.agentConnectionRegistry = agentConnectionRegistry;
        this.scriptManifestIngestionService = scriptManifestIngestionService;
        this.machineStatsService = machineStatsService;
        this.machineStatusCheckService = machineStatusCheckService;
        this.statusCheckConfigurationService = statusCheckConfigurationService;
        this.machineNetworkInterfaceService = machineNetworkInterfaceService;
        this.agentCommandResultService = agentCommandResultService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ServerIdentityKey serverIdentityKey = serverIdentityService.activeKey();
        ConnectionState state = new ConnectionState(randomToken(), randomToken(), serverIdentityKey.getKeyId());
        connections.put(session.getId(), state);
        send(session, Map.of(
            "type", "SERVER_CHALLENGE",
            "serverNonce", state.serverNonce(),
            "serverKeyId", state.serverKeyId(),
            "sessionId", state.sessionId()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            handleAgentMessage(session, message);
        } catch (Exception exception) {
            ConnectionState state = connections.get(session.getId());
            LOGGER.warn("Could not process agent WebSocket message session={} agentId={}", session.getId(), state == null ? null : state.agentId(), exception);
            if (session.isOpen()) {
                send(session, Map.of(
                    "type", "ERROR",
                    "message", "Could not process message: " + exception.getMessage()
                ));
            }
        }
    }

    private void handleAgentMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText();
        if ("AGENT_AUTH".equals(type)) {
            handleAgentAuth(session, root);
            return;
        }
        ConnectionState state = requireAuthenticated(session);
        if (state == null) {
            return;
        }
        if ("AGENT_HELLO".equals(type)) {
            agentRuntimeService.markHello(
                state.agentId(),
                optionalText(root, "hostname"),
                optionalText(root, "agentVersion")
            );
            send(session, Map.of("type", "AGENT_HELLO_ACK", "receivedAt", Instant.now()));
            return;
        }
        if ("HEARTBEAT".equals(type)) {
            agentRuntimeService.markHeartbeat(state.agentId());
            send(session, Map.of("type", "HEARTBEAT_ACK", "receivedAt", Instant.now()));
            return;
        }
        if ("SCRIPT_MANIFEST".equals(type)) {
            scriptManifestIngestionService.ingest(state.agentId(), objectMapper.treeToValue(root, ScriptManifestMessage.class));
            send(session, Map.of("type", "SCRIPT_MANIFEST_ACK", "receivedAt", Instant.now()));
            return;
        }
        if ("STATS_SNAPSHOT".equals(type)) {
            machineStatsService.updateLatest(state.agentId(), objectMapper.treeToValue(root, StatsSnapshotMessage.class));
            return;
        }
        if ("STATUS_SNAPSHOT".equals(type)) {
            try {
                machineStatusCheckService.updateLatest(state.agentId(), objectMapper.treeToValue(root, StatusSnapshotMessage.class));
            } catch (RuntimeException exception) {
                LOGGER.warn("Could not process status snapshot from agent {}", state.agentId(), exception);
                send(session, Map.of(
                    "type", "ERROR",
                    "message", "Could not process status snapshot: " + exception.getMessage()
                ));
            }
            return;
        }
        if ("NETWORK_INTERFACES".equals(type)) {
            machineNetworkInterfaceService.replaceForAgent(state.agentId(), objectMapper.treeToValue(root, NetworkInterfaceSnapshotMessage.class));
            return;
        }
        if ("COMMAND_ACCEPTED".equals(type)) {
            agentCommandResultService.markAccepted(UUID.fromString(requiredText(root, "commandId")));
            return;
        }
        if ("COMMAND_REJECTED".equals(type)) {
            agentCommandResultService.markRejected(UUID.fromString(requiredText(root, "commandId")), root);
            return;
        }
        if ("COMMAND_FINISHED".equals(type)) {
            agentCommandResultService.markFinished(UUID.fromString(requiredText(root, "commandId")), root);
            return;
        }
        send(session, Map.of("type", "ERROR", "message", "Unsupported message type: " + type));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ConnectionState state = connections.remove(session.getId());
        if (state != null && state.agentId() != null) {
            agentConnectionRegistry.unregister(state.agentId(), session);
            agentRuntimeService.markDisconnected(state.agentId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        LOGGER.warn("Agent WebSocket transport error for session {}", session.getId(), exception);
    }

    private void handleAgentAuth(WebSocketSession session, JsonNode root) throws IOException {
        ConnectionState state = connections.get(session.getId());
        if (state == null) {
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        UUID agentId = UUID.fromString(requiredText(root, "agentId"));
        String agentKeyId = requiredText(root, "agentKeyId");
        String agentNonce = requiredText(root, "agentNonce");
        String timestamp = requiredText(root, "timestamp");
        String signature = requiredText(root, "signature");
        String signedPayload = AgentWebSocketSignatures.agentAuthPayload(
            agentId.toString(),
            state.serverNonce(),
            agentNonce,
            state.sessionId(),
            timestamp
        );
        Agent agent = agentRuntimeService.authenticate(agentId, agentKeyId, signedPayload, signature);
        agentConnectionRegistry.register(agent.getId(), session);
        ServerIdentityKey serverIdentityKey = serverIdentityService.activeKey();
        String serverTimestamp = Instant.now().toString();
        String serverPayload = AgentWebSocketSignatures.serverAuthPayload(
            agentNonce,
            state.serverNonce(),
            state.sessionId(),
            serverTimestamp
        );
        connections.put(session.getId(), state.authenticated(agent.getId(), agentNonce));
        send(session, Map.of(
            "type", "SERVER_AUTH",
            "serverKeyId", serverIdentityKey.getKeyId(),
            "timestamp", serverTimestamp,
            "signature", serverIdentityService.sign(serverPayload, serverIdentityKey)
        ));
        statusCheckConfigurationService.publishAssignments(agent.getId());
    }

    private ConnectionState requireAuthenticated(WebSocketSession session) throws IOException {
        ConnectionState state = connections.get(session.getId());
        if (state == null || state.agentId() == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Agent is not authenticated"));
            return null;
        }
        return state;
    }

    private String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String optionalText(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private void send(WebSocketSession session, Object payload) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private record ConnectionState(
        String serverNonce,
        String sessionId,
        String serverKeyId,
        UUID agentId,
        String agentNonce
    ) {

        ConnectionState(String serverNonce, String sessionId, String serverKeyId) {
            this(serverNonce, sessionId, serverKeyId, null, null);
        }

        ConnectionState authenticated(UUID agentId, String agentNonce) {
            return new ConnectionState(serverNonce, sessionId, serverKeyId, agentId, agentNonce);
        }
    }
}
