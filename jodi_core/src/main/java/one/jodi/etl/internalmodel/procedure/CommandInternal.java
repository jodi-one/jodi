package one.jodi.etl.internalmodel.procedure;

public interface CommandInternal {

    TaskInternal getParent();

    String getTechnology();

    String getLogicalSchema();

    String getCommand();

    //
    // will be enriched with default values
    //

    // default value: true
    boolean isAlwaysExecuteOptions();

    // default value: 'No Change'
    String getTransactionIsolation();

    // default value: "Execution Context"
    String getExecutionContext();

    // default value: "Autocommit"
    String getTransaction();

}
