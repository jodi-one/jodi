package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.AggregateFunctionEnum;
import one.jodi.etl.internalmodel.OutputAttribute;
import one.jodi.etl.internalmodel.Pivot;

import java.util.ArrayList;
import java.util.List;

public class PivotImpl extends FlowImpl implements Pivot {

    String rowLocator;
    AggregateFunctionEnum aggregateFunction;
    ArrayList<OutputAttribute> outputAttributes = new ArrayList<>();

    public PivotImpl() {

    }


    public PivotImpl(String name, int order, String rowLocator, AggregateFunctionEnum aggregateFunction) {
        this.name = name;
        this.order = order;
        this.rowLocator = rowLocator;
        this.aggregateFunction = aggregateFunction;
    }


    @Override
    public String getRowLocator() {
        return rowLocator;
    }

    public void setRowLocator(String rowLocator) {
        this.rowLocator = rowLocator;
    }

    @Override
    public List<OutputAttribute> getOutputAttributes() {
        return outputAttributes;
    }

    @Override
    public AggregateFunctionEnum getAggregateFunction() {
        return aggregateFunction;
    }

    public void setAggregateFunction(AggregateFunctionEnum aggregateFunction) {
        this.aggregateFunction = aggregateFunction;
    }

    public boolean add(OutputAttribute e) {
        return outputAttributes.add(e);
    }
}
