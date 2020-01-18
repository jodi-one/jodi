package one.jodi.odi.packages;

public class OdiPackageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OdiPackageException() {
        super();
    }

    public OdiPackageException(String message, Throwable cause) {
        super(message, cause);
    }

    public OdiPackageException(String message) {
        super(message);
    }

    public OdiPackageException(Throwable cause) {
        super(cause);
    }
}
