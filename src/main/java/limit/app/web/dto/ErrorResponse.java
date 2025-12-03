package limit.app.web.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String message,
        String code,
        OffsetDateTime timestamp
) {
}
