package one.jodi.etl.internalmodel;

public interface Variable {
    String getName();

    Datatype getDataType();

    String getSchema();

    String getQuery();

    Boolean getGlobal();

    String getDefaultValue();

    String getDescription();

    Keephistory getKeephistory();

    public enum Datatype {
        ALPHANUMERIC, TEXT, NUMERIC, DATE
    }

    public enum Keephistory {
        ALL_VALUES, LATEST_VALUE, NO_HISTORY, NONE
    }
}
