package de.shadowsoft.centaurus.server.machinefunction;

import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.scriptconfig.ScriptButtonConfiguration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "machine_function_assignments",
    uniqueConstraints = @UniqueConstraint(name = "uk_machine_function", columnNames = {"machine_id", "function_type"})
)
public class MachineFunctionAssignment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(name = "function_type", nullable = false, length = 50)
    private MachineFunctionType functionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_configuration_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ScriptButtonConfiguration scriptConfiguration;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MachineFunctionAssignment() {
    }

    public MachineFunctionAssignment(
        Machine machine,
        MachineFunctionType functionType,
        ScriptButtonConfiguration scriptConfiguration
    ) {
        this.id = UUID.randomUUID();
        this.machine = machine;
        this.functionType = functionType;
        this.scriptConfiguration = scriptConfiguration;
    }

    public UUID getId() {
        return id;
    }

    public Machine getMachine() {
        return machine;
    }

    public MachineFunctionType getFunctionType() {
        return functionType;
    }

    public ScriptButtonConfiguration getScriptConfiguration() {
        return scriptConfiguration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assign(ScriptButtonConfiguration scriptConfiguration) {
        this.scriptConfiguration = scriptConfiguration;
    }
}
