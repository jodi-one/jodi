package one.jodi.core.km.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.config.km.KnowledgeModulePropertiesProvider;
import one.jodi.core.extensions.contexts.CheckKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.KnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.LoadKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.StagingKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.metadata.ETLSubsystemService;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.KmType;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.KmTypeImpl;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.SourceImpl;
import one.jodi.etl.km.KnowledgeModuleType;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/*
 * Context aka socket class which uses two strategies (KnowledgeModuleStrategy) to compute ODI Knowledge Module default values.  This insulates ODI from the strategies themselves.
 *
 */
public class KnowledgeModuleContextImpl implements KnowledgeModuleContext {

    private static final Logger logger = LogManager.getLogger(KnowledgeModuleContextImpl.class);
    private static final String newLine = System.getProperty("line.separator");

    private static final String ERROR_MESSAGE_03200 =
            "An unknown exception was raised in KM strategy '%2$s' while " + "determining model for data store '%1$s'.";
    private static final String ERROR_MESSAGE_03201 =
            "Illegal KM reference %s with KM code %s and type %s: rule cannot be global";
    private static final String ERROR_MESSAGE_03202 =
            "Illegal KM reference found in transformation %s with KM code %s " +
                    "and type %s: code appears to point to rule of differing KM type.";
    private static final String ERROR_MESSAGE_03203 =
            "Illegal KM reference found in transformation %s with KM code %s and " +
                    "type %s: code appears to point to undefined rule.";
    private static final String ERROR_MESSAGE_03210 = "Errors found in LKM";
    private static final String ERROR_MESSAGE_03220 = "CKM Validation Errors in package sequence %d. See error report.";
    private static final String ERROR_MESSAGE_03230 = "IKM Validation Errors.  See error report of packageSequence %s.";
    private static final String ERROR_MESSAGE_03240 = "KnowledgeModuleConfiguration can't be null.";

    private final DatabaseMetadataService databaseMetadataService;
    private final ETLSubsystemService etlSubsystemService;
    private final KnowledgeModulePropertiesProvider provider;
    private final ETLValidator validator;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final KnowledgeModuleStrategy defaultStrategy;
    private final KnowledgeModuleStrategy customStrategy;
    private final JodiProperties jodiProperties;

    @Inject
    public KnowledgeModuleContextImpl(final DatabaseMetadataService databaseMetadataService,
                                      final ETLSubsystemService etlSubsystemService,
                                      final @DefaultStrategy KnowledgeModuleStrategy defaultStrategy,
                                      final KnowledgeModulePropertiesProvider provider,
                                      final KnowledgeModuleStrategy customStrategy, final JodiProperties jodiProperties,
                                      final ETLValidator validator,
                                      final ErrorWarningMessageJodi errorWarningMessages) {
        this.databaseMetadataService = databaseMetadataService;
        this.etlSubsystemService = etlSubsystemService;
        this.defaultStrategy = defaultStrategy;
        this.customStrategy = customStrategy;
        this.provider = provider;
        this.jodiProperties = jodiProperties;
        this.validator = validator;
        this.errorWarningMessages = errorWarningMessages;
    }

    private boolean preValidate(String code, KnowledgeModuleType type, Transformation transformation) {
        List<KnowledgeModuleProperties> rules = provider.getProperties(type);

        KnowledgeModuleProperties foundRule = null;
        for (KnowledgeModuleProperties rule : rules) {
            if (code != null && code.equals(rule.getId())) {
                foundRule = rule;
                break;
            }
        }

        // Make sure rule code appears valid.
        if (foundRule != null) {
            if (foundRule.isGlobal()) {
                String transformationSpecific = "found in transformation" + transformation.getName();
                String msg = errorWarningMessages.formatMessage(3201, ERROR_MESSAGE_03201, this.getClass(),
                                                                transformationSpecific, code, type);
                logger.debug(msg);
//				errorWarningMessages.addMessage(
//						transformation.getPackageSequence(), msg, 
//						MESSAGE_TYPE.ERRORS);
//				throw new RuntimeException(msg);
            }
        } else {
            // If the property exists it belongs to a different KM type
            // TODO can we refactor to get rid of the .name?
            String s = jodiProperties.getProperty(code + ".name");
            if (s != null && s.length() > 0) {
                String msg = errorWarningMessages.formatMessage(3202, ERROR_MESSAGE_03202, this.getClass(),
                                                                transformation.getName(), code, type);
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new RuntimeException(msg);
            } else {
                String msg = errorWarningMessages.formatMessage(3203, ERROR_MESSAGE_03203, this.getClass(),
                                                                transformation.getName(), code, type);
                logger.error(msg);
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                throw new RuntimeException(msg);
            }
        }

        return true;
    }


    private KnowledgeModuleConfigurationImpl putModelOptions(
            KnowledgeModuleConfigurationImpl knowledgeModuleConfiguration, Map<String, String> options) {

        for (String key : options.keySet()) {
            String value = options.get(key);

            if (value.toLowerCase()
                     .trim()
                     .equals("false")) {
                knowledgeModuleConfiguration.putOption(key, false);
            } else if (value.toLowerCase()
                            .trim()
                            .equals("true")) {
                knowledgeModuleConfiguration.putOption(key, true);
            } else {
                knowledgeModuleConfiguration.putOption(key, value);
            }
        }

        return knowledgeModuleConfiguration;
    }

    @Override
    public KnowledgeModuleConfiguration getLKMConfig(final Source source, final String dataSetName) {

        KnowledgeModuleConfigurationImpl explicitKmConfiguration = null;

        if (source.getLkm() != null) {
            if (validator.validateLKM(source)) {
                preValidate(source.getLkm()
                                  .getName(), KnowledgeModuleType.Loading, source.getParent()
                                                                                 .getParent());

                explicitKmConfiguration = new KnowledgeModuleConfigurationImpl();
                explicitKmConfiguration.setType(KnowledgeModuleType.Loading);
                explicitKmConfiguration.setName(source.getLkm()
                                                      .getName());
                putModelOptions(explicitKmConfiguration, source.getLkm()
                                                               .getOptions());
            } else {
                String msg = errorWarningMessages.formatMessage(3210, ERROR_MESSAGE_03210, this.getClass());
                logger.error(msg);
                throw new UnRecoverableException(msg);
            }
        }

        final Mappings mappings = source.getParent()
                                        .getParent()
                                        .getMappings();

        // This is a hack but works for now, grouping needs to occur
        final DataStore sourceDataStore =
                databaseMetadataService.getSourceDataStoreInModel(source.getName(), source.getModel());

        final DataStore targetDataStore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        LoadKnowledgeModuleExecutionContext executionContext =
                this.createLoadExecutionContext(sourceDataStore, targetDataStore, dataSetName, source);

        KnowledgeModuleConfiguration knowledgeModuleConfiguration;
        try {
            knowledgeModuleConfiguration = defaultStrategy.getLKMConfig(explicitKmConfiguration, executionContext);

            knowledgeModuleConfiguration = customStrategy.getLKMConfig(knowledgeModuleConfiguration, executionContext);
            if (knowledgeModuleConfiguration != null) {
                ((SourceImpl) source).setLkm(generateKMType(knowledgeModuleConfiguration));
            } else {
                logger.info(String.format("Set LKM to null for source %s ", source.getName()));
                ((SourceImpl) source).setLkm(null);
            }
            validator.validateLKM(source, customStrategy);

        } catch (RuntimeException re) {
            validator.handleLKM(re, source);
            throw re;
        }

        if (knowledgeModuleConfiguration != null) {
            ((SourceImpl) source).setLkm(generateKMType(knowledgeModuleConfiguration));
        } else {
            ((SourceImpl) source).setLkm(null);
        }

        return knowledgeModuleConfiguration;

    }


    @Override
    public KnowledgeModuleConfiguration getCKMConfig(final Transformation transformation) {
        KnowledgeModuleConfigurationImpl explicitKmConfiguration = null;

        KmType kmType = transformation.getMappings()
                                      .getCkm();
        if (kmType != null) {
            if (validator.validateCKM(transformation.getMappings())) {
                explicitKmConfiguration = new KnowledgeModuleConfigurationImpl();
                explicitKmConfiguration.setType(KnowledgeModuleType.Check);
                explicitKmConfiguration.setName(kmType.getName());
                putModelOptions(explicitKmConfiguration, kmType.getOptions());
            } else {
                String msg = errorWarningMessages.formatMessage(3220, ERROR_MESSAGE_03220, this.getClass(),
                                                                transformation.getPackageSequence());
                logger.error(msg);
                throw new UnRecoverableException(msg);
            }
        }
        final Mappings mappings = transformation.getMappings();
        final DataStore targetDataStore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        CheckKnowledgeModuleExecutionContext executionContext =
                createCheckExecutionContext(targetDataStore, transformation);

        KnowledgeModuleConfiguration knowledgeModuleConfiguration = null;

        try {
            knowledgeModuleConfiguration = defaultStrategy.getCKMConfig(explicitKmConfiguration, executionContext);

            if (customStrategy != null) {
                knowledgeModuleConfiguration =
                        customStrategy.getCKMConfig(knowledgeModuleConfiguration, executionContext);
            }

            if (knowledgeModuleConfiguration != null && generateKMType(knowledgeModuleConfiguration) != null) {
                ((MappingsImpl) transformation.getMappings()).setCkm(generateKMType(knowledgeModuleConfiguration));
            }
            validator.validateCKM(mappings, customStrategy);
        } catch (RuntimeException e) {
            validator.handleCKM(e, mappings);
        }

        return knowledgeModuleConfiguration;
    }


    @Override
    public KnowledgeModuleConfiguration getIKMConfig(final Transformation transformation) {
        KnowledgeModuleConfigurationImpl explicitKmConfiguration = null;

        //determine explicitly defined KM if exists
        KmType kmType = transformation.getMappings()
                                      .getIkm();
        if (kmType != null) {

            //preValidate(kmType.getName(), KnowledgeModuleType.Integration, transformation);
            if (validator.validateIKM(transformation.getMappings())) {
                explicitKmConfiguration = new KnowledgeModuleConfigurationImpl();
                explicitKmConfiguration.setType(KnowledgeModuleType.Integration);
                explicitKmConfiguration.setName(kmType.getName());
                putModelOptions(explicitKmConfiguration, kmType.getOptions());
            } else {
                String msg = errorWarningMessages.formatMessage(3230, ERROR_MESSAGE_03230, this.getClass(),
                                                                transformation.getPackageSequence());
                logger.error(msg);
                throw new UnRecoverableException(msg);
            }
        }

        final Mappings mappings = transformation.getMappings();
        final DataStore targetDataStore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        KnowledgeModuleExecutionContext executionContext =
                createIntegrationExecutionContext(targetDataStore, transformation);

        KnowledgeModuleConfiguration kmConfiguration = null;

        try {
            kmConfiguration = defaultStrategy.getIKMConfig(explicitKmConfiguration, executionContext);
            if (customStrategy != null) {
                kmConfiguration = customStrategy.getIKMConfig(kmConfiguration, executionContext);
            }
            ((MappingsImpl) mappings).setIkm(generateKMType(kmConfiguration));
            //this.postValidate(kmConfiguration);
            validator.validateIKM(mappings, customStrategy);
        } catch (RuntimeException e) {
            validator.handleIKM(e, mappings);
        }
        return kmConfiguration;
    }


    @Override
    public String getStagingModel(final Transformation transformation) {

        StagingKnowledgeModuleExecutionContext executionContext = createStagingExecutionContext(transformation);

        String explicitStagingModel = transformation.getMappings()
                                                    .getStagingModel();
        String stagingModel = null;

        try {
            stagingModel = defaultStrategy.getStagingModel(explicitStagingModel, executionContext);
        } catch (RuntimeException ex) {
            String msg = errorWarningMessages.formatMessage(3200, ERROR_MESSAGE_03200, this.getClass(),
                                                            databaseMetadataService.getTargetDataStoreInModel(
                                                                    transformation.getMappings()),
                                                            defaultStrategy.toString());
            errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new RuntimeException(msg, ex);
        }

        // will execute custom strategy as Guice forces a non-null object unless
        // it is defined as nullable
        if (customStrategy != null) {
            try {
                stagingModel = customStrategy.getStagingModel(stagingModel, executionContext);
            } catch (RuntimeException ex) {
                String msg = errorWarningMessages.formatMessage(3200, ERROR_MESSAGE_03200, this.getClass(),
                                                                databaseMetadataService.getTargetDataStoreInModel(
                                                                        transformation.getMappings()),
                                                                defaultStrategy.toString());
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new IncorrectCustomStrategyException(msg, ex);
            }
        }

        ((MappingsImpl) transformation.getMappings()).setStagingModel(stagingModel);

        return stagingModel;
    }


    private KnowledgeModuleExecutionContext createIntegrationExecutionContext(final DataStore targetDataStore,
                                                                              final Transformation transformation) {
        return new KnowledgeModuleExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return targetDataStore;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return databaseMetadataService.getCoreProperties();
            }


            @Override
            public TransformationExtension getTransformationExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<TransformationExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getExtension());
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<MappingsExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getMappings()
                                                  .getExtension());
            }

            @Override
            public List<KnowledgeModuleProperties> getConfigurations() {
                try {
                    return provider.getProperties(KnowledgeModuleType.Integration);
                } catch (RuntimeException e) { // TODO overly broad catch - required?
                    // TODO - validate that logging is required as error may be entered
                    // in the KnowledgeModuleProperties object
                    StringBuilder sb = new StringBuilder();
                    for (String error : provider.getErrorMessages()) {
                        sb.append(error)
                          .append(newLine);
                    }
                    logger.debug(sb.toString());
                    throw e;
                }
            }

            @Override
            public List<KnowledgeModule> getKMs() {
                return etlSubsystemService.getKMs();
            }
        };
    }

    private CheckKnowledgeModuleExecutionContext createCheckExecutionContext(final DataStore targetDataStore,
                                                                             final Transformation transformation) {
        return new CheckKnowledgeModuleExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return targetDataStore;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return databaseMetadataService.getCoreProperties();
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<TransformationExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getExtension());
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<MappingsExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getMappings()
                                                  .getExtension());
            }

            @Override
            public List<KnowledgeModuleProperties> getConfigurations() {
                return provider.getProperties(KnowledgeModuleType.Check);
            }

            @Override
            public List<KnowledgeModule> getKMs() {
                return etlSubsystemService.getKMs();
            }

            @Override
            public DataModel getStagingDataModel() {
                String stagingModel = transformation.getMappings()
                                                    .getStagingModel();
                return stagingModel != null ? databaseMetadataService.getDataModel(stagingModel) : null;
            }
        };
    }


    private LoadKnowledgeModuleExecutionContext createLoadExecutionContext(final DataStore sourceDataStore,
                                                                           final DataStore targetDataStore,
                                                                           final String dataSetName,
                                                                           final Source source) {
        return new LoadKnowledgeModuleExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return targetDataStore;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return databaseMetadataService.getCoreProperties();
            }


            @Override
            public String getDatasetName() {
                return dataSetName;
            }

            @Override
            public DataStore getSourceDataStore() {
                return sourceDataStore;
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<TransformationExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(source.getParent()
                                          .getParent()
                                          .getExtension());
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<MappingsExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(source.getParent()
                                          .getParent()
                                          .getMappings()
                                          .getExtension());
            }

            @Override
            public List<KnowledgeModuleProperties> getConfigurations() {
                return provider.getProperties(KnowledgeModuleType.Loading);
            }

            @Override
            public List<KnowledgeModule> getKMs() {
                return etlSubsystemService.getKMs();
            }

            @Override
            public DataModel getStagingDataModel() {
                String stagingModel = source.getParent()
                                            .getParent()
                                            .getMappings()
                                            .getStagingModel();
                return stagingModel != null ? databaseMetadataService.getDataModel(stagingModel) : null;
            }
        };
    }

    private StagingKnowledgeModuleExecutionContext createStagingExecutionContext(final Transformation transformation) {
        return new StagingKnowledgeModuleExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return databaseMetadataService.getTargetDataStoreInModel(transformation.getMappings());
            }

            @Override
            public List<KnowledgeModuleProperties> getConfigurations() {
                return provider.getProperties(KnowledgeModuleType.Integration);
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                ClonerUtil<MappingsExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getMappings()
                                                  .getExtension());
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                ClonerUtil<TransformationExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getExtension());
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return databaseMetadataService.getCoreProperties();
            }

            @Override
            public List<DataStoreWithAlias> getSourceDataStores() {
                ArrayList<DataStoreWithAlias> list = new ArrayList<>();

                for (Dataset dataset : transformation.getDatasets()) {
                    for (final Source source : dataset.getSources()) {
                        final DataStore sourceDataStore =
                                databaseMetadataService.getSourceDataStoreInModel(source.getName(), source.getModel());
                        list.add(new DataStoreWithAlias() {

                            @Override
                            public String getAlias() {
                                return source.getAlias();
                            }

                            @Override
                            public DataStore getDataStore() {
                                return sourceDataStore;
                            }

                            @Override
                            public Type getType() {
                                return DataStoreWithAlias.Type.Source;
                            }

                            @Override
                            public SourceExtension getSourceExtension() {
                                ClonerUtil<SourceExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                                return cloner.clone(source.getExtension());
                            }
                        });
                        for (final Lookup lookup : source.getLookups()) {
                            final DataStore lookupDataStore =
                                    databaseMetadataService.getSourceDataStoreInModel(lookup.getLookupDataStore(),
                                                                                      lookup.getModel());
                            list.add(new DataStoreWithAlias() {

                                @Override
                                public String getAlias() {
                                    return lookup.getAlias();
                                }

                                @Override
                                public DataStore getDataStore() {
                                    return lookupDataStore;
                                }

                                @Override
                                public Type getType() {
                                    return DataStoreWithAlias.Type.Lookup;
                                }

                                @Override
                                public SourceExtension getSourceExtension() {
                                    ClonerUtil<SourceExtension> cloner = new ClonerUtil<>(errorWarningMessages);
                                    return cloner.clone(source.getExtension());
                                }
                            });
                        }
                    }
                }

                return Collections.unmodifiableList(list);
            }

            @Override
            public List<KnowledgeModule> getKMs() {
                return etlSubsystemService.getKMs();
            }

            @Override
            public String getIKMCode() {
                return transformation.getMappings()
                                     .getIkm()
                                     .getName();
            }
        };
    }


    private KmType generateKMType(KnowledgeModuleConfiguration kmc) {
        if (kmc == null) {
            String msg = errorWarningMessages.formatMessage(3240, ERROR_MESSAGE_03240, this.getClass());
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }
        if (kmc == KnowledgeModuleConfiguration.Null) {
            return null;
        }

        KmTypeImpl type = new KmTypeImpl();
        type.setName(kmc.getName());
        for (String k : kmc.getOptionKeys()) {
            type.addOption(k, kmc.getOptionValue(k) + "");
        }
        return type;
    }
}
