package one.jodi.base.service.files;

public class FileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FileException() {
        super();
    }

    public FileException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileException(String message) {
        super(message);
    }

    public FileException(Throwable cause) {
        super(cause);
    }

}