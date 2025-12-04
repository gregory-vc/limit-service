package limit.app.repository;

import limit.domain.LimitOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimitOperationRepository extends JpaRepository<LimitOperation, Long> {
}
