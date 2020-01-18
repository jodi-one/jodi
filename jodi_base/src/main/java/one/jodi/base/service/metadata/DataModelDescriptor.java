package one.jodi.base.service.metadata;

import java.util.Map;

/**
 * Interface describing a data model
 *
 */
public interface DataModelDescriptor {

    /**
     * @return code of the model in which the data store is located
     */
    String getModelCode();

    /**
     * @return key-value pairs of additional custom data of type String or
     * Integer that is associated with the data model
     */
    Map<String, Object> getModelFlexfields();

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
     * @return name of the database service the datamodel resides in.
     */
    String getDataBaseServiceName();

    /**
     * @return port of the database service the datamodel resides in.
     */
    int getDataBaseServicePort();

    /**
     * @return name of the data server technology with which the data model is
     * associated; e.g. "Oracle", "XML" etc.
     */
    String getDataServerTechnology();

    /**
     * @return name of the schema within the generated context.
     */
    String getSchemaName();

}
