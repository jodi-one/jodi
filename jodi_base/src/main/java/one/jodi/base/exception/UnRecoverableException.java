package one.jodi.base.exception;

public class UnRecoverableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnRecoverableException(String message) {
        super(message);
    }

    public UnRecoverableException(String message, Throwable throwable) {
        super(message, throwable);
    }
}