package one.jodi.base.model.types;

import java.util.List;
import java.util.Map;

/**
 * Metadata that describes and characterizes a data store and
 * is extended to include layering information.
 *
 */
public interface DataStore {

    /**
     * @return name of the data store; e.g. table name
     */
    String getDataStoreName();

    /**
     * @return true if this is a temporary data store; otherwise false
     */
    boolean isTemporary();

    /**
     * @return map of column names and meta data describing the column
     */
    Map<String, DataStoreColumn> getColumns();

    /**
     * @return higher-level data store type with reference primarily to role in
     * data mart
     */
    DataStoreType getDataStoreType();

    /**
     * @return a list of primary or alternate keys or indexes associated with this
     * DataStore.
     */
    List<DataStoreKey> getDataStoreKeys();

    /**
     * Special case of {@link #getDataStoreKeys()}.
     *
     * @return primary key of the data store or null if no primary key exists
     */
    DataStoreKey getPrimaryKey();

    /**
     * @return list of foreign references (a.k.a. foreign key relationships) that
     * this data store has with other data stores.
     */
    List<DataStoreForeignReference> getDataStoreForeignReference();

    /**
     * @return key-value pairs of additional custom data of type String or
     * Integer that is associated with the data store definition
     */
    Map<String, Object> getDataStoreFlexfields();

    /**
     * @return Description associated with the data store
     */
    String getDescription();


    /**
     * @return an object that describes the data model in which this data store is located
     */
    DataModel getDataModel();

    /**
     * @return an alternate or also named unique key for the datastore
     */
    DataStoreKey getAlternateKey();
}