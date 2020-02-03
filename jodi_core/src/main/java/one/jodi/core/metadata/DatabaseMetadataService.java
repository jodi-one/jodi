package one.jodi.core.metadata;

import one.jodi.base.context.Context;
import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.service.schema.DatabaseSchemaService;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.etl.internalmodel.Mappings;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service Interface to retrieve model and data store meta data. It combines
 * information from the ETL subsystem with information available through the
 * Jodi properties.
 */
public interface DatabaseMetadataService extends DatabaseSchemaService {


//  /**
//   * Determines if a name represents a temporary data store.
//   * Temporary helper method only until layer refactoring has been completed.
//   */
//  @Deprecated
//	boolean isTemporaryTable(String dataStoreName);

    /**
     * returns map with Jodi configuration properties with exception of those
     * that describe information related to ODI repositories or ODI schemas.
     *
     * @return map of properties
     */
//	Map<String, String> getCoreProperties();
    Map<String, PropertyValueHolder> getCoreProperties();

    /**
     * @return list of all models that exist in the ETL tool repository
     */
    List<ModelProperties> getConfiguredModels();

    /**
     * Finds the target data store for a given name and target model in the meta
     * data repository. The last parameter is used when a temporary table is
     * requested to retrieve associated column information .
     * <p>
     * The resulting data store is recorded in the {@link Context} to
     * consistently provide the
     *
     * @param mappings mapping object required if <code>dataStoreName</code> is a
     *                 temporary table
     * @return data store describing the repository meta data or a temporary
     * table
     */
    DataStore getTargetDataStoreInModel(final Mappings mappings);

    /**
     * Finds a source or lookup data store for a given name and source model in
     * the meta data repository. In case a data store temporary table is
     * requested, the temporary table must have been defined in a previous request to
     * {@link #getTargetDataStoreInModel(String, String, Mappings)}.
     *
     * @param dataStoreName name of the data store for which meta data is requested
     * @param modelCode     code for the model in which the data store is located
     * @return data store describing the repository meta data or a temporary
     * table
     */
    DataStore getSourceDataStoreInModel(final String dataStoreName,
                                        final String modelCode);

    /**
     * Finds all data sources in existing data models that have a given name.
     *
     * @param dataStoreName name of the data store to be found
     * @return determines data stores with expected name in existing data models
     */
    List<DataStore> findDataStoreInAllModels(final String dataStoreName);

    DataModel getDataModel(String modelCode);

    /**
     * Checks if is of type scd2.
     *
     * @param dataStoreName the data store name
     * @return true, if is of type sc d2
     */
    boolean isOfTypeSCD2(String dataStoreName);

    /**
     * @param dataStoreName name of data store to be considered; Temporary table postfix
     *                      must be stripped before calling this method
     * @return name of data store to be considered
     */
    DataStoreType getDataStoreType(final String dataStoreName);

    /**
     * Checks if is connector model.
     *
     * @param modelCode the model code
     * @return true, if is connector model
     */
    boolean isConnectorModel(String modelCode);

    /**
     * Checks if is source model.
     *
     * @param modelCode the model code
     * @return true, if is source model
     */
    @Deprecated
    boolean isSourceModel(String modelCode);

    /**
     * Checks if the transformation is temporary.
     *
     * @param tableName the table name
     * @return true, if is temporary transformation
     */
    boolean isTemporaryTransformation(String tableName);


    public abstract boolean projectVariableExists(String projectCode, String variableName);

    public abstract boolean globalVariableExists(String variableName);

    Map<String, String> retrieveModelsFromCache();

    Set<String> getColumnNames();

    Set<String> getTableNames();

    Set<String> getSchemaNames();

}