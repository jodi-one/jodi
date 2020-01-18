package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ReferenceAttribute;

public class ReferenceAttributeImpl implements ReferenceAttribute {

    final String fk;
    final String pk;

    public ReferenceAttributeImpl(String fk, String pk) {
        this.fk = fk;
        this.pk = pk;

    }

    @Override
    public String getFKColumnName() {
        return fk;
    }

    @Override
    public String getPKColumnName() {
        return pk;
    }

}
