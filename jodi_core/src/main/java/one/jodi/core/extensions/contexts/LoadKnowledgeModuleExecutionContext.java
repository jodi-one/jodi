package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;

/**
 * Contextual information for the for the
 * {@link KnowledgeModuleStrategy
 * KnowledgeModuleStrategy} when determining the Load Knowledge Module for a
 * specific data source.
 * <p>
 * This extends {@link KnowledgeModuleExecutionContext} in order to provide data
 * source information used for KM configuration process.
 */
public interface LoadKnowledgeModuleExecutionContext extends
        KnowledgeModuleExecutionContext {

    /**
     * Gets the dataset name the source data store is associated with (see
     * method {@link #getSourceDataStore()}).
     *
     * @return dataset name
     */
    public String getDatasetName();

    /**
     * Get the source data store for which a LKM is defined. The
     * {@link DataStore} contains read-only information used to determine KM
     * settings.
     *
     * @return source data store
     */
    public DataStore getSourceDataStore();

    /**
     * Get the staging model when applicable, e.g. a multi-technology IKM has been selected.
     * Otherwise will return null.
     *
     * @return staging data model
     */
    public DataModel getStagingDataModel();

}
