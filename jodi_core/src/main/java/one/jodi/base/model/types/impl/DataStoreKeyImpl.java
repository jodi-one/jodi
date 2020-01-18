package one.jodi.base.model.types.impl;

import one.jodi.base.model.types.DataStoreKey;
import one.jodi.base.service.metadata.Key;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

class DataStoreKeyImpl implements DataStoreKey, Serializable {

    private static final long serialVersionUID = 3251914312836598528L;

    private final String name;
    private final KeyType type;
    private final List<String> columns;
    private final boolean inDatabase;
    private final boolean enabledInDatabase;

    protected DataStoreKeyImpl(Key key) {
        super();
        this.name = key.getName();
        this.type = mapKeyType(key.getType());
        this.columns = Collections.unmodifiableList(key.getColumns());
        this.inDatabase = key.existsInDatabase();
        this.enabledInDatabase = key.isEnabledInDatabase();
    }

    private DataStoreKey.KeyType mapKeyType(Key.KeyType type) {

        DataStoreKey.KeyType result;
        switch (type) {
            case PRIMARY:
                result = DataStoreKey.KeyType.PRIMARY;
                break;
            case ALTERNATE:
                result = DataStoreKey.KeyType.ALTERNATE;
                break;
            default:
                result = DataStoreKey.KeyType.INDEX;
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public KeyType getType() {
        return type;
    }

    @Override
    public List<String> getColumns() {
        return columns;
    }

    @Override
    public boolean existsInDatabase() {
        return inDatabase;
    }

    @Override
    public boolean isEnabledInDatabase() {
        return enabledInDatabase;
    }

    @Override
    public String toString() {
        String sep = "";
        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append(" : ");
        sb.append(type);
        sb.append(" (");
        for (String column : columns) {
            sb.append(sep);
            sb.append(column);
            sep = ", ";
        }
        sb.append(")");
        return sb.toString();
    }

}
