package one.jodi.etl.internalmodel.impl;


import one.jodi.etl.internalmodel.ConditionConstraint;

public class ConditionConstraintImpl extends ConstraintImpl implements ConditionConstraint {

    final Type type;
    final String where;
    final String message;

    public ConditionConstraintImpl(
            String filename, String name, String schema,
            String table, boolean definedInDatabase,
            boolean active, boolean flow,
            boolean _static, Type type, String where,
            String message,
            String model) {
        super(filename, name, schema, table, definedInDatabase, active, flow, _static, model);
        this.type = type;
        this.where = where;
        this.message = message;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getWhere() {
        return where;
    }

    @Override
    public String getMessage() {
        return message;
    }
}