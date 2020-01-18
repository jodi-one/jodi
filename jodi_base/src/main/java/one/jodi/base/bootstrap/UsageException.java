package one.jodi.base.bootstrap;

public class UsageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UsageException() {
        super();
    }

    public UsageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UsageException(String message) {
        super(message);
    }

    public UsageException(Throwable cause) {
        super(cause);
    }

}
