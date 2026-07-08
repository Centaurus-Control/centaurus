package de.shadowsoft.centaurus.server.scriptconfig;

public class ScriptConfigurationException extends RuntimeException {

    public ScriptConfigurationException(String message) {
        super(message);
    }

    public ScriptConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
