package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.Constraint;

import java.io.File;

public abstract class ConstraintImpl implements Constraint {
    final String name;
    final String schema;
    final String table;
    final boolean definedInDatabase;
    final boolean flow;
    final boolean active;
    final boolean _static;
    final String filename;
    String model;
    String comments;

    protected ConstraintImpl(String filename, String name, String schema, String table, boolean definedInDatabase, boolean active, boolean flow, boolean _static, String model) {
        this.name = name;
        this.schema = schema;
        this.table = table;
        this.definedInDatabase = definedInDatabase;
        this.flow = flow;
        this.active = active;
        this._static = _static;
        this.filename = filename;
        this.model = model;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public boolean isDefinedInDatabase() {
        return definedInDatabase;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isFlow() {
        return flow;
    }

    @Override
    public boolean isStatic() {
        return _static;
    }

    @Override
    public String getFileName() {
        return new File(filename).getAbsolutePath();
    }

    @Override
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public void setModel(String model) {
        this.model = model;
    }
}
