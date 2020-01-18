package one.jodi.etl.builder;

import one.jodi.etl.internalmodel.Variables;

public interface VariableTransformationBuilder {

    Variables transmute(one.jodi.core.variables.Variables externalVariables);

    one.jodi.core.variables.Variables transmute(Variables internalVariables);
}
