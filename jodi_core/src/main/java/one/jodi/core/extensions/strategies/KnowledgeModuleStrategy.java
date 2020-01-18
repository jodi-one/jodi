package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.CheckKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.KnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.LoadKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.StagingKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;

/**
 * Strategy interface used to provide context with information used to set the
 * name and options for ODI Knowledge Modules.
 * <p>
 * Customization of behavior using Java may be achieved by writing a class that
 * implements the KnowledgeModuleStrategy interface.
 *
 */
public interface KnowledgeModuleStrategy {

    /**
     * This method computes the Load Knowledge Module name and options to be
     * applied to load the transformation's source and lookup data stores into
     * the staging tables associated with the target data store.
     *
     * @param defaultLKMConfig is an object representing the explicitly defined LKM and
     *                         options or <code>null</code> when passed into the default
     *                         strategy; the value is the result of the default strategy when
     *                         passed to the custom strategy. In this case the value will not
     *                         be <code>null</code>.
     * @param executionContext information relevant to the sources, lookups and target of the
     *                         transformation
     * @return the populated KnowledgeModuleConfiguration instance. This may be
     * the configuration passed in or a new configuration.
     * @throws IncorrectCustomStrategyException if return value is <code>null</code>
     */
    public KnowledgeModuleConfiguration getLKMConfig(
            KnowledgeModuleConfiguration defaultLKMConfig,
            LoadKnowledgeModuleExecutionContext executionContext);

    /**
     * This method determines a Checking Knowledge Module (CKM) and its options
     * to be applied when checking data to be inserted into the target data store.
     *
     * @param defaultCKMConfig is an object representing the explicitly defined CKM and
     *                         options or <code>null</code> when passed into the default
     *                         strategy; the value is the result of the default strategy when
     *                         passed to the custom strategy. In this case the value will not
     *                         be <code>null</code>.
     * @param executionContext information relevant to the target of the transformation
     * @return the populated KnowledgeModuleConfiguration instance. This may be
     * the configuration passed in or a new configuration.
     * @throws IncorrectCustomStrategyException if return value is <code>null</code>
     */
    public KnowledgeModuleConfiguration getCKMConfig(
            KnowledgeModuleConfiguration defaultCKMConfig,
            CheckKnowledgeModuleExecutionContext executionContext);

    /**
     * This method determines a Insert Knowledge Module (IKM) and its options to
     * be applied for the given target data store.
     *
     * @param defaultIKMConfig is an object representing the explicitly defined IKM and
     *                         options or <code>null</code> when passed into the default
     *                         strategy; the value is the result of the default strategy when
     *                         passed to the custom strategy. In this case the value will not
     *                         be <code>null</code>.
     * @param executionContext information relevant to the target of the transformation
     * @return the populated KnowledgeModuleConfiguration instance. This may be
     * the configuration passed in or a new configuration.
     * @throws IncorrectCustomStrategyException if return value is <code>null</code>
     */
    public KnowledgeModuleConfiguration getIKMConfig(
            KnowledgeModuleConfiguration defaultIKMConfig,
            KnowledgeModuleExecutionContext executionContext);

    /**
     * This method determines the staging model
     *
     * @param defaultStagingModel
     * @param executionContext
     * @return model code
     */
    public String getStagingModel(
            String defaultStagingModel,
            StagingKnowledgeModuleExecutionContext executionContext);

}
