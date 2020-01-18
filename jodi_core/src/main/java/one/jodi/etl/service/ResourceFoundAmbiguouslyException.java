package one.jodi.etl.service;

@SuppressWarnings("serial")
public class ResourceFoundAmbiguouslyException extends ProviderServiceException {

    public ResourceFoundAmbiguouslyException(String message) {
        super(message);
    }

    public ResourceFoundAmbiguouslyException(String message, Exception e) {
        super(message, e);
    }
}
