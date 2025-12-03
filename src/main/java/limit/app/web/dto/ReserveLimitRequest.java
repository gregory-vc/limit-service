package limit.app.web.dto;

import java.math.BigDecimal;

public record ReserveLimitRequest(
        Long userId,
        BigDecimal amount,
        String requestId
) {
}
