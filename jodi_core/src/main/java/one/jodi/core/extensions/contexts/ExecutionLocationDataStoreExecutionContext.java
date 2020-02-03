package one.jodi.core.extensions.contexts;

import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.model.extensions.MappingsExtension;

/**
 * This interface is used to provide contextual information for
 * {@link ExecutionLocationStrategy
 * ExecutionLocationStrategy} that is used to determine the execution locations
 * for target column mappings.
 */
public interface ExecutionLocationDataStoreExecutionContext extends ExecutionLocationExecutionContext {

    /**
     * Gets the data set index of the target data set.
     *
     * @return the data set index
     */
    int getDataSetIndex();

    /**
     * Gets the dataset name the source data stores are associated with.
     *
     * @return dataset name
     */
    String getDataSetName();

    /**
     * Gets mappings extension that is associated with the XML specification
     * mappings element.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations.
     */
    MappingsExtension getMappingsExtension();
}
