package one.jodi.base.service.schema;

import one.jodi.base.model.types.DataStore;

import java.util.Map;

public interface DatabaseSchemaService {

    /**
     * Gets a Map of all data stores that exists in a specified model.
     *
     * @param modelCode model code for which all data stores are returned
     * @return Map of data stores in model. The key of the map is composed of the
     * name of the model and the name of the data store:<p>
     * <p>
     * model_name+"."+data_store_name
     */
    Map<String, DataStore> getAllDataStoresInModel(final String modelCode);

    void initializeDbConnection(String jdbcUrl, String userName, String password);

    void closeDbConnection();
}
