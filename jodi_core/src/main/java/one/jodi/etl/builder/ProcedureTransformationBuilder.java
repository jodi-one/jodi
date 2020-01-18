package one.jodi.etl.builder;

import one.jodi.core.procedure.Procedure;
import one.jodi.etl.internalmodel.procedure.ProcedureHeader;
import one.jodi.etl.internalmodel.procedure.ProcedureInternal;

import java.util.Optional;

public interface ProcedureTransformationBuilder {

    Optional<ProcedureInternal> build(Procedure externalProcedure, String filePath);

    Optional<ProcedureHeader> buildHeader(Procedure externalProcedure, String filePath);

}
