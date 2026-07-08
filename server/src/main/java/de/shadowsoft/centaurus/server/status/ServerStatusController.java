package de.shadowsoft.centaurus.server.status;

import de.shadowsoft.centaurus.server.agent.AgentRepository;
import de.shadowsoft.centaurus.server.command.CommandRepository;
import de.shadowsoft.centaurus.server.machine.MachineRepository;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/server")
public class ServerStatusController {

    private final MachineRepository machineRepository;
    private final AgentRepository agentRepository;
    private final CommandRepository commandRepository;

    public ServerStatusController(
        MachineRepository machineRepository,
        AgentRepository agentRepository,
        CommandRepository commandRepository
    ) {
        this.machineRepository = machineRepository;
        this.agentRepository = agentRepository;
        this.commandRepository = commandRepository;
    }

    @GetMapping("/status")
    public ServerStatusResponse getStatus() {
        return new ServerStatusResponse(
            "centaurus-server",
            "UP",
            Instant.now(),
            machineRepository.countByDeletedFalse(),
            agentRepository.countByDeletedFalse(),
            commandRepository.count()
        );
    }
}
