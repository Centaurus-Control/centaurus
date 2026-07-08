package de.shadowsoft.centaurus.agent.enrollment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentEnrollmentController {

    private final AgentEnrollmentService enrollmentService;

    public AgentEnrollmentController(AgentEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/enroll")
    public EnrollAgentResponse enroll(@RequestBody EnrollAgentRequest request) {
        return enrollmentService.enroll(request);
    }
}
