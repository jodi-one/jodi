package one.jodi.core.extensions.strategies;

import one.jodi.base.service.schema.DataStoreNotInModelException;
import one.jodi.core.extensions.contexts.ModelNameExecutionContext;

/**
 * This interface defines the plug-in for determining the model code in the meta
 * data repository that a source, lookup or target data store referenced by name
 * in the transformation XML specification. It is used to implement the default
 * model code policy and a custom policy. The default policy plug-in is always
 * executed before the custom plug-in is executed.
 */
public interface ModelCodeStrategy {

    /**
     * This method determines the model code using the provided execution
     * context for this plug-in feature.
     *
     * @param defaultModelCode is the explicitly defined model code or <code>null</code> when
     *                         passed into the default strategy; the value is the result of
     *                         the default strategy when passed to the custom strategy. In
     *                         this case the value will not be <code>null</code>.
     * @param execContext      execution context object that provides contextual information
     *                         related to the model code decision.
     * @return model code which is a non-empty String
     * @throws IncorrectCustomStrategyException if model code is <code>null</code> or empty String
     * @throws AmbiguousModelException          if data store is found in more than one model and no policy
     *                                          exists to resolve the ambiguity.
     * @throws DataStoreNotInModelException     if the data store is explicitly assigned a model code in the
     *                                          XML specification and the data store does exist in this
     *                                          model.
     * @throws NoModelFoundException            if the data store cannot be found in the meta data repository
     */
    String getModelCode(String defaultModelCode,
                        ModelNameExecutionContext execContext)
            throws IncorrectCustomStrategyException;

}
