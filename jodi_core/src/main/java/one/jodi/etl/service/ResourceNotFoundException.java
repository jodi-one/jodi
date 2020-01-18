package one.jodi.etl.service;

@SuppressWarnings("serial")
public class ResourceNotFoundException extends ProviderServiceException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
