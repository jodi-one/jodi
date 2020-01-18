package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataModel;

/**
 * Execution context for use with Check KM strategies.
 *
 */
public interface CheckKnowledgeModuleExecutionContext extends KnowledgeModuleExecutionContext {

    /**
     * Get the staging data model, when applicable.
     *
     * @return data model descriptor
     */
    DataModel getStagingDataModel();

}
