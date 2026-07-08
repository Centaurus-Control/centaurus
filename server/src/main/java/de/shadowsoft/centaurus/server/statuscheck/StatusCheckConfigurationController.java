package de.shadowsoft.centaurus.server.statuscheck;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusCheckConfigurationController {

    private final StatusCheckConfigurationService service;

    public StatusCheckConfigurationController(StatusCheckConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/api/machines/{machineId}/status-check-configurations")
    public List<StatusCheckConfigurationResponse> listConfigurations(@PathVariable UUID machineId) {
        return service.listConfigurations(machineId);
    }

    @PostMapping("/api/admin/machines/{machineId}/status-check-configurations")
    public StatusCheckConfigurationResponse createConfiguration(
        @PathVariable UUID machineId,
        @RequestBody StatusCheckConfigurationRequest request,
        Authentication authentication
    ) {
        return service.createConfiguration(machineId, request, authentication);
    }

    @PutMapping("/api/admin/machines/{machineId}/status-check-configurations/{configurationId}")
    public StatusCheckConfigurationResponse updateConfiguration(
        @PathVariable UUID machineId,
        @PathVariable UUID configurationId,
        @RequestBody StatusCheckConfigurationRequest request,
        Authentication authentication
    ) {
        return service.updateConfiguration(machineId, configurationId, request, authentication);
    }

    @DeleteMapping("/api/admin/machines/{machineId}/status-check-configurations/{configurationId}")
    public void deleteConfiguration(
        @PathVariable UUID machineId,
        @PathVariable UUID configurationId,
        Authentication authentication
    ) {
        service.deleteConfiguration(machineId, configurationId, authentication);
    }
}
