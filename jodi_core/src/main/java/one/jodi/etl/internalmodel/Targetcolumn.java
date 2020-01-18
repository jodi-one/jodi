package one.jodi.etl.internalmodel;

import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.model.extensions.TargetColumnExtension;

import java.util.List;
import java.util.Set;


/**
 * The TargetColumn class defines mapping expressions from one or multiple columns of source datasets (result
 * of join, filter and lookup operations per dataset) to a target column. In the case the target column is
 * defined for a temporary table, it defines its data type. Lastly, the target column may include an indication
 * if the column is mandatory (not null) or used as a key for merge operations.
 *
 */
public interface Targetcolumn {
    /**
     * Convenience method used to fetch parent object.
     *
     * @return parent
     */
    Mappings getParent();

    /**
     * Get the name of the target data store column.
     *
     * @return name
     */
    String getName();

    /**
     * Get the list of mappings expressions, each map a dataset to this column.
     *
     * @return expressions
     */
    List<String> getMappingExpressions();

    /**
     * Returns the mandatory flag, which proscribes if that column is nullable.
     *
     * @return column is nullable
     */
    Boolean isMandatory();


    /**
     * Returns the mandatory flag, which proscribes if that column is nullable.
     *
     * @return column is nullable
     */
    Boolean isExplicitlyMandatory();

    /**
     * Determine if target column is used as an update key for merge operations.
     *
     * @return column used as update key
     */
    Boolean isUpdateKey();

    /**
     * Determine if target column is used as an update key for merge operations.
     *
     * @return column used as update key
     */
    Boolean isExplicitlyUpdateKey();


    /**
     * Determine if the target column is insert derived by Jodi.
     *
     * @return key may be used for updating
     */
    Boolean isInsert();

    /**
     * Determine if the target column should update.  This is derived by Jodi.
     *
     * @return boolean indication whether this column should be updated
     */
    Boolean isUpdate();

    /**
     * Return a set of user defined flags.  The flag is only set when the value is true.
     *
     * @return user defined flag value
     */
    //Map<String, Boolean> getFlags();
    Set<UserDefinedFlag> getUserDefinedFlags();

    Set<UserDefinedFlag> getExplicitUserDefinedFlags();

    /**
     * Execution location for where target column mapping expressions {@link #getMappingExpressions()} should be executed.  This may be either {@link one.jodi.core.internalmodel.FilterExecutionLocationtypeEnum.SOURCE} or
     * {@link one.jodi.core.model.ExecutionLocationtypeEnum.ExecutionLocationtypeEnum.SOURCE}.  This may be optionally specified using either the
     * by higher level specifications, or directly via  <code>Transformation/Mappings/TargetColumn/ExecutionLocation</code>.  When not specified the property is
     * derived by Jodi; this behavior can be extended through the use of {@link ExecutionLocationStrategy}
     * <p>
     * The list should be of same cardinality as {@link #getMappingExpressions()}
     *
     * @return execution location for target column expression
     */
    //ExecutionLocationtypeEnum getExecutionLocation();
    List<ExecutionLocationtypeEnum> getExecutionLocations();

    /**
     * Fetch target column type.
     * <p>
     * When not defined will be set to null
     * .
     *
     * @return type
     */
    String getDataType();

    /**
     * Fetch target column scale.
     * <p>
     * When not defined will be set to 0.
     *
     * @return length
     */
    int getLength();

    /**
     * Fetch scale indicator used only when target data store is temporary.
     * <p>
     * When not defined will be set to 0.
     *
     * @return scale
     */
    int getScale();

    /**
     * Fetch the extension object as defined by Jodi customization.
     *
     * @return extension
     */
    TargetColumnExtension getExtension();

    /**
     * @param datasetNumber (NOT INDEX)
     * @return Indicating that all mapping expressions is an aggregate or not.
     */
    boolean isAggregateColumn(int dataset);

    /**
     * @return Indicating that all mapping expressions for this target column are analytical functions
     */
    boolean isAnalyticalFunction(int dataSetNumber);

    /**
     * @return The position of the targetcolumn; a number indication order in the textual specifications.
     */
    int getPosition();


    /**
     * @return The targetcolumn execution location when explicitly defined else null.
     */
    ExecutionLocationType getTargetcolumnExplicitExecutionLocation();

}

