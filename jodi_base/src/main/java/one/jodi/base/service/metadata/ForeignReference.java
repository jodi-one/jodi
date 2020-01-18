package one.jodi.base.service.metadata;

import java.util.List;

/**
 * Represents a foreign key relationship between two data stores based on
 * column equality. data store has a foreign key that refers to a primary key in
 * the primary data store.<p>
 * <p>
 * Since a primary key can be a composite key (referring to multiple data store
 * columns), the foreign key must consist of the same number of columns. The
 * equality between each column pair from primary and foreign key is defined in a
 * {@link RefColumn reference column}.<p>
 * <p>
 * A class instance must be referenced by a {@link DataStoreDescriptor}.<p>
 *
 * <b>Note</b>: references between data stores can also be expressed by complex
 * expressions. However, this feature is not yet implemented at this point.
 *
 */
public interface ForeignReference {

    /**
     * @return name of foreign key reference
     */
    String getName();

    /**
     * @return name of data store at the primary key side of the relationship
     */
    String getPrimaryKeyDataStoreName();

    /**
     * @return model code of the primary data store
     */
    String getPrimaryKeyDataStoreModelCode();

    /**
     * @return list of objects define column pairs in primary and foreign data
     * store that must have a equality relationship
     */
    List<RefColumns> getReferenceColumns();

    /**
     * @return <code>true</code> if FK reference exists in database and is enabled in the
     * database; <code>false</code> otherwise
     */
    boolean isEnabledInDatabase();

    /**
     * Defines a foreign and primary column pair for which column equality is
     * required.
     */
    interface RefColumns {
        String getForeignKeyColumnName();

        String getPrimaryKeyColumnName();
    }

}
