package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;

import java.util.List;

/**
 * Contextual information for the for the
 * {@link KnowledgeModuleStrategy
 * KnowledgeModuleStrategy} when determining the Staging Model for applying
 * multi-technology Knowledge Module's staging model.
 * <p>
 * <p>
 * This extends {@link KnowledgeModuleExecutionContext} in order to provide data
 * source information used for KM configuration process.
 */
public interface StagingKnowledgeModuleExecutionContext extends
        KnowledgeModuleExecutionContext {

    /**
     * Get all source data stores for the transformation. The
     * {@link DataStore} contains read-only information used to determine
     * staging model.
     * <p>
     * The list ordered by traversing each <code>Dataset</code>, its <code>Source</code> children
     * and its <code>Lookup</code> children
     * in the input model. For example consider
     *
     * <pre>
     * Dataset X
     *   Source 1
     *   	Lookup A
     *   	Lookup B
     *   Source 2
     *   	Lookup C
     * Dataset Y
     *   Source 3
     * </pre>
     * <p>
     * This would return the list
     * <code>Source 1, Lookup A, Lookup B, Source 2, Lookup C, Source 3 </code>.
     *
     * @return source data stores
     */
    List<DataStoreWithAlias> getSourceDataStores();

    String getIKMCode();
}
