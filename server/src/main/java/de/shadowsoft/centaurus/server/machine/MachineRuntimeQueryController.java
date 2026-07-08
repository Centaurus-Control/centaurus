package de.shadowsoft.centaurus.server.machine;

import de.shadowsoft.centaurus.server.command.CommandResponse;
import de.shadowsoft.centaurus.server.network.MachineNetworkInterfaceResponse;
import de.shadowsoft.centaurus.server.script.ScriptDefinitionResponse;
import de.shadowsoft.centaurus.server.stats.MachineStatsLatestResponse;
import de.shadowsoft.centaurus.server.statuscheck.MachineStatusCheckLatestResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/machines/{machineId}")
public class MachineRuntimeQueryController {

    private final MachineRuntimeQueryService machineRuntimeQueryService;

    public MachineRuntimeQueryController(MachineRuntimeQueryService machineRuntimeQueryService) {
        this.machineRuntimeQueryService = machineRuntimeQueryService;
    }

    @GetMapping("/scripts")
    public List<ScriptDefinitionResponse> listScripts(@PathVariable UUID machineId) {
        return machineRuntimeQueryService.listScripts(machineId);
    }

    @GetMapping("/stats/latest")
    public MachineStatsLatestResponse latestStats(@PathVariable UUID machineId) {
        return machineRuntimeQueryService.latestStats(machineId);
    }

    @GetMapping("/status-checks/latest")
    public List<MachineStatusCheckLatestResponse> listLatestStatusChecks(@PathVariable UUID machineId) {
        return machineRuntimeQueryService.listLatestStatusChecks(machineId);
    }

    @GetMapping("/network-interfaces")
    public List<MachineNetworkInterfaceResponse> listNetworkInterfaces(@PathVariable UUID machineId) {
        return machineRuntimeQueryService.listNetworkInterfaces(machineId);
    }

    @GetMapping("/commands")
    public List<CommandResponse> listCommands(@PathVariable UUID machineId) {
        return machineRuntimeQueryService.listCommands(machineId);
    }
}
