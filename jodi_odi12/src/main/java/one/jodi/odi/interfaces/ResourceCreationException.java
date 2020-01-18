package one.jodi.odi.interfaces;

@SuppressWarnings("serial")
public class ResourceCreationException extends ETLProviderException {

    public ResourceCreationException(String message) {
        super(message);
    }

    public ResourceCreationException(String message, Exception e) {
        super(message, e);
    }
}
