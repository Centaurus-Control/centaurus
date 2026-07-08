package de.shadowsoft.centaurus.agent.statuscheck;

import de.shadowsoft.centaurus.agent.connection.AgentRuntimeMessenger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.agent.script.ScriptExecutionService;
import de.shadowsoft.centaurus.agent.script.ScriptRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentStatusCheckPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentStatusCheckPublisher.class);
    private static final int OUTPUT_LIMIT = 1_024;

    private final AgentRuntimeMessenger messenger;
    private final ScriptExecutionService scriptExecutionService;
    private final ObjectMapper objectMapper;
    private final List<StatusCheckAssignment> assignments = new CopyOnWriteArrayList<>();
    private final Map<UUID, Instant> lastRunByConfigurationId = new ConcurrentHashMap<>();
    private final Set<UUID> runningConfigurationIds = ConcurrentHashMap.newKeySet();

    public AgentStatusCheckPublisher(
        AgentRuntimeMessenger messenger,
        ScriptExecutionService scriptExecutionService,
        ObjectMapper objectMapper
    ) {
        this.messenger = messenger;
        this.scriptExecutionService = scriptExecutionService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1_000)
    public void publishScheduled() {
        if (!messenger.isConnected()) {
            return;
        }
        Instant now = Instant.now();
        for (StatusCheckAssignment assignment : assignments) {
            if (!assignment.enabled() || assignment.configurationId() == null || assignment.scriptId() == null) {
                continue;
            }
            int intervalSeconds = assignment.intervalSeconds() > 0 ? assignment.intervalSeconds() : 30;
            Instant lastRun = lastRunByConfigurationId.get(assignment.configurationId());
            if (lastRun != null && lastRun.plusSeconds(intervalSeconds).isAfter(now)) {
                continue;
            }
            publishCheckIfIdle(assignment, now);
        }
    }

    public void publishNow() {
        if (!messenger.isConnected()) {
            return;
        }
        for (StatusCheckAssignment assignment : assignments) {
            if (assignment.enabled() && assignment.configurationId() != null && assignment.scriptId() != null) {
                publishCheckIfIdle(assignment, Instant.now());
            }
        }
    }

    public void replaceAssignments(JsonNode checksNode) {
        List<StatusCheckAssignment> updatedAssignments = objectMapper.convertValue(
            checksNode,
            objectMapper.getTypeFactory().constructCollectionType(List.class, StatusCheckAssignment.class)
        );
        assignments.clear();
        assignments.addAll(updatedAssignments);
        lastRunByConfigurationId.keySet().retainAll(
            updatedAssignments.stream().map(StatusCheckAssignment::configurationId).collect(java.util.stream.Collectors.toSet())
        );
        LOGGER.info("Loaded {} status check assignments", updatedAssignments.size());
    }

    private void publishCheckIfIdle(StatusCheckAssignment assignment, Instant startedAt) {
        if (!runningConfigurationIds.add(assignment.configurationId())) {
            return;
        }
        try {
            lastRunByConfigurationId.put(assignment.configurationId(), startedAt);
            publishCheck(assignment);
        } finally {
            runningConfigurationIds.remove(assignment.configurationId());
        }
    }

    private void publishCheck(StatusCheckAssignment assignment) {
        Instant checkedAt = Instant.now();
        try {
            ScriptExecutionService.ScriptExecutionResult executionResult = scriptExecutionService.executeStatusCheck(
                assignment.scriptId(),
                objectMapper.valueToTree(assignment.parameters() == null ? Map.of() : assignment.parameters())
            );
            Map<String, Object> result = executionResult.result();
            String stdout = String.valueOf(result.getOrDefault("stdout", ""));
            String stderr = String.valueOf(result.getOrDefault("stderr", ""));
            Integer exitCode = result.get("exitCode") instanceof Number number ? number.intValue() : null;
            Boolean healthy = parseHealth(stdout);
            if (healthy == null && (exitCode != null && (exitCode == 0 || exitCode == 1))) {
                healthy = exitCode == 0;
            }
            publishResult(
                assignment,
                healthy,
                exitCode,
                stdout,
                stderr,
                checkedAt,
                healthy == null ? "UNSUPPORTED_RESULT" : null
            );
        } catch (ScriptRejectedException exception) {
            LOGGER.warn("Status check rejected id={} label=\"{}\" reason={}", assignment.configurationId(), assignment.label(), exception.getReason());
            publishResult(assignment, null, null, "", exception.getMessage(), checkedAt, exception.getReason());
        } catch (Exception exception) {
            LOGGER.warn("Status check failed id={} label=\"{}\" message={}", assignment.configurationId(), assignment.label(), exception.getMessage(), exception);
            publishResult(assignment, null, null, "", exception.getMessage(), checkedAt, "EXECUTION_FAILED");
        }
    }

    private void publishResult(
        StatusCheckAssignment assignment,
        Boolean healthy,
        Integer exitCode,
        String stdout,
        String stderr,
        Instant checkedAt,
        String error
    ) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("id", assignment.configurationId());
        status.put("label", assignment.label());
        status.put("healthy", healthy);
        status.put("exitCode", exitCode);
        status.put("stdout", truncate(stdout));
        status.put("stderr", truncate(stderr));
        status.put("checkedAt", checkedAt);
        status.put("sortOrder", assignment.sortOrder());
        if (StringUtils.hasText(error)) {
            status.put("error", error);
        }
        messenger.send(Map.of(
            "type", "STATUS_SNAPSHOT",
            "statuses", List.of(status),
            "sampledAt", checkedAt
        ));
    }

    private Boolean parseHealth(String stdout) {
        if (!StringUtils.hasText(stdout)) {
            return null;
        }
        String normalized = stdout.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized)) {
            return false;
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace("\r", "\\r").replace("\n", "\\n");
        if (sanitized.length() <= OUTPUT_LIMIT) {
            return sanitized;
        }
        return sanitized.substring(0, OUTPUT_LIMIT) + "...<truncated>";
    }

    public record StatusCheckAssignment(
        UUID configurationId,
        UUID scriptId,
        String label,
        boolean enabled,
        int intervalSeconds,
        int sortOrder,
        Map<String, Object> parameters
    ) {
    }
}
