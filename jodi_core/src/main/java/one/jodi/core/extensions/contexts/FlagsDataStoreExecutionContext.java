package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.model.extensions.MappingsExtension;

/**
 * This interface is used to provide contextual information for
 * {@link FlagsStrategy
 * FlagsStrategy} that is used to determine flags associated with
 * the target columns mappings.
 * <p>
 * Flags Data Store execution context information.
 */
public interface FlagsDataStoreExecutionContext extends DataStoreExecutionContext {

    /**
     * Gets the target data store description.
     *
     * @return A description of the data store with the name defined as a target
     * data store in the Mappings element of the input XSL. The object
     * describes the selected model the data store is located and and
     * further details related to the physical location of the model.
     */
    DataStore getTargetDataStore();

    /**
     * Get the code specifying the Integration Knowledge Module associated with
     * the transformation specification. This code is used both in the input XML
     * and and found in the Jodi properties file in the form
     * <code>km.{group ID}</code>.
     * <p>
     * The IKM name can be determined by retrieving the Jodi property value of
     * the name key for the specified IKM code from {@link #getProperties()}.
     * The following code is to be implemented in the custom version of the
     * {@link FlagsStrategy
     * FlagsStrategy}:<p>
     *
     * <code>
     * FlagsExecutionContext execContext = ... <br>
     * String ikmName = execContext.getProperties().get(execContext.getIKMCode()+".name");
     * </code>
     *
     * @return Code for the IKM that is selected in this transformation.
     */
    String getIKMCode();

    /**
     * Gets mappings extension that is associated with the XML specification
     * mappings element.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations when determining the model of a target data store.
     */
    MappingsExtension getMappingsExtension();

}
