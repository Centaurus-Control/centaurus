package de.shadowsoft.centaurus.server.command;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommandController {

    private final CommandDispatchService commandDispatchService;

    public CommandController(CommandDispatchService commandDispatchService) {
        this.commandDispatchService = commandDispatchService;
    }

    @GetMapping("/commands")
    public List<CommandResponse> listCommands() {
        return commandDispatchService.listCommands();
    }

    @PostMapping("/machines/{machineId}/commands/execute-script")
    public CommandResponse executeScript(
        @PathVariable UUID machineId,
        @RequestBody ExecuteScriptRequest request,
        Authentication authentication
    ) {
        return commandDispatchService.executeScript(machineId, request, authentication);
    }

    @PostMapping("/machines/{machineId}/commands/wake-on-lan")
    public CommandResponse sendWakeOnLan(
        @PathVariable UUID machineId,
        @RequestBody SendWakeOnLanRequest request,
        Authentication authentication
    ) {
        return commandDispatchService.sendWakeOnLan(machineId, request, authentication);
    }

    @PostMapping("/machines/{machineId}/commands/refresh-stats")
    public CommandResponse refreshStats(@PathVariable UUID machineId, Authentication authentication) {
        return commandDispatchService.refreshStats(machineId, authentication);
    }

    @PostMapping("/machines/{machineId}/commands/refresh-script-manifest")
    public CommandResponse refreshScriptManifest(@PathVariable UUID machineId, Authentication authentication) {
        return commandDispatchService.refreshScriptManifest(machineId, authentication);
    }
}
