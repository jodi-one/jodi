package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.SpecificSequence;

public class SpecificSequenceImpl extends SequenceImpl
        implements SpecificSequence {

    private String schema;
    private String table;
    private String column;
    private String filter;

    public SpecificSequenceImpl(final String name, final Integer increment,
                                final Boolean global, final String schema,
                                final String table, final String column,
                                final String filter) {
        super(name, increment, global);
        this.schema = schema;
        this.table = table;
        this.column = column;
        this.filter = filter;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    @Override
    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    @Override
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
