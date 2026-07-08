package de.shadowsoft.centaurus.server.enrollment;

public record CreateEnrollmentTokenResponse(
    EnrollmentTokenResponse token,
    String enrollmentBundle
) {
}
