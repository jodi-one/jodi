package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.annotations.JournalizedData;
import one.jodi.core.service.TransformationService;


/**
 * An {@link ActionRunner} implementation that invokes the {@link
 * TransformationService#deleteTransformations()} method.
 *
 */
public class DeleteTransformationsActionRunner implements ActionRunner {

    private final static String ERROR_MESSAGE_01010 = "Could not delete interfaces,\n"
            + "This could be due to incorrect jodi.properties where the jodi.properties are not in line with ODI,\n"
            + "e.g. check that the MODEL_CODE in ODI corresponds to the responding jodi.properties model.code,\n"
            + "or this could be due to an invalid XML file.";

    private final static String ERROR_MESSAGE_01059 = "The metadata directory is required to run Transformation delete";
    private final static String ERROR_MESSAGE_01060 = "The configuration property file is required to run Transformation delete";

    private final TransformationService transformationService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final boolean journalized;

    /**
     * Creates a new DeleteTransformationsActionRunner instance.
     *
     * @param transformationService
     */
    @Inject
    protected DeleteTransformationsActionRunner(
            final TransformationService transformationService,
            final ErrorWarningMessageJodi errorWarningMessages,
            final @JournalizedData String journalized) {
        this.transformationService = transformationService;
        this.errorWarningMessages = errorWarningMessages;
        this.journalized = Boolean.parseBoolean(journalized);
    }

    /**
     * @see one.jodi.bootstrap.RunConfig$ActionRunner#run(RunConfig)
     */
    @Override
    public void run(final RunConfig config) {
        try {
            transformationService.deleteTransformations(this.journalized);
        } catch (Exception ex) {
            throw new UnRecoverableException(
                    errorWarningMessages.formatMessage(1010,
                            ERROR_MESSAGE_01010, this.getClass()), ex);
        }
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getMetadataDirectory())) {
            String msg = errorWarningMessages.formatMessage(1059,
                    ERROR_MESSAGE_01059, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException();
        }

        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(1060,
                    ERROR_MESSAGE_01060, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException();
        }
    }

}
