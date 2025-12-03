package limit.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import limit.domain.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByExternalId(String externalId);
}
