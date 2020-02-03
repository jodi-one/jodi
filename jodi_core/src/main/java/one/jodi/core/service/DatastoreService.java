package one.jodi.core.service;

/**
 * Service that daastore management functionality.
 */
public interface DatastoreService {

    /**
     * Delete the datastore that matches the provided name.
     *
     * @param datastoreName
     */
    void deleteDatastore(String datastoreName, String modelCode);

    /**
     * Delete all temporary datastores.
     */
//    void deleteTempDatastores();

}
