package one.jodi.odi.interfaces;

/**
 * N
 * Base exception type for issues an ETL Provider can produce.
 */
public class ETLProviderException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ETLProviderException(String message) {
        super(message);
    }

    public ETLProviderException(String message, Exception e) {
        super(message, e);
    }
}
