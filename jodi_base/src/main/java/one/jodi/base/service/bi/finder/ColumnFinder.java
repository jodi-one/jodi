package one.jodi.base.service.bi.finder;

import java.util.Optional;

/**
 * Is supplied to BiExpressionParser to allow validation of column and to
 * retrieve type information. This interface must not be implemented by Spoofax
 * parser.
 *
 */
public interface ColumnFinder {
    /**
     * The column finder is passed with each expression string to the Spoofax
     * parser. The implementation details are completely hidden from the parser.
     * The initialization is performed by the Generator main logic and may
     * vary depending on type of column that is defined (pre- or post-aggregation
     * columns).
     *
     * @param tableName name of the physical or logical table
     * @param columnName    name of the column. This name may be the physical column name or
     *                  may be a logical name.
     * @return column descriptor that meets the table and column name criteria.
     * An empty Optional indicates that no such column was found in the
     * context.
     */
    FinderResult find(Optional<String> tableName, String columnName);
}
