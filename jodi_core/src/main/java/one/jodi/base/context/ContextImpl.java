package one.jodi.base.context;

import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


/**
 * The implementation of Context delegates all requests to a class that
 * maintains the global state (see {@link GlobalContextState} ). This class must
 * not maintain any state than references to context state class instances.
 * <p>
 * The main purpose of this is to simplify the implementation of of the scoping
 * and cleanup mechanism and avoid making unintended mistakes that can result in
 * corruption of the context over multiple uses of the Jodi tool.
 * <p>
 * This design will simplify extending of the class to manage other non-global
 * contexts.
 *
 * <b>Must be a Singleton!</b>
 */
public class ContextImpl implements Context {
    private final static Logger logger = LogManager.getLogger(ContextImpl.class);

    private GlobalContextState state = new GlobalContextState();

    /*
     * simply creates a new GlobalContextState and thereby makes previous state
     * unreachable and eligible for garbage collection.
     */
    @Override
    public void clear() {
        this.state = new GlobalContextState();
    }

    @Override
    public void addDataStore(final DataStore dataStore) {
        assert (dataStore.getColumns() != null && !dataStore.getColumns().isEmpty())
                : "Incorrectly formed data store " + dataStore.getDataStoreName();
        if ((dataStore.getDataStoreName() != null) &&
                (dataStore.getDataModel().getModelCode() != null)) {
            state.addPersistedDataStore(dataStore);
        }
        if (dataStore.isTemporary()) {
            logger.debug("add temp table to context: " + dataStore.toString());
        }
    }

    @Override
    public DataStore getDataStore(final String dataStoreName, String modelCode) {
        return state.getPersistedDataStore(dataStoreName, modelCode);
    }

    @Override
    public DataModel getDataModel(final String modelCode) {
        return state.getDataModel(modelCode);
    }

    @Override
    public List<DataStore> getAllTempTables() {
        return state.getAllTempTables();
    }

    @Override
    public void logStatistics() {
    }

    @Override
    public void flush() {
    }

    private class GlobalContextState {

        //contains only references to persisted data stores
        private Map<String, Map<String, DataStore>> persistedDataStore =
                new TreeMap<>();

        private void addPersistedDataStore(DataStore dataStore) {
            Map<String, DataStore> dataStoreMap;
            if (persistedDataStore.containsKey(dataStore.getDataModel().getModelCode())) {
                dataStoreMap = persistedDataStore.get(dataStore.getDataModel().getModelCode());
            } else {
                dataStoreMap = new HashMap<>();
                persistedDataStore.put(dataStore.getDataModel().getModelCode(), dataStoreMap);
            }
            dataStoreMap.put(dataStore.getDataStoreName(), dataStore);
        }

        private DataStore getPersistedDataStore(String dataStoreName, String modelCode) {
            DataStore result = null;
            if (persistedDataStore.containsKey(modelCode)) {
                result = persistedDataStore.get(modelCode).get(dataStoreName);
            }
            return result;
        }

        private DataModel getDataModel(final String modelCode) {
            DataModel model = null;
            if (persistedDataStore.containsKey(modelCode)) {
                // pick first data store and return its model
                DataStore ds = persistedDataStore.get(modelCode).entrySet().iterator().next().getValue();
                model = ds.getDataModel();
            }
            return model;
        }

        private List<DataStore> getAllTempTables() {
            List<DataStore> temps = new ArrayList<>();
            for (Map<String, DataStore> modelMap : persistedDataStore.values()) {
                temps.addAll(modelMap.values().stream().filter(DataStore::isTemporary)
                        .collect(Collectors.toList()));
            }
            return temps;
        }

    }

}