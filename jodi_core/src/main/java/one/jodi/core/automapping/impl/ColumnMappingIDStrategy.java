package one.jodi.core.automapping.impl;

import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.ColumnMappingStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The AutoMappingIDStrategy is a simple strategy that merely returns the mappings it consumes.
 * Its intended use is for the customStrategy for the {@link ColumnMappingContextImpl}
 */
public class ColumnMappingIDStrategy implements ColumnMappingStrategy {

    private final static Logger log = LogManager.getLogger(ColumnMappingIDStrategy.class);


    @Override
    public String getMappingExpression(
            String currentMappingExpression,
            ColumnMappingExecutionContext context,
            TargetColumnExecutionContext columnContext) {
        log.debug("returning identical mapping expression");
        return currentMappingExpression;
    }

}
