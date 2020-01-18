package one.jodi.odi.loadplan;

public class OdiLoadPlanTransformationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OdiLoadPlanTransformationException() {
        super();
    }

    public OdiLoadPlanTransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OdiLoadPlanTransformationException(String message) {
        super(message);
    }

    public OdiLoadPlanTransformationException(Throwable cause) {
        super(cause);
    }
}