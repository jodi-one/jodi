package one.jodi.core.service;

public class SequenceServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SequenceServiceException() {
        super();
    }

    public SequenceServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SequenceServiceException(String message) {
        super(message);
    }

    public SequenceServiceException(Throwable cause) {
        super(cause);
    }

}
