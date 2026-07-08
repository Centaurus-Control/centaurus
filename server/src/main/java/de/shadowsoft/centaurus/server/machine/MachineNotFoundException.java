package de.shadowsoft.centaurus.server.machine;

public class MachineNotFoundException extends RuntimeException {

    public MachineNotFoundException(String message) {
        super(message);
    }
}
