package de.shadowsoft.centaurus.server.machine;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/machines")
public class AdminMachineManagementController {

    private final MachineManagementService machineManagementService;

    public AdminMachineManagementController(MachineManagementService machineManagementService) {
        this.machineManagementService = machineManagementService;
    }

    @PutMapping("/{machineId}/rename")
    public MachineResponse renameMachine(
        @PathVariable UUID machineId,
        @RequestBody RenameMachineRequest request,
        Authentication authentication
    ) {
        return machineManagementService.renameMachine(machineId, request, authentication);
    }

    @DeleteMapping("/{machineId}")
    public ResponseEntity<Void> removeMachine(@PathVariable UUID machineId, Authentication authentication) {
        machineManagementService.removeMachine(machineId, authentication);
        return ResponseEntity.noContent().build();
    }
}
