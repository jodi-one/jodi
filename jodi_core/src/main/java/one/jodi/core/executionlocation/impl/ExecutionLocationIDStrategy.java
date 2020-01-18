package one.jodi.core.executionlocation.impl;

import one.jodi.core.extensions.contexts.*;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.types.ExecutionLocationType;

/**
 * The ExecutionLocationStrategy implementation that simply echoes back the
 * default ExecutionLocation.
 *
 */
public class ExecutionLocationIDStrategy implements ExecutionLocationStrategy {

    /**
     * @see ExecutionLocationStrategy#getFilterExecutionLocation(ExecutionLocationType, ExecutionLocationFilterExecutionContext)
     */
    @Override
    public ExecutionLocationType getFilterExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationFilterExecutionContext context) {
        return defaultExecutionLocation;
    }

    /**
     * @see ExecutionLocationStrategy#getJoinExecutionLocation(ExecutionLocationType, ExecutionLocationJoinExecutionContext)
     */
    @Override
    public ExecutionLocationType getJoinExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationJoinExecutionContext context) {
        return defaultExecutionLocation;
    }

    /**
     * @see ExecutionLocationStrategy#getTargetColumnExecutionLocation(ExecutionLocationType, ExecutionLocationDataStoreExecutionContext, ExecutionLocationTargetColumnExecutionContext)
     */
    @Override
    public ExecutionLocationType getTargetColumnExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationDataStoreExecutionContext dsContext,
            ExecutionLocationTargetColumnExecutionContext tcContext) {
        return defaultExecutionLocation;
    }

    /**
     * @see ExecutionLocationStrategy#getLookupExecutionLocation(ExecutionLocationType, ExecutionLocationLookupExecutionContext)
     */
    @Override
    public ExecutionLocationType getLookupExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationLookupExecutionContext context) {
        return defaultExecutionLocation;
    }

    @Override
    public ExecutionLocationType getSubQueryExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationSubQueryExecutionContext context) {
        return defaultExecutionLocation;
    }
}
