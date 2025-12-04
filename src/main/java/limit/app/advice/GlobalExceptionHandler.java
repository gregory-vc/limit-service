package limit.app.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import limit.app.exception.InsufficientLimitException;
import limit.app.exception.InvalidRequestException;
import limit.app.exception.ReservationInvalidStateException;
import limit.app.exception.ReservationNotFoundException;
import limit.app.exception.UserLimitNotFoundException;
import limit.app.web.dto.ErrorResponse;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleReservationNotFound(ReservationNotFoundException ex) {
        return notFound(ex.getMessage(), "RESERVATION_NOT_FOUND");
    }

    @ExceptionHandler({InvalidRequestException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRequest(RuntimeException ex) {
        return error(ex.getMessage(), "BAD_REQUEST");
    }

    @ExceptionHandler({InsufficientLimitException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInsufficient(InsufficientLimitException ex) {
        return error(ex.getMessage(), "INSUFFICIENT_LIMIT");
    }

    @ExceptionHandler({ReservationInvalidStateException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidReservationState(ReservationInvalidStateException ex) {
        return error(ex.getMessage(), "INVALID_RESERVATION_STATE");
    }

    @ExceptionHandler({UserLimitNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserLimitNotFound(UserLimitNotFoundException ex) {
        return notFound(ex.getMessage(), "USER_LIMIT_NOT_FOUND");
    }

    private ErrorResponse notFound(String message, String code) {
        return new ErrorResponse(
                message,
                code,
                OffsetDateTime.now()
        );
    }

    private ErrorResponse error(String message, String code) {
        return new ErrorResponse(
                message,
                code,
                OffsetDateTime.now()
        );
    }
}
