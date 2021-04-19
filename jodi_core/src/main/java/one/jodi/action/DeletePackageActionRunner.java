package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.PackageService;


/**
 * An {@link ActionRunner} implementation that invokes the {@link
 * PackageService#deletePackage(String, boolean)} method.
 */
public class DeletePackageActionRunner implements ActionRunner {

    private static final String ERROR_MESSAGE_01054 =
            "The configuration property file is required to run Package delete";
    private static final String ERROR_MESSAGE_01055 =
            "The target package is required in form to run package delete";
    private static final String ERROR_MESSAGE_01056 =
            "The folder name is required to run package delete";
    private static final String ERROR_MESSAGE_01504 =
            "The Jodi properties file sets jodi.update to true. " +
                    "Packages will not be truncated and not deleted.";


    private final PackageService packageService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final JodiProperties properties;


    /**
     * Creates a new DeleteTransformationsActionRunner instance.
     *
     * @param packageService
     */
    @Inject
    protected DeletePackageActionRunner(
            final PackageService packageService,
            final ErrorWarningMessageJodi errorWarningMessages,
            final JodiProperties properties) {
        this.packageService = packageService;
        this.errorWarningMessages = errorWarningMessages;
        this.properties = properties;
    }

    /**
     * @see one.jodi.bootstrap.RunConfig$ActionRunner#run(RunConfig)
     */
    @Override
    public void run(final RunConfig config) {
        boolean raiseErrorOnFailure = true;
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        if (properties.isUpdateable()) {
            String msg = errorWarningMessages.formatMessage(1504, ERROR_MESSAGE_01504,
                    this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
            packageService.truncatePackage(etlConfig.getPackage(), etlConfig.getFolder(),
                    raiseErrorOnFailure);
        } else {
            packageService.deletePackage(etlConfig.getPackage(), etlConfig.getFolder(),
                    raiseErrorOnFailure);
        }
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(1054,
                    ERROR_MESSAGE_01054, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        if (!StringUtils.hasLength(etlConfig.getPackage())) {
            String msg = errorWarningMessages.formatMessage(1055,
                    ERROR_MESSAGE_01055, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(etlConfig.getFolder())) {
            String msg = errorWarningMessages.formatMessage(1055,
                    ERROR_MESSAGE_01056, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

}
