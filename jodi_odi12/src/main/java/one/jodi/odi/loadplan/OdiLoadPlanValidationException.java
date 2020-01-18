package one.jodi.odi.loadplan;

public class OdiLoadPlanValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OdiLoadPlanValidationException() {
        super();
    }

    public OdiLoadPlanValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OdiLoadPlanValidationException(String message) {
        super(message);
    }

    public OdiLoadPlanValidationException(Throwable cause) {
        super(cause);
    }
}
