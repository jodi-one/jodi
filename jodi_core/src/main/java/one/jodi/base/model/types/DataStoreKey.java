package one.jodi.base.model.types;

import java.util.List;

/**
 * Meta data that describes keys associated with a data store.
 */
public interface DataStoreKey {

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
