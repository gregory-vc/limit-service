package limit.app.repository;

import limit.domain.LimitReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimitReservationRepository extends JpaRepository<LimitReservation, Long> {
}
