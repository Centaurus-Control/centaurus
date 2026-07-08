package de.shadowsoft.centaurus.server.scriptconfig;

import de.shadowsoft.centaurus.server.machine.Machine;
import de.shadowsoft.centaurus.server.script.ScriptDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "script_button_configurations")
public class ScriptButtonConfiguration {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "script_definition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScriptDefinition scriptDefinition;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "parameters_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String parametersJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScriptButtonConfiguration() {
    }

    public ScriptButtonConfiguration(
        Machine machine,
        ScriptDefinition scriptDefinition,
        String label,
        boolean enabled,
        int sortOrder,
        String parametersJson
    ) {
        this.id = UUID.randomUUID();
        this.machine = machine;
        this.scriptDefinition = scriptDefinition;
        this.label = label;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.parametersJson = parametersJson;
    }

    public UUID getId() {
        return id;
    }

    public Machine getMachine() {
        return machine;
    }

    public ScriptDefinition getScriptDefinition() {
        return scriptDefinition;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(ScriptDefinition scriptDefinition, String label, boolean enabled, int sortOrder, String parametersJson) {
        this.scriptDefinition = scriptDefinition;
        this.label = label;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.parametersJson = parametersJson;
    }
}
