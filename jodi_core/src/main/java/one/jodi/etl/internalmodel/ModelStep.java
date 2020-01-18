package one.jodi.etl.internalmodel;


public interface ModelStep extends ETLStep {

    String getModel();

    ModelActionType getActionType();

    Boolean getCreateSubscribers();

    Boolean getDropSubscribers();

    Boolean getInstallJournalization();

    Boolean getUninstallJournalization();

    String getSubscriber();

    Boolean getExtendWindow();

    Boolean getPurgeJournal();

    Boolean getLockSubscribers();

    Boolean getUnlockSubscribers();

    public enum ModelActionType {
        CONTROL,
        JOURNALIZE,
        REVERSE_ENGINEER
    }
}
