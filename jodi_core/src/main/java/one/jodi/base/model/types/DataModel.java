package one.jodi.base.model.types;

import java.util.Map;


// TODO refactor with DataStore etc 

/**
 * Metadata that describes and characterizes an ODI data model
 */
public interface DataModel {

    /**
     * @return the model code
     */
    String getModelCode();

    /**
     * @return name of the data server with which the data model is associated;
     * e.g. the name of the Oracle DB server
     */
    String getDataServerName();

    /**
     * @return the physical name of the data server, e.g. 'localhost' or IP address
     */
    String getPhysicalDataServerName();

    /**
     * @return name of the data server technology with which the data model is
     * associated; e.g. "Oracle", "XML" etc.
     */
    String getDataServerTechnology();

    /**
     * @return name of the schema within the generated context.
     */
    String getSchemaName();

    /**
     * @return key-value pairs of additional custom data of type String or
     * Integer that is associated with the data model
     */
    Map<String, Object> getModelFlexfields();

    /**
     * @return layer in a ETL solution with focus on Oracle GBU data warehouse
     * reference architecture
     */
    ModelSolutionLayer getSolutionLayer();

    /**
     * @return true if the model should be ignored when resolving conflicts
     * that occur when two models contain a table with the same name.
     * @deprecated because this feature will likely be removed in the next version.
     * We explore a more generalized heuristics for version 1.1 of the product
     */
    boolean isModelIgnoredbyHeuristics();

    /**
     * @return The database service the datastore resides in.
     */
    String getDataBaseServiceName();

    /**
     * @return The port of the database service the datastore resides in.
     */
    int getDataBaseServicePort();
}
