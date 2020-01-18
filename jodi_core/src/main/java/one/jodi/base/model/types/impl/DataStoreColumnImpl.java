package one.jodi.base.model.types.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.model.types.SCDType;

import java.io.Serializable;

public class DataStoreColumnImpl implements DataStoreColumn, Serializable {

    private static final long serialVersionUID = 7827635423668687003L;

    private final DataStore parent;
    private final String name;
    private final int length;
    private final int scale;
    private final String columnDataType;
    private final SCDType scdType;
    private final boolean mandatory;
    private final String description;
    private final int position;

    public DataStoreColumnImpl(final DataStore parent,
                               final String name, final int length, final int scale,
                               final String columnDataType, final SCDType scdType,
                               final boolean mandatory, final String description,
                               final int position) {
        super();
        this.parent = parent;
        this.name = name;
        this.length = length;
        this.scale = scale;
        this.columnDataType = columnDataType;
        this.scdType = scdType;
        this.mandatory = mandatory;
        this.description = description;
        this.position = position;
    }

    @Override
    public DataStore getParent() {
        return parent;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getScale() {
        return scale;
    }

    @Override
    public String getColumnDataType() {
        return columnDataType;
    }

    @Override
    public SCDType getColumnSCDType() {
        return scdType;
    }

    @Override
    public boolean hasNotNullConstraint() {
        return mandatory;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append(" : ");
        sb.append(columnDataType);
        if (mandatory)
            sb.append(" [mandatory] ");
        if (scdType != null)
            sb.append(" ").append(scdType);

        return sb.toString();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getPosition() {
        return position;
    }

}
