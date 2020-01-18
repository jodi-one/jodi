package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.KeyConstraint;

import java.util.ArrayList;
import java.util.List;

public class KeyConstraintImpl extends ConstraintImpl implements KeyConstraint {

    final ArrayList<String> attributes;
    final KeyType keyType;

    public KeyConstraintImpl(String filename, String name, String schema, String table,
                             boolean definedInDatabase, boolean active, boolean flow,
                             boolean _static, KeyType keyType, String model) {
        super(filename, name, schema, table, definedInDatabase, active, flow, _static, model);
        this.keyType = keyType;
        attributes = new ArrayList<String>();
    }

    @Override
    public List<String> getAttributes() {
        return attributes;
    }

    @Override
    public KeyType getKeyType() {
        return keyType;
    }

}
