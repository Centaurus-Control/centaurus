package de.shadowsoft.centaurus.agent.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentFileBrowserService {

    public AgentFileListResponse list(String requestedPath) {
        Path path = StringUtils.hasText(requestedPath)
            ? Path.of(requestedPath).toAbsolutePath().normalize()
            : Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Path does not exist");
        }
        if (!Files.isDirectory(path)) {
            path = path.getParent();
        }
        if (path == null) {
            path = Path.of("/").toAbsolutePath().normalize();
        }
        try (var stream = Files.list(path)) {
            List<AgentFileEntryResponse> entries = stream
                .map(this::toEntry)
                .sorted(Comparator
                    .comparing(AgentFileEntryResponse::directory).reversed()
                    .thenComparing(entry -> entry.name().toLowerCase()))
                .toList();
            Path parent = path.getParent();
            return new AgentFileListResponse(path.toString(), parent == null ? null : parent.toString(), entries);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not list path", exception);
        }
    }

    private AgentFileEntryResponse toEntry(Path path) {
        return new AgentFileEntryResponse(
            path.getFileName() == null ? path.toString() : path.getFileName().toString(),
            path.toAbsolutePath().normalize().toString(),
            Files.isDirectory(path),
            Files.isExecutable(path)
        );
    }
}
