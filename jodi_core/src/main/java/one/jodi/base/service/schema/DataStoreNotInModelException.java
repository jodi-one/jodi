package one.jodi.base.service.schema;

public class DataStoreNotInModelException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataStoreNotInModelException(String message) {
        super(message);
    }
}