package de.shadowsoft.centaurus.server.agentws;

public class AgentNotConnectedException extends RuntimeException {

    public AgentNotConnectedException(String message) {
        super(message);
    }

    public AgentNotConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
