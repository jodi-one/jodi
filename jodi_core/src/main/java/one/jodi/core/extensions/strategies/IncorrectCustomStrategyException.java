package one.jodi.core.extensions.strategies;

public class IncorrectCustomStrategyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IncorrectCustomStrategyException(String message) {
        super(message);
    }

    public IncorrectCustomStrategyException(String message, RuntimeException ex) {
        super(message, ex);
    }

}
