package one.jodi.etl.internalmodel;

public interface UnPivot extends Flow {

    String getRowLocator();

    boolean getIsIncludeNulls();

}
