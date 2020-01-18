package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;

import java.util.List;

/**
 * This interface is used to provide contextual information for
 * {@link ModelCodeStrategy
 * ModelCodeStrategy} that is used to determine the model a given data store is
 * associated with.
 *
 */
public interface ModelNameExecutionContext extends DataStoreExecutionContext {

    /**
     * Gets name of the data store for which a model is to be determined. This
     * name is explicitly defined in the transformation XML specification.
     *
     * @return Name of the data store.
     */
    String getDataStoreName();

    /**
     * Gets name of the data store alias for which a model is to be determined.
     * This name is either explicitly defined in the transformation XML
     * specification or is assigned the value of {@link #getDataStoreName} if
     * the alias is not explicitly defined.
     *
     * @return Alias for the data store
     */
    String getDataStoreAlias();

    /**
     * Determines if the data store is a temporary table.
     *
     * @return <code>true</code> if data store is a temporary table;
     * otherwise returns <code>false</code>
     */
    boolean isTemporaryTable();

    /**
     * Gets the role of the data store. The role reflects if the data store is a
     * {@link DataStoreRole#SOURCE source}, {@link DataStoreRole#LOOKUP lookup}
     * or {@link DataStoreRole#TARGET target} data store.
     *
     * @return Data store role for the data store under consideration.
     */
    DataStoreRole getDataStoreRole();

    /**
     * Gets all data stores from the meta-data repository with the same
     * name as the data store defined in this execution context. See method
     * {@link #getDataStoreName()} to determine the name of the data store.
     *
     * @return A list of data stores in all visible models that have the same
     * name as the data store defined in this execution context.
     */
    List<DataStore> getMatchingDataStores();

    /**
     * Get a list of all model configurations that are defined in the properties
     * file starting with the <code>model.</code> prefix.
     *
     * @return Model properties in the configuration file in ascending order of
     * the DW architecture stack.
     */
    List<ModelProperties> getConfiguredModels();

    /**
     * Gets source extension that is associated with the XML specification
     * source element when data store role is {@link DataStoreRole#SOURCE} or
     * {@link DataStoreRole#LOOKUP}, which can be determined by calling
     * {@link #getDataStoreRole()}.
     *
     * @return extension element that may be customized as a consequence of XSD
     * customizations when determining the model code of a source and lookup
     * data store. This method returns null when determining the model
     * for target data stores.
     */
    SourceExtension getSourceExtension();

    /**
     * Gets mappings extension that is associated with the XML specification
     * mappings element when data store role is {@link DataStoreRole#TARGET},
     * which can be determined by calling {@link #getDataStoreRole()}.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations when determining the model code of a target data store.
     * This method returns null when determining the model for source
     * and lookup data stores.
     */
    MappingsExtension getMappingsExtension();

    public enum DataStoreRole {
        SOURCE, LOOKUP, TARGET
    }

}