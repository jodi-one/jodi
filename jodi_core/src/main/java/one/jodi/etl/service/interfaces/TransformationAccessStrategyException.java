package one.jodi.etl.service.interfaces;

public class TransformationAccessStrategyException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public TransformationAccessStrategyException() {
        super();
    }

    public TransformationAccessStrategyException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformationAccessStrategyException(String message) {
        super(message);
    }

    public TransformationAccessStrategyException(Throwable cause) {
        super(cause);
    }
}
