package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.VariableStep;

public class VariableStepImpl extends ETLStepImpl implements VariableStep {
    private VariableStepType stepType;
    private String value;
    private VariableSetOperatorType setOperator;
    private String operator;
    private Integer incrementBy;

    public VariableStepImpl(String name, String label) {
        super(name, label);
    }

    public VariableStepImpl(String name, String label,
                            ETLStep nextStepOnSuccess, ETLStep nextStepOnFailure) {
        super(name, label, nextStepOnSuccess, nextStepOnFailure);
    }

    @Override
    public VariableStepType getStepType() {
        return stepType;
    }

    public void setStepType(VariableStepType stepType) {
        this.stepType = stepType;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public VariableSetOperatorType getSetOperator() {
        return setOperator;
    }

    public void setSetOperator(VariableSetOperatorType setOperator) {
        this.setOperator = setOperator;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    public void getSetOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public Integer getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(Integer incrementBy) {
        this.incrementBy = incrementBy;
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
