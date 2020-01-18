package one.jodi.base.model.types;

import java.util.List;

/**
 * Represents a foreign key relationship between two data stores based on
 * column equality. data store has a foreign key that refers to a primary key in
 * the primary data store.<p>
 * <p>
 * Since a primary key can be a composite key (referring to multiple data store
 * columns), the foreign key must consist of the same number of columns. The
 * equality between each column pair from primary and foreign key is defined in a
 * {@link FKReference reference column}.<p>
 * <p>
 * A class instance must be referenced by a {@link DataStore}.<p>
 *
 * <b>Note</b>: references between data stores can also be expressed by complex
 * expressions. However, this feature is not yet implemented at this point.
 *
 */
public interface DataStoreForeignReference {

    /**
     * @return name of foreign key reference
     */
    String getName();

    /**
     * @return data store at the foreign key side of the relationship, which will
     * be the parent object for this class instance.
     */
    DataStore getForeignKeyDataStore();

    /**
     * @return data store at the primary key side of the relationship
     */
    DataStore getPrimaryKeyDataStore();

    /**
     * @return list of objects define column pairs in primary and foreign data
     * store that must have a equality relationship
     */
    List<DataStoreReferenceColumn> getReferenceColumns();

    /**
     * @return <code>true</code> if FK reference exists in database and is enabled in the
     * database; <code>false</code> otherwise
     */
    boolean isEnabledInDatabase();

    /**
     * Defines a foreign and primary column pair for which column equality is
     * required.
     */
    interface DataStoreReferenceColumn {
        String getForeignKeyColumnName();

        String getPrimaryKeyColumnName();
    }

}
