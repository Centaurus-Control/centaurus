package de.shadowsoft.centaurus.agent.auth;

public class AgentUiAuthenticationException extends RuntimeException {

    public AgentUiAuthenticationException(String message) {
        super(message);
    }

    public AgentUiAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
