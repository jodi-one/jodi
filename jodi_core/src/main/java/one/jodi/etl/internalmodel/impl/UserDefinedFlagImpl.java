package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.UserDefinedFlag;

public class UserDefinedFlagImpl implements UserDefinedFlag {
    private final boolean value;
    private final int number;

    public UserDefinedFlagImpl(final int number, final boolean value) {
        this.value = value;
        this.number = number;
    }

    @Override
    public String getName() {
        return "ud" + this.number;
    }

    @Override
    public boolean getValue() {
        return value;
    }

    @Override
    public int getNumber() {
        return number;
    }
}
