package limit.app.exception;

public class UserLimitNotFoundException extends RuntimeException {
    public UserLimitNotFoundException(String message) {
        super(message);
    }
}
