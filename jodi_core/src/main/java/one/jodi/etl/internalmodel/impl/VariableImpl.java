package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.Variable;

public class VariableImpl implements Variable {

    private String name;
    private Datatype dataType;
    private String schema;
    private String query;
    private Boolean global;
    private String defaultValue;
    private String description;
    private Keephistory keephistory;

    public VariableImpl(String name, Datatype dataType, String schema, String query,
                        Boolean global, String defaultValue, String description,
                        Keephistory keephistory) {
        this.name = name;
        this.dataType = dataType;
        this.schema = schema;
        this.query = query;
        this.global = global;
        this.defaultValue = defaultValue;
        this.description = description;
        this.keephistory = keephistory;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Datatype getDataType() {
        return dataType;
    }

    public void setDataType(Datatype dataType) {
        this.dataType = dataType;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Keephistory getKeephistory() {
        return keephistory;
    }

    public void setKeephistory(Keephistory keephistory) {
        this.keephistory = keephistory;
    }

}