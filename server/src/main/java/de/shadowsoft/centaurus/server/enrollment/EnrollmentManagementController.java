package de.shadowsoft.centaurus.server.enrollment;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/enrollment-tokens")
public class EnrollmentManagementController {

    private final EnrollmentManagementService enrollmentManagementService;

    public EnrollmentManagementController(EnrollmentManagementService enrollmentManagementService) {
        this.enrollmentManagementService = enrollmentManagementService;
    }

    @GetMapping
    public List<EnrollmentTokenResponse> listTokens() {
        return enrollmentManagementService.listTokens();
    }

    @PostMapping
    public CreateEnrollmentTokenResponse createToken(@RequestBody CreateEnrollmentTokenRequest request, Authentication authentication) {
        return enrollmentManagementService.createToken(request, authentication);
    }
}
