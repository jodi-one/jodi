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
     * @param message
     * @param cause
     */
    public ConfigurationException(final String message,
                                  final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ConfigurationException instance.
     *
     * @param message
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Creates a new ConfigurationException instance.
     *
     * @param cause
     */
    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

}
