package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.SCDType;
import one.jodi.core.extensions.strategies.FlagsStrategy;

/**
 * This interface is used to provide contextual information for
 * {@link FlagsStrategy FlagsStrategy}
 * for a selected target column.
 */
public interface FlagsTargetColumnExecutionContext extends TargetColumnExecutionContext {

    /**
     * Gets the data type of the current column in the target data store. See
     * {@link #getTargetColumnName()} to retrieve the column name.
     *
     * @return the target column data type
     */
    public String getColumnDataType();

    /**
     * Gets the slowly-changing dimension (SCD) type of the column as defined in
     * the meta data repository for the data store.
     *
     * @return the column SCD type
     */
    public SCDType getColumnSCDType();

    /**
     * Determines if the column has an NOT NULL constraint in the meta data
     * repository for the data store.
     *
     * @return <code>true</code> if NOT NULL constraint is set; otherwise
     * <code>false</code>
     */
    boolean hasNotNullConstraint();

    /**
     * Determines whether or not to use an Expression in ODI.
     *
     * @return use expressions or not
     */
    boolean useExpression();

}