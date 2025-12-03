package limit.app.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import limit.app.exception.UserNotFoundException;
import limit.app.web.dto.ErrorResponse;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserNotFoundException ex) {
        return notFound(ex.getMessage(), "USER_NOT_FOUND");
    }

    private ErrorResponse notFound(String message, String code) {
        return new ErrorResponse(
                message,
                code,
                OffsetDateTime.now()
        );
    }
}
