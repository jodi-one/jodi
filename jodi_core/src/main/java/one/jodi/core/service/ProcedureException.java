package one.jodi.core.service;

import one.jodi.etl.internalmodel.procedure.ProcedureHeader;

import java.util.Collections;
import java.util.List;

public class ProcedureException extends RuntimeException {

    private static final long serialVersionUID = 120689978880331744L;

    private final List<ProcedureHeader> procedures;

    public ProcedureException(final String message, final Throwable internalException,
                              final List<ProcedureHeader> procedures) {
        super(message, internalException);
        this.procedures = procedures;
    }

    public List<ProcedureHeader> getProcedures() {
        return Collections.unmodifiableList(this.procedures);
    }

}
