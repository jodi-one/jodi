package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.CommandStep;
import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.StepParameter;

import java.util.ArrayList;
import java.util.List;

public class CommandStepImpl extends ETLStepImpl implements CommandStep {
    List<StepParameter> parameters;

    public CommandStepImpl(String name, String label) {
        super(name, label);
    }

    public CommandStepImpl(String name, String label,
                           ETLStep nextStepOnSuccess, ETLStep nextStepOnFailure) {
        super(name, label, nextStepOnSuccess, nextStepOnFailure);
    }

    @Override
    public List<StepParameter> getParameters() {
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
