package one.jodi.core.transformation.impl;

import one.jodi.core.extensions.contexts.TransformationNameExecutionContext;
import one.jodi.core.extensions.strategies.TransformationNameStrategy;

/**
 * Identity strategy that is used as a placeholder for a custom strategy.
 *
 */
public class TransformationNameIDStrategy implements TransformationNameStrategy {

    /**
     * Implements the identity (ID) strategy that returns the default value
     * passed into this strategy method
     *
     * @see TransformationNameStrategy#getTransformationName(java.lang.String, TransformationNameExecutionContext)
     */
    @Override
    public String getTransformationName(final String defaultName,
                                        final TransformationNameExecutionContext execContext) {

        return defaultName;
    }

}