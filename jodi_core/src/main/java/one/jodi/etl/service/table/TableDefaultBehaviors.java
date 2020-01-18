package one.jodi.etl.service.table;

import java.util.List;

public interface TableDefaultBehaviors {

    // defines the model of the datastore / table
    String getModel();

    // defines name of the DataStore / Table to which the operations are applied
    String getTableName();

    /**
     * Metadata defining the operation at the table level where the value can be
     * null implies ignore set operation
     */
    public String getDefaultAlias();

    public OlapType getOlapType();

    public List<ColumnDefaultBehaviors> getColumnDefaultBehaviors();

    public boolean isConnectorModel();

    public enum OlapType {FACT, DIMENSION, SLOWLY_CHANGING_DIMENSION} // need to verify SCD or longer version?
}
