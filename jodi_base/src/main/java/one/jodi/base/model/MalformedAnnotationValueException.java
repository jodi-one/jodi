package one.jodi.base.model;

public class MalformedAnnotationValueException extends RuntimeException {

    private static final long serialVersionUID = 3163579543655305544L;

    public MalformedAnnotationValueException(final String msg) {
        super(msg);
    }
}
