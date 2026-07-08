package de.shadowsoft.centaurus.agent.script;

public class ScriptRejectedException extends RuntimeException {

    private final String reason;

    public ScriptRejectedException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
