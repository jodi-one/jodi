package one.jodi.core.journalizing.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.extensions.contexts.JournalizingExecutionContext;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.strategies.JournalizingStrategy;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.journalizng.JournalizingConfiguration;
import one.jodi.etl.journalizng.JournalizingConfigurationImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * The class manages a default strategy and a custom strategy for
 * determining the journalizing strategy.
 * <p>
 * The default strategy is always executed first and the result is passed to
 * the custom strategy, where it may be overwritten.
 * <p>
 * This class is also responsible for building the execution context object that
 * is passed into strategies.
 * <p>
 * The class is the Context that participates in the Strategy Pattern.
 *
 */
public class JournalizingContextImpl implements JournalizingContext {

    private final static Logger logger = LogManager.getLogger(JournalizingContextImpl.class);
    private final static String ERROR_MESSAGE_02031 = "An unknown exception was raised in journalizing strategy '%1$s'.";
    private final static String ERROR_MESSAGE_02040 = "Error initializing JournalizingExecutonContext %1$s.";
    private final static String ERROR_MESSAGE_02050 = "Unsuccessful attempt to parse name value pair";
    // default strategy is created without DI because it is
    // considered to be hard-coded and is not designed to be
    // modified or extended at this time
    private final JournalizingStrategy defaultStrategy;
    // custom strategy by default will be a ID strategy that returns
    // the default value; it is configured through Guice injection
    private final JournalizingStrategy customStrategy;
    private final DatabaseMetadataService databaseMetadataService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /*
     * Note: Constructor is used mainly for injection of dependencies.
     * Additional references can be added
     *
     * @param customStrategy - defines non-null custom strategy
     *
     * @param databaseMetadataService - reference to the databaseMetadataService that are injected
     */
    @Inject
    public JournalizingContextImpl(
            final @DefaultStrategy JournalizingStrategy defaultStrategy,
            final JournalizingStrategy customStrategy,
            final DatabaseMetadataService databaseMetadataService,
            final ETLValidator validator,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.defaultStrategy = defaultStrategy;
        this.customStrategy = customStrategy;
        this.databaseMetadataService = databaseMetadataService;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public boolean isJournalizedSource(Source source) {
        JournalizingExecutionContext exc = getJournalizingExecutionContext();
        Map<String, DataStore> defaultCDC = defaultStrategy.getCDCCandidateDatastores(null, exc);
        Map<String, DataStore> finalCDC = defaultCDC;
        if (customStrategy != null) {
            finalCDC = customStrategy.getCDCCandidateDatastores(defaultCDC, exc);
        }

        List<String> storeNames =
                finalCDC.entrySet().stream().map(Entry<String, DataStore>::getKey)
                        .collect(Collectors.toList());

        String model = source.getModel();
        String datasetName = model + "." + source.getName();
        if (storeNames.contains(datasetName))
            return source.isJournalized();
        return false;
    }


    @Override
    public boolean isJournalizedLookup(Lookup lookup) {
        JournalizingExecutionContext exc = getJournalizingExecutionContext();
        Map<String, DataStore> defaultCDC = defaultStrategy.getCDCCandidateDatastores(null, exc);
        Map<String, DataStore> finalCDC = defaultCDC;
        if (customStrategy != null) {
            finalCDC = customStrategy.getCDCCandidateDatastores(defaultCDC, exc);
        }

        List<String> storeNames =
                finalCDC.entrySet().stream().map(Entry<String, DataStore>::getKey)
                        .collect(Collectors.toList());

        String model = lookup.getModel();
        String datasetName = model + "." + lookup.getLookupDataStore();
        if (storeNames.contains(datasetName)) {
            return lookup.isJournalized();
        }
        return false;
    }


    private JournalizingExecutionContext getJournalizingExecutionContext() {
        try {
            JournalizingExecutionContext exc = new JournalizingExecutionContext() {

                public Map<String, DataStore> getDatastores() {
                    Map<String, DataStore> cdc = new TreeMap<>();
                    for (ModelProperties modelCode : databaseMetadataService.getConfiguredModels()) {
                        cdc.putAll(databaseMetadataService
                                .getAllDataStoresInModel(modelCode.getCode()));
                    }
                    return Collections.unmodifiableMap(cdc);
                }

                @Override
                public Map<String, Object> getJKMOptions(String modelCode) {
                    for (ModelProperties modelProperties : databaseMetadataService
                            .getConfiguredModels()) {
                        if (modelProperties.getCode().equals(modelCode)
                                && modelProperties.isJournalized())
                            return Collections.unmodifiableMap(parseMap(modelProperties.getJkmoptions()));
                    }
                    return Collections.unmodifiableMap(new HashMap<>());
                }

                @Override
                public List<String> getModelCodesEnabledForCDC() {
                    List<String> cdcModels = databaseMetadataService
                            .getConfiguredModels().stream()
                            .filter(ModelProperties::isJournalized).map(ModelProperties::getCode)
                            .collect(Collectors.toList());
                    return Collections.unmodifiableList(cdcModels);
                }

                @Override
                public List<String> getSubscribers(String modelCode) {
                    List<String> subscribers = new ArrayList<>();
                    for (ModelProperties modelProperties : databaseMetadataService
                            .getConfiguredModels()) {
                        if (modelProperties.isJournalized() && modelCode.equals(modelProperties.getCode())) {
                            subscribers = modelProperties.getSubscribers();
                        }
                    }
                    if (subscribers == null || subscribers.size() == 0) {
                        subscribers = new ArrayList<>();
                        subscribers.add("SUNOPSIS");
                    }
                    return Collections.unmodifiableList(subscribers);
                }

                @Override
                public String getName(String modelCode) {
                    String name = null;
                    for (ModelProperties modelProperties : databaseMetadataService
                            .getConfiguredModels()) {
                        if (modelProperties.isJournalized() && modelCode.equals(modelProperties.getCode())) {
                            name = modelProperties.getJkm();
                            break;
                        }
                    }
                    return name;
                }

            };
            return exc;
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            String msg = errorWarningMessages.formatMessage(2040,
                    ERROR_MESSAGE_02040, this.getClass(), message);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new UnRecoverableException(msg, e);
        }
    }

    private Map<String, Object> parseMap(List<String> options) {
        HashMap<String, Object> map = new HashMap<>();
        for (String option : options) {
            String[] list = option.split(",");
            for (String s : list) {
                String[] keyValuePair = s.split(":", 2);
                if (keyValuePair.length != 2) {
                    String msg = errorWarningMessages.formatMessage(2050,
                            ERROR_MESSAGE_02050, this.getClass());
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(), msg,
                            MESSAGE_TYPE.ERRORS);
                    throw new UnRecoverableException(msg);
                } else {
                    try {
                        map.put(keyValuePair[0].trim(),
                                Integer.valueOf(keyValuePair[1].trim()));
                    } catch (NumberFormatException nfe) {
                        if ("true".equalsIgnoreCase(keyValuePair[1].trim())
                                || "false".equalsIgnoreCase(keyValuePair[1].trim())) {
                            map.put(keyValuePair[0].trim(),
                                    Boolean.valueOf(keyValuePair[1].trim()));
                        } else {
                            map.put(keyValuePair[0].trim(), keyValuePair[1].trim());
                        }
                    }
                }
            }
        }
        return map;
    }

    @Override
    public List<JournalizingConfiguration> getJournalizingConfiguration() {
        JournalizingExecutionContext exc = getJournalizingExecutionContext();
        List<String> defaultModelsForCDC = defaultStrategy
                .getModelCodesEnabledForCDC(null, exc);
        List<String> finalModelsForCDC = defaultModelsForCDC;
        if (customStrategy != null) {
            finalModelsForCDC = customStrategy.getModelCodesEnabledForCDC(
                    defaultModelsForCDC, exc);
        }
        List<JournalizingConfiguration> journalzingOptionsConfigurations =
                new ArrayList<>();
        for (String modelCode : finalModelsForCDC) {
            Map<String, Object> defaultJKMOptions = defaultStrategy
                    .getJKMOptions(null, exc, modelCode);
            Map<String, Object> finalJKMOptions = defaultJKMOptions;
            if (customStrategy != null) {
                finalJKMOptions = customStrategy.getJKMOptions(
                        defaultJKMOptions, exc, modelCode);
            }

            List<String> defaultSubscribers = defaultStrategy.getSubscribers(null, exc, modelCode);
            List<String> finalSubscribers = defaultSubscribers;
            try {
                if (customStrategy != null) {
                    finalSubscribers = customStrategy.getSubscribers(defaultSubscribers, exc, modelCode);
                }
            } catch (RuntimeException ex) {
                String msg = errorWarningMessages.formatMessage(2031,
                        ERROR_MESSAGE_02031, this.getClass(), customStrategy.toString());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new IncorrectCustomStrategyException(msg, ex);
            }


            Map<String, DataStore> defaultCDC = defaultStrategy
                    .getCDCCandidateDatastores(null, exc);
            Map<String, DataStore> finalCDC = defaultCDC;
            if (customStrategy != null) {
                finalCDC = customStrategy.getCDCCandidateDatastores(defaultCDC, exc);
            }

            List<DataStore> returningList = finalCDC.entrySet().stream()
                    .map(Entry<String, DataStore>::getValue)
                    .collect(Collectors.toList());
            String defaultName = defaultStrategy.getName(null, exc, modelCode);
            String finalName = defaultName;
            if (customStrategy != null) {
                finalName = customStrategy.getName(defaultName, exc, modelCode);
            }
            journalzingOptionsConfigurations.add(new JournalizingConfigurationImpl(finalJKMOptions, modelCode, finalSubscribers, returningList, finalName));
        }
        return journalzingOptionsConfigurations;
    }
	
	
	/*
	private JournalizingValidationResult validateCDCDescriptors() {
		List<String> messages = new ArrayList<String>();
		JournalizingExecutionContext exc = getJournalizingExecutionContext();
		Map<String, DataStore> defaultCDC = defaultStrategy
				.getCDCCandidateDatastores(null, exc);
		Map<String, DataStore> finalCDC = defaultCDC;
		try{
			if (customStrategy != null) {
				finalCDC = customStrategy.getCDCCandidateDatastores(defaultCDC, exc);
			}
		} catch (RuntimeException ex) {
			String msg = errorWarningMessages.formatMessage(2030,
					ERROR_MESSAGE_02030, this.getClass(), customStrategy.toString());
			errorWarningMessages.addMessage(
					errorWarningMessages.assignSequenceNumber(), msg,
					MESSAGE_TYPE.ERRORS);
			messages.add(msg);
		}
		for (Entry<String, DataStore> entry : finalCDC.entrySet()) {
			JournalizingValidationResult results =
					tableServiceProvider.validateDatastore(
							   entry.getValue().getDataStoreName(),
							   entry.getValue().getDataModel().getModelCode());
			if (results.getValidationMessages().size() != 0) {
				messages.addAll(results.getValidationMessages());
			}
		}
		if(messages.size() == 0)
			return new JournalizingValidationResult(true, messages);
		else 
			return new JournalizingValidationResult(false, messages);
	}

*/
/*
	@Override
	public JournalizingValidationResult validate() {
		return this.validateJKMOptions();
		JournalizingValidationResult result1 =  validateCDCDescriptors();
		JournalizingValidationResult result2 =  validateJKMOptions();
		boolean result = result1.isValid() && result2.isValid() ? true : false;
		List<String> messages = new ArrayList<String>();
		messages.addAll(result1.getValidationMessages());
		messages.addAll(result2.getValidationMessages());
		return new JournalizingValidationResult(result, messages);
	}	
	*/
}