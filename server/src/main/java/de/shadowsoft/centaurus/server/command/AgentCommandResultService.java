package de.shadowsoft.centaurus.server.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.server.audit.AuditResult;
import de.shadowsoft.centaurus.server.audit.AuditService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentCommandResultService {

    private final CommandRepository commandRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public AgentCommandResultService(
        CommandRepository commandRepository,
        ObjectMapper objectMapper,
        AuditService auditService
    ) {
        this.commandRepository = commandRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional
    public void markAccepted(UUID commandId) {
        commandRepository.findByCommandId(commandId)
            .ifPresent(command -> command.markAccepted(Instant.now()));
    }

    @Transactional
    public void markRejected(UUID commandId, JsonNode message) {
        commandRepository.findByCommandId(commandId)
            .ifPresent(command -> {
                command.markRejected(Instant.now(), toJson(message));
                auditCommandResult(command, AuditResult.FAILURE, "COMMAND_REJECTED");
            });
    }

    @Transactional
    public void markFinished(UUID commandId, JsonNode message) {
        commandRepository.findByCommandId(commandId)
            .ifPresent(command -> {
                String status = message.path("status").asText();
                if ("SUCCESS".equals(status)) {
                    command.markFinished(Instant.now(), toJson(message.path("result")));
                    auditCommandResult(command, AuditResult.SUCCESS, "COMMAND_FINISHED");
                } else {
                    command.markFailed(Instant.now(), toJson(message));
                    auditCommandResult(command, AuditResult.FAILURE, "COMMAND_FAILED");
                }
            });
    }

    private void auditCommandResult(Command command, AuditResult result, String action) {
        auditService.record(
            action,
            result,
            command.getCreatedByUser(),
            command.getCreatedByUser() == null ? null : command.getCreatedByUser().getUsername(),
            "COMMAND",
            command.getId(),
            command.getCommandId().toString(),
            AuditService.details(
                "commandId", command.getCommandId(),
                "commandType", command.getCommandType(),
                "status", command.getStatus(),
                "machineId", command.getMachine() == null ? null : command.getMachine().getId(),
                "agentId", command.getAgent() == null ? null : command.getAgent().getId()
            )
        );
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize command result", exception);
        }
    }
}
