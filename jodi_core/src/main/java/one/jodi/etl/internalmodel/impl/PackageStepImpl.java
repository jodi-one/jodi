package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.PackageStep;

public class PackageStepImpl extends ETLStepImpl implements PackageStep {
    private String sourceFolderCode;
    private boolean executeAsynchronously;

    public PackageStepImpl(String name, String label) {
        super(name, label);
    }

    public PackageStepImpl(String name, String label,
                           ETLStep nextStepOnSuccess, ETLStep nextStepOnFailure) {
        super(name, label, nextStepOnSuccess, nextStepOnFailure);
    }

    @Override
    public String getSourceFolderCode() {
        return sourceFolderCode;
    }

    public void setSourceFolderCode(String code) {
        this.sourceFolderCode = code;
    }

    @Override
    public boolean executeAsynchronously() {
        return executeAsynchronously;
    }

    public void setExecuteAsynchronously(boolean value) {
        this.executeAsynchronously = value;
    }

    @Override
    public boolean useScenario() {
        return false;
    }
}
