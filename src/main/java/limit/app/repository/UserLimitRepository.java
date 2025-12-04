package limit.app.repository;

import limit.domain.UserLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserLimitRepository extends JpaRepository<UserLimit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ul from UserLimit ul where ul.userId = :userId")
    Optional<UserLimit> findByUserIdForUpdate(@Param("userId") Long userId);
}
