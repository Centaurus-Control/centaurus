package de.shadowsoft.centaurus.server.scriptconfig;

import de.shadowsoft.centaurus.server.command.CommandResponse;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionAssignmentRequest;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionResponse;
import de.shadowsoft.centaurus.server.machinefunction.MachineFunctionType;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScriptButtonConfigurationController {

    private final ScriptButtonConfigurationService scriptButtonConfigurationService;

    public ScriptButtonConfigurationController(ScriptButtonConfigurationService scriptButtonConfigurationService) {
        this.scriptButtonConfigurationService = scriptButtonConfigurationService;
    }

    @GetMapping("/api/machines/{machineId}/script-configurations")
    public List<ScriptButtonConfigurationResponse> listConfigurations(@PathVariable UUID machineId) {
        return scriptButtonConfigurationService.listConfigurations(machineId);
    }

    @GetMapping("/api/machines/{machineId}/functions")
    public List<MachineFunctionResponse> listFunctions(@PathVariable UUID machineId) {
        return scriptButtonConfigurationService.listFunctions(machineId);
    }

    @PostMapping("/api/machines/{machineId}/script-configurations/{configurationId}/execute")
    public CommandResponse executeConfiguration(
        @PathVariable UUID machineId,
        @PathVariable UUID configurationId,
        Authentication authentication
    ) {
        return scriptButtonConfigurationService.executeConfiguration(machineId, configurationId, authentication);
    }

    @PostMapping("/api/machines/{machineId}/functions/{functionType}/execute")
    public CommandResponse executeFunction(
        @PathVariable UUID machineId,
        @PathVariable MachineFunctionType functionType,
        Authentication authentication
    ) {
        return scriptButtonConfigurationService.executeFunction(machineId, functionType, authentication);
    }

    @PostMapping("/api/admin/machines/{machineId}/script-configurations")
    public ScriptButtonConfigurationResponse createConfiguration(
        @PathVariable UUID machineId,
        @RequestBody ScriptButtonConfigurationRequest request,
        Authentication authentication
    ) {
        return scriptButtonConfigurationService.createConfiguration(machineId, request, authentication);
    }

    @PutMapping("/api/admin/machines/{machineId}/script-configurations/{configurationId}")
    public ScriptButtonConfigurationResponse updateConfiguration(
        @PathVariable UUID machineId,
        @PathVariable UUID configurationId,
        @RequestBody ScriptButtonConfigurationRequest request,
        Authentication authentication
    ) {
        return scriptButtonConfigurationService.updateConfiguration(machineId, configurationId, request, authentication);
    }

    @DeleteMapping("/api/admin/machines/{machineId}/script-configurations/{configurationId}")
    public void deleteConfiguration(@PathVariable UUID machineId, @PathVariable UUID configurationId, Authentication authentication) {
        scriptButtonConfigurationService.deleteConfiguration(machineId, configurationId, authentication);
    }

    @PutMapping("/api/admin/machines/{machineId}/functions/{functionType}")
    public MachineFunctionResponse assignFunction(
        @PathVariable UUID machineId,
        @PathVariable MachineFunctionType functionType,
        @RequestBody MachineFunctionAssignmentRequest request,
        Authentication authentication
    ) {
        return scriptButtonConfigurationService.assignFunction(machineId, functionType, request, authentication);
    }
}
