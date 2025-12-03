package limit.app.web.dto;

import java.math.BigDecimal;

public record ReserveLimitRequest(
        String externalUserId,
        BigDecimal amount,
        String requestId
) {
}
