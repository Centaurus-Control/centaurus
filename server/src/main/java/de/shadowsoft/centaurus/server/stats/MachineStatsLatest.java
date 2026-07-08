package de.shadowsoft.centaurus.server.stats;

import de.shadowsoft.centaurus.server.agent.Agent;
import de.shadowsoft.centaurus.server.machine.Machine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_stats_latest")
public class MachineStatsLatest {

    @Id
    @Column(name = "machine_id", nullable = false, updatable = false)
    private UUID machineId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "cpu_load", nullable = false)
    private double cpuLoad;

    @Column(name = "memory_used_percent", nullable = false)
    private double memoryUsedPercent;

    @Column(name = "uptime_seconds", nullable = false)
    private long uptimeSeconds;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MachineStatsLatest() {
    }

    public MachineStatsLatest(
        Machine machine,
        Agent agent,
        double cpuLoad,
        double memoryUsedPercent,
        long uptimeSeconds,
        Instant updatedAt
    ) {
        this.machine = machine;
        this.agent = agent;
        this.cpuLoad = cpuLoad;
        this.memoryUsedPercent = memoryUsedPercent;
        this.uptimeSeconds = uptimeSeconds;
        this.updatedAt = updatedAt;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public Machine getMachine() {
        return machine;
    }

    public Agent getAgent() {
        return agent;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public double getMemoryUsedPercent() {
        return memoryUsedPercent;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(Agent agent, double cpuLoad, double memoryUsedPercent, long uptimeSeconds, Instant updatedAt) {
        this.agent = agent;
        this.cpuLoad = cpuLoad;
        this.memoryUsedPercent = memoryUsedPercent;
        this.uptimeSeconds = uptimeSeconds;
        this.updatedAt = updatedAt;
    }
}
