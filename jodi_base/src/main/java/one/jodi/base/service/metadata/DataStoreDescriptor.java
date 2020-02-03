package one.jodi.base.service.metadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface describing a data store a.k.a. table
 */
public interface DataStoreDescriptor {

    /**
     * @return name of the data store; e.g. table name
     */
    String getDataStoreName();

    /**
     * @return <code>true</code> if this is a temporary data store; otherwise <code>false</code>
     */
    boolean isTemporary();

    /**
     * @return key-value pairs of additional custom data of type String or
     * Integer that is associated with the data store definition
     */
    Map<String, Object> getDataStoreFlexfields();

    /**
     * @return descriptors of the columns of the data stores
     */
    Collection<ColumnMetaData> getColumnMetaData();

    /**
     * @return description of the model the data store is located in.
     */
    DataModelDescriptor getDataModelDescriptor();

    /**
     * @return keys associated with this data store
     */
    List<Key> getKeys();

    /**
     * @return foreign key relationships between this data store and other data
     * stores
     */
    List<ForeignReference> getFKRelationships();

    /**
     * @return the description of the datastore
     */
    String getDescription();
}