package de.shadowsoft.centaurus.server.statuscheck;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.machine.Machine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "machine_status_check_latest",
    uniqueConstraints = @UniqueConstraint(name = "uk_machine_status_check_latest_check", columnNames = {"machine_id", "check_id"})
)
public class MachineStatusCheckLatest {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Agent agent;

    @Column(name = "check_id", nullable = false)
    private UUID checkId;

    @Column(nullable = false)
    private String label;

    @Column
    private Boolean healthy;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(length = 2048)
    private String stdout;

    @Column(length = 2048)
    private String stderr;

    @Column(length = 100)
    private String error;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MachineStatusCheckLatest() {
    }

    public MachineStatusCheckLatest(
        Machine machine,
        Agent agent,
        UUID checkId,
        String label,
        Boolean healthy,
        Integer exitCode,
        String stdout,
        String stderr,
        String error,
        int sortOrder,
        Instant checkedAt,
        Instant updatedAt
    ) {
        this.id = UUID.randomUUID();
        this.machine = machine;
        this.agent = agent;
        this.checkId = checkId;
        update(agent, label, healthy, exitCode, stdout, stderr, error, sortOrder, checkedAt, updatedAt);
    }

    public UUID getId() {
        return id;
    }

    public Machine getMachine() {
        return machine;
    }

    public Agent getAgent() {
        return agent;
    }

    public UUID getCheckId() {
        return checkId;
    }

    public String getLabel() {
        return label;
    }

    public Boolean getHealthy() {
        return healthy;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public String getError() {
        return error;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
        Agent agent,
        String label,
        Boolean healthy,
        Integer exitCode,
        String stdout,
        String stderr,
        String error,
        int sortOrder,
        Instant checkedAt,
        Instant updatedAt
    ) {
        this.agent = agent;
        this.label = label;
        this.healthy = healthy;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.error = error;
        this.sortOrder = sortOrder;
        this.checkedAt = checkedAt;
        this.updatedAt = updatedAt;
    }
}
