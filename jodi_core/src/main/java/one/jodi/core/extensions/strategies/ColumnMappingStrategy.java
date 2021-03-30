package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;

/**
 * This interface defines the plug-in for determining how target columns
 * are mapped from source datasets.
 */
public interface ColumnMappingStrategy {

    /**
     * Derive the column mappings using the execution contexts and expression.
     * The execution context provided is specific to a single <code>Source/Dataset</code> with the <code>DataStore</code>s provided
     * for the Source and Lookups defined in the <code>Dataset</code>.  The strategy may elect to preserve the mapping or otherwise
     * set a new expression for the Dataset's mappings expression.  (Note that each <code>Dataset</code> needs to generate its own
     * mapping expression.)
     *
     * @param currentMappingExpression      As a {@link String}
     * @param columnMappingExecutionContext {@link ColumnMappingExecutionContext}
     * @param targetColumnExecutionContext  {@link TargetColumnExecutionContext}
     * @return mapping expression or null if indeterminate
     */
    String getMappingExpression(String currentMappingExpression,
                                ColumnMappingExecutionContext columnMappingExecutionContext,
                                TargetColumnExecutionContext targetColumnExecutionContext);

}
