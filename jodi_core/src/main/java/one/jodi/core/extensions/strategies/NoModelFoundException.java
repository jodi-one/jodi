package one.jodi.core.extensions.strategies;

public class NoModelFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoModelFoundException(String message) {
        super(message);
    }
}