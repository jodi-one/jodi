package one.jodi.core.datastore.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.extensions.contexts.ModelNameExecutionContext;
import one.jodi.core.extensions.strategies.AmbiguousModelException;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.SubQuery;
import one.jodi.etl.internalmodel.impl.LookupImpl;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.SourceImpl;
import one.jodi.etl.internalmodel.impl.SubQueryImpl;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * The class manages a default strategy and a custom strategy for
 * determining the model name for the given data store.
 * <p>
 * The default strategy is always executed first and the result is passed to
 * the custom strategy, where it may be overwritten.
 * <p>
 * This class is also responsible for building the execution context object that
 * is passed into strategies.
 * <p>
 * The class is the Context that participates in the Strategy Pattern.
 */
public class ModelCodeContextImpl implements ModelCodeContext {

    private final static String ERROR_MESSAGE_03090 = "Model name strategy '%2$s' must return non-empty string. Error occurred while determining model for data store '%1$s'.";
    private final static String ERROR_MESSAGE_03091 = "Previously thrown ambiguous model exception: %s %s";
    private final static String ERROR_MESSAGE_03092 = "Ambiguous model exception: %s %s";

    private final static String WARNING_UNDEF = "Default model name strategy '%2$s' was unable to determine a "
            + "model for data store '%1$s'.";

    private final static Logger logger = LogManager.getLogger(ModelCodeContextImpl.class);

    private final JodiProperties wfProperties;
    private final DatabaseMetadataService databaseMetadataService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    // default strategy is created without DI because it is
    // considered to be hard-coded and is not designed to be
    // modified or extended at this time
    private final ModelCodeStrategy defaultStrategy;
    // custom strategy by default will be a ID strategy that returns
    // the default value; it is configured through Guice injection
    private final ModelCodeStrategy customStrategy;

    private final ETLValidator validator;

    /**
     * @param wfProperties            - reference to the Jodi properties files that are injected
     * @param databaseMetadataService - refers to a service for building an execution context
     * @param defaultStrategy         - defines the default strategy with the core business rules
     * @param customStrategy          - defines non-null custom strategy
     * @param validator               - the validator to be used
     * @param errorWarningMessages    - the error warning messages framework
     */
    @Inject
    public ModelCodeContextImpl(
            final JodiProperties wfProperties,
            final DatabaseMetadataService databaseMetadataService,
            final @DefaultStrategy ModelCodeStrategy defaultStrategy,
            final ModelCodeStrategy customStrategy,
            final ETLValidator validator,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.wfProperties = wfProperties;
        this.databaseMetadataService = databaseMetadataService;
        this.customStrategy = customStrategy;
        this.defaultStrategy = defaultStrategy;
        this.validator = validator;
        this.errorWarningMessages = errorWarningMessages;
    }

    /*
     * Entry point that constructs execution context and calls
     * default and custom strategies.
     */

    /**
     * Determine model which is the source of the data store.
     *
     * @param explicitModelName that is defined in the input specification
     * @return model name the data store is sourced from
     * @throws exception if explicitly defined model does not exists
     * @throws exception if the data store is not found in at least one data model
     */
    private String getModelCode(final String explicitModelName, final ModelNameExecutionContext.DataStoreRole role,
                                final String dataStoreName, final String dataStoreAlias,
                                final TransformationExtension transformationExtension,
                                final SourceExtension sourceExtension,
                                final MappingsExtension mappingsExtension) {

        final List<DataStore> foundDataStores = databaseMetadataService
                .findDataStoreInAllModels(dataStoreName);

        // Construct execution context object based on the collected and
        // passed information
        ModelNameExecutionContext exc = new ModelNameExecutionContext() {

            @Override
            public String getDataStoreName() {
                return dataStoreName;
            }

            @Override
            public String getDataStoreAlias() {
                return dataStoreAlias;
            }

            @Override
            public boolean isTemporaryTable() {
                return databaseMetadataService
                        .isTemporaryTransformation(dataStoreName);
            }

            @Override
            public DataStoreRole getDataStoreRole() {
                return role;
            }

            @Override
            public List<DataStore> getMatchingDataStores() {
                return foundDataStores;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return databaseMetadataService.getCoreProperties();
            }

            @Override
            public List<ModelProperties> getConfiguredModels() {
                return databaseMetadataService.getConfiguredModels();
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<TransformationExtension> cloner =
                        new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformationExtension);
            }

            @Override
            public SourceExtension getSourceExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<SourceExtension> cloner =
                        new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(sourceExtension);
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<MappingsExtension> cloner =
                        new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(mappingsExtension);
            }
        };

        String modelName;
        AmbiguousModelException previouslyThownException = null;
        // execute default strategy
        try {
            modelName = defaultStrategy.getModelCode(explicitModelName, exc);
            assert ((modelName != null) && (!modelName.equals("")));
        } catch (AmbiguousModelException aex) {
            modelName = null;
            previouslyThownException = aex;
            logger.debug(String.format(WARNING_UNDEF, exc.getDataStoreName(),
                    defaultStrategy.toString()), aex);
        }


        // execute custom strategy
        // We assume here that the custom strategy may not exist in which case
        // the result of the default strategy is applied.
        // Note: Guice forces the definition of a custom strategy unless the
        // @Nullable annotation (JSR305) is used in the constructor

        // If explicit model is defined, skip execution of custom strategy
        // because in that case the semantics is hard-coded.
        if ((this.customStrategy != null) && (explicitModelName == null)) {
            try {
                modelName = customStrategy.getModelCode(modelName, exc);
            } catch (AmbiguousModelException ex) {
                if ((ex.isDefault()) && (previouslyThownException != null)) {
                    String msg = errorWarningMessages.formatMessage(3091,
                            ERROR_MESSAGE_03091, this.getClass(),
                            previouslyThownException.getMessage(),
                            previouslyThownException);
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(), msg,
                            MESSAGE_TYPE.ERRORS);
                    throw previouslyThownException;
                } else {
                    String msg = errorWarningMessages.formatMessage(3092,
                            ERROR_MESSAGE_03092, this.getClass(), ex.getMessage(), ex);
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(), msg,
                            MESSAGE_TYPE.ERRORS);
                    throw ex;
                }
            }

            if ((modelName == null) || (modelName.equals(""))) {

                String msg = errorWarningMessages.formatMessage(3090,
                        ERROR_MESSAGE_03090, this.getClass(), exc.getDataStoreName(),
                        customStrategy.toString());
                logger.error(msg);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new IncorrectCustomStrategyException(msg);
            }
        }
        return modelName;
    }


    @Override
    public String getModelCode(Source source) {
        try {
            String explicitModelName = source.getModel() != null ? wfProperties.getProperty(source.getModel()) : null;
            String model = getModelCode(explicitModelName, ModelNameExecutionContext.DataStoreRole.SOURCE,
                    source.getName(), source.getAlias(), source.getParent().getParent().getExtension(), source.getExtension(), null);
            ((SourceImpl) source).setModel(model);
            return model;
        } catch (RuntimeException e) {
            validator.handleModelCode(e, source);
            throw e;
        }

    }


    @Override
    public String getModelCode(Lookup lookup) {

        try {
            String explicitModelName = lookup.getModel() != null ? wfProperties.getProperty(lookup.getModel()) : null;

            String model = getModelCode(explicitModelName, ModelNameExecutionContext.DataStoreRole.LOOKUP, lookup.getLookupDataStore(), lookup.getAlias(),
                    lookup.getParent().getParent().getParent().getExtension(), lookup.getParent().getExtension(), null);
            ((LookupImpl) lookup).setModel(model);
            return model;

        } catch (RuntimeException e) {
            validator.handleModelCode(e, lookup);
            throw e;
        }
    }


    @Override
    public String getModelCode(Mappings mappings) {
        try {
            String explicitModelName = mappings.getModel() != null ? wfProperties.getProperty(mappings.getModel()) : null;

            String model = getModelCode(explicitModelName, ModelNameExecutionContext.DataStoreRole.TARGET,
                    mappings.getTargetDataStore(), mappings.getTargetDataStore(),
                    mappings.getParent().getExtension(), null, mappings.getExtension());
            ((MappingsImpl) mappings).setModel(model);
            return model;

        } catch (RuntimeException e) {
            validator.handleModelCode(e, mappings);
            throw e;
        }
    }


    @Override
    public String getModelCode(String model) {
        return wfProperties.getProperty(model);
    }

    @Override
    public String getModelCode(SubQuery subQuery) {
        String model = getModelCode(null, ModelNameExecutionContext.DataStoreRole.SOURCE,
                subQuery.getFilterSource(), subQuery.getFilterSource(),
                null, null, null);
        ((SubQueryImpl) subQuery).setFilterSourceModel(model);
        return model;
    }
}