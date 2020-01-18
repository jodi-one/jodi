package one.jodi.etl.service.procedure;

import one.jodi.etl.internalmodel.procedure.ProcedureHeader;
import one.jodi.etl.internalmodel.procedure.ProcedureInternal;
import oracle.odi.domain.project.OdiUserProcedure;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProcedureServiceProvider {


    Map<String, OdiUserProcedure> findFolderPathsAndProcedures(String projectCode);

    // TODO review no folder path
    Collection<OdiUserProcedure> findProcedures(String procedureName, String projectCode);

    void createProcedures(List<ProcedureInternal> procedures, boolean generateScenarios,
                          String projectCode);

    void deleteProcedures(List<ProcedureHeader> procedures, String projectCode);

    List<ProcedureInternal> extractProcedures(String projectCode);

}
