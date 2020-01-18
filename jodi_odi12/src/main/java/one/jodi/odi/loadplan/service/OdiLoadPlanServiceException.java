package one.jodi.odi.loadplan.service;

public class OdiLoadPlanServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OdiLoadPlanServiceException() {
        super();
    }

    public OdiLoadPlanServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public OdiLoadPlanServiceException(String message) {
        super(message);
    }

    public OdiLoadPlanServiceException(Throwable cause) {
        super(cause);
    }
}