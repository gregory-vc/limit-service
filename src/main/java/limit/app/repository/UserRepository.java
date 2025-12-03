package limit.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import limit.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
