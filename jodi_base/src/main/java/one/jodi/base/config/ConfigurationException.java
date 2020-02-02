package one.jodi.base.config;

/**
 * Thrown when there is a problem accessing application configuration
 * properties.
 *
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new ConfigurationException instance.
     */
    public ConfigurationException() {
        super();
    }

    /**
     * Creates a new ConfigurationException instance.
     *
     * @param message meaningful message of the exception
     * @param cause the cause of the exception
     */
    public ConfigurationException(final String message,
                                  final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ConfigurationException instance.
     *
     * @param message meaningful message
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Creates a new ConfigurationException instance.
     *
     * @param cause cause of exception
     */
    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

}
