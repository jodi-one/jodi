package one.jodi.odi.loadplan;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStepType;
import one.jodi.etl.service.loadplan.internalmodel.Variable;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class OdiLoadPlanValidator implements OdiLoadPlanVisitor<Odiloadplanstep> {

    private final Logger logger = LogManager.getLogger(OdiLoadPlanValidator.class);
    @SuppressWarnings("rawtypes")
    private final OdiLoadPlanAccessStrategy odiLoadPlanAccessStrategy;
    private final ErrorWarningMessageJodi errorWarningMessages;
    // stateful result of validator, needs to be reset when validating new plan.
    private boolean valid = false;
    private String loadPlanName;
    private Set<String> uniqueNames = new HashSet<>();

    @SuppressWarnings("rawtypes")
    @Inject
    public OdiLoadPlanValidator(final OdiLoadPlanAccessStrategy odiLoadPlanAccessStrategy,
                                final ErrorWarningMessageJodi errorWarningMessages) {
        this.odiLoadPlanAccessStrategy = odiLoadPlanAccessStrategy;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public OdiLoadPlanVisitor<Odiloadplanstep> visit(OdiLoadPlanTree<Odiloadplanstep> tree) {
        return this;
    }

    @Override
    public Collection<Odiloadplanstep> getChildren() {
        return null;
    }

    @Override
    public void visit(Odiloadplanstep pLoadPlanStep) {
        if (pLoadPlanStep == null) {
            return;
        }
        validateSuperStep(pLoadPlanStep);
        if (pLoadPlanStep.getType().equals(LoadPlanStepType.CASE)) {
            validateStepCase(pLoadPlanStep);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.CASEWHEN)) {
            validateStepWhen(pLoadPlanStep);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.CASEELSE)) {
            validateStepElse(pLoadPlanStep);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.PARALLEL)) {
            validateStepParallel(pLoadPlanStep);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.SCENARIO)) {
            validateScenarioNode(pLoadPlanStep);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.SERIAL)) {
            validateSerialNode(pLoadPlanStep);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.EXCEPTION)) {
            validateExceptionNode(pLoadPlanStep);
        } else {
            throw new UnsupportedOperationException("Can't validate loadplanstep of type; " + pLoadPlanStep.getType().value());
        }
    }

    private void validateExceptionNode(Odiloadplanstep pLoadPlanStep) {
        // TODO Auto-generated method stub
    }

    private void validateStepElse(Odiloadplanstep pLoadPlanStep) {
        if (pLoadPlanStep.getExceptionStep() != null && pLoadPlanStep.getExceptionStep().length() > 0) {
            this.valid = false;
            String message = String.format("A case else statement shouldn't have a exceptions for loadplan '%1$s'.", this.loadPlanName);
            logger.error(message);
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
    }

    private void validateStepWhen(Odiloadplanstep pLoadPlanStep) {
        if (pLoadPlanStep.getExceptionStep() != null && pLoadPlanStep.getExceptionStep().length() > 0) {
            this.valid = false;
            String message = String.format("A case when statement shouldn't have a exceptions '%2$s' for loadplan '%1$s'.", this.loadPlanName, pLoadPlanStep.getExceptionStep());
            logger.error(message);
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
    }

    private void validateSuperStep(Odiloadplanstep pLoadPlanStep) {
        if (uniqueNames.contains(pLoadPlanStep.getName())) {
            // check for names of the steps to be unique.
            this.valid = false;
            String message = String.format("Name of loadplanstep must be unique it is not unique for step '%1$s' in loadplan '%2$s'.", pLoadPlanStep.getName(), this.loadPlanName);
            logger.error(message);
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
        uniqueNames.add(pLoadPlanStep.getName());
        if (pLoadPlanStep.getName() == null) {
            this.valid = false;
            String message = String.format("Name of loadplanstep can't be null.");
            logger.error(message);
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
        if (pLoadPlanStep.getRestartType() == null) {
            this.valid = false;
            String message = String.format("Restarttype of loadplanstep can't be null for loadstep '%1$s' of loadplan '%2$s'.", pLoadPlanStep.getName(), this.loadPlanName);
            logger.error(message);
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
        if (pLoadPlanStep.getVariables() != null && !pLoadPlanStep.getVariables().isEmpty()) {
            for (Variable v : pLoadPlanStep.getVariables()) {
                if (v.isRefresh() && v.getValue() != null) {
                    this.valid = false;
                    String message = String.format("The variable %s should be set to either refresh or set value not both in loadplan %s.", v.getName(), this.loadPlanName);
                    logger.error(message);
                    errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
                }
            }
        }
//		if(pLoadPlanStep.getType().equals(LoadPlanStepType.CASE)
//				|| pLoadPlanStep.getType().equals(LoadPlanStepType.CASEWHEN
//						|| pLoadPlanStep.getType().equals(LoadPlanStepType.CASEELSE
//				== null || pLoadPlanStep.getOrder() < 0){
//			this.valid = false;
//			String message = String.format("Order of loadplanstep must be greater than -1 for loadstep '%1$s' of loadplan '%2$s'.", pLoadPlanStep.getName(), this.loadPlanName);
//			logger.error(message);
//			errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
//		}
    }

    private void validateSerialNode(Odiloadplanstep pLoadPlanStep) {
    }

    private void validateScenarioNode(Odiloadplanstep pLoadPlanStep) {
        OdiScenario scenario = this.odiLoadPlanAccessStrategy.findScenario(pLoadPlanStep.getScenario(), pLoadPlanStep.getScenarioVersion());
        if (scenario == null) {
            this.valid = false;
            String message = String.format("Scenario '%1$s' not found please regenerate it for loadplan '%2$s'.", pLoadPlanStep.getScenario(), this.loadPlanName);
            logger.error(message);
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
    }

    private void validateStepParallel(Odiloadplanstep pLoadPlanStep) {
    }

    private void validateStepCase(Odiloadplanstep pLoadPlanStep) {
        if (pLoadPlanStep.getTestVariable() == null
                || pLoadPlanStep.getTestVariable().length() < 1) {
            this.valid = false;
            String message = String.format("A case statement should have a test variable for loadplan '%1$s'.", this.loadPlanName);
            logger.error(message);
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
    }

    public void reset(String loadPlanName) {
        this.loadPlanName = loadPlanName;
        this.valid = true;
        uniqueNames = new HashSet<>();
    }

    public boolean isValid() {
        return valid;
    }

}
