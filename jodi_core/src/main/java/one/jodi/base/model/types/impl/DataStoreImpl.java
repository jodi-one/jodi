package one.jodi.base.model.types.impl;

import one.jodi.base.model.types.*;
import one.jodi.base.model.types.DataStoreKey.KeyType;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.ForeignReference;
import one.jodi.base.service.metadata.SlowlyChangingDataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of a {@link DataStore} interface and represents both
 * standard data stores and temporary tables.
 *
 */
public class DataStoreImpl implements DataStore, Serializable {

    private static final long serialVersionUID = 2517766478207443763L;
    private final static Logger logger = LogManager.getLogger(DataStoreImpl.class);
    // Attributes that describe the data store / table
    private final String dataStoreName;
    private final boolean temporary;
    private final DataStoreType dataStoreType;
    private final List<DataStoreKey> keys;
    private final Map<String, Object> dataStoreFlexfields;
    private final String description;
    // model
    private final DataModel dataModel;
    // Attributes used for lazy evaluation
    private transient LazyCreation creationService;
    private transient List<ForeignReference> foreignRef;
    private Map<String, DataStoreColumn> dataStoreColumnMap;
    // foreign references are lazily evaluated
    private List<DataStoreForeignReference> foreignReferences = null;

    /**
     * Constructor for creation of an class instance.
     *
     * @param desc          describes the data store using the information provided by the
     *                      ETL subsystem
     * @param dataStoreType derived information about the data store use as defined by the
     *                      naming conventions in the property file
     * @param dataModel     The datamodel of the datastore.
     */
    public DataStoreImpl(final DataStoreDescriptor desc,
                         final DataStoreType dataStoreType,
                         final DataModel dataModel,
                         final LazyCreation creationService) {
        super();
        this.creationService = creationService;
        // data store attributes are assigned
        this.dataStoreName = desc.getDataStoreName();
        this.dataStoreType = dataStoreType;
        this.dataStoreColumnMap = createDataStoreColumnMap(this, desc);
        this.temporary = desc.isTemporary();
        this.keys = createDataStoreKeys(desc);
        this.foreignRef = desc.getFKRelationships();
        this.dataStoreFlexfields = desc.getDataStoreFlexfields();
        this.description = desc.getDescription();

        // model attributes are assigned
        this.dataModel = dataModel;
    }

    /**
     * Constructor for creating a temporary data store
     *
     * @param dataStoreName
     * @param dataStoreColumns
     * @param dataModel
     */
    public DataStoreImpl(final String dataStoreName,
                         final Map<String, DataStoreColumn> dataStoreColumns,
                         final DataModel dataModel) {
        super();
        assert (dataStoreColumns != null);

        // data store attributes are assigned
        this.dataStoreName = dataStoreName;
        this.dataStoreType = DataStoreType.UNKNOWN;
        this.dataStoreColumnMap = dataStoreColumns;
        this.temporary = true;
        this.keys = Collections.emptyList();
        this.foreignReferences = Collections.emptyList();
        this.dataStoreFlexfields = Collections.emptyMap();
        this.description = "Temporary Table metadata derived from XML Specification";

        // model is assigned
        this.dataModel = dataModel;
    }

    /**
     * This method intercepts request to serialize this Object. Due to the lazy
     * evaluation of the FK relationship, the object graph may not be completely
     * initiated at the time of the serialization request. This interceptor
     * completes the initialization of the object before it is serialized.
     *
     * @param stream uses by Java to serialize this object to a stream
     * @throws java.io.IOException
     */
    private void writeObject(java.io.ObjectOutputStream stream)
            throws java.io.IOException {
        // The FK references have not been initialized if still null.
        if (foreignReferences == null) {
            // Initialize method by calling the method that constructs the FK
            // relationship.
            getDataStoreForeignReference();
        }
        stream.defaultWriteObject();
    }

    private Map<String, DataStoreColumn> createDataStoreColumnMap(
            final DataStore parent,
            final DataStoreDescriptor dataStoreDescriptor) {
        LinkedHashMap<String, DataStoreColumn> result =
                new LinkedHashMap<>(dataStoreDescriptor.getColumnMetaData().size());
        for (ColumnMetaData column : dataStoreDescriptor.getColumnMetaData()) {
            String type = column.getColumnDataType();
            if (type == null) {
                String model = parent.getDataModel() != null
                        ? parent.getDataModel().getModelCode()
                        : "Unknown.";
                String msg = String.format(
                        "This column has no datatype attached; '%s' in model '%s'.",
                        parent.getDataStoreName() + "." + column.getName(), model);
                logger.warn(msg);
                continue;
            }
            result.put(column.getName(), new DataStoreColumnImpl(
                    parent, column.getName(),
                    column.getLength(), column.getScale(),
                    type,
                    convertToSCDType(column.getColumnSCDType()),
                    column.hasNotNullConstraint(),
                    column.getDescription(),
                    column.getPosition()));
        }
        return result;
    }

    private List<DataStoreKey> createDataStoreKeys(final DataStoreDescriptor desc) {
        // collect Key information
        final List<DataStoreKey> keys = desc.getKeys()
                .stream()
                .map(DataStoreKeyImpl::new)
                .collect(Collectors.toList());
        return Collections.unmodifiableList(keys);
    }

    private List<DataStoreForeignReference> createForeignReferences(
            final List<ForeignReference> foreignRefs,
            final DataStore foreignDataStore) {
        return creationService.createForeignReferences(foreignRefs, foreignDataStore);
    }

    @Override
    public String getDataStoreName() {
        return dataStoreName;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public Map<String, DataStoreColumn> getColumns() {
        return Collections.unmodifiableMap(dataStoreColumnMap);
    }

    /**
     * Only to be used with creation of temporary table
     *
     * @param dataStoreColumnMap
     */
    protected void setColumns(Map<String, DataStoreColumn> dataStoreColumnMap) {
        // assumes that an empty Map is assigned when using this method
        assert (this.dataStoreColumnMap != null && this.dataStoreColumnMap.isEmpty());
        this.dataStoreColumnMap = dataStoreColumnMap;
    }

    @Override
    public DataStoreType getDataStoreType() {
        return dataStoreType;
    }

    @Override
    public List<DataStoreKey> getDataStoreKeys() {
        return keys;
    }

    @Override
    public DataStoreKey getPrimaryKey() {
        DataStoreKey primary = null;
        for (DataStoreKey key : keys) {
            if (key.getType() == KeyType.PRIMARY) {
                primary = key;
                break;
            }
        }
        return primary;
    }

    @Override
    public DataStoreKey getAlternateKey() {
        DataStoreKey primary = null;
        for (DataStoreKey key : keys) {
            if (key.getType() == KeyType.ALTERNATE) {
                primary = key;
                break;
            }
        }
        return primary;
    }


    @Override
    public List<DataStoreForeignReference> getDataStoreForeignReference() {
        // Lazy evaluation to prevent that the primary data stores referenced in
        // each created DataStoreForeignReference is not created before this object
        // has been created. This is needed because otherwise we may end in an
        // endless  loop, e.g. due to self-reference of a data store. These loops is
        // broken by creating the DataStoreForeignReference after its parent object
        // is created and cached.
        if (foreignReferences == null) {
            try {
                this.foreignReferences = createForeignReferences(foreignRef, this);
//    		  } catch (NullPointerException npe) {
//    			  // no foreign references present.
//    			  this.foreignReferences = null;
            } finally {
                if (foreignReferences != null) {
                    // the common builder instance and the foreign reference can be
                    // released after the creation of the foreign references.
                    creationService = null;
                    foreignRef = null;
                }
            }
        }
        return foreignReferences;
    }

    @Override
    public Map<String, Object> getDataStoreFlexfields() {
        return dataStoreFlexfields;
    }

    @Override
    public String toString() {
        final String lineSeperator = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        sb.append(dataModel.getModelCode());
        sb.append(":");
        sb.append(dataStoreName);
        if (temporary)
            sb.append(" [temporary] ");
        if ((dataStoreType != null) && (dataStoreType != DataStoreType.UNKNOWN))
            sb.append("[type: ").append(dataStoreType).append("]");
        sb.append(lineSeperator);

        if (getColumns().size() > 0) {
            sb.append(" COLUMNS:");
            sb.append(lineSeperator);
            for (DataStoreColumn column : getColumns().values()) {
                sb.append("  ");
                sb.append(column.toString());
                sb.append(lineSeperator);
            }
        } else {
            sb.append("  NO COLUMNS DEFINED");
            sb.append(lineSeperator);
        }

        if (keys.size() > 0) {
            sb.append(" KEYS:");
            sb.append(lineSeperator);
            for (DataStoreKey key : keys) {
                sb.append("  ");
                sb.append(key.toString());
                sb.append(lineSeperator);
            }
        }

        // force evaluation of all data store relationships
        if (foreignReferences == null) {
            // Initialize method by calling the method that constructs the FK
            // relationship.
            getDataStoreForeignReference();
        }

        if (foreignReferences.size() > 0) {
            sb.append(" FOREIGN KEY RETATIONSHIP:");
            sb.append(lineSeperator);
            for (DataStoreForeignReference fref : foreignReferences) {
                sb.append("  ");
                sb.append(fref.toString());
                sb.append(lineSeperator);
            }
        }

        return sb.toString();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DataModel getDataModel() {
        return dataModel;
    }

    private SCDType convertToSCDType(SlowlyChangingDataType type) {

        SCDType result = null;
        if (type != null) {
            switch (type) {
                case SURROGATE_KEY:
                    result = SCDType.SURROGATE_KEY;
                    break;
                case NATURAL_KEY:
                    result = SCDType.NATURAL_KEY;
                    break;
                case OVERWRITE_ON_CHANGE:
                    result = SCDType.OVERWRITE_ON_CHANGE;
                    break;
                case ADD_ROW_ON_CHANGE:
                    result = SCDType.ADD_ROW_ON_CHANGE;
                    break;
                case CURRENT_RECORD_FLAG:
                    result = SCDType.CURRENT_RECORD_FLAG;
                    break;
                case START_TIMESTAMP:
                    result = SCDType.START_TIMESTAMP;
                    break;
                case END_TIMESTAMP:
                    result = SCDType.END_TIMESTAMP;
                    break;
                default:
                    result = SCDType.valueOf(type.toString());
            }
        }
        return result;
    }
}
