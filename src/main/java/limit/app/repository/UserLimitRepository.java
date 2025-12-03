package limit.app.repository;

import limit.domain.UserLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLimitRepository extends JpaRepository<UserLimit, Long> {
}
