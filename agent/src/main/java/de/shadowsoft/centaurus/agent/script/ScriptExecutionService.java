package de.shadowsoft.centaurus.agent.script;

import com.fasterxml.jackson.databind.JsonNode;
import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import de.shadowsoft.centaurus.agent.config.AgentScriptConfig;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ScriptExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptExecutionService.class);
    private static final int LOG_OUTPUT_LIMIT = 8_192;

    private final AgentConfigStore configStore;
    private final Map<UUID, Instant> lastExecutionByScriptId = new ConcurrentHashMap<>();
    private final Executor outputReaderExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ScriptExecutionService(AgentConfigStore configStore) {
        this.configStore = configStore;
    }

    public ScriptExecutionResult execute(UUID scriptId, JsonNode parameters) {
        return execute(scriptId, parameters, true);
    }

    public ScriptExecutionResult executeStatusCheck(UUID scriptId, JsonNode parameters) {
        return execute(scriptId, parameters, false);
    }

    private ScriptExecutionResult execute(UUID scriptId, JsonNode parameters, boolean enforceCooldown) {
        AgentScriptConfig script = findScript(scriptId);
        if (enforceCooldown) {
            validateCooldown(script);
        }
        Map<String, Object> parameterValues = validateParameters(script, parameters);
        Instant startedAt = Instant.now();
        List<String> command = command(script, parameterValues);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (StringUtils.hasText(script.getWorkingDirectory())) {
            processBuilder.directory(new File(script.getWorkingDirectory()));
        }
        parameterValues.forEach((key, value) -> processBuilder.environment().put(
            "CENTAURUS_PARAM_" + key.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_"),
            String.valueOf(value)
        ));
        try {
            LOGGER.info(
                "Starting script id={} label=\"{}\" command={} workingDirectory={}",
                script.getId(),
                script.getLabel(),
                command,
                processBuilder.directory() == null ? null : processBuilder.directory().getAbsolutePath()
            );
            Process process = processBuilder.start();
            CompletableFuture<String> stdoutFuture = readOutput(process.getInputStream());
            CompletableFuture<String> stderrFuture = readOutput(process.getErrorStream());
            boolean finished = process.waitFor(script.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                String stdout = awaitOutput(stdoutFuture);
                String stderr = awaitOutput(stderrFuture);
                LOGGER.warn(
                    "Script timed out id={} label=\"{}\" timeoutSeconds={} stdout=\"{}\" stderr=\"{}\"",
                    script.getId(),
                    script.getLabel(),
                    script.getTimeoutSeconds(),
                    truncate(stdout),
                    truncate(stderr)
                );
                throw new ScriptRejectedException("TIMEOUT", "Script execution timed out");
            }
            Instant finishedAt = Instant.now();
            String stdout = stdoutFuture.join();
            String stderr = stderrFuture.join();
            lastExecutionByScriptId.put(script.getId(), finishedAt);
            if (process.exitValue() == 0) {
                LOGGER.info(
                    "Script finished id={} label=\"{}\" exitCode={} durationMs={} stdout=\"{}\" stderr=\"{}\"",
                    script.getId(),
                    script.getLabel(),
                    process.exitValue(),
                    Duration.between(startedAt, finishedAt).toMillis(),
                    truncate(stdout),
                    truncate(stderr)
                );
            } else {
                LOGGER.warn(
                    "Script failed id={} label=\"{}\" exitCode={} durationMs={} stdout=\"{}\" stderr=\"{}\"",
                    script.getId(),
                    script.getLabel(),
                    process.exitValue(),
                    Duration.between(startedAt, finishedAt).toMillis(),
                    truncate(stdout),
                    truncate(stderr)
                );
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("exitCode", process.exitValue());
            result.put("stdout", stdout);
            result.put("stderr", stderr);
            result.put("status", process.exitValue() == 0 ? "SUCCESS" : "FAILED");
            return new ScriptExecutionResult(
                process.exitValue() == 0 ? "SUCCESS" : "FAILED",
                startedAt,
                finishedAt,
                Duration.between(startedAt, finishedAt).toMillis(),
                result,
                process.exitValue() == 0 ? null : Map.of("message", "Script exited with code " + process.exitValue())
            );
        } catch (ScriptRejectedException exception) {
            LOGGER.warn("Script rejected id={} label=\"{}\" reason={} message={}", script.getId(), script.getLabel(), exception.getReason(), exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            LOGGER.warn("Script execution failed id={} label=\"{}\" message={}", script.getId(), script.getLabel(), exception.getMessage(), exception);
            throw new ScriptRejectedException("EXECUTION_FAILED", exception.getMessage());
        }
    }

    private CompletableFuture<String> readOutput(java.io.InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (inputStream) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception exception) {
                return "<could not read output: " + exception.getMessage() + ">";
            }
        }, outputReaderExecutor);
    }

    private String awaitOutput(CompletableFuture<String> outputFuture) {
        try {
            return outputFuture.get(1, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return "<output unavailable>";
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace("\r", "\\r").replace("\n", "\\n");
        if (sanitized.length() <= LOG_OUTPUT_LIMIT) {
            return sanitized;
        }
        return sanitized.substring(0, LOG_OUTPUT_LIMIT) + "...<truncated>";
    }

    private AgentScriptConfig findScript(UUID scriptId) {
        AgentConfig config = configStore.load();
        return config.getScripts()
            .stream()
            .filter(script -> scriptId.equals(script.getId()))
            .findFirst()
            .orElseThrow(() -> new ScriptRejectedException("UNKNOWN_SCRIPT", "Script is not configured on this agent"));
    }

    private void validateCooldown(AgentScriptConfig script) {
        if (!script.getSpamProtection().isEnabled()) {
            return;
        }
        Instant lastExecution = lastExecutionByScriptId.get(script.getId());
        if (lastExecution == null) {
            return;
        }
        if (lastExecution.plusSeconds(script.getSpamProtection().getCooldownSeconds()).isAfter(Instant.now())) {
            throw new ScriptRejectedException("COOLDOWN_ACTIVE", "Script cooldown is still active");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> validateParameters(AgentScriptConfig script, JsonNode parameters) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> schema = script.getParameters();
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> definition = entry.getValue() instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
            boolean required = Boolean.TRUE.equals(definition.get("required"));
            JsonNode value = parameters == null ? null : parameters.get(name);
            if ((value == null || value.isNull()) && required) {
                throw new ScriptRejectedException("MISSING_PARAMETER", name + " is required");
            }
            if (value == null || value.isNull()) {
                if (definition.containsKey("default")) {
                    result.put(name, definition.get("default"));
                }
                continue;
            }
            result.put(name, validateParameterValue(name, definition, value));
        }
        if (parameters != null) {
            parameters.fieldNames().forEachRemaining(name -> {
                if (!schema.containsKey(name)) {
                    throw new ScriptRejectedException("UNKNOWN_PARAMETER", name + " is not allowed");
                }
            });
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object validateParameterValue(String name, Map<String, Object> definition, JsonNode value) {
        String type = String.valueOf(definition.getOrDefault("type", "string"));
        return switch (type) {
            case "boolean" -> {
                if (!value.isBoolean()) {
                    throw new ScriptRejectedException("INVALID_PARAMETER_TYPE", name + " must be boolean");
                }
                yield value.booleanValue();
            }
            case "bool" -> {
                if (!value.isBoolean()) {
                    throw new ScriptRejectedException("INVALID_PARAMETER_TYPE", name + " must be boolean");
                }
                yield value.booleanValue();
            }
            case "integer" -> {
                if (!value.isInt() && !value.isLong()) {
                    throw new ScriptRejectedException("INVALID_PARAMETER_TYPE", name + " must be integer");
                }
                yield value.longValue();
            }
            case "int" -> {
                if (!value.isInt() && !value.isLong()) {
                    throw new ScriptRejectedException("INVALID_PARAMETER_TYPE", name + " must be integer");
                }
                yield value.longValue();
            }
            case "string[]" -> validateArrayParameter(name, value, JsonNode::isTextual, "string");
            case "integer[]", "int[]" -> validateArrayParameter(name, value, item -> item.isInt() || item.isLong(), "integer");
            case "enum" -> {
                String text = value.asText();
                List<Object> allowed = definition.get("allowedValues") instanceof List<?> list
                    ? (List<Object>) list
                    : List.of();
                if (!allowed.contains(text)) {
                    throw new ScriptRejectedException("INVALID_PARAMETER_VALUE", name + " has an unsupported value");
                }
                yield text;
            }
            default -> value.asText();
        };
    }

    private List<Object> validateArrayParameter(
        String name,
        JsonNode value,
        java.util.function.Predicate<JsonNode> validator,
        String expectedType
    ) {
        if (!value.isArray()) {
            throw new ScriptRejectedException("INVALID_PARAMETER_TYPE", name + " must be " + expectedType + "[]");
        }
        List<Object> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!validator.test(item)) {
                throw new ScriptRejectedException(
                    "INVALID_PARAMETER_TYPE",
                    name + " must contain only " + expectedType + " values"
                );
            }
            result.add("integer".equals(expectedType) ? item.longValue() : item.asText());
        }
        return result;
    }

    private List<String> command(AgentScriptConfig script, Map<String, Object> parameterValues) {
        if (!StringUtils.hasText(script.getCommand())) {
            throw new ScriptRejectedException("SCRIPT_NOT_EXECUTABLE", "Script command is not configured");
        }
        List<String> command = new ArrayList<>();
        command.add(script.getCommand());
        if (script.getArgumentMappings().isEmpty()) {
            command.addAll(script.getArguments());
            return command;
        }
        for (AgentScriptConfig.ScriptArgumentConfig mapping : script.getArgumentMappings()) {
            switch (mapping.getType()) {
                case FIXED -> appendFixedArgument(command, mapping);
                case PARAMETER -> appendParameterArgument(command, mapping, parameterValues);
                case NAMED_PARAMETER -> appendNamedParameterArgument(command, mapping, parameterValues);
                case FLAG_PARAMETER -> appendFlagParameterArgument(command, mapping, parameterValues);
            }
        }
        return command;
    }

    private void appendFixedArgument(List<String> command, AgentScriptConfig.ScriptArgumentConfig mapping) {
        if (StringUtils.hasText(mapping.getValue())) {
            command.add(mapping.getValue());
        }
    }

    private void appendParameterArgument(
        List<String> command,
        AgentScriptConfig.ScriptArgumentConfig mapping,
        Map<String, Object> parameterValues
    ) {
        String parameterName = mapping.getParameterName();
        if (!StringUtils.hasText(parameterName) || !parameterValues.containsKey(parameterName)) {
            return;
        }
        Object value = parameterValues.get(parameterName);
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                command.add(String.valueOf(item));
            }
            return;
        }
        command.add(String.valueOf(value));
    }

    private void appendNamedParameterArgument(
        List<String> command,
        AgentScriptConfig.ScriptArgumentConfig mapping,
        Map<String, Object> parameterValues
    ) {
        if (!StringUtils.hasText(mapping.getName())) {
            return;
        }
        int sizeBefore = command.size();
        appendParameterArgument(command, mapping, parameterValues);
        if (command.size() == sizeBefore) {
            return;
        }
        command.add(sizeBefore, mapping.getName());
    }

    private void appendFlagParameterArgument(
        List<String> command,
        AgentScriptConfig.ScriptArgumentConfig mapping,
        Map<String, Object> parameterValues
    ) {
        if (!StringUtils.hasText(mapping.getName()) || !StringUtils.hasText(mapping.getParameterName())) {
            return;
        }
        Object value = parameterValues.get(mapping.getParameterName());
        if (Boolean.TRUE.equals(value)) {
            command.add(mapping.getName());
        }
    }

    public record ScriptExecutionResult(
        String status,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        Map<String, Object> result,
        Map<String, Object> error
    ) {
    }
}
