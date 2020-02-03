package one.jodi.core.extensions.contexts;

import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;

import java.util.List;

/**
 * This interface is used to provide contextual information for
 * {@link ExecutionLocationStrategy
 * ExecutionLocationStrategy} that is used to determine the execution locations
 * for mappings for each target column to be processed.
 */
public interface ExecutionLocationTargetColumnExecutionContext extends TargetColumnExecutionContext {

    /**
     * Gets the SQL expressions associated with the target column mapping.
     *
     * @return the SQL expressions
     */
    List<String> getSqlExpressions();
}
