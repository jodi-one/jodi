package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.internalmodel.Variables;

import java.util.Collection;

public class VariablesImpl implements Variables {

    private final Collection<Variable> variables;

    public VariablesImpl(Collection<Variable> variables) {
        this.variables = variables;
    }

    @Override
    public Collection<Variable> getVariables() {
        return variables;
    }
}
