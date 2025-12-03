package limit.app.exception;

public class ReservationInvalidStateException extends RuntimeException {
    public ReservationInvalidStateException(String message) {
        super(message);
    }
}
