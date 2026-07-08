package de.shadowsoft.centaurus.server.script;

import de.shadowsoft.centaurus.server.agent.Agent;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "script_definitions",
    uniqueConstraints = @UniqueConstraint(name = "uk_agent_script_id", columnNames = {"agent_id", "script_id"})
)
public class ScriptDefinition {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "script_id", nullable = false)
    private UUID scriptId;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "manifest_version", nullable = false)
    private long manifestVersion;

    @Column(name = "parameter_schema_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String parameterSchemaJson;

    @Column(name = "result_schema_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultSchemaJson;

    @Column(nullable = false)
    private boolean active;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScriptDefinition() {
    }

    public ScriptDefinition(
        Agent agent,
        UUID scriptId,
        String label,
        long manifestVersion,
        String parameterSchemaJson,
        String resultSchemaJson
    ) {
        this.id = UUID.randomUUID();
        this.agent = agent;
        this.scriptId = scriptId;
        this.label = label;
        this.manifestVersion = manifestVersion;
        this.parameterSchemaJson = parameterSchemaJson;
        this.resultSchemaJson = resultSchemaJson;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public Agent getAgent() {
        return agent;
    }

    public UUID getScriptId() {
        return scriptId;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public long getManifestVersion() {
        return manifestVersion;
    }

    public String getParameterSchemaJson() {
        return parameterSchemaJson;
    }

    public String getResultSchemaJson() {
        return resultSchemaJson;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateFromManifest(
        String label,
        String description,
        long manifestVersion,
        String parameterSchemaJson,
        String resultSchemaJson
    ) {
        this.label = label;
        this.description = description;
        this.manifestVersion = manifestVersion;
        this.parameterSchemaJson = parameterSchemaJson;
        this.resultSchemaJson = resultSchemaJson;
        this.active = true;
    }
}
