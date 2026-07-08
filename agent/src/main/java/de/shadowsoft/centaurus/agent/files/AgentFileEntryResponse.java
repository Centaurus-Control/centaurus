package de.shadowsoft.centaurus.agent.files;

public record AgentFileEntryResponse(
    String name,
    String path,
    boolean directory,
    boolean executable
) {
}
