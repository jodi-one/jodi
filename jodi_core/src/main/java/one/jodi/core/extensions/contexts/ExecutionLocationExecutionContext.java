package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;

/**
 * This interface is used to provide shared contextual information for the
 * {@link ExecutionLocationStrategy
 * ExecutionLocationStrategy} that is used when determining the execution locations
 * for all filters, joins, lookups and target column mappings.
 */
public interface ExecutionLocationExecutionContext extends DataStoreExecutionContext {

    /**
     * Gets a description of the target data store.
     *
     * @return a description of the data store with the name defined as a target
     * data store in the Mappings element of the input XSL. The object
     * describes the selected model the data store is located and and
     * further details related to the physical location of the model.
     */
    DataStore getTargetDataStore();

    /**
     * Indicates whether all data stores part of the same transformation
     * definition are located on the same server. This information can be
     * helpful for making coarse-grained execution location decisions.
     *
     * @return <code>true</code> if all source, lookup and target data stores
     * specified in the transformation specification are located on the
     * same DB server; otherwise <code>false</code>
     */
    boolean isSameModelInTransformation();
}
