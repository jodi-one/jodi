package one.jodi.core.validation.variables;

import one.jodi.etl.internalmodel.Variables;

@FunctionalInterface
public interface VariableValidator {

    boolean validate(Variables internalVariables);
}
