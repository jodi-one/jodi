package one.jodi.base.model.types.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreForeignReference;
import one.jodi.base.service.metadata.ForeignReference;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DataStoreForeignReferenceImpl implements DataStoreForeignReference, Serializable {

    private static final long serialVersionUID = -2259190836751928131L;

    private final String name;
    private final boolean isEnabledInDatabase;
    private final DataStore foreignDataStore;
    private final DataStore primaryDataStore;
    private final List<DataStoreReferenceColumn> dataStoreReferenceColumns;

    public DataStoreForeignReferenceImpl(final ForeignReference fref,
                                         final DataStore foreign,
                                         final DataStore primary) {
        this.name = fref.getName();
        this.isEnabledInDatabase = fref.isEnabledInDatabase();
        this.foreignDataStore = foreign;
        this.primaryDataStore = primary;
        this.dataStoreReferenceColumns = convert(fref.getReferenceColumns());
    }

    private List<DataStoreForeignReference.DataStoreReferenceColumn>
    convert(List<ForeignReference.RefColumns> refColumns) {

        List<DataStoreForeignReference.DataStoreReferenceColumn>
                references = refColumns.stream()
                .map(DataStoreReferenceColumnImpl::new)
                .collect(Collectors.toList());
        return Collections.unmodifiableList(references);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataStore getForeignKeyDataStore() {
        return foreignDataStore;
    }

    @Override
    public DataStore getPrimaryKeyDataStore() {
        return primaryDataStore;
    }

    @Override
    public List<DataStoreReferenceColumn> getReferenceColumns() {
        return dataStoreReferenceColumns;
    }

    @Override
    public boolean isEnabledInDatabase() {
        return isEnabledInDatabase;
    }

    @Override
    public String toString() {
        String sep = "";
        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append(" : ");
        sb.append(foreignDataStore.getDataModel().getModelCode());
        sb.append(":");
        sb.append(foreignDataStore.getDataStoreName());
        sb.append(" (");
        for (DataStoreReferenceColumn refColumn : dataStoreReferenceColumns) {
            sb.append(sep);
            sb.append(refColumn.getForeignKeyColumnName());
            sep = ", ";
        }
        sb.append(") --> ");
        sb.append(primaryDataStore.getDataModel().getModelCode());
        sb.append(":");
        sb.append(primaryDataStore.getDataStoreName());
        sb.append(" (");
        sep = "";
        for (DataStoreReferenceColumn refColumn : dataStoreReferenceColumns) {
            sb.append(sep);
            sb.append(refColumn.getPrimaryKeyColumnName());
            sep = ", ";
        }
        sb.append(")");

        return sb.toString();
    }

    private static class DataStoreReferenceColumnImpl implements
            DataStoreForeignReference.DataStoreReferenceColumn, Serializable {

        private static final long serialVersionUID = 704500959050839741L;

        private final String foreignKeyColumnName;
        private final String primaryKeyColumnName;

        DataStoreReferenceColumnImpl(ForeignReference.RefColumns refColumns) {
            super();
            this.foreignKeyColumnName = refColumns.getForeignKeyColumnName();
            this.primaryKeyColumnName = refColumns.getPrimaryKeyColumnName();
        }

        @Override
        public String getForeignKeyColumnName() {
            return foreignKeyColumnName;
        }

        @Override
        public String getPrimaryKeyColumnName() {
            return primaryKeyColumnName;
        }
    }
}
