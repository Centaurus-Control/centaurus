package de.shadowsoft.centaurus.server.agent;

import de.shadowsoft.centaurus.server.machine.MachineManagementService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agents")
public class AdminAgentManagementController {

    private final MachineManagementService machineManagementService;

    public AdminAgentManagementController(MachineManagementService machineManagementService) {
        this.machineManagementService = machineManagementService;
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> removeAgent(@PathVariable UUID agentId, Authentication authentication) {
        machineManagementService.removeAgent(agentId, authentication);
        return ResponseEntity.noContent().build();
    }
}
