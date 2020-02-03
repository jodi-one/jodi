package one.jodi.core.journalizing.impl;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.extensions.contexts.JournalizingExecutionContext;
import one.jodi.core.extensions.strategies.JournalizingStrategy;

import java.util.List;
import java.util.Map;

/**
 * Identity strategy that is used as a placeholder for a custom strategy.
 */
public class JournalizingIDStrategy implements JournalizingStrategy {

    @Override
    public List<String> getModelCodesEnabledForCDC(
            List<String> defaultModelCodes, JournalizingExecutionContext ex) {
        return defaultModelCodes;
    }

    @Override
    public Map<String, DataStore> getCDCCandidateDatastores(
            Map<String, DataStore> defaultCDC, JournalizingExecutionContext exc) {
        return defaultCDC;
    }

    @Override
    public Map<String, Object> getJKMOptions(
            Map<String, Object> defaultValueMap,
            JournalizingExecutionContext ex, String modelCode) {
        return defaultValueMap;
    }

    @Override
    public List<String> getSubscribers(List<String> defaultSubscribers,
                                       JournalizingExecutionContext ex, String modelCode) {
        return defaultSubscribers;
    }

    @Override
    public String getName(String defaultName,
                          JournalizingExecutionContext journalizingExecutionContext,
                          String modelCode) {
        return defaultName;
    }

}