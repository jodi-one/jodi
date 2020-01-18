package one.jodi.etl.internalmodel.impl;


import one.jodi.etl.internalmodel.Flow;
import one.jodi.etl.internalmodel.Source;


public abstract class FlowImpl implements Flow {

    int order = 0;
    String name;
    Source parent;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public Source getParent() {
        return parent;
    }

    public void setParent(Source parent) {
        this.parent = parent;
    }
}
