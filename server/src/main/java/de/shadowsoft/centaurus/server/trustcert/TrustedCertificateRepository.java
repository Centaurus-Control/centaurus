package de.shadowsoft.centaurus.server.trustcert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedCertificateRepository extends JpaRepository<TrustedCertificate, UUID> {

    List<TrustedCertificate> findAllByOrderByAliasAsc();

    List<TrustedCertificate> findByEnabledTrueOrderByAliasAsc();

    Optional<TrustedCertificate> findByAlias(String alias);

    boolean existsByAlias(String alias);

    boolean existsByAliasAndIdNot(String alias, UUID id);
}
