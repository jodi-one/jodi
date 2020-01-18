package one.jodi.base.config;

public class JodiPropertyNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String property;

    public JodiPropertyNotFoundException(String message, String property) {
        super(message);
        this.property = property;
    }

    String getProperty() {
        return property;
    }
}
