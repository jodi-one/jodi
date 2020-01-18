package one.jodi.etl.journalizng;

import one.jodi.base.model.types.DataStore;

import java.util.List;
import java.util.Map;

public interface JournalizingConfiguration {
    /**
     * @return the Journalizing Knowledge Module Options for a specific model.
     */
    Map<String, Object> getJkmOptions();

    /**
     * @return the modelCode of the specific configuration.
     */
    String getModelCode();

    /**
     * @return The subscribers of the model.
     */
    List<String> getSubscribers();

    /**
     * @return The datastores for Journalizing.
     */
    List<DataStore> getDataStores();

    /**
     * @return Journalizing Module configuration
     */
    String getName();
}
