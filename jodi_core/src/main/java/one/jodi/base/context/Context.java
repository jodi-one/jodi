package one.jodi.base.context;

import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.util.Resource;

import java.util.List;

/**
 * The responsibility of the <code>Context</code> is to provide a managed
 * environment for maintaining information that is either to cumbersome to pass
 * along as method parameters or that are global in nature.
 * <p>
 * The context object must be cleared after the execution of the last activities
 * from the Jodi command line or service to avoid side effects if the Jodi tool
 * instance is used for executing another operation.
 *
 */
public interface Context extends Resource {

    /**
     * removes state from the context object
     */
    void clear();

    void addDataStore(DataStore dataStore);

    DataStore getDataStore(String dataStoreName, String modelCode);

    DataModel getDataModel(String modelCode);

    List<DataStore> getAllTempTables();

}
