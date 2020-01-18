package one.jodi.etl.service;

public class ProviderServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public ProviderServiceException() {
        super();
    }

    public ProviderServiceException(String message) {
        super(message);
    }

    public ProviderServiceException(Throwable cause) {
        super(cause);
    }

    public ProviderServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
