package one.jodi.odi.variables;

import one.jodi.etl.internalmodel.Variable;
import oracle.odi.domain.project.OdiVariable;

import java.util.Collection;

/**
 * Created by duvanl on 5/3/2016.
 */
public interface OdiVariableAccessStrategy {

    OdiVariable findProjectVariable(String projectVariableName, String projectCode);

    Collection<OdiVariable> findAllVariables();

    Collection<OdiVariable> findAllGlobalVariables();

    Collection<OdiVariable> findAllProjectVariables(String projectCode);

    void create(Variable internalVar, String projectCode, String dateFormat);

    void delete(Variable internalVar, String projectCode);

}
