package one.jodi.core.service;

public class PackageServiceException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PackageServiceException() {
        super();
    }

    public PackageServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PackageServiceException(String message) {
        super(message);
    }

    public PackageServiceException(Throwable cause) {
        super(cause);
    }
}
