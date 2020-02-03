package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.service.TransformationService;


/**
 * An {@link ActionRunner} implementation that invokes the {@link
 * TransformationService#createOrReplaceTransformations(boolean)} method.
 */
public class CreateTransformationsActionRunner implements ActionRunner {

    private final static String ERROR_MESSAGE_01050 = "The metadata directory is required to run Transformation creation";
    private final static String ERROR_MESSAGE_01051 = "The configuration property file is required to run Transformation creation";
    private final static String ERROR_MESSAGE_01052 = "The prefix configuration is required to run Transformation creation";

    private final TransformationService transformationService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new CreateTransformationsActionRunner instance.
     *
     * @param transformationService
     */
    @Inject
    protected CreateTransformationsActionRunner(
            final TransformationService transformationService,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.transformationService = transformationService;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * @see one.jodi.bootstrap.RunConfig$ActionRunner#run(RunConfig)
     */
    @Override
    public void run(final RunConfig config) {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        transformationService.createOrReplaceTransformations(
                etlConfig.isJournalized());
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;

        if (!StringUtils.hasLength(etlConfig.getPrefix())) {
            String msg = errorWarningMessages.formatMessage(1052,
                    ERROR_MESSAGE_01052, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

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
