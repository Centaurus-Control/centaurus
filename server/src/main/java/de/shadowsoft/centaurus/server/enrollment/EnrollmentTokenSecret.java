package de.shadowsoft.centaurus.server.enrollment;

public record EnrollmentTokenSecret(
    String token,
    String hash
) {
}
