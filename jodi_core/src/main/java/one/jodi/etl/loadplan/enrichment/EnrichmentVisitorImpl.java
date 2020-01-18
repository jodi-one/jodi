package one.jodi.etl.loadplan.enrichment;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.lpmodel.Exceptionbehavior;
import one.jodi.etl.service.loadplan.Visitor;
import one.jodi.etl.service.loadplan.internalmodel.*;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionstepType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.ViolatebehaviourType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Collection;

/**
 * Actionrunner to create loadplans from textual specifications.
 *
 */
public class EnrichmentVisitorImpl implements EnrichmentVisitor {

    private final static String DEFAULT_RESTARTTYPE_SERIAL = "odi.loadplan.restartTypeSerial";
    private final static String DEFAULT_RESTARTTYPE_PARALLEL = "odi.loadplan.restartTypeParallel";
    private final static String DEFAULT_RESTARTTYPE_SCENARIO = "odi.loadplan.restartTypeScenario";
    private final Logger logger = LogManager.getLogger(EnrichmentVisitorImpl.class);
    private final JodiProperties jodiProperties;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private int stepCounter = 0;
    private LoadPlanDetails loadPlanDetails;

    @Inject
    public EnrichmentVisitorImpl(final JodiProperties jodiProperties,
                                 final ErrorWarningMessageJodi errorWarningMessages
    ) {
        this.jodiProperties = jodiProperties;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public Visitor<LoadPlanStep> visit(LoadPlanTree<LoadPlanStep> tree) {
        this.loadPlanDetails = tree.getLoadPlanDetails();
        return this;
    }

    @Override
    public Collection<LoadPlanStep> getChildren() {
        return null;
    }

    @Override
    public void visit(LoadPlanStep loadPlanStep) {
        enrich(loadPlanStep);
    }

    @Override
    public void enrich(final LoadPlanStep loadPlanStep) {
        stepCounter++;
        if (loadPlanDetails != null && loadPlanDetails.getLogsessionType() == null) {
            loadPlanDetails.setLogsessionType(LogsessionType.ALWAYS);
        }
        if (loadPlanDetails != null && loadPlanDetails.getLogsessionstepType() == null) {
            loadPlanDetails.setLogsessionstepType(LogsessionstepType.BYSCENARIOSETTINGS);
        }
        if (loadPlanDetails != null && loadPlanDetails.getSessiontaskloglevel() == null) {
            loadPlanDetails.setSessiontaskloglevel(5);
        }
        if (loadPlanDetails != null && loadPlanDetails.isLimitconcurrentexecutions() == null) {
            loadPlanDetails.setLimitconccurentexecutions(false);
        }
        if (loadPlanDetails != null && loadPlanDetails.getNumberOfConcurrentexecutions() == null) {
            loadPlanDetails.setNumberOfConcurrentexecutions(0);
        }
        if (loadPlanDetails != null && loadPlanDetails.getViolatebehaviourType() == null) {
            loadPlanDetails.setViolatebehaviourType(ViolatebehaviourType.WAIT_TO_EXECUTE);
        }
        if (loadPlanDetails != null && loadPlanDetails.getWaitpollinginterval() == null) {
            loadPlanDetails.setWaitpollinginterval(BigInteger.ZERO);
        }
        if (loadPlanDetails != null && loadPlanDetails.getKeeplogHistory() == null) {
            loadPlanDetails.setKeeplogHistory(42);
        }
        if (loadPlanStep.isEnabled() == null) {
            loadPlanStep.setEnabled(true);
        }
        if (loadPlanStep.getTimeout() == null) {
            loadPlanStep.setTimeout(0);
        }
        if (loadPlanStep.getType().equals(LoadPlanStepType.SCENARIO)
                && (loadPlanStep.getScenarioVersion() == null || loadPlanStep.getScenarioVersion() == 0)) {
            loadPlanStep.setScenarioVersion(1);
        }
        if (loadPlanStep.getType().equals(LoadPlanStepType.SCENARIO)
                && (loadPlanStep.getPriority() == null || loadPlanStep.getPriority() == 0)) {
            loadPlanStep.setPriority(0);
        }
        if (loadPlanStep.getType().equals(LoadPlanStepType.PARALLEL)
                && loadPlanStep.getMaxErrorChildCount() == null) {
            loadPlanStep.setMaxErrorChildCount(0);
        }
        if (loadPlanStep.getExceptionBehavior() == null || loadPlanStep.getExceptionBehavior().equals(Exceptionbehavior.NONE)) {
            loadPlanStep.setExceptionBehavior(Exceptionbehavior.RUN_EXCEPTION_AND_RAISE_ERROR);
        }
        if (loadPlanStep.getType().equals(LoadPlanStepType.SERIAL)
                && (loadPlanStep.getRestartType() == null
                || loadPlanStep.getRestartType().equals(RestartType.NONE))) {
            if (this.jodiProperties.getPropertyKeys().contains(DEFAULT_RESTARTTYPE_SERIAL)) {
                loadPlanStep.setRestartType(mapFromStringToSerialRestartType(this.jodiProperties.getProperty(DEFAULT_RESTARTTYPE_SERIAL)));
            } else {
                loadPlanStep.setRestartType(RestartType.SERIAL_STEP_FROM_FAILURE);
            }
        }
        if (loadPlanStep.getType().equals(LoadPlanStepType.PARALLEL)
                && (loadPlanStep.getRestartType() == null
                || loadPlanStep.getRestartType().equals(RestartType.NONE))) {
            if (this.jodiProperties.getPropertyKeys().contains(DEFAULT_RESTARTTYPE_PARALLEL)) {
                loadPlanStep.setRestartType(mapFromStringToParallelRestartType(this.jodiProperties.getProperty(DEFAULT_RESTARTTYPE_PARALLEL)));
            } else {
                loadPlanStep.setRestartType(RestartType.PARALLEL_STEP_FAILED_CHILDREN);
            }
        }
        if (loadPlanStep.getType().equals(LoadPlanStepType.SCENARIO)
                && (loadPlanStep.getRestartType() == null
                || loadPlanStep.getRestartType().equals(RestartType.NONE))) {
            if (this.jodiProperties.getPropertyKeys().contains(DEFAULT_RESTARTTYPE_SCENARIO)) {
                loadPlanStep.setRestartType(mapFromStringToScenarioRestartType(this.jodiProperties.getProperty(DEFAULT_RESTARTTYPE_SCENARIO)));
            } else {
                loadPlanStep.setRestartType(RestartType.RUN_SCENARIO_FROM_STEP);
            }
        }
        if (loadPlanStep.getName() == null) {
            String name = stepCounter + ") ";
            if (loadPlanStep.getType().equals(LoadPlanStepType.SCENARIO)) {
                name += loadPlanStep.getScenario();
            }
            if (loadPlanStep.getType().equals(LoadPlanStepType.PARALLEL)) {
                name += "Parallel";
            }
            if (loadPlanStep.getType().equals(LoadPlanStepType.SERIAL)) {
                name += "Serial";
            }
            if (loadPlanStep.getType().equals(LoadPlanStepType.EXCEPTION)) {
                name += "Exception";
                // can't be moved elsewhere since then it is enriched.
                String nameLP = "Unknown Loadplan";
                String message = String.format("It is not recommended to omit the name xml tag for an exception for loadplan '%1$s'.", nameLP);
                logger.warn(message);
                errorWarningMessages.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.WARNINGS);
            }
            if (loadPlanStep.getType().equals(LoadPlanStepType.CASE)) {
                name += "Case with variable: " + loadPlanStep.getTestVariable();
            }
            if (loadPlanStep.getType().equals(LoadPlanStepType.CASEELSE)) {
                name += "Else";
            }
            if (loadPlanStep.getType().equals(LoadPlanStepType.CASEWHEN)) {
                name += "When: " + loadPlanStep.getOperator();
            }
            loadPlanStep.setName(name);
        }
    }

    private RestartType mapFromStringToScenarioRestartType(String property) {
        if (property.equals(RestartType.RUN_SCENARIO_FROM_STEP.name())) {
            return RestartType.RUN_SCENARIO_FROM_STEP;
        } else if (property.equals(RestartType.RUN_SCENARIO_FROM_TASK.name())) {
            return RestartType.RUN_SCENARIO_FROM_TASK;
        } else if (property.equals(RestartType.RUN_SCENARIO_NEW_SESSION.name())) {
            return RestartType.RUN_SCENARIO_NEW_SESSION;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private RestartType mapFromStringToParallelRestartType(String property) {
        if (property.equals(RestartType.PARALLEL_STEP_ALL_CHILDREN.name())) {
            return RestartType.PARALLEL_STEP_ALL_CHILDREN;
        } else if (property.equals(RestartType.PARALLEL_STEP_FAILED_CHILDREN.name())) {
            return RestartType.PARALLEL_STEP_FAILED_CHILDREN;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private RestartType mapFromStringToSerialRestartType(String property) {
        if (property.equals(RestartType.SERIAL_STEP_FROM_FAILURE.name())) {
            return RestartType.SERIAL_STEP_FROM_FAILURE;
        } else if (property.equals(RestartType.SERIAL_STEP_ALL_CHILDREN.name())) {
            return RestartType.SERIAL_STEP_ALL_CHILDREN;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void visitVariables(LoadPlanTree<LoadPlanStep> pInteralLoadPlan) {
    }

    @Override
    public void reset() {
        this.stepCounter = 0;
    }

}
