package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.strategies.FolderNameStrategy;

/**
 * This interface is used to provide contextual information for
 * {@link FolderNameStrategy
 * FolderNameStrategy} that is used to determine the folder name into which a
 * newly created ODI interface is inserted.
 */
public interface FolderNameExecutionContext extends DataStoreExecutionContext {

    /**
     * Gets the target data store description.
     *
     * @return A description of the data store with the name defined as a target
     * data store in the Mappings element of the input XSL. The object
     * describes the selected model the data store is located and and
     * further details related to the physical location of the model.
     */
    DataStore getTargetDataStore();

}
