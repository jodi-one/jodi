package one.jodi.etl.service.packages;

import one.jodi.etl.service.ProviderServiceException;

public class ProcedureNotFoundException extends ProviderServiceException {
    private static final long serialVersionUID = 1L;

    public ProcedureNotFoundException() {
        super();
    }

    public ProcedureNotFoundException(String message) {
        super(message);
    }

    public ProcedureNotFoundException(Throwable cause) {
        super(cause);
    }

    public ProcedureNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
