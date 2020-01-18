package one.jodi.etl.internalmodel;

public interface SpecificSequence extends Sequence {
    String getSchema();

    String getTable();

    String getColumn();

    String getFilter();

}
