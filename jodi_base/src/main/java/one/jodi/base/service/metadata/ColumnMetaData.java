package one.jodi.base.service.metadata;

import java.util.Map;

/**
 * Column meta data information.
 *
 */
public interface ColumnMetaData {

    /**
     *
     * @return  dataStoreName name of the datastore
     */
    String getDataStoreName();

    /**
     * Name of data store column
     *
     * @return name of column
     */
    String getName();

    /**
     * Gets the column data type.
     *
     * @return the column data type
     */
    String getColumnDataType();

    /**
     * @return Get the length or precision of this column.
     */
    int getLength();

    /**
     * @return Get the scale for numeric types.
     */
    int getScale();

    /**
     * Gets the column scd type.
     *
     * @return the column scd type
     */
    SlowlyChangingDataType getColumnSCDType();

    /**
     * Determines if the column has an associated NOT NULL constraint
     *
     * @return true if NOT NULL constraint is set
     */
    boolean hasNotNullConstraint();

    /**
     * Gets the flex field values.
     *
     * @return the flex field values
     */
    Map<String, Object> getFlexFieldValues();

    /**
     * @return the column description
     */
    String getDescription();

    /**
     * @return a number greater than 0 that indicates the sequence in which
     * columns are ordered in a table definition
     */
    int getPosition();

}
