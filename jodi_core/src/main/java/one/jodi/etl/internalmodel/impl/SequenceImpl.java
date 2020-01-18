package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.Sequence;

public abstract class SequenceImpl
        implements Sequence {

    private String name;
    private Integer increment;
    private Boolean global;

    public SequenceImpl(final String name, final Integer increment,
                        final Boolean global) {
        this.name = name;
        this.increment = increment;
        this.global = global;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }

    @Override
    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }
}
