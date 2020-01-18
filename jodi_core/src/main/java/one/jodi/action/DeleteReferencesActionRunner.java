package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.etl.service.table.TableServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * An {@link ActionRunner} implementation that invokes the {@link DeleteReferencesActionRunner} method.
 *
 */
public class DeleteReferencesActionRunner implements ActionRunner {
    private final static Logger logger =
            LogManager.getLogger(DeleteReferencesActionRunner.class);
    private final static String ERROR_MESSAGE_01056 = "The configuration property model code is required to run Delete References.";

    private final TableServiceProvider tableService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new DeleteTransformationsActionRunner instance.
     *
     * @param tableService
     */
    @Inject
    protected DeleteReferencesActionRunner(
            final TableServiceProvider tableService,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.tableService = tableService;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * @see one.jodi.bootstrap.RunConfig$ActionRunner#run(RunConfig)
     */
    @Override
    public void run(final RunConfig config) {
        logger.info("Delete references started.");
        tableService.deleteReferencesByModel(config.getModelCode());
        logger.info("Delete references finished.");
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getModelCode())) {
            String msg = errorWarningMessages.formatMessage(1056,
                    ERROR_MESSAGE_01056, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

}
