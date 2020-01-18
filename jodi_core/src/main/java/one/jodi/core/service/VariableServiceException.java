package one.jodi.core.service;

public class VariableServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VariableServiceException() {
        super();
    }

    public VariableServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public VariableServiceException(String message) {
        super(message);
    }

    public VariableServiceException(Throwable cause) {
        super(cause);
    }

}