package de.shadowsoft.centaurus.agent.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AgentScriptConfig {

    private UUID id;
    private String label;
    private String description;
    private String command;
    private List<String> arguments = List.of();
    private List<ScriptArgumentConfig> argumentMappings = List.of();
    private String workingDirectory;
    private int timeoutSeconds = 900;
    private Map<String, Object> parameters = new LinkedHashMap<>();
    private Map<String, Object> resultSchema = new LinkedHashMap<>();
    private SpamProtection spamProtection = new SpamProtection();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments == null ? List.of() : arguments;
    }

    public List<ScriptArgumentConfig> getArgumentMappings() {
        return argumentMappings;
    }

    public void setArgumentMappings(List<ScriptArgumentConfig> argumentMappings) {
        this.argumentMappings = argumentMappings == null ? List.of() : argumentMappings;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? new LinkedHashMap<>() : parameters;
    }

    public Map<String, Object> getResultSchema() {
        return resultSchema;
    }

    public void setResultSchema(Map<String, Object> resultSchema) {
        this.resultSchema = resultSchema == null ? new LinkedHashMap<>() : resultSchema;
    }

    public SpamProtection getSpamProtection() {
        return spamProtection;
    }

    public void setSpamProtection(SpamProtection spamProtection) {
        this.spamProtection = spamProtection == null ? new SpamProtection() : spamProtection;
    }

    public static class SpamProtection {

        private boolean enabled = true;
        private int cooldownSeconds = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(int cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    public static class ScriptArgumentConfig {

        private ArgumentType type = ArgumentType.FIXED;
        private String name;
        private String value;
        private String parameterName;

        public ArgumentType getType() {
            return type;
        }

        public void setType(ArgumentType type) {
            this.type = type == null ? ArgumentType.FIXED : type;
        }

        public void setSource(ArgumentType source) {
            setType(source);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getParameterName() {
            return parameterName;
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }
    }

    public enum ArgumentType {
        FIXED,
        PARAMETER,
        NAMED_PARAMETER,
        FLAG_PARAMETER
    }
}
