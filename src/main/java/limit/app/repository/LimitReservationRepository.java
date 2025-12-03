package limit.app.repository;

import limit.domain.LimitReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LimitReservationRepository extends JpaRepository<LimitReservation, Long> {

    Optional<LimitReservation> findByRequestId(String requestId);
}
