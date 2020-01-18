package one.jodi.etl.internalmodel;

public interface Pivot extends Flow {

    String getRowLocator();


    AggregateFunctionEnum getAggregateFunction();

}
