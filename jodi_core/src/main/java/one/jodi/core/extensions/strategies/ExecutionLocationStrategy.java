package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.*;
import one.jodi.core.extensions.types.ExecutionLocationType;

/**
 * This interface defines the plug-in for determining the execution location of
 * filters, joins, lookups and target column mappings. It is used to implement
 * the default execution location policy and a custom policy. The default policy
 * plug-in is always executed before the custom plug-in is executed.
 * <p>
 * The ExecutionLocation strategy interface.
 *
 */
public interface ExecutionLocationStrategy {

    /**
     * Determines the mapping execution location for a target column. The column
     * is defined in
     * ExecutionLocationTargetColumnExecutionContext.getTargetColumnName(). The
     * potential values returned by this method are SOURCE, WORK or TARGET.
     *
     * @param defaultExecutionLocation the default execution location
     * @param dsContext                context information related to the target data store
     * @param tcContext                contextual information related to the target column of the
     *                                 target data store that is processed
     * @return mapping execution location for target column with potential
     * values of SOURCE, WORK or TARGET
     */
    ExecutionLocationType getTargetColumnExecutionLocation(ExecutionLocationType defaultExecutionLocation,
                                                           ExecutionLocationDataStoreExecutionContext dsContext,
                                                           ExecutionLocationTargetColumnExecutionContext tcContext);

    /**
     * Determines the filter execution location using the provided execution
     * context for this plug-in feature.
     *
     * @param defaultExecutionLocation the default execution location that is either explicitly
     *                                 defined in the XML specification or determined by a previously
     *                                 executed strategy (typically default strategy)
     * @param context                  contextual information related to the filter
     * @return the filter execution location with potential values of SOURCE or
     * WORK
     */
    ExecutionLocationType getFilterExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationFilterExecutionContext context);

    /**
     * Determines the join execution location using the provided execution
     * context for this plug-in feature.
     *
     * @param defaultExecutionLocation the default execution location that is either explicitly
     *                                 defined in the XML specification or determined by a previously
     *                                 executed strategy (typically default strategy)
     * @param context                  contextual information for the join
     * @return the join execution location with potential values of SOURCE or
     * WORK
     */
    ExecutionLocationType getJoinExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationJoinExecutionContext context);

    /**
     * Determines the lookup execution location using the provided execution
     * context for this plug-in feature.
     *
     * @param defaultExecutionLocation the default execution location that is either explicitly
     *                                 defined in the XML specification or determined by a previously
     *                                 executed strategy (typically default strategy)
     * @param context                  contextual information for the lookup
     * @return the lookup execution location with potential values of SOURCE or
     * WORK
     */
    ExecutionLocationType getLookupExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationLookupExecutionContext context);

    /**
     * Determines the subquery execution location using the provided execution
     * context for this plug-in feature.
     *
     * @param defaultExecutionLocation the default execution location that is either explicitly
     *                                 defined in the XML specification or determined by a previously
     *                                 executed strategy (typically default strategy)
     * @param context                  contextual information for the lookup
     * @return the lookup execution location with potential values of SOURCE or
     * WORK
     */
    ExecutionLocationType getSubQueryExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationSubQueryExecutionContext context);
}
