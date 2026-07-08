package de.shadowsoft.centaurus.agent.enrollment;

public record EnrollAgentRequest(
    String enrollmentBundle,
    String displayName
) {
}
