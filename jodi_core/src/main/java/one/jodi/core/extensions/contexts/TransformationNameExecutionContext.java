package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.strategies.TransformationNameStrategy;

/**
 * This interface is used to provide contextual information for
 * {@link TransformationNameStrategy
 * TransfromationNameStrategy} that is used to determine the name of the ODI
 * interface to be generated.
 */
public interface TransformationNameExecutionContext extends DataStoreExecutionContext {

    /**
     * Gets the string that is passed to Jodi via the command line and is
     * intended to be a prefix to the ODI interface name.
     *
     * @return Prefix that is passed to Jodi via the command line.
     */
    String getPrefix();

    /**
     * Determines if <code>distinct</code> is applied to the results data set
     * before inserting it to the target data store.
     *
     * @return <code>true</code> if the distinct option is defined in the
     * transformation specification; otherwise returns <code>false</code>.
     */
    boolean isTargetDistinct();

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
