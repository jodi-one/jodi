package one.jodi.base.service.metadata;

import java.util.List;

/**
 * Represents a key associated with a data store. It is either a primary key, an
 * alternative key (unique key that is not the primary key) or an index, which
 * may or may not be unique.
 * <p>
 * A class instance must be referenced by a DataStore.
 */
public interface Key {

    /**
     * @return name of key
     */
    String getName();

    /**
     * @return type of key
     */
    KeyType getType();

    /**
     * @return non-empty, unmodifiable list of column names that are part of the
     * key.
     */
    List<String> getColumns();

    /**
     * @return <code>true</code> if the key exists in the database;
     * <code>false</code> otherwise which implies that the information was
     * added only in the underlying meta data repository
     */
    boolean existsInDatabase();

    /**
     * @return <code>true</code> if key exists in database and is enabled in the
     * database; <code>false</code> otherwise
     */
    boolean isEnabledInDatabase();

    /**
     * @return the datastore belonging to the key.
     */
    String getDataStoreName();

    /**
     * Sets the datastorename; required for view capability in OdbMetaDataHelper.
     *
     * @param datastoreName name of the datastore to be set
     */
    void setDataStoreName(String datastoreName);

    /**
     * Defines the key type in the underlying database management system.
     */
    enum KeyType {
        /**
         * unique key in the database system
         */
        PRIMARY,
        /**
         * Unique key that is not the primary key
         */
        ALTERNATE,
        /**
         * An index that may not be unique to improve access performance
         */
        INDEX
    }
}
