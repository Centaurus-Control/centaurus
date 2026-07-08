package de.shadowsoft.centaurus.server.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerIdentityKeyRepository extends JpaRepository<ServerIdentityKey, UUID> {

    Optional<ServerIdentityKey> findByKeyIdAndActiveTrue(String keyId);

    Optional<ServerIdentityKey> findFirstByActiveTrueOrderByCreatedAtDesc();
}
