package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.OutputAttribute;
import one.jodi.etl.internalmodel.UnPivot;

import java.util.ArrayList;
import java.util.List;

public class UnPivotImpl extends FlowImpl implements UnPivot {

    String rowLocator;
    boolean isIncludeNulls;
    ArrayList<OutputAttribute> outputAttributes = new ArrayList<>();

    public UnPivotImpl() {

    }

    public UnPivotImpl(String name, int order, String rowLocator, Boolean isIncludeNulls) {
        this.name = name;
        this.order = order;
        this.rowLocator = rowLocator;
        this.isIncludeNulls = isIncludeNulls;
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

    public boolean add(OutputAttribute e) {
        return outputAttributes.add(e);
    }

    @Override
    public boolean getIsIncludeNulls() {
        return isIncludeNulls == false ? false : isIncludeNulls;
    }
}
