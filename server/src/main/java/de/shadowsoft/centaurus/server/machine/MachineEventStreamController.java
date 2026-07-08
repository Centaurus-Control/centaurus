package de.shadowsoft.centaurus.server.machine;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/machines/events")
public class MachineEventStreamController {

    private final MachineEventStreamService machineEventStreamService;

    public MachineEventStreamController(MachineEventStreamService machineEventStreamService) {
        this.machineEventStreamService = machineEventStreamService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return machineEventStreamService.subscribe();
    }
}
