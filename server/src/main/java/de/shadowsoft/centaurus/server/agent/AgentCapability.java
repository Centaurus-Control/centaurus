package de.shadowsoft.centaurus.server.agent;

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
import java.util.UUID;

@Entity
@Table(
    name = "agent_capabilities",
    uniqueConstraints = @UniqueConstraint(name = "uk_agent_capability", columnNames = {"agent_id", "capability"})
)
public class AgentCapability {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AgentCapabilityType capability;

    @Column(nullable = false)
    private boolean enabled;

    protected AgentCapability() {
    }

    public AgentCapability(Agent agent, AgentCapabilityType capability, boolean enabled) {
        this.id = UUID.randomUUID();
        this.agent = agent;
        this.capability = capability;
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public Agent getAgent() {
        return agent;
    }

    public AgentCapabilityType getCapability() {
        return capability;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
