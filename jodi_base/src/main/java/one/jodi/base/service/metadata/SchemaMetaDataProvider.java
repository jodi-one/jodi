package one.jodi.base.service.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface with methods to access database schema metadata.
 */
public interface SchemaMetaDataProvider {

    /**
     * @param projectCode project code used in the Jodi project
     * @return true if project with provided code exists
     */
    boolean existsProject(final String projectCode);

    /**
     * Gets the model codes.
     *
     * @return list of names of all existing models for project
     */
    List<String> getModelCodes();

    /**
     * @return return a list of descriptors for all top-level models
     */
    List<DataModelDescriptor> getDataModelDescriptors();

    /**
     * Gets the data store descriptors in model.
     *
     * @param modelCode the model code
     * @return list of descriptors for all data stores in a model
     */
    Map<String, DataStoreDescriptor> getDataStoreDescriptorsInModel(String modelCode);


    /**
     * Initializes a database connection.
     *
     * @param jdbcUrl jdbc url of the db connection
     * @param userName username / schemaname of the db connection
     * @param password password of the user of the db connection
     */
    void initDBConnection(String jdbcUrl, String userName, String password);

    /**
     * Closes the database connection.
     */
    void closeDBConnection();

    boolean projectVariableExists(String projectCode, String variableName);

    boolean globalVariableExists(String variableName);

    /**
     * @return Map with keys of model codes,
     * values of logical schema names.
     */
    Map<String, String> translateModelToLogicalSchema();

    Set<String> getColumnNames();

    Set<String> getTableNames();

    Set<String> getLogicalSchemaNames();

}