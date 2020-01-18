package one.jodi.core.km.impl;

import com.google.inject.Inject;
import one.jodi.core.extensions.contexts.CheckKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.KnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.LoadKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.StagingKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;

/**
 * This is the Identity Strategy that returns the very same
 * KnowledgeModuleConfiguration as consumed.
 *
 */
public class KnowledgeModuleIDStrategy implements KnowledgeModuleStrategy {

    @Inject
    public KnowledgeModuleIDStrategy() {
        super();
    }

    @Override
    public KnowledgeModuleConfiguration getLKMConfig(
            KnowledgeModuleConfiguration defaultLkmConfig,
            LoadKnowledgeModuleExecutionContext executionContext) {
        return defaultLkmConfig;
    }

    @Override
    public KnowledgeModuleConfiguration getCKMConfig(
            KnowledgeModuleConfiguration defaultCkmConfig,
            CheckKnowledgeModuleExecutionContext executionContext) {
        return defaultCkmConfig;
    }

    @Override
    public KnowledgeModuleConfiguration getIKMConfig(
            KnowledgeModuleConfiguration defaultIkmConfig,
            KnowledgeModuleExecutionContext executionContext) {
        return defaultIkmConfig;
    }

    @Override
    public String getStagingModel(String defaultStagingModel,
                                  StagingKnowledgeModuleExecutionContext executionContext) {
        return defaultStagingModel;
    }

}
