package limit.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import limit.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByExternalId(String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockingByExternalId(String externalId);
}
