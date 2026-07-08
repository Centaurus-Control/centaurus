package de.shadowsoft.centaurus.server.command;

public class CommandDispatchException extends RuntimeException {

    public CommandDispatchException(String message) {
        super(message);
    }

    public CommandDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
