package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.service.ScenarioService;


/**
 * An {@link ActionRunner} implementation that invokes the {@link
 * ScenarioService#deleteScenario(java.util.List)} method.
 */
public class DeleteScenarioActionRunner implements ActionRunner {

    private static final String ERROR_MESSAGE_01057 =
            "The configuration property file is required to run Scenario delete";
    private static final String ERROR_MESSAGE_01058 = "The target Scenario is required to run Scenario delete";

    private final ScenarioService scenarioService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new DeleteTransformationsActionRunner instance.
     *
     * @param scenarioService the scenario service
     */
    @Inject
    protected DeleteScenarioActionRunner(final ScenarioService scenarioService,
                                         final ErrorWarningMessageJodi errorWarningMessages) {
        this.scenarioService = scenarioService;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * @see one.jodi.bootstrap.RunConfig$ActionRunner#run(RunConfig)
     */
    @Override
    public void run(final RunConfig config) {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        scenarioService.deleteScenario(etlConfig.getScenario());
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(1057, ERROR_MESSAGE_01057, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        if (!StringUtils.hasLength(etlConfig.getScenario())) {
            String msg = errorWarningMessages.formatMessage(1058, ERROR_MESSAGE_01058, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

}
