package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLStep;

public abstract class ETLStepImpl implements ETLStep {
    private String name;
    private String label;
    private ETLStep nextStepOnSuccess;
    private ETLStep nextStepOnFailure;

    public ETLStepImpl(String name, String label) {
        super();
        this.name = name;
        this.label = label;
    }

    public ETLStepImpl(String name, String label, ETLStep nextStepOnSuccess,
                       ETLStep nextStepOnFailure) {
        super();
        this.name = name;
        this.label = label;
        this.nextStepOnSuccess = nextStepOnSuccess;
        this.nextStepOnFailure = nextStepOnFailure;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public ETLStep getNextStepOnSuccess() {
        return nextStepOnSuccess;
    }

    public void setNextStepOnSuccess(ETLStep step) {
        this.nextStepOnSuccess = step;
    }

    @Override
    public ETLStep getNextStepOnFailure() {
        return nextStepOnFailure;
    }

    public void setNextStepOnFailure(ETLStep step) {
        this.nextStepOnFailure = step;
    }
}
