package de.shadowsoft.centaurus.agent.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import de.shadowsoft.centaurus.agent.config.AgentProperties;
import de.shadowsoft.centaurus.agent.network.NetworkInterfacePublisher;
import de.shadowsoft.centaurus.agent.script.ScriptExecutionService;
import de.shadowsoft.centaurus.agent.script.ScriptManifestPublisher;
import de.shadowsoft.centaurus.agent.script.ScriptRejectedException;
import de.shadowsoft.centaurus.agent.stats.AgentStatsPublisher;
import de.shadowsoft.centaurus.agent.statuscheck.AgentStatusCheckPublisher;
import de.shadowsoft.centaurus.agent.wol.WakeOnLanService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentConnectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentConnectionService.class);

    private final AgentConfigStore configStore;
    private final AgentProperties agentProperties;
    private final AgentCryptoService cryptoService;
    private final AgentRuntimeMessenger messenger;
    private final ScriptManifestPublisher scriptManifestPublisher;
    private final ScriptExecutionService scriptExecutionService;
    private final AgentStatsPublisher agentStatsPublisher;
    private final AgentStatusCheckPublisher agentStatusCheckPublisher;
    private final NetworkInterfacePublisher networkInterfacePublisher;
    private final WakeOnLanService wakeOnLanService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private volatile WebSocket webSocket;
    private volatile boolean authenticated;
    private volatile String currentServerNonce;
    private volatile String currentAgentNonce;
    private volatile String currentSessionId;
    private volatile Instant lastConnectAttempt = Instant.EPOCH;
    private volatile Instant lastHeartbeatSent = Instant.EPOCH;

    public AgentConnectionService(
        AgentConfigStore configStore,
        AgentProperties agentProperties,
        AgentCryptoService cryptoService,
        AgentRuntimeMessenger messenger,
        ScriptManifestPublisher scriptManifestPublisher,
        ScriptExecutionService scriptExecutionService,
        AgentStatsPublisher agentStatsPublisher,
        AgentStatusCheckPublisher agentStatusCheckPublisher,
        NetworkInterfacePublisher networkInterfacePublisher,
        WakeOnLanService wakeOnLanService,
        ObjectMapper objectMapper
    ) {
        this.configStore = configStore;
        this.agentProperties = agentProperties;
        this.cryptoService = cryptoService;
        this.messenger = messenger;
        this.scriptManifestPublisher = scriptManifestPublisher;
        this.scriptExecutionService = scriptExecutionService;
        this.agentStatsPublisher = agentStatsPublisher;
        this.agentStatusCheckPublisher = agentStatusCheckPublisher;
        this.networkInterfacePublisher = networkInterfacePublisher;
        this.wakeOnLanService = wakeOnLanService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectOnStartup() {
        connectIfNeeded();
    }

    @Scheduled(fixedDelay = 5_000)
    public void reconnectIfNeeded() {
        connectIfNeeded();
    }

    @Scheduled(fixedDelay = 1_000)
    public void sendHeartbeat() {
        WebSocket socket = webSocket;
        if (socket == null || !authenticated) {
            return;
        }
        Instant now = Instant.now();
        if (lastHeartbeatSent.plusSeconds(heartbeatIntervalSeconds()).isAfter(now)) {
            return;
        }
        lastHeartbeatSent = now;
        messenger.send(Map.of("type", "HEARTBEAT", "sentAt", Instant.now()));
    }

    public int heartbeatIntervalSeconds() {
        AgentConfig config = configStore.load();
        if (config.getHeartbeatIntervalSeconds() > 0) {
            return config.getHeartbeatIntervalSeconds();
        }
        return 30;
    }

    public boolean isConnected() {
        return webSocket != null;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void connectNow() {
        close();
        lastConnectAttempt = Instant.EPOCH;
        connectIfNeeded();
    }

    private void connectIfNeeded() {
        if (!agentProperties.getConnection().isAutoConnect()) {
            return;
        }
        if (lastConnectAttempt.plusSeconds(agentProperties.getConnection().getReconnectDelaySeconds()).isAfter(Instant.now())) {
            return;
        }
        if (webSocket != null || !connecting.compareAndSet(false, true)) {
            return;
        }
        AgentConfig config = configStore.load();
        if (!isConnectionConfigComplete(config)) {
            connecting.set(false);
            return;
        }
        lastConnectAttempt = Instant.now();
        httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(config.getWsUrl()), new Listener(config))
            .whenComplete((socket, throwable) -> {
                connecting.set(false);
                if (throwable != null) {
                    logConnectionFailure(config.getWsUrl(), throwable);
                    webSocket = null;
                    authenticated = false;
                    return;
                }
                webSocket = socket;
            });
    }

    private void logConnectionFailure(String wsUrl, Throwable throwable) {
        WebSocketHandshakeException handshakeException = findHandshakeException(throwable);
        if (handshakeException != null) {
            HttpResponse<?> response = handshakeException.getResponse();
            LOGGER.warn(
                "Could not connect to Centaurus server WebSocket at {}. Handshake response status={} headers={}",
                wsUrl,
                response.statusCode(),
                response.headers().map(),
                throwable
            );
            return;
        }
        LOGGER.warn("Could not connect to Centaurus server WebSocket at {}", wsUrl, throwable);
    }

    private WebSocketHandshakeException findHandshakeException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WebSocketHandshakeException handshakeException) {
                return handshakeException;
            }
            if (current instanceof CompletionException && current.getCause() != null) {
                current = current.getCause();
                continue;
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean isConnectionConfigComplete(AgentConfig config) {
        return config.getAgentId() != null
            && StringUtils.hasText(config.getWsUrl())
            && StringUtils.hasText(config.getAgentPrivateKey())
            && StringUtils.hasText(config.getAgentKeyId())
            && StringUtils.hasText(config.getServerPublicKey());
    }

    private void handleMessage(AgentConfig config, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText();
            if ("SERVER_CHALLENGE".equals(type)) {
                handleServerChallenge(config, root);
                return;
            }
            if ("SERVER_AUTH".equals(type)) {
                handleServerAuth(config, root);
                return;
            }
            if ("ERROR".equals(type)) {
                LOGGER.warn("Server WebSocket error: {}", root.path("message").asText());
                return;
            }
            if ("EXECUTE_SCRIPT".equals(type)) {
                handleExecuteScript(root);
                return;
            }
            if ("SEND_WOL".equals(type)) {
                handleWakeOnLan(root);
                return;
            }
            if ("REFRESH_STATS".equals(type)) {
                agentStatsPublisher.publishNow();
                agentStatusCheckPublisher.publishNow();
                return;
            }
            if ("REFRESH_SCRIPT_MANIFEST".equals(type)) {
                scriptManifestPublisher.publish();
                return;
            }
            if ("STATUS_CHECK_CONFIG".equals(type)) {
                agentStatusCheckPublisher.replaceAssignments(root.path("checks"));
                return;
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not process server WebSocket message", exception);
        }
    }

    private void handleServerChallenge(AgentConfig config, JsonNode root) {
        currentServerNonce = root.path("serverNonce").asText();
        currentSessionId = root.path("sessionId").asText();
        currentAgentNonce = randomToken();
        String timestamp = Instant.now().toString();
        String payload = AgentWebSocketSignatures.agentAuthPayload(
            config.getAgentId().toString(),
            currentServerNonce,
            currentAgentNonce,
            currentSessionId,
            timestamp
        );
        messenger.send(Map.of(
            "type", "AGENT_AUTH",
            "agentId", config.getAgentId(),
            "agentKeyId", config.getAgentKeyId(),
            "agentNonce", currentAgentNonce,
            "timestamp", timestamp,
            "signature", cryptoService.sign(config.getAgentPrivateKey(), payload)
        ));
    }

    private void handleServerAuth(AgentConfig config, JsonNode root) {
        String timestamp = root.path("timestamp").asText();
        String signature = root.path("signature").asText();
        String payload = AgentWebSocketSignatures.serverAuthPayload(
            currentAgentNonce,
            currentServerNonce,
            currentSessionId,
            timestamp
        );
        if (!cryptoService.verify(config.getServerPublicKey(), payload, signature)) {
            LOGGER.warn("Server WebSocket authentication failed");
            close();
            return;
        }
        authenticated = true;
        messenger.bind(webSocket);
        sendHello(config);
        scriptManifestPublisher.publish();
        networkInterfacePublisher.publish();
        agentStatsPublisher.publishNow();
        agentStatusCheckPublisher.publishNow();
    }

    private void sendHello(AgentConfig config) {
        messenger.send(Map.of(
            "type", "AGENT_HELLO",
            "agentId", config.getAgentId(),
            "agentVersion", agentProperties.getVersion(),
            "hostname", hostname(),
            "capabilities", List.of("STATS", "SCRIPT_EXECUTION"),
            "sentAt", Instant.now()
        ));
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "unknown-host";
        }
    }

    private void close() {
        WebSocket socket = webSocket;
        webSocket = null;
        authenticated = false;
        messenger.clear(socket);
        if (socket != null) {
            socket.abort();
        }
    }

    private void handleExecuteScript(JsonNode root) {
        UUID commandId = UUID.fromString(root.path("commandId").asText());
        UUID scriptId = UUID.fromString(root.path("scriptId").asText());
        JsonNode parameters = root.path("parameters");
        CompletableFuture.runAsync(() -> {
            try {
                messenger.send(Map.of(
                    "type", "COMMAND_ACCEPTED",
                    "commandId", commandId,
                    "acceptedAt", Instant.now()
                ));
                ScriptExecutionService.ScriptExecutionResult result = scriptExecutionService.execute(scriptId, parameters);
                messenger.send(Map.of(
                    "type", "COMMAND_FINISHED",
                    "commandId", commandId,
                    "scriptId", scriptId,
                    "status", result.status(),
                    "startedAt", result.startedAt(),
                    "finishedAt", result.finishedAt(),
                    "durationMs", result.durationMs(),
                    "result", result.result(),
                    "error", result.error() == null ? Map.of() : result.error()
                ));
            } catch (ScriptRejectedException exception) {
                messenger.send(Map.of(
                    "type", "COMMAND_REJECTED",
                    "commandId", commandId,
                    "reason", exception.getReason(),
                    "message", exception.getMessage()
                ));
            }
        });
    }

    private void handleWakeOnLan(JsonNode root) {
        UUID commandId = UUID.fromString(root.path("commandId").asText());
        CompletableFuture.runAsync(() -> {
            try {
                messenger.send(Map.of("type", "COMMAND_ACCEPTED", "commandId", commandId, "acceptedAt", Instant.now()));
                wakeOnLanService.send(
                    root.path("macAddress").asText(),
                    root.path("broadcastAddress").asText("255.255.255.255"),
                    root.path("port").asInt(9)
                );
                messenger.send(Map.of(
                    "type", "COMMAND_FINISHED",
                    "commandId", commandId,
                    "status", "SUCCESS",
                    "startedAt", Instant.now(),
                    "finishedAt", Instant.now(),
                    "durationMs", 0,
                    "result", Map.of("sent", true),
                    "error", Map.of()
                ));
            } catch (Exception exception) {
                messenger.send(Map.of(
                    "type", "COMMAND_REJECTED",
                    "commandId", commandId,
                    "reason", "WOL_FAILED",
                    "message", exception.getMessage()
                ));
            }
        });
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private class Listener implements WebSocket.Listener {

        private final AgentConfig config;
        private final StringBuilder textBuffer = new StringBuilder();

        Listener(AgentConfig config) {
            this.config = config;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            AgentConnectionService.this.webSocket = webSocket;
            messenger.bind(webSocket);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                handleMessage(config, textBuffer.toString());
                textBuffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            AgentConnectionService.this.webSocket = null;
            authenticated = false;
            messenger.clear(webSocket);
            LOGGER.info("Centaurus server WebSocket closed with status {}: {}", statusCode, reason);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            AgentConnectionService.this.webSocket = null;
            authenticated = false;
            messenger.clear(webSocket);
            LOGGER.warn("Centaurus server WebSocket error", error);
        }
    }
}
