package one.jodi.odi.scenarios;

public class OdiScenarioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OdiScenarioException() {
        super();
    }

    public OdiScenarioException(String message, Throwable cause) {
        super(message, cause);
    }

    public OdiScenarioException(String message) {
        super(message);
    }

    public OdiScenarioException(Throwable cause) {
        super(cause);
    }

}