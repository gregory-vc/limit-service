package limit.app.web.dto;

import limit.domain.LimitReservationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LimitReservationResponse(
        Long id,
        Long userId,
        String externalUserId,
        BigDecimal amount,
        LimitReservationStatus status,
        OffsetDateTime expiresAt
) {
}
