package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;

import java.util.List;
import java.util.Map;

public interface JournalizingExecutionContext {


    /**
     * @return A Map of DataStores.
     */
    Map<String, DataStore> getDatastores();

    /**
     * @param modelCode of the model on which the JKMOptions are applicable.
     * @return Map of JKMOptions
     */
    Map<String, Object> getJKMOptions(String modelCode);

    /**
     * @return List of modelcodes that are capable of Changed Data Capture.
     */
    List<String> getModelCodesEnabledForCDC();

    /**
     * @param modelCode of the underlying subsystem
     * @return a list of subscribers
     */
    List<String> getSubscribers(String modelCode);

    /**
     * @param modelCode
     * @return Journalizing Knowledge Module name
     */
    String getName(String modelCode);
}
