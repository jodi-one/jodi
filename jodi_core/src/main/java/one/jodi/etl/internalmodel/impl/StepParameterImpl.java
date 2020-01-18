package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.StepParameter;

public class StepParameterImpl implements StepParameter {
    private String name;
    private String value;

    public StepParameterImpl(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

}
