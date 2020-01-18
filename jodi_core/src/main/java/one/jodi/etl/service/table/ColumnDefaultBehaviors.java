package one.jodi.etl.service.table;

public interface ColumnDefaultBehaviors {

    //defines the name of the column that needs to be updated
    String getColumnName();

    String getScdType();

    boolean isFlowCheckEnabled();  //optionally Boolean if you like line-by-line control

    boolean isMandatory();

    boolean isStaticCheckEnabled();

    boolean isDataServiceAllowUpdate();

    boolean isDataServiceAllowSelect();

    boolean isInDatabase();

}
