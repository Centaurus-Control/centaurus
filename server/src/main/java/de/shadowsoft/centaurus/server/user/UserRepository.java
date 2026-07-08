package de.shadowsoft.centaurus.server.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByIdAndDeletedFalse(UUID id);

    List<User> findByDeletedFalseOrderByUsernameAsc();

    boolean existsByUsername(String username);

    boolean existsByUsernameAndDeletedFalse(String username);

    long countByRoleAndDeletedFalse(UserRole role);
}
