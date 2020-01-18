package one.jodi.odi.loadplan;

public class OdiValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OdiValidationException() {
        super();
    }

    public OdiValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OdiValidationException(String message) {
        super(message);
    }

    public OdiValidationException(Throwable cause) {
        super(cause);
    }
}