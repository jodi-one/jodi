package one.jodi.core.folder.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.FolderNameExecutionContext;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import one.jodi.core.folder.FolderNameContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.TransformationImpl;
import one.jodi.model.extensions.TransformationExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * The class manages a default strategy and a custom strategy for
 * determining the folder name in which the transformation is inserted.
 * <p>
 * The default strategy is always executed first and the result is passed to
 * the custom strategy, where it may be overwritten.
 * <p>
 * This class is also responsible for building the execution context object that
 * is passed into strategies.
 * <p>
 * The class is the Context that participates in the Strategy Pattern.
 */
public class FolderNameContextImpl implements FolderNameContext {

    private final static Logger logger =
            LogManager.getLogger(FolderNameContextImpl.class);

    private final static String ERROR_MESSAGE_03800 = "Error setting folder name.";

    private final DatabaseMetadataService databaseMetadataService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    // default strategy is created without DI because it is
    // considered to be hard-coded and is not designed to be
    // modified or extended at this time
    private final FolderNameStrategy defaultStrategy;
    // custom strategy by default will be a ID strategy that returns
    // the default value; it is configured through Guice injection
    private final FolderNameStrategy customStrategy;

    private final ETLValidator etlValidator;

    /**
     * Constructor is used mainly for injection of dependencies.
     *
     * @param databaseMetadataService reference to a builder interface used for execution context
     *                                creation
     * @param modelCodeContext        entry point for determining the model associated with the
     *                                target data store
     * @param defaultStrategy         defines non-null default strategy that implements the default
     *                                behavior for this plug-in
     * @param customStrategy          defines non-null custom strategy
     */
    @Inject
    public FolderNameContextImpl(final DatabaseMetadataService databaseMetadataService,
                                 final @DefaultStrategy FolderNameStrategy defaultStrategy,
                                 final FolderNameStrategy customStrategy,
                                 final ETLValidator etlValidator,
                                 final ErrorWarningMessageJodi errorWarningMessages) {
        this.databaseMetadataService = databaseMetadataService;
        this.defaultStrategy = defaultStrategy;
        this.customStrategy = customStrategy;
        this.etlValidator = etlValidator;
        this.errorWarningMessages = errorWarningMessages;
    }


    @Override
    public String getFolderName(final Transformation transformation,
                                final boolean isJournalizedData) {
        final Mappings mappings = transformation.getMappings();
        final DataStore dataStore = databaseMetadataService.getTargetDataStoreInModel(mappings);

        FolderNameExecutionContext exc = new FolderNameExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return dataStore;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return databaseMetadataService.getCoreProperties();
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                ClonerUtil<TransformationExtension> cloner =
                        new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getExtension());
            }
        };

        // execute default strategy
        String defaultFolderName =
                defaultStrategy.getFolderName(transformation.getOriginalFolderPath(), exc,
                        isJournalizedData);
        assert ((defaultFolderName != null) && (!defaultFolderName.equals("")));

        // execute custom strategy
        String folderName = defaultFolderName;
        // We assume here that the custom strategy may not exist in which case
        // the result of the default strategy is applied.
        // Note: Guice forces the definition of a custom strategy unless the
        // @Nullable annotation (JSR305) is used in the constructor
        if (this.customStrategy != null) {
            try {
                folderName = customStrategy.getFolderName(defaultFolderName, exc,
                        isJournalizedData);
            } catch (RuntimeException e) {
                etlValidator.handleFolderName(e, transformation);
                throw e;
            }
        }
        logger.debug("derived folder name as " + folderName);
        if (transformation.getFolderName() != null)
            folderName = transformation.getFolderName();
        ((TransformationImpl) transformation).setFolderName(folderName);
        if (!etlValidator.validateFolderName(transformation,
                customStrategy != null ? customStrategy
                        : defaultStrategy)) {
            throw new UnRecoverableException(
                    errorWarningMessages.formatMessage(3800,
                            ERROR_MESSAGE_03800, this.getClass()));
        }

        return folderName;
    }
}