package one.jodi.base.model;

public class MalformedModelException extends RuntimeException {

    private static final long serialVersionUID = 7320723054970646344L;

    private final ModelNode entityWithError;

    public MalformedModelException(final ModelNode entityWithError,
                                   final String errorMessage) {
        super(errorMessage);
        this.entityWithError = entityWithError;
    }

    public ModelNode getErrorTable() {
        return entityWithError;
    }

}
