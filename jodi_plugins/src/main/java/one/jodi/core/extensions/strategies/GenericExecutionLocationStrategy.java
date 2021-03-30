package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.ExecutionLocationDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.ExecutionLocationFilterExecutionContext;
import one.jodi.core.extensions.contexts.ExecutionLocationJoinExecutionContext;
import one.jodi.core.extensions.contexts.ExecutionLocationLookupExecutionContext;
import one.jodi.core.extensions.contexts.ExecutionLocationSubQueryExecutionContext;
import one.jodi.core.extensions.contexts.ExecutionLocationTargetColumnExecutionContext;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.model.extensions.TargetColumnExtension;

@Deprecated
public class GenericExecutionLocationStrategy implements ExecutionLocationStrategy {

    /* (non-Javadoc)
     * @see one.jodi.core.extensions.strategies.ExecutionLocationStrategy#getExecutionLocation(
     * one.jodi.core.extensions.contexts.ExecutionLocationExecutionContext,
     * one.jodi.core.executionlocation.ExecutionLocationType)
     */
    @Override
    public ExecutionLocationType getTargetColumnExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationDataStoreExecutionContext dsContext,
            ExecutionLocationTargetColumnExecutionContext tcContext) {
        if (tcContext == null) {
            return ExecutionLocationType.WORK;
        }
        TargetColumnExtension extension = tcContext.getTargetColumnExtension();
        ExecutionLocationType result;
        if (extension != null && extension.getExecutionLocation() != null) {
            switch (extension.getExecutionLocation()) {
                case SOURCE:
                    result = ExecutionLocationType.SOURCE;
                    break;
                case TARGET:
                    result = ExecutionLocationType.TARGET;
                    break;
                default:
                    result = ExecutionLocationType.WORK;
            }
        } else {
            result = defaultExecutionLocation;
        }
        return result;
    }

    @Override
    public ExecutionLocationType getFilterExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationFilterExecutionContext context) {
        return defaultExecutionLocation;
    }

    @Override
    public ExecutionLocationType getJoinExecutionLocation(
            ExecutionLocationType defaultExecutionLocation,
            ExecutionLocationJoinExecutionContext context) {
        return defaultExecutionLocation;
    }

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
