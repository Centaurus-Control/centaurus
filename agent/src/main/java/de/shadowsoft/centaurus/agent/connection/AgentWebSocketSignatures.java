package de.shadowsoft.centaurus.agent.connection;

public final class AgentWebSocketSignatures {

    private AgentWebSocketSignatures() {
    }

    public static String agentAuthPayload(
        String agentId,
        String serverNonce,
        String agentNonce,
        String sessionId,
        String timestamp
    ) {
        return String.join("\n", agentId, serverNonce, agentNonce, sessionId, timestamp);
    }

    public static String serverAuthPayload(
        String agentNonce,
        String serverNonce,
        String sessionId,
        String timestamp
    ) {
        return String.join("\n", agentNonce, serverNonce, sessionId, timestamp);
    }
}
