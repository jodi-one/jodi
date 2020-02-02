package one.jodi.core.extensions.strategies;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.contexts.JournalizingExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * This class implements the default logic to determine the journalizing strategy.
 * This logic is always executed before the custom plug-in is executed.
 * <p>
 * The interface defines the strategy and participates in the Strategy Pattern.
 *
 */
public interface JournalizingStrategy {
    /**
     * Determines a list of changed data capture enabled models.
     *
     * @param defaultModelCodes default model codes
     * @param journalizingExecutionContext execution context
     * @return list of model codes
     */
    List<String> getModelCodesEnabledForCDC(List<String> defaultModelCodes,
                                            JournalizingExecutionContext journalizingExecutionContext);

    /**
     * This method determines a map of datastores on which to apply the CDCDescriptor on.
     *
     * @param defaultCDCDataStore          enabled datastores
     * @param journalizingExecutionContext execution context
     * @return Map of Change Data Capture enabled datastores, the key contains the model_code.data_store_name
     */
    Map<String, DataStore> getCDCCandidateDatastores(Map<String, DataStore> defaultCDCDataStore, JournalizingExecutionContext journalizingExecutionContext);

    /**
     * This method determines a Map of JKMOptions on which to apply to a CDC enabled model.
     *
     * @param defaultJKMOptions default JKM options
     * @param journalizingExecutionContext execution context of journalizing
     * @param modelCode model code of the journalizing
     * @return Map of JKMOptions
     */
    Map<String, Object> getJKMOptions(Map<String, Object> defaultJKMOptions,
                                      JournalizingExecutionContext journalizingExecutionContext, String modelCode);

    /**
     * @param defaultSubscribers default set of subscribers
     * @param journalizingExecutionContext execution context of the journalizing
     * @param modelCode the model code of journalizing
     * @return List of subscribers
     */
    List<String> getSubscribers(List<String> defaultSubscribers,
                                JournalizingExecutionContext journalizingExecutionContext, String modelCode);

    /**
     * @param defaultName                  default name of the Journalizing Knowledge Module
     * @param journalizingExecutionContext execution context
     * @param modelCode the model code
     * @return Journalizing Knowledge Module name
     */
    String getName(String defaultName, JournalizingExecutionContext journalizingExecutionContext, String modelCode);
}
