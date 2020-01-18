package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.InterfaceStep;

public class InterfaceStepImpl extends ETLStepImpl implements InterfaceStep {
    private final boolean useScenarios;
    //    private String interfaceName;
//    private String folderCode;
    private int packageSequence;
    private boolean asynchronous;

    public InterfaceStepImpl(String name, String label, int packageSequence, boolean asynchronous, boolean useScenario) {
        super(name, label);
        this.packageSequence = packageSequence;
        this.asynchronous = asynchronous;
        this.useScenarios = useScenario;
    }

    public InterfaceStepImpl(String name, String label,
                             ETLStep nextStepOnSuccess, ETLStep nextStepOnFailure,
                             int packageSequence, boolean asynchronous, boolean useScenario) {
        super(name, label, nextStepOnSuccess, nextStepOnFailure);
        this.packageSequence = packageSequence;
        this.asynchronous = asynchronous;
        this.useScenarios = useScenario;
    }

//    public InterfaceStepImpl(String name, String label,
//            ETLStep nextStepOnSuccess, ETLStep nextStepOnFailure, 
//            String folderCode, int packageSequence) {
//        this(name, label, nextStepOnSuccess, nextStepOnFailure, folderCode, packageSequence);
//    }

//    @Override
//    public String getInterfaceName() {
//        return interfaceName;
//    }

//    @Override
//    public String getFolderCode() {
//        return folderCode;
//    }

    @Override
    public int getPackageSequence() {
        return packageSequence;
    }

    @Override
    public boolean executeAsynchronously() {
        return asynchronous;
    }


    @Override
    public boolean useScenario() {
        return this.useScenarios;
    }
}
