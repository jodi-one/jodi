package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ReferenceAttribute;
import one.jodi.etl.internalmodel.ReferenceConstraint;

import java.util.ArrayList;
import java.util.List;

public class ReferenceConstraintImpl extends ConstraintImpl implements ReferenceConstraint {

    final Type referenceType;
    final String primaryTable;
    final String expression;
    final List<ReferenceAttribute> attributes;
    String primaryModel;
    Action deleteBehavior;
    Action updateBehavior;

    public ReferenceConstraintImpl(
            String filename, String name,
            String schema, String table,
            boolean definedInDatabase, boolean active,
            boolean flow, boolean _static, Type referenceType,
            String primaryTable, String primaryModel,
            String expression,
            Action deleteBehavior, Action updateBehavior,
            String model) {
        super(filename, name, schema, table, definedInDatabase, active, flow, _static, model);
        this.referenceType = referenceType;
        this.primaryTable = primaryTable;
        this.primaryModel = primaryModel;
        this.expression = expression;
        this.deleteBehavior = deleteBehavior;
        this.updateBehavior = updateBehavior;

        this.attributes = new ArrayList<ReferenceAttribute>();
    }


    @Override
    public Type getReferenceType() {
        return referenceType;
    }

    @Override
    public String getPrimaryModel() {
        return primaryModel;
    }

    public void setPrimaryModel(String primaryModel) {
        this.primaryModel = primaryModel;
    }

    @Override
    public String getPrimaryTable() {
        return primaryTable;
    }

    @Override
    public List<ReferenceAttribute> getReferenceAttributes() {
        return attributes;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public Action getDeleteBehavior() {
        return deleteBehavior;
    }

    @Override
    public Action getUpdateBehavior() {
        return updateBehavior;
    }

}
