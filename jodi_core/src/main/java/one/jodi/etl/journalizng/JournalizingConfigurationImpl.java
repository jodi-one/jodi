package one.jodi.etl.journalizng;

import one.jodi.base.model.types.DataStore;

import java.util.List;
import java.util.Map;

public class JournalizingConfigurationImpl implements JournalizingConfiguration {

    private Map<String, Object> jkmOptions;
    private String modelCode;
    private List<String> subscribers;
    private List<DataStore> datastores;
    private String name;

    public JournalizingConfigurationImpl(final Map<String, Object> jkmOptions, final String modelCode, final List<String> subscribers, final List<DataStore> dataStores, final String name) {
        this.jkmOptions = jkmOptions;
        this.modelCode = modelCode;
        this.subscribers = subscribers;
        this.datastores = dataStores;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<DataStore> getDatastores() {
        return datastores;
    }

    public Map<String, Object> getJkmOptions() {
        return jkmOptions;
    }

    public String getModelCode() {
        return modelCode;
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    @Override
    public List<DataStore> getDataStores() {
        return datastores;
    }
}
