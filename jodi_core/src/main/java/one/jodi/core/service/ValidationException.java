package one.jodi.core.service;

/**
 * Thrown when a fatal validation exception occurs
 */
public class ValidationException extends RuntimeException {

    /**
     * Use serialVersionUID for interoperability.
     */
    private final static long serialVersionUID = -1866926226888752836L;

    /**
     * Creates a new ValidationException instance.
     */
    public ValidationException() {
        super();
    }

    /**
     * Creates a new ValidationException instance.
     *
     * @param message
     * @param cause
     */
    public ValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ValidationException instance.
     *
     * @param message
     */
    public ValidationException(final String message) {
        super(message);
    }

    /**
     * Creates a new ValidationException instance.
     *
     * @param cause
     */
    public ValidationException(final Throwable cause) {
        super(cause);
    }
}
