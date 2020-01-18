package one.jodi.etl.service.variables;

import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.internalmodel.Variables;

public interface VariableServiceProvider {
    void create(Variable iV, String projectCode, String dateFormat);

    void delete(Variable internalVariable, String projectCode);

    Variables findAll();
}
