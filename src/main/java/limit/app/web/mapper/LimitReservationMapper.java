package limit.app.web.mapper;

import limit.app.web.dto.LimitReservationResponse;
import limit.domain.LimitReservation;
import org.springframework.stereotype.Component;

@Component
public class LimitReservationMapper {

    public LimitReservationResponse toResponse(LimitReservation reservation) {
        return new LimitReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getAmount(),
                reservation.getStatus(),
                reservation.getExpiresAt()
        );
    }
}
