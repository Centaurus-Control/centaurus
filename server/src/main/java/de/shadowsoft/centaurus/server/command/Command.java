package de.shadowsoft.centaurus.server.command;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "commands")
public class Command {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "command_id", nullable = false, unique = true)
    private UUID commandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 80)
    private CommandType commandType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CommandStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User createdByUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "hidden_from_ui", nullable = false)
    private boolean hiddenFromUi;

    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payloadJson;

    @Column(name = "result_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultJson;

    @Column(name = "error_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String errorJson;

    protected Command() {
    }

    public Command(CommandType commandType, String payloadJson) {
        this.id = UUID.randomUUID();
        this.commandId = UUID.randomUUID();
        this.commandType = commandType;
        this.status = CommandStatus.CREATED;
        this.payloadJson = payloadJson;
        this.hiddenFromUi = false;
    }

    public Command(
        Machine machine,
        Agent agent,
        CommandType commandType,
        User createdByUser,
        String payloadJson
    ) {
        this(commandType, payloadJson);
        this.machine = machine;
        this.agent = agent;
        this.createdByUser = createdByUser;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public Machine getMachine() {
        return machine;
    }

    public Agent getAgent() {
        return agent;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public boolean isHiddenFromUi() {
        return hiddenFromUi;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getErrorJson() {
        return errorJson;
    }

    public void markSent(Instant sentAt) {
        this.status = CommandStatus.SENT;
        this.sentAt = sentAt;
    }

    public void markAccepted(Instant acceptedAt) {
        if (this.status != CommandStatus.FINISHED && this.status != CommandStatus.FAILED && this.status != CommandStatus.REJECTED) {
            this.status = CommandStatus.ACCEPTED;
        }
        this.acceptedAt = acceptedAt;
    }

    public void markRejected(Instant finishedAt, String errorJson) {
        this.status = CommandStatus.REJECTED;
        if (this.acceptedAt == null) {
            this.acceptedAt = finishedAt;
        }
        this.finishedAt = finishedAt;
        this.errorJson = errorJson;
    }

    public void markFinished(Instant finishedAt, String resultJson) {
        this.status = CommandStatus.FINISHED;
        if (this.acceptedAt == null) {
            this.acceptedAt = finishedAt;
        }
        this.finishedAt = finishedAt;
        this.resultJson = resultJson;
    }

    public void markFailed(Instant finishedAt, String errorJson) {
        this.status = CommandStatus.FAILED;
        if (this.acceptedAt == null) {
            this.acceptedAt = finishedAt;
        }
        this.finishedAt = finishedAt;
        this.errorJson = errorJson;
    }
}
