package one.jodi.core.extensions.strategies;

public class AmbiguousModelException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private boolean isDefault = false;

    public AmbiguousModelException(String message) {
        super(message);
    }

    public AmbiguousModelException(final String message, final boolean isDefault) {
        super(message);
        this.isDefault = isDefault;
    }

    public AmbiguousModelException(final String message, final Throwable e) {
        super(message, e);
    }

    public boolean isDefault() {
        return this.isDefault;
    }
}