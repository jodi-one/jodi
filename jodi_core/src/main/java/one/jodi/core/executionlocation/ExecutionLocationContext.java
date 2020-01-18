package one.jodi.core.executionlocation;

import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.etl.internalmodel.*;

import java.util.List;
import java.util.Map;

/**
 * Context interface for the strategy to determine the ExecutionLocation of a column.
 *
 */
public interface ExecutionLocationContext {

    /**
     * Determines the ExecutionLocation for all columns associated with the specified Mappings.
     *
     * @param mappings     the Mappings instance whose columns will be processed
     * @param datasetName  the target dataset name
     * @param datasetIndex the target dataset index
     * @return Map of column name to ExecutionLocation for all columns associated with the Mappings instance
     */

    Map<String, ExecutionLocationType> getTargetColumnExecutionLocation(
            Mappings mappings,
            String datasetName,
            int datasetIndex);

    /**
     * Determines ExecutionLocation(s) for the target column.
     *
     * @param targetColumn
     * @return list of ExecutionLocationtypeEnums ordered by expression and associated dataset index.
     */
    List<ExecutionLocationType> getTargetColumnExecutionLocation(
            Targetcolumn targetColumn);

    /**
     * Gets the filter execution location.
     *
     * @param source the source
     * @return the filter execution location
     */
    ExecutionLocationType getFilterExecutionLocation(Source source);

    /**
     * Gets the join execution location.
     *
     * @param source the source
     * @return the join execution location
     */
    ExecutionLocationType getJoinExecutionLocation(Source source);

    /**
     * Gets the lookup execution location.
     *
     * @param lookup the lookup
     * @return the lookup execution location
     */
    ExecutionLocationType getLookupExecutionLocation(Lookup lookup);

    /**
     * Gets the SubQuery execution location.
     *
     * @param subquery the SubQuery
     * @return the subquery execution location
     */
    ExecutionLocationType getSubQueryExecutionLocation(SubQuery subquery);


}
