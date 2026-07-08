package de.shadowsoft.centaurus.server.enrollment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentEnrollmentController {

    private final AgentEnrollmentService agentEnrollmentService;

    public AgentEnrollmentController(AgentEnrollmentService agentEnrollmentService) {
        this.agentEnrollmentService = agentEnrollmentService;
    }

    @PostMapping("/enroll")
    public AgentEnrollmentResponse enroll(@RequestBody AgentEnrollmentRequest request) {
        return agentEnrollmentService.enroll(request);
    }
}
