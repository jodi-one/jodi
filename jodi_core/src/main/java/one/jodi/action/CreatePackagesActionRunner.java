package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.service.MetadataServiceProvider.TransformationMetadataHandler;
import one.jodi.core.service.PackageService;
import one.jodi.core.service.TransformationService;
import one.jodi.etl.builder.DeleteTransformationContext;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.TransformationImpl;

import java.util.List;


/**
 * An {@link ActionRunner} implementation that invokes the {@link
 * TransformationService#createOrReplaceTransformations(boolean)} method.
 */
public class CreatePackagesActionRunner implements ActionRunner {

    private final static String ERROR_MESSAGE_01050 = "The metadata directory is required to run Transformation creation";
    private final static String ERROR_MESSAGE_01051 = "The configuration property file is required to run Transformation creation";

    private final PackageService packageService;
    private final MetadataServiceProvider metadataServiceProvider;
    private final EnrichingBuilder enrichingBuilder;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new CreateTransformationsActionRunner instance.
     *
     * @param packageService          the package service
     * @param metadataServiceProvider the metadata service provider
     */
    @Inject
    protected CreatePackagesActionRunner(
            final PackageService packageService,
            final MetadataServiceProvider metadataServiceProvider,
            final PackageCache packageCache,
            final EnrichingBuilder enrichingBuilder,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.packageService = packageService;
        this.metadataServiceProvider = metadataServiceProvider;
        this.enrichingBuilder = enrichingBuilder;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * @see EtlRunConfig (one.jodi.base
     * .bootstrap
     * .RunConfig)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run(final RunConfig config) {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        // prepare cache ?
        metadataServiceProvider.getPackageHeaders(etlConfig.isJournalized());
        metadataServiceProvider.provideTransformationMetadata(new TransformationMetadataHandler() {
            @Override
            public void handleTransformationDESC(Transformation transformation) {

            }

            @Override
            public void handleTransformation(Transformation transformation) {
            }

            @Override
            public void handleTransformationASC(Transformation transformation, int packageSequence) {
                //enrichingBuilder.enrich(transformation, etlConfig.isJournalized());
                DeleteTransformationContext context = enrichingBuilder.createDeleteContext(transformation, etlConfig.isJournalized());
                ((TransformationImpl) transformation).setName(context.getName());
                ((TransformationImpl) transformation).setOriginalFolderPath(context.getName());
                ((TransformationImpl) transformation).setFolderName(context.getFolderName());
                ((MappingsImpl) transformation.getMappings()).setModel(context.getModel());

            }

            @Override
            public void post() {
            }

            @Override
            public void postASC() {
            }

            @Override
            public void postDESC() {
            }

            @Override
            public void pre() {
            }

            @Override
            public void preASC() {
            }

            @Override
            public void preDESC() {
            }
        });
        List<ETLPackage> packages = metadataServiceProvider.getPackages(etlConfig.isJournalized());
        //packageCache.initializePackageCache(packages);

        // Remove all packages first
        boolean throwErrorOnFailureWhileDeleting = false;
        packageService.deletePackages((List<ETLPackageHeader>) (List<?>) packages,
                throwErrorOnFailureWhileDeleting);
        boolean throwErrorOnFailureWhileCreating = true;
        //Then recreate
        packageService.createPackages(packages, throwErrorOnFailureWhileCreating);
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {

        if (!StringUtils.hasLength(config.getMetadataDirectory())) {
            String msg = errorWarningMessages.formatMessage(1050,
                    ERROR_MESSAGE_01050, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(1051,
                    ERROR_MESSAGE_01051, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }
}
