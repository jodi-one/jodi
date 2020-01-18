package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.ProcedureStep;
import one.jodi.etl.internalmodel.StepParameter;

import java.util.ArrayList;
import java.util.Collection;

public class ProcedureStepImpl extends ETLStepImpl implements ProcedureStep {
    private Collection<StepParameter> parameters;

    public ProcedureStepImpl(String name, String label) {
        super(name, label);
    }

    public ProcedureStepImpl(String name, String label,
                             ETLStep nextStepOnSuccess, ETLStep nextStepOnFailure) {
        super(name, label, nextStepOnSuccess, nextStepOnFailure);
    }

    @Override
    public Collection<StepParameter> getParameters() {
        return parameters;
    }

    public void addParameter(StepParameter param) {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }

        parameters.add(param);
    }

    @Override
    public boolean executeAsynchronously() {
        return false;
    }

    @Override
    public boolean useScenario() {
        return false;
    }
}
