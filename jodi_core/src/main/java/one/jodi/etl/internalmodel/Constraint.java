package one.jodi.etl.internalmodel;


public interface Constraint {
    public String getName();

    public String getSchema();

    public String getModel();

    void setModel(String model);

    public String getTable();

    public boolean isDefinedInDatabase();

    public boolean isActive();

    public boolean isFlow();

    public boolean isStatic();

    public String getFileName();

    public String getComments();
}
