package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.base.util.Version;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.service.MetadataServiceProvider.TransformationMetadataHandler;
import one.jodi.core.service.TransformationService;
import one.jodi.core.service.ValidationException;
import one.jodi.etl.builder.DeleteTransformationContext;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.SetOperatorTypeEnum;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.DatasetImpl;
import one.jodi.etl.service.datastore.DatastoreServiceProvider;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.etl.service.interfaces.TransformationException;
import one.jodi.etl.service.interfaces.TransformationServiceProvider;
import one.jodi.etl.service.scenarios.ScenarioServiceProvider;
import one.jodi.logging.ErrorReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.io.InputStream;


/**
 * Implementation of the {@link TransformationService} interface.
 */
@Singleton
public class TransformationServiceImpl implements TransformationService {
    private final static Logger logger = LogManager.getLogger(
            TransformationServiceImpl.class);
    private final static String newLine = System.getProperty("line.separator");

    private final static String ERROR_MESSAGE_00240 = "Fatal error: %s";

    private final static String ERROR_MESSAGE_03270 = JodiConstants.VERSION_HEADER;

    private final static String ERROR_MESSAGE_03290 = "The transformation "
            + "'%1$s' could not be deleted, try deleting any packages that "
            + "refer to these interfaces.";

    private final static String ERROR_MESSAGE_03300 = "The configured project"
            + " '%1$s' is not found. Please consult the user manual to define"
            + " the correct project code.";

    private final static String ERROR_MESSAGE_03310 = JodiConstants.ERROR_FOOTER;

    private final static String ERROR_MESSAGE_03320 = "An unknown error "
            + "occurred, please review warning validation messages. %s";

    private final static String ERROR_MESSAGE_03330 = "Runtime exception: %s";

    private final static String ERROR_MESSAGE_03340 = "A jodi.property "
            + "jodi.update is required while updating.";

    private final static String ERROR_MESSAGE_03350 = "The properties "
            + JodiConstants.INITIAL_LOAD_FOLDER + " and "
            + JodiConstants.INCREMENTALL_LOAD_FOLDER
            + " cannot have the same prefix '%1$s'.";

    private final static String ERROR_MESSAGE_08000 =
            "Property " + JodiConstants.INCREMENTALL_LOAD_FOLDER +
                    " not found. Default value " +
                    JodiConstants.INCREMENTALL_LOAD_FOLDER_DEFAULT +
                    " will be selected as a folder prefix.";

    private final DatabaseMetadataService databaseMetadataService;
    private final TransformationServiceProvider transformationService;
    private final TransformationBuilder transformationBuilder;
    private final DatastoreServiceProvider dataStoreService;
    private final MetadataServiceProvider metadataProvider;
    private final SchemaMetaDataProvider etlProvider;
    private final JodiProperties properties;
    private final EnrichingBuilder enrichingBuilder;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final ScenarioServiceProvider scenarioServiceProvider;
    private final EtlSubSystemVersion etlSubSystemVersion;
    private final boolean useScenario;
    private final boolean ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS;
    private Boolean projectExists = null;

    @Inject
    protected TransformationServiceImpl(
            final TransformationServiceProvider transformationService,
            final TransformationBuilder transformationBuilder,
            final DatastoreServiceProvider dataStoreService,
            final MetadataServiceProvider metadataProvider,
            final SchemaMetaDataProvider etlProvider,
            final JodiProperties properties,
            final EnrichingBuilder enrichingBuilder,
            final DatabaseMetadataService databaseMetadataService,
            final ErrorWarningMessageJodi errorWarningMessages,
            final ScenarioServiceProvider scenarioServiceProvider,
            final EtlSubSystemVersion etlSubSystemVersion) {
        this.transformationService = transformationService;
        this.transformationBuilder = transformationBuilder;
        this.dataStoreService = dataStoreService;
        this.metadataProvider = metadataProvider;
        this.etlProvider = etlProvider;
        this.properties = properties;
        this.enrichingBuilder = enrichingBuilder;
        this.databaseMetadataService = databaseMetadataService;
        this.errorWarningMessages = errorWarningMessages;
        this.scenarioServiceProvider = scenarioServiceProvider;
        this.etlSubSystemVersion = etlSubSystemVersion;
        this.useScenario = this.properties.getPropertyKeys().contains(JodiConstants.USE_SCENARIOS_INSTEAD_OF_MAPPINGS) ?
                Boolean.valueOf(this.properties.getProperty(JodiConstants.USE_SCENARIOS_INSTEAD_OF_MAPPINGS)).booleanValue() : false;

        this.ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS = this.properties.getPropertyKeys()
                .contains(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS) ? Boolean.valueOf(this.properties.getProperty(JodiConstants.ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS)) : false;

    }

    /**
     * @see TransformationService#createOrReplaceTransformations(boolean)
     */
    @Override
    public void createOrReplaceTransformations(final boolean journalized) {
        logger.info("Journalized: " + journalized);
        validateProjectExists();
        validateJKM();
        ErrorReport.reset();
        metadataProvider.provideTransformationMetadata(
                new TransformationMetadataHandler() {
                    @Override
                    public void handleTransformationASC(
                            final Transformation transformation,
                            final int packageSequence) {
                        logger.debug("Journalized: " + journalized);
                        logger.debug("Transformation is asynchronous: " + transformation.isAsynchronous() + " or useScenario " + useScenario);
                        createTransformation(transformation, packageSequence, journalized);
                        if (!transformation.isTemporary() && (transformation.isAsynchronous() || useScenario || ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS)) {
                            createScenarioForMapping(transformation);
                        }
                    }

                    private void createScenarioForMapping(Transformation transformation) {
                        scenarioServiceProvider.generateScenarioForMapping(transformation.getName(), transformation.getFolderName());
                    }

                    @Override
                    public void handleTransformationDESC(
                            final Transformation transformation) {
                        if (properties.isUpdateable()) {
                            truncateTransformations(transformation, journalized);
                        } else {
                            deleteTransformation(transformation, journalized);
                        }
                    }

                    @Override
                    public void preDESC() {
                        logger.info("Delete transformations: started");
                    }

                    @Override
                    public void postDESC() {
                        logger.info("Delete transformations: completed");
                    }

                    @Override
                    public void preASC() {
                        logger.info("Create transformations: started");
                    }

                    @Override
                    public void postASC() {
                        logger.info("Create transformations: completed");
                    }

                    @Override
                    public void pre() {
                    }

                    @Override
                    public void post() {
                    }

                    @Override
                    public void handleTransformation(
                            final Transformation transformation) {
                    }
                });

        if (ErrorReport.getErrorReport().length() > 1) {
            String msg = errorWarningMessages.formatMessage(3270,
                    ERROR_MESSAGE_03270, this.getClass(),
                    Version.getProductVersion());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);

            String errorReport = ErrorReport.getErrorReport().toString();
            logger.debug("------- ErrorReport:" + newLine +
                    errorReport + newLine +
                    "------- End ErrorReport");
            ErrorReport.reset();

            throw new UnRecoverableException(
                    errorWarningMessages.formatMessage(3310,
                            ERROR_MESSAGE_03310 + "\n" + errorReport, this.getClass()));
        }
        ErrorReport.reset();
    }

    /**
     * Creates an ETL transformation given the input model transformation.
     *
     * @param transformation
     * @param packageSequence
     * @throws Exception
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected void createTransformation(final Transformation transformation,
                                        final int packageSequence, final boolean isJournalizedData) {
        //ignoreValuesWhenNotApplicable(transformation);
        try {
            logger.debug(packageSequence + " applying transformation name and folder name");
            enrichingBuilder.enrich(transformation, isJournalizedData);

            logger.debug(packageSequence + " validate transformation");
            logger.debug(packageSequence + " create transformation :" + transformation.getName());
            transformationService.createTransformation(transformation, isJournalizedData, packageSequence);
            logger.debug(packageSequence + ": adding datasets.");

            //jkm

            if (logger.isDebugEnabled()) {
                logger.debug("Trying to set flex mappings.");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Flex mappings are set.");
            }
        } catch (NullPointerException e) {
            // A NullPointerException occurs on invalid joins
            String message = e.getMessage() != null ? e.getMessage() : "";

            String msg = errorWarningMessages.formatMessage(3320, ERROR_MESSAGE_03320, this.getClass(), message);
            errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
        } catch (ValidationException e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            String msg = errorWarningMessages.formatMessage(240, ERROR_MESSAGE_00240, this.getClass(), message);
            errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
        } catch (Throwable e) {
            String friendly = ">>>> Failed to create ODI interface \"" +
                    packageSequence + " " + transformation.getName();
            if (e.getCause() != null) {
                friendly = friendly + " " + e.getCause();
            }
            if (e.getClass() != null) {
                friendly = friendly + " " + e.getClass();
            }
            if (e.getMessage() != null) {
                friendly = friendly + " " + e.getMessage();
            }
            String msg = errorWarningMessages.formatMessage(240, ERROR_MESSAGE_00240, this.getClass(), friendly);
            errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
        }
    }


    /**
     * @see TransformationService#createTransformations(boolean)
     */
    @Override
    public void createTransformations(final boolean journalized) {
        metadataProvider.provideTransformationMetadata(
                new TransformationMetadataHandler() {
                    @Override
                    public void preASC() {
                        logger.info("Create transformations: started");
                    }

                    @Override
                    public void handleTransformationASC(
                            final Transformation transformation,
                            final int packageSequence) {
                        createTransformation(transformation, packageSequence, journalized);
                    }

                    @Override
                    public void postASC() {
                        logger.info("Create transformations: completed");
                    }

                    @Override
                    public void handleTransformationDESC(
                            final Transformation transformation) {
                    }

                    @Override
                    public void preDESC() {
                    }

                    @Override
                    public void postDESC() {
                    }

                    @Override
                    public void pre() {
                    }

                    @Override
                    public void post() {
                    }

                    @Override
                    public void handleTransformation(
                            final Transformation transformation) {
                    }
                });

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteDatastores(DeleteTransformationContext context) {
        if (databaseMetadataService.isTemporaryTransformation(context.getName())) {
            logger.debug(context.getPackageSequence() + " Attempting to delete temp:" + context.getName());
            dataStoreService.deleteDatastore(context.getName(), context.getModel());
        }
    }

    /**
     * @see TransformationService#deleteTransformation(String, String)
     */
    //@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteTransformation(final Transformation transformation,
                                     boolean isJournalizedData) {
        if (properties.isUpdateable()) {
            return;
        }
        DeleteTransformationContext context =
                enrichingBuilder.createDeleteContext(transformation, isJournalizedData);
        // enrichingBuilder.enrichTransformationNameAndModelAndFolder(transformation,
        // false);

        assert (context.getName() != null) : context.getPackageSequence() +
                " Fatal error: table name must be defined at this point.";
        logger.info(context.getPackageSequence() + " Attempting to delete:" +
                context.getName() + " in folder: " + context.getFolderName() + ".");
        try {
            doDeleteTransformation(context.getName(), context.getFolderName());
            if (etlSubSystemVersion.isVersion11()) {
                deleteDatastores(context);
            }
            //
            // From jodi1.0 earlier version there were temporary datastores created
            // These remain were they are however they are no longer used.
            //
            // deletedJodi10TempTransformation(transformation);
        } catch (Exception ole) {
            String msg =
                    errorWarningMessages.formatMessage(3290, ERROR_MESSAGE_03290,
                            this.getClass(), context.getName());
            errorWarningMessages.addMessage(transformation.getPackageSequence(), msg,
                    MESSAGE_TYPE.ERRORS);
            logger.error(msg, ole);
            throw new RuntimeException(msg, ole);
        }
    }


    /**
     * @throws TransformationAccessStrategyException
     */
    @Override
    public void deleteTransformation(final String name, String folder) {
        logger.debug("deleting: " + name + " in folder: " + folder);
        transformationService.deleteTransformation(name, folder);
    }

    /**
     *
     */
    @Override
    public void deleteTransformations(final boolean journalized) {
        metadataProvider.provideTransformationMetadata(
                new TransformationMetadataHandler() {
                    @Override
                    public void preDESC() {
                        logger.info("Delete transformations: started");
                    }

                    @Override
                    public void handleTransformationDESC(
                            final Transformation transformation) {
                        deleteTransformation(transformation, journalized);
                    }

                    @Override
                    public void postDESC() {
                        logger.info("Delete transformations: completed");
                    }

                    @Override
                    public void handleTransformationASC(
                            final Transformation transformation,
                            final int packageSequence) {
                    }

                    @Override
                    public void preASC() {
                    }

                    @Override
                    public void postASC() {
                    }

                    @Override
                    public void pre() {
                    }

                    @Override
                    public void post() {
                    }

                    @Override
                    public void handleTransformation(
                            final Transformation transformation) {
                    }
                });
    }

    public void doDeleteTransformation(final String name, final String folder) throws Exception {
        try {
            transformationService.deleteTransformation(name, folder);
            if (logger.isDebugEnabled()) {
                logger.info("Interface removed:" + name);
            }
        } catch (IllegalStateException ise) {
            logger.info("Can't delete mapping " + name + " since it does not exists.");
        } catch (TransformationException e) {
            logger.info("Cannot delete mapping with " + name + " as it does not exist or is non-unique.");
        }

    }

    protected void applySetOperator(Dataset dataset, SetOperatorTypeEnum e) {
        ((DatasetImpl) dataset).setSetOperator(e);
    }


    @Override
    public void createTransformation(InputStream xmlFile, int packageSequence, boolean isJournalizedData) {
        StreamingXMLMetadataServiceProvider streamingXMLMetadataServiceProvider =
                new StreamingXMLMetadataServiceProvider(null, properties,
                        this.transformationBuilder,
                        errorWarningMessages);
        Transformation transformation = streamingXMLMetadataServiceProvider
                .getTransformation(xmlFile, packageSequence);

        createTransformation(transformation, packageSequence, isJournalizedData);

    }

    @Override
    public void deleteTransformation(InputStream xmlFile) {
        StreamingXMLMetadataServiceProvider streamingXMLMetadataServiceProvider =
                new StreamingXMLMetadataServiceProvider(null, properties,
                        this.transformationBuilder,
                        errorWarningMessages);
        Transformation transformation = streamingXMLMetadataServiceProvider
                .getTransformation(xmlFile, 0);
        deleteTransformation(transformation, false);
    }


    //jkm
    @Override
    public void mergeTransformations(InputStream xmlFile, final int packageSequence, final boolean journalized) {
        StreamingXMLMetadataServiceProvider streamingXMLMetadataServiceProvider =
                new StreamingXMLMetadataServiceProvider(null, properties,
                        this.transformationBuilder,
                        errorWarningMessages);
        Transformation transformation = streamingXMLMetadataServiceProvider
                .getTransformation(xmlFile, packageSequence);
        mergeTransformations(transformation, packageSequence, journalized);
    }

    public void mergeTransformations(final Transformation transformation, final int packageSequence, final boolean journalized) {
        validateMerge();
        truncateTransformations(transformation, journalized);
        createTransformation(transformation, packageSequence, journalized);
    }

    //@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void truncateTransformations(final Transformation transformation,
                                        final boolean journalized) {
        logger.debug(transformation.getPackageSequence() +
                " enriched in truncate transfromationserviceimpl.");
        try {

            DeleteTransformationContext deleteContext = enrichingBuilder.createDeleteContext(transformation, journalized);

            transformationService.truncateInterfaces(deleteContext.getName(), deleteContext.getFolderName());

        } catch (Exception e) {
            int pckSequence = transformation.getPackageSequence();
            if (pckSequence == 0) {
                pckSequence = errorWarningMessages.assignSequenceNumber();
            }

            String msg = errorWarningMessages.formatMessage(3330, ERROR_MESSAGE_03330,
                    this.getClass(), e);
            errorWarningMessages.addMessage(pckSequence, msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    private void validateMerge() {
        if (!properties.isUpdateable()) {
            String msg = errorWarningMessages.formatMessage(3340,
                    ERROR_MESSAGE_03340, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new ValidationException(msg);
        }
    }

    private void validateProjectExists() {

        if (projectExists == null) {
            projectExists = etlProvider.existsProject(properties.getProjectCode());
        }
        if (!projectExists) {
            String msg = errorWarningMessages.formatMessage(3300,
                    ERROR_MESSAGE_03300, this.getClass(), properties.getProjectCode());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
    }

    private void validateJKM() {
        String prefixBulk = properties.getProperty(JodiConstants.INITIAL_LOAD_FOLDER);
        String prefixIncremental = "";
        try {
            prefixIncremental = properties.getProperty(JodiConstants.INCREMENTALL_LOAD_FOLDER);
        } catch (RuntimeException e) {
            prefixIncremental = JodiConstants.INCREMENTALL_LOAD_FOLDER_DEFAULT;
            String msg = errorWarningMessages.formatMessage(8000,
                    ERROR_MESSAGE_08000, this.getClass());
            logger.error(msg);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
        }
        if (prefixBulk == null) {
            return;
        }
        if (prefixBulk.length() > 0 && prefixIncremental.length() > 0 &&
                prefixBulk.substring(0, 1).equalsIgnoreCase(prefixIncremental.substring(0, 1))) {

            String msg = errorWarningMessages.formatMessage(3350,
                    ERROR_MESSAGE_03350, this.getClass(),
                    prefixBulk.substring(0, 1));
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
    }
}