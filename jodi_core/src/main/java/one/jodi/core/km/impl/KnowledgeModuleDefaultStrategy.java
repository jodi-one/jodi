package one.jodi.core.km.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.extensions.contexts.CheckKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.KnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.LoadKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.StagingKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.strategies.NoKnowledgeModuleFoundException;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.etl.km.KnowledgeModuleType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * This is the as-is Jodi solution for computing KM values in Java using both
 * property files and XML input model. It is not meant to be extended but to
 * serve until a refactored solution is presented using groovy.
 *
 */
public class KnowledgeModuleDefaultStrategy implements KnowledgeModuleStrategy {

    private final static Logger log = LogManager.getLogger(KnowledgeModuleDefaultStrategy.class);
    private final static String ERROR_MESSAGE_03250 = "Null name property.";
    private final ErrorWarningMessageJodi errorWarningMessages;
    private JodiProperties properties;

    @Inject
    public KnowledgeModuleDefaultStrategy(JodiProperties properties,
                                          final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.properties = properties;
        this.errorWarningMessages = errorWarningMessages;
    }

    public KnowledgeModuleConfiguration getLKMConfig(
            KnowledgeModuleConfiguration explicitLkmConfig,
            LoadKnowledgeModuleExecutionContext executionContext) {

        return this.configure(explicitLkmConfig, executionContext, KnowledgeModuleType.Loading);
    }

    @Override
    public KnowledgeModuleConfiguration getCKMConfig(
            KnowledgeModuleConfiguration explicitCkmConfig,
            CheckKnowledgeModuleExecutionContext executionContext) {

        return this.configure(explicitCkmConfig, executionContext, KnowledgeModuleType.Check);
    }

    @Override
    public KnowledgeModuleConfiguration getIKMConfig(
            KnowledgeModuleConfiguration explicitIkmConfig,
            KnowledgeModuleExecutionContext executionContext) {

        return this.configure(explicitIkmConfig, executionContext, KnowledgeModuleType.Integration);
    }

    private KnowledgeModuleConfiguration configure(KnowledgeModuleConfiguration explicitKmConfig, KnowledgeModuleExecutionContext executionContext, KnowledgeModuleType type) {

        log.debug("Configuring " + type + " KM for transformation target data store = " + executionContext.getTargetDataStore().getDataStoreName());

        KnowledgeModuleProperties nameProperty = findNameProperty(executionContext, explicitKmConfig);

        if (nameProperty == null) {
            if (type == KnowledgeModuleType.Check) {
                return KnowledgeModuleConfiguration.Null;
            }
            String msg = errorWarningMessages.formatMessage(3250, ERROR_MESSAGE_03250, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            log.error(msg);
            throw new NoKnowledgeModuleFoundException("");
        }

        List<KnowledgeModuleProperties> optionProperties = findOptionProperties(
                executionContext,
                explicitKmConfig != null ? properties.getProperty(explicitKmConfig.getName() + ".name")
                        : nameProperty.getName().get(0));

        optionProperties.add(nameProperty);
        Collections.sort(optionProperties,
                KnowledgeModuleProperties::compareOrderTo);

        KnowledgeModuleConfigurationImpl kmConfig = new KnowledgeModuleConfigurationImpl();
        for (KnowledgeModuleProperties p : optionProperties) {
            for (String key : p.getOptions().keySet()) {
                kmConfig.putOption(key, p.getOptions().get(key));
            }
        }

        if (explicitKmConfig == null) {
            //kmConfig.setCode(nameProperty.getName().get(0));
            kmConfig.setName(nameProperty.getId());
            kmConfig.setType(type);
        } else { // explicitly defined
            kmConfig.setName(explicitKmConfig.getName());
            kmConfig.setType(explicitKmConfig.getType());

            for (String key : explicitKmConfig.getOptionKeys()) {
                kmConfig.putOption(key, explicitKmConfig.getOptionValue(key));
            }
        }
        log.debug("Choosing " + type + " KM of name: " + kmConfig.getName());
        //assert(kmConfig.getName() != null && kmConfig.getType() != null)
        //                  : "program error in KM Plug-in object";

        return kmConfig;
    }

    /**
     * @param p                rule to be checked for match
     * @param executionContext contextual information representing state
     * @param checkDefault     whether to check as default or not.
     * @return true if a match is found, false otherwise
     */
    private boolean matches(KnowledgeModuleProperties p, KnowledgeModuleExecutionContext executionContext, boolean checkDefault) {
        assert (executionContext != null);

        String targetTechnology = executionContext.getTargetDataStore().getDataModel().getDataServerTechnology();

        if (executionContext instanceof LoadKnowledgeModuleExecutionContext) {
            LoadKnowledgeModuleExecutionContext loadExecutionContext = (LoadKnowledgeModuleExecutionContext) executionContext;
            if (!p.getSrc_technology().equalsIgnoreCase(
                    loadExecutionContext.getSourceDataStore().getDataModel().getDataServerTechnology())) {
                return false;
            }

            if (loadExecutionContext.getStagingDataModel() != null) {
                targetTechnology = loadExecutionContext.getStagingDataModel().getDataServerTechnology();
            }
        }
        if (targetTechnology != null &&
                !p.getTrg_technology().equalsIgnoreCase(targetTechnology)) {
            return false;
        }

        // At this point the LKM source and technologies are similar, so if this is a default, return true;
        if (checkDefault && p.isDefault()) {
            return true;
        }
        if (!matchesRegex(p.getTrg_regex(), executionContext.getTargetDataStore().getDataStoreName())) {
            return false;
        }
        if (!matchesDataStoreType(p.getTrg_tabletype(), executionContext.getTargetDataStore().getDataStoreType())) {
            return false;
        }
        if (!matchesList(executionContext.getTargetDataStore().getDataModel()
                .getSolutionLayer().getSolutionLayerName(), p.getTrg_layer())) {
            return false;
        }

        if (executionContext instanceof LoadKnowledgeModuleExecutionContext) {
            if (!matchesRegex(p.getSrc_regex(), ((LoadKnowledgeModuleExecutionContext) executionContext).getSourceDataStore().getDataStoreName())) {
                return false;
            }
            if (!matchesDataStoreType(p.getSrc_tabletype(),
                    ((LoadKnowledgeModuleExecutionContext) executionContext).getSourceDataStore().getDataStoreType())) {
                return false;
            }
            if (!matchesList(((LoadKnowledgeModuleExecutionContext) executionContext)
                    .getSourceDataStore().getDataModel().getSolutionLayer()
                    .getSolutionLayerName(), p.getSrc_layer())) {
                return false;
            }
        } else {
            // Make sure that if trg_temporary is specified we make sure the target data store is applied
            if (p.getTrg_temporary() != null &&
                    ((executionContext.getTargetDataStore().isTemporary() ? 1 : -1) * p.getTrg_temporary()) < 0) {
                return false;
            }
        }


        return true;
    }

    private boolean isMultiTechnology(List<String> names, KnowledgeModuleExecutionContext executionContext) {
        for (String name : names) {
            for (KnowledgeModule km : executionContext.getKMs()) {
                if (km.getName().equalsIgnoreCase(name) && km.isMultiTechnology()) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * @param value value to search for
     * @param list  collection to search
     * @return true if found in list or list null or empty
     */
    private boolean matchesList(String value, List<String> list) {
        if (list != null && list.size() > 0) {
            return list.contains(value);
        } else {
            return true;
        }
    }

    private boolean matchesDataStoreType(List<String> typeStrings, DataStoreType type) {
        if (typeStrings != null && typeStrings.size() > 0) {
            for (String typeString : typeStrings) {
                DataStoreType matchType = DataStoreType.valueOf(typeString);
                if (matchType.equals(type)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }


    // Determines if the regex matches input, case insensitive.
    private boolean matchesRegex(String regex, String input) {
        if (regex == null) return true;

        Pattern srcPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = srcPattern.matcher(input);
        return m.matches();
    }


    /**
     * Finds the first matching rule whose specifications match the properties of the executionOCntext.
     * <p>
     * THis is intended to be used to fetch for KM name only.
     *
     * @param executionContext object representative of state
     * @return properties to be set
     */
    private KnowledgeModuleProperties findNameProperty(KnowledgeModuleExecutionContext executionContext, KnowledgeModuleConfiguration explicit) {
        if (explicit != null) {
            for (KnowledgeModuleProperties p : executionContext.getConfigurations()) {
                if (p.getId().equalsIgnoreCase(explicit.getName())) {
                    return p;
                }
            }

            return null;
        } else {
            KnowledgeModuleProperties found = findNameProperty(executionContext, false);
            return (found != null) ? found : findNameProperty(executionContext, true);
        }

    }

    /**
     * Finds the first matching rule whose specifications match the properties of the executionOCntext.
     * <p>
     * This is intended to be used to fetch for KM name only.
     *
     * @param executionContext object representative of state
     * @param checkDefault     if this is true the match is based only on technologies
     * @return properties to be set
     */
    private KnowledgeModuleProperties findNameProperty(KnowledgeModuleExecutionContext executionContext, boolean checkDefault) {
        for (KnowledgeModuleProperties p : executionContext.getConfigurations()) {
            if (matches(p, executionContext, checkDefault)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Add global-defined rules to the list, including the selected rule.
     *
     * @param executionContext
     * @param selected         The selected properties rule used for KM name
     * @return list of properties in sorted order.
     */
    private List<KnowledgeModuleProperties> findOptionProperties(KnowledgeModuleExecutionContext executionContext, String name) {
        ArrayList<KnowledgeModuleProperties> list =
                executionContext.getConfigurations().stream()
                        .filter(p -> p.isGlobal() && !p.isDefault() &&
                                p.getName().contains(name) && matches(p,
                                executionContext,
                                false))
                        .collect(Collectors.toCollection(ArrayList::new));

        return list;
    }


    @Override
    public String getStagingModel(String defaultStagingModel,
                                  StagingKnowledgeModuleExecutionContext executionContext) {
        String stagingModel = defaultStagingModel;

        String ikm = properties.getProperty(executionContext.getIKMCode() + ".name");
        if (isMultiTechnology(Collections.singletonList(ikm), executionContext)) {
            if (stagingModel == null) {
                // The first Source in the first Dataset of the Transformation is considered the driver determining model
                stagingModel = executionContext.getSourceDataStores().get(0).getDataStore().getDataModel().getModelCode();
            } else {
                stagingModel = properties.getProperty(stagingModel);
            }

            return stagingModel;
        } else {
            return null;
        }
    }

}

