package one.jodi.odi12.service;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.ViolatebehaviourType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStepType;
import one.jodi.odi.loadplan.OdiLoadPlanAccessStrategy;
import one.jodi.odi.loadplan.OdiLoadPlanTransformationService;
import one.jodi.odi.loadplan.Odiloadplanstep;
import oracle.odi.domain.runtime.common.CecBehavior;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStep;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepParallel;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepSerial;
import oracle.odi.domain.runtime.scenario.OdiScenarioFolder;

public class Odi12LoadPlanTransformationService extends OdiLoadPlanTransformationService {

    @Inject
    public Odi12LoadPlanTransformationService(
            final OdiLoadPlanAccessStrategy loadPlanAccessStrategy,
            final ErrorWarningMessageJodi errorWarningMessages) {
        super(loadPlanAccessStrategy, errorWarningMessages);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiLoadPlanStepSerial populateSerialNode(final Odiloadplanstep pLoadPlanStep,
                                                    final OdiLoadPlanStep pParent,
                                                    final OdiLoadPlan pOdiLoadPlan,
                                                    final LoadPlanDetails loadPlanDetails,
                                                    final String projectCode) {
        final OdiLoadPlanStepSerial aSerialNode;
        if (pParent == null) {
            if (pLoadPlanStep.getType().equals(LoadPlanStepType.SERIAL)) {
                // this is the root node
                assert (pOdiLoadPlan != null);
                aSerialNode = pOdiLoadPlan.getRootStep();
                assert (loadPlanDetails != null);
                pOdiLoadPlan.setName(loadPlanDetails.getLoadPlanName());
                OdiScenarioFolder pScenarioFolder =
                        getLoadPlanAccessStrategy().findScenarioFolder(loadPlanDetails.getFolderName());
                pOdiLoadPlan.setScenarioFolder(pScenarioFolder);
                pOdiLoadPlan.setLogHistoryRetainedNbDays(loadPlanDetails.getKeeplogHistory());
                pOdiLoadPlan.setSessionLogsDefaultBehaviorForScenarios(
                        super.mapFrom(loadPlanDetails.getLogsessionType()));
                pOdiLoadPlan.setSessionLogsDefaultBehaviorForScenarioSteps(
                        super.mapFrom(loadPlanDetails.getLogsessionstepType()));
                pOdiLoadPlan.setTaskLogLevel(loadPlanDetails.getSessiontaskloglevel());
                pOdiLoadPlan.setSessionKeywordsAsString(loadPlanDetails.getKeywords());
                pOdiLoadPlan.setCecLprBehavior(mapFrom(loadPlanDetails.getViolatebehaviourType()));
                pOdiLoadPlan.setCecLprPollIntv(loadPlanDetails.getWaitpollinginterval());
                pOdiLoadPlan.setDescription(loadPlanDetails.getDescription());
                /*
                 * The corresponding UI field for the API corresponds to the
                 * "Limit Concurrent Execution" checkbox in Studio UI. Currently a
                 * positive value, e.g. 1, enables limiting concurrent executions such
                 * that no more than 1 execution is allowed at a time. Although the API
                 * accepts any integer values, a positive value is sufficient to enable
                 * the feature and the API allows future enhancements of limiting N
                 * number of concurrent executions at a time. The spec describes the
                 * field as follows:
                 * http://review.us.oracle.com/review2/Review.html#reviewId=178705;scope
                 * =document;status=open,fixed "Indicates the maximum number of allowed
                 * concurrent executions associated with the same object. If its value
                 * is null or <= 0, concurrent execution control is not enabled and
                 * there is no limit on concurrent executions, i.e. any number of
                 * concurrent executions of the object is allowed. If the column is set
                 * to a positive value, in 12.1.3, the limit for concurrent executions
                 * is always 1, meaning only one runnable execution instance is allowed
                 * at one time."
                 *
                 */
                if (!loadPlanDetails.isLimitconcurrentexecutions()) {
                    pOdiLoadPlan.setMaxCecLpr(0);
                } else {
                    pOdiLoadPlan.setMaxCecLpr(loadPlanDetails.getNumberOfConcurrentexecutions());
                }
            } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.EXCEPTION)) {
                throw new UnsupportedOperationException();
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (pParent instanceof OdiLoadPlanStepSerial) {
            aSerialNode =
                    ((OdiLoadPlanStepSerial) pParent).addStepSerial(pLoadPlanStep.getName());
        } else if (pParent instanceof OdiLoadPlanStepParallel) {
            aSerialNode =
                    ((OdiLoadPlanStepParallel) pParent).addStepSerial(pLoadPlanStep.getName());
        } else {
            throw new UnsupportedOperationException();
        }
        aSerialNode.setEnabled(pLoadPlanStep.isEnabled());
        // this.odiLoadPlan.getRootStep().setException(pException);
        // this.odiLoadPlan.getRootStep().setExceptionBehavior(exceptionBehavior);
        aSerialNode.setName(pLoadPlanStep.getName());
        aSerialNode.setRestartType(super.mapFromRestartTypeSerial(pLoadPlanStep.getRestartType()));
        aSerialNode.setSessionKeywordsAsString(pLoadPlanStep.getKeywords());
        aSerialNode.setTimeout(pLoadPlanStep.getTimeout());
        if (pLoadPlanStep.getExceptionStep() != null &&
                pLoadPlanStep.getExceptionStep().length() > 0) {
            aSerialNode.setException(super.findExceptionByName(pOdiLoadPlan,
                    pLoadPlanStep.getExceptionStep()));
        }
        return aSerialNode;
    }

    private CecBehavior mapFrom(ViolatebehaviourType violatebehaviourType) {
        final CecBehavior cecBehavior;
        if (violatebehaviourType.equals(ViolatebehaviourType.WAIT_TO_EXECUTE)) {
            cecBehavior = CecBehavior.WAIT;
        } else if (violatebehaviourType.equals(ViolatebehaviourType.RAISE_EXECUTION_ERROR)) {
            cecBehavior = CecBehavior.ERROR;
        } else {
            throw new UnsupportedOperationException();
        }
        return cecBehavior;
    }
}
