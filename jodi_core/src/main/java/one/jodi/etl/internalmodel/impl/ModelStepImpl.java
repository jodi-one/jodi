package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.ModelStep;

public class ModelStepImpl extends ETLStepImpl implements ModelStep {
    private String model;
    private ModelActionType actionType;
    private Boolean createSubscribers;
    private Boolean dropSubscribers;
    private Boolean installJournalization;
    private Boolean uninstallJournalization;
    private String subscriber;
    private Boolean extendWindow;
    private Boolean purgeJournal;
    private Boolean lockSubscribers;
    private Boolean unlockSubscribers;

    public ModelStepImpl(String name, String label) {
        super(name, label);
    }

    public ModelStepImpl(String name, String label, ETLStep nextStepOnSuccess,
                         ETLStep nextStepOnFailure) {
        super(name, label, nextStepOnSuccess, nextStepOnFailure);
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public ModelActionType getActionType() {
        return actionType;
    }

    public void setActionType(ModelActionType actionType) {
        this.actionType = actionType;
    }

    @Override
    public Boolean getCreateSubscribers() {
        return createSubscribers;
    }

    public void setCreateSubscribers(Boolean createSubscribers) {
        this.createSubscribers = createSubscribers;
    }

    @Override
    public Boolean getDropSubscribers() {
        return dropSubscribers;
    }

    public void setDropSubscribers(Boolean dropSubscribers) {
        this.dropSubscribers = dropSubscribers;
    }

    @Override
    public Boolean getInstallJournalization() {
        return installJournalization;
    }

    public void setInstallJournalization(Boolean installJournalization) {
        this.installJournalization = installJournalization;
    }

    @Override
    public Boolean getUninstallJournalization() {
        return uninstallJournalization;
    }

    public void setUninstallJournalization(Boolean uninstallJournalization) {
        this.uninstallJournalization = uninstallJournalization;
    }

    @Override
    public String getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public Boolean getExtendWindow() {
        return extendWindow;
    }

    public void setExtendWindow(Boolean extendWindow) {
        this.extendWindow = extendWindow;
    }

    @Override
    public Boolean getPurgeJournal() {
        return purgeJournal;
    }

    public void setPurgeJournal(Boolean purgeJournal) {
        this.purgeJournal = purgeJournal;
    }

    @Override
    public Boolean getLockSubscribers() {
        return lockSubscribers;
    }

    public void setLockSubscribers(Boolean lockSubscribers) {
        this.lockSubscribers = lockSubscribers;
    }

    @Override
    public Boolean getUnlockSubscribers() {
        return unlockSubscribers;
    }

    public void setUnlockSubscribers(Boolean unlockSubscribers) {
        this.unlockSubscribers = unlockSubscribers;
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
