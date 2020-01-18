package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.TransformationNameExecutionContext;

/**
 * This interface defines the plug-in for naming the generated transformation
 * (a.k.a. ODI interface). It is used to implement the default naming policy and
 * a custom policy. The default policy plug-in is always executed before the
 * custom plug-in is executed.
 *
 */
public interface TransformationNameStrategy {

    /**
     * This method determines the name of the transformation using the provided
     * execution context for this plug-in feature.
     *
     * @param defaultName is the explicitly defined transformation name or
     *                    <code>null</code> when passed into the default strategy; the
     *                    value is the result of the default strategy when passed to the
     *                    custom strategy. In this case the value will not be
     *                    <code>null</code>.
     * @param execContext execution context object that provides contextual information
     *                    related to the transformation name decision.
     * @return the name of the transformation
     * @throws IncorrectCustomStrategyException if transformation name is <code>null</code> or empty String
     */
    String getTransformationName(String defaultName, TransformationNameExecutionContext execContext);

}
