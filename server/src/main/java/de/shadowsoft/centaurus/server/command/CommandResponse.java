package de.shadowsoft.centaurus.server.command;

import java.time.Instant;
import java.util.UUID;

public record CommandResponse(
    UUID id,
    UUID commandId,
    UUID machineId,
    UUID agentId,
    CommandType commandType,
    CommandStatus status,
    UUID createdByUserId,
    Instant createdAt,
    Instant sentAt,
    Instant acceptedAt,
    Instant finishedAt,
    String payloadJson,
    String resultJson,
    String errorJson
) {

    public static CommandResponse from(Command command) {
        return new CommandResponse(
            command.getId(),
            command.getCommandId(),
            command.getMachine() == null ? null : command.getMachine().getId(),
            command.getAgent() == null ? null : command.getAgent().getId(),
            command.getCommandType(),
            command.getStatus(),
            command.getCreatedByUser() == null ? null : command.getCreatedByUser().getId(),
            command.getCreatedAt(),
            command.getSentAt(),
            command.getAcceptedAt(),
            command.getFinishedAt(),
            command.getPayloadJson(),
            command.getResultJson(),
            command.getErrorJson()
        );
    }
}
