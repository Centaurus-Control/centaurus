package de.shadowsoft.centaurus.server.machine;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/machines")
public class MachineManagementController {

    private final MachineManagementService machineManagementService;

    public MachineManagementController(MachineManagementService machineManagementService) {
        this.machineManagementService = machineManagementService;
    }

    @GetMapping
    public List<MachineResponse> listMachines() {
        return machineManagementService.listMachines();
    }

    @GetMapping("/{machineId}")
    public MachineResponse getMachine(@PathVariable UUID machineId) {
        return machineManagementService.getMachine(machineId);
    }

    @PutMapping("/{machineId}/wake-on-lan")
    public MachineResponse updateWakeOnLanConfiguration(
        @PathVariable UUID machineId,
        @RequestBody UpdateWakeOnLanConfigurationRequest request,
        Authentication authentication
    ) {
        return machineManagementService.updateWakeOnLanConfiguration(machineId, request, authentication);
    }
}
