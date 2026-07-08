package de.shadowsoft.centaurus.agent.stats;

import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import de.shadowsoft.centaurus.agent.connection.AgentRuntimeMessenger;
import java.time.Instant;
import java.util.Map;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AgentStatsPublisher {

    private final AgentConfigStore configStore;
    private final AgentRuntimeMessenger messenger;
    private final SystemInfo systemInfo = new SystemInfo();
    private long[] previousCpuTicks;
    private Instant lastPublished = Instant.EPOCH;

    public AgentStatsPublisher(AgentConfigStore configStore, AgentRuntimeMessenger messenger) {
        this.configStore = configStore;
        this.messenger = messenger;
        this.previousCpuTicks = systemInfo.getHardware().getProcessor().getSystemCpuLoadTicks();
    }

    @Scheduled(fixedDelay = 1_000)
    public void publishScheduled() {
        AgentConfig config = configStore.load();
        int interval = config.getStatsIntervalSeconds() > 0 ? config.getStatsIntervalSeconds() : 30;
        if (lastPublished.plusSeconds(interval).isAfter(Instant.now())) {
            return;
        }
        publishNow();
    }

    public void publishNow() {
        if (!messenger.isConnected()) {
            return;
        }
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(previousCpuTicks);
        previousCpuTicks = processor.getSystemCpuLoadTicks();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        double memoryUsedPercent = memory.getTotal() == 0
            ? 0
            : ((double) (memory.getTotal() - memory.getAvailable()) / (double) memory.getTotal()) * 100.0;
        lastPublished = Instant.now();
        messenger.send(Map.of(
            "type", "STATS_SNAPSHOT",
            "cpuLoad", cpuLoad,
            "memoryUsedPercent", memoryUsedPercent,
            "uptimeSeconds", systemInfo.getOperatingSystem().getSystemUptime(),
            "sampledAt", lastPublished
        ));
    }
}
