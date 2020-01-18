package one.jodi.base.service.annotation;

public class MalformedAnnotationException extends RuntimeException {

    private static final long serialVersionUID = -8623337171083627695L;

    public MalformedAnnotationException(final String message) {
        super(message);
    }

    public MalformedAnnotationException(final String message, final Throwable e) {
        super(message, e);
    }

}
