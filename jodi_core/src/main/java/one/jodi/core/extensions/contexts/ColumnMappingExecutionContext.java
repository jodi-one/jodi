package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.TransformationExtension;

import java.util.List;
import java.util.Map;

/**
 * This interface is used to provide contextual information used to determine
 * source-to-target column mappings.
 *
 */
public interface ColumnMappingExecutionContext {

    /**
     * Gets the target data store description.
     *
     * @return returns a description of the data store with the name defined as
     * a target data store in the Mappings element of the input XSL.
     * The object describes the selected model  the data store is located and
     * and further details related to the physical location of the model.
     */
    DataStore getTargetDataStore();


    /**
     * Gets the source DataStore used for transformation when DataStores
     * when data store role is {@link DataStoreRole#SOURCE} which can be determined
     * by calling {@link #getDataStoreRole()}..
     * <p>
     * The list ordered by traversing each <code>Source</code> its <code>Lookup</code> children
     * in the input model. For example consider
     *
     * <pre>
     * Dataset
     *   Source 1
     *   	Lookup A
     *   	Lookup B
     *   Source 2
     *   	Lookup C
     *   	Lookup D
     *   </pre>
     * <p>
     * This would return the list
     * <code>Source 1, Lookup A, Lookup B, Source 2, Lookup C, Lookup D </code>.
     *
     * @return Datastores with aliases
     */
    List<DataStoreWithAlias> getDataStores();


    /**
     * Gets mappings extension that is associated with the XML specification
     * mappings element.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations when determining the model code of a target data store.
     */
    MappingsExtension getMappingsExtension();


    /**
     * Gets the transformation extension that is associated with the XML specification
     * transformation element.
     *
     * @return extension element that may be customized as a consequence of XSD
     * customizations when determining the model code of a target data store.
     */
    TransformationExtension getTransformationExtension();


    /**
     * Gets core Jodi properties, as defined in the jodi.properties file.
     *
     * @return Map of properties Map<property, propertyvalueholder>
     */
    Map<String, PropertyValueHolder> getCoreProperties();

    Dataset getDataset();

    /**
     * @return a list of all column names in scope that are mapped to list of column
     * names with alias postfix, e.g. columnName -> aliasName.columnName.
     * Only those alias names are provided that are in scope defined by the
     * dataSource and its Source and Lookup objects
     */
    Map<String, List<String>> getAllColumnToAlias();
}
