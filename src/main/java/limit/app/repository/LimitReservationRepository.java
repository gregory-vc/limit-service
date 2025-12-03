package limit.app.repository;

import limit.domain.LimitReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface LimitReservationRepository extends JpaRepository<LimitReservation, Long> {

    Optional<LimitReservation> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lr from LimitReservation lr where lr.id = :id")
    Optional<LimitReservation> findByIdForUpdate(@Param("id") Long id);
}
