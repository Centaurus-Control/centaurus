package de.shadowsoft.centaurus.server.enrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentTokenRepository extends JpaRepository<EnrollmentToken, UUID> {

    Optional<EnrollmentToken> findByTokenHash(String tokenHash);

    List<EnrollmentToken> findByUsedByAgentId(UUID agentId);

    List<EnrollmentToken> findAllByOrderByCreatedAtDesc();
}
