package limit.app.web.mapper;

import limit.app.web.dto.LimitReservationResponse;
import limit.domain.LimitReservation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LimitReservationMapper {

    public LimitReservationResponse toResponse(LimitReservation reservation, BigDecimal availableLimit) {
        return new LimitReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getAmount(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                availableLimit
        );
    }
}
