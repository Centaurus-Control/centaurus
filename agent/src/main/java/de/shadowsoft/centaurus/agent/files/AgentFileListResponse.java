package de.shadowsoft.centaurus.agent.files;

import java.util.List;

public record AgentFileListResponse(
    String path,
    String parentPath,
    List<AgentFileEntryResponse> entries
) {
}
