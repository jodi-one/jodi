package one.jodi.core.journalizing.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreForeignReference;
import one.jodi.core.extensions.contexts.JournalizingExecutionContext;
import one.jodi.core.extensions.strategies.JournalizingStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class implements the default logic to determine the Journalizing strategy and implements
 * the interface {@link JournalizingStrategy}. This logic is always executed before the
 * custom plug-in is executed.
 * <p>
 * The class is a concrete strategy participating in the Strategy Pattern.
 */
public class JournalizingDefaultStrategy implements JournalizingStrategy {
    private static final Logger logger = LogManager.getLogger(JournalizingDefaultStrategy.class);

    /**
     * This method determines on which datastores to apply the Change Data Capture flag to.
     *
     * @param defaultCDC       default change data capture datastores
     * @param executionContext execution context
     * @return Map of datastores where the key is <Model_Code>.<DataStoreName>
     */
    @Override
    public Map<String, DataStore> getCDCCandidateDatastores(Map<String, DataStore> defaultCDC, final JournalizingExecutionContext executionContext) {
        assert (defaultCDC == null);
        final Map<String, DataStore> defaultValue = new TreeMap<>();

        //get data stores that are eligible to be enabled for CDC
        List<String> enabledModels = getModelCodesEnabledForCDC(null, executionContext);
        executionContext.getDatastores().entrySet().stream()
                .filter(entity -> enabledModels
                        .contains(entity.getValue().getDataModel()
                                .getModelCode()))
                .forEach(entity -> defaultValue.put(entity.getKey(),
                        entity.getValue()));
        Comparator<String> comparator = (o1, o2) -> {
            DataStore ds01 = defaultValue.get(o1);
            DataStore ds02 = defaultValue.get(o2);
            for (DataStoreForeignReference dsForeign : ds01.getDataStoreForeignReference()) {
                if (dsForeign.getPrimaryKeyDataStore().getDataStoreName()
                        .equalsIgnoreCase(ds02.getDataStoreName())) {
                    logger.debug(ds01.getDataStoreName() + " has reference to: " +
                            dsForeign.getPrimaryKeyDataStore().getDataStoreName() +
                            " compared to " + ds02.getDataStoreName());
                    return ds01.getDataStoreName().hashCode() * -1;
                }
            }
            return ds01.getDataStoreName().hashCode();
        };
        Map<String, DataStore> defaultValueSorted = new TreeMap<>(comparator);
        defaultValueSorted.putAll(defaultValue);
        return defaultValueSorted;
    }

    /**
     * This method determines the JKMOptions to be applied to a Model.
     *
     * @param defaultJKMOptions default JKM options
     * @param executionContext  the execution context
     * @param modelCode         the model code
     * @return Map of JKMOptions
     */
    @Override
    public Map<String, Object> getJKMOptions(Map<String, Object> defaultJKMOptions, JournalizingExecutionContext executionContext, String modelCode) {
        assert (defaultJKMOptions == null);
        Map<String, Object> value = executionContext.getJKMOptions(modelCode);
        return value;
    }

    /**
     * @param defaultModelCodes default model codes
     * @param executionContext  the journalizing execution context
     * @return List of model codes
     */
    @Override
    public List<String> getModelCodesEnabledForCDC(List<String> defaultModelCodes, JournalizingExecutionContext executionContext) {
        assert (defaultModelCodes == null);
        return executionContext.getModelCodesEnabledForCDC();
    }

    /**
     * @param defaultSubscribers default list of subscribers
     * @param executionContext   execution context
     * @param modelCode          the model code in odi
     * @return List of subscribers
     */
    @Override
    public List<String> getSubscribers(List<String> defaultSubscribers,
                                       JournalizingExecutionContext executionContext, String modelCode) {
        assert (defaultSubscribers == null);
        return executionContext.getSubscribers(modelCode);
    }

    @Override
    public String getName(String defaultName,
                          JournalizingExecutionContext executionContext, String modelCode) {
        assert (defaultName == null);
        return executionContext.getName(modelCode);
    }

}