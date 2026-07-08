package de.shadowsoft.centaurus.server.audit;

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
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @Column(name = "username")
    private String username;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "target_label")
    private String targetLabel;

    @Column(name = "details_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String detailsJson;

    protected AuditEvent() {
    }

    public AuditEvent(
        String action,
        AuditResult result,
        User user,
        String username,
        String targetType,
        UUID targetId,
        String targetLabel,
        String detailsJson
    ) {
        this.id = UUID.randomUUID();
        this.action = action;
        this.result = result;
        this.user = user;
        this.username = username;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetLabel = targetLabel;
        this.detailsJson = detailsJson;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAction() {
        return action;
    }

    public AuditResult getResult() {
        return result;
    }

    public User getUser() {
        return user;
    }

    public String getUsername() {
        return username;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String getDetailsJson() {
        return detailsJson;
    }
}
