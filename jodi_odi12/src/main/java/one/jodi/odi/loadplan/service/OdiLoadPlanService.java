package one.jodi.odi.loadplan.service;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.lpmodel.*;
import one.jodi.etl.service.loadplan.LoadPlanService;
import one.jodi.etl.service.loadplan.internalmodel.*;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionstepType;
import one.jodi.etl.service.loadplan.internalmodel.Variable;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.ViolatebehaviourType;
import one.jodi.odi.loadplan.*;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.runtime.scenario.OdiScenarioFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class OdiLoadPlanService implements LoadPlanService {

    private static final Logger logger = LogManager.getLogger(OdiLoadPlanService.class);
    @SuppressWarnings("rawtypes")
    private final OdiLoadPlanAccessStrategy odiLoadPlanAccessStrategy;
    private final JodiProperties jodiProperties;
    private final OdiLoadPlanValidationService loadPlanValidationService;
    private final OdiLoadPlanTransformationService odiLoadPlanTransformationService;

    @SuppressWarnings("rawtypes")
    public OdiLoadPlanService(final OdiLoadPlanAccessStrategy pOdiLoadPlanAccessStrategy,
                              final JodiProperties properties, final OdiLoadPlanValidationService loadPlanValidationService,
                              final OdiLoadPlanTransformationService odiLoadPlanTransformationService,
                              final ErrorWarningMessageJodi errorWarningMessageJodi) {
        this.odiLoadPlanAccessStrategy = pOdiLoadPlanAccessStrategy;
        this.jodiProperties = properties;
        this.loadPlanValidationService = loadPlanValidationService;
        this.odiLoadPlanTransformationService = odiLoadPlanTransformationService;
    }

    /**
     * Transforms an external loadplan into an internal one.
     */
    @Override
    public List<LoadPlanTree<LoadPlanStep>> transform(Loadplan pExternalLoadplan) {

        final String loadPlanName = pExternalLoadplan.getName();
        final String folderName = pExternalLoadplan.getFolder();
        final Integer keeplogHistory = pExternalLoadplan.getKeeploghistory() == null ? 0 : pExternalLoadplan.getKeeploghistory().intValue();
        LogsessionType logsessionType;
        if (pExternalLoadplan.getLogsessions() == null) {
            logsessionType = LogsessionType.ALWAYS;
        } else if (pExternalLoadplan.getLogsessions().equals(LogsessionsType.ALWAYS)) {
            logsessionType = LogsessionType.ALWAYS;
        } else if (pExternalLoadplan.getLogsessions().equals(LogsessionsType.ERRORS)) {
            logsessionType = LogsessionType.ERRORS;
        } else if (pExternalLoadplan.getLogsessions().equals(LogsessionsType.NEVER)) {
            logsessionType = LogsessionType.NEVER;
        } else {
            String message = "In loadplan " + pExternalLoadplan.getName() + " the logsessiontype is not found.";
            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
        final LogsessionstepType logsessionstepType;
        if (pExternalLoadplan.getLogsessionstep() == null) {
            logsessionstepType = LogsessionstepType.ERRORS;
        } else if (pExternalLoadplan.getLogsessionstep()
                .equals(one.jodi.core.lpmodel.LogsessionstepType.BYSCENARIOSETTINGS)) {
            logsessionstepType = LogsessionstepType.BYSCENARIOSETTINGS;
        } else if (pExternalLoadplan.getLogsessionstep().equals(one.jodi.core.lpmodel.LogsessionstepType.ERRORS)) {
            logsessionstepType = LogsessionstepType.ERRORS;
        } else if (pExternalLoadplan.getLogsessionstep().equals(one.jodi.core.lpmodel.LogsessionstepType.NEVER)) {
            logsessionstepType = LogsessionstepType.NEVER;
        } else {
            String message = "In loadplan " + pExternalLoadplan.getName() + " the LogsessionstepType is not found.";
            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
        final Integer sessiontaskloglevel = pExternalLoadplan.getSessiontaskloglevel() == null ? 0 : pExternalLoadplan.getSessiontaskloglevel().intValue();
        final String keywords = pExternalLoadplan.getKeywords();
        final Boolean limitconccurentexecutions = pExternalLoadplan.isLimitconcurrentexecutions() == null ? Boolean.valueOf(true) : pExternalLoadplan.isLimitconcurrentexecutions();
        ViolatebehaviourType violatebehaviourType;
        if (pExternalLoadplan.getViolateBehavior() == null) {
            violatebehaviourType = ViolatebehaviourType.WAIT_TO_EXECUTE;
        } else if (pExternalLoadplan.getViolateBehavior()
                .equals(ViolatebehaviorType.WAIT_TO_EXECUTE)) {
            violatebehaviourType = ViolatebehaviourType.WAIT_TO_EXECUTE;
        } else if (pExternalLoadplan.getViolateBehavior()
                .equals(ViolatebehaviorType.RAISE_EXECUTION_ERROR)) {
            violatebehaviourType = ViolatebehaviourType.RAISE_EXECUTION_ERROR;
        } else {
            String message = "In loadplan " + pExternalLoadplan.getName() + " the ViolatebehaviourType is not found.";
            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
        final BigInteger waitpollinginterval = pExternalLoadplan.getWaitpollinginterval();
        final String description = pExternalLoadplan.getDescription();
        final Number numberOfConcurrentexecutions = pExternalLoadplan.getNumberOfConcurrentexecutions();
        LoadPlanDetails details = new LoadPlanDetails(loadPlanName, folderName, keeplogHistory, logsessionType,
                logsessionstepType, sessiontaskloglevel, keywords, limitconccurentexecutions, numberOfConcurrentexecutions,
                violatebehaviourType, waitpollinginterval, description);

        List<LoadPlanTree<LoadPlanStep>> exceptionTreesAndRootTree =
                new ArrayList<>();

        if (pExternalLoadplan.getExceptions() != null
                && !pExternalLoadplan.getExceptions().getException().isEmpty()) {
            for (Loadplanstepexceptiontype exception : pExternalLoadplan
                    .getExceptions().getException()) {
                LoadPlanStep rootNode = transform(exception, null);
                LoadPlanTree<LoadPlanStep> internalLoadPlanTree =
                        new LoadPlanTree<>(rootNode,
                                details);
                transformChildren(exception.getChildren(),
                        internalLoadPlanTree);
                exceptionTreesAndRootTree.add(internalLoadPlanTree);
            }
        }
        LoadPlanStep rootNode = transform(pExternalLoadplan.getSerial(), null);
        LoadPlanTree<LoadPlanStep> internalLoadPlanTree =
                new LoadPlanTree<>(rootNode,
                        details);
        transformChildren(pExternalLoadplan.getSerial().getChildren(), internalLoadPlanTree);
        exceptionTreesAndRootTree.add(internalLoadPlanTree);

        return exceptionTreesAndRootTree;
    }

    private List<LoadPlanStep> transformChildren(
            Children pExternalLoadPlanStepChildren,
            LoadPlanTree<LoadPlanStep> pParent) {
        List<LoadPlanStep> listloadPlanSteps = new ArrayList<>();
        for (JAXBElement<? extends Loadplansteptype> externalChild : pExternalLoadPlanStepChildren.getLoadplancorestep()) {
            LoadPlanStep loadPlanStep = transform(externalChild.getValue(), pParent);
            listloadPlanSteps.add(loadPlanStep);
        }
        return listloadPlanSteps;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public LoadPlanStep transform(Loadplansteptype pExternalLoadPlanStep, final LoadPlanTree<LoadPlanStep> pRoot) {
        String name = pExternalLoadPlanStep.getName();
        Boolean enabled = null;
        String exceptionStep = null;
        Exceptionbehavior exceptionBehavior;
        List<Variable> variables = null;
        Integer timeout = null;
        RestartType restartType;
        LoadPlanStepType type;
        Integer priority = null;
        String keywords;
        Integer maxErrorChildCount = null;
        String scenario = null;
        Integer scenarioVersion = null;
        String testVariable = null;
        String operator = null;
        Object value = null;
        if (pExternalLoadPlanStep instanceof Loadplanstepserialtype) {
            restartType = mapFromRestartTypeSerial(((Loadplanstepserialtype) pExternalLoadPlanStep).getRestartType());
            type = LoadPlanStepType.SERIAL;
            keywords = ((Loadplanstepserialtype) pExternalLoadPlanStep).getKeywords();
            enabled = ((Loadplanstepserialtype) pExternalLoadPlanStep).isEnabled();
            exceptionStep = ((Loadplanstepserialtype) pExternalLoadPlanStep).getExceptionName();
            exceptionBehavior = ((Loadplanstepserialtype) pExternalLoadPlanStep).getExceptionbehavior();
            variables = transform(((Loadplanstepserialtype) pExternalLoadPlanStep).getVariables());
            timeout = ((Loadplanstepserialtype) pExternalLoadPlanStep).getTimeout();
        } else if (pExternalLoadPlanStep instanceof Loadplanstepscenariotype) {
            restartType = mapFromRestartTypeScenarioType(
                    ((Loadplanstepscenariotype) pExternalLoadPlanStep).getRestartType());
            type = LoadPlanStepType.SCENARIO;
            priority = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getPriority();
            keywords = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getKeywords();
            enabled = ((Loadplanstepscenariotype) pExternalLoadPlanStep).isEnabled();
            exceptionStep = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getExceptionName();
            exceptionBehavior = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getExceptionbehavior();
            variables = transform(((Loadplanstepscenariotype) pExternalLoadPlanStep).getVariables());
            timeout = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getTimeout();
            scenario = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getScenario();
            scenarioVersion = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getVersion() == null ? null : ((Loadplanstepscenariotype) pExternalLoadPlanStep).getVersion().intValue();
            priority = ((Loadplanstepscenariotype) pExternalLoadPlanStep).getPriority();
        } else if (pExternalLoadPlanStep instanceof Loadplanstepparalleltype) {
            restartType = mapFromRestartTypeParallelType(
                    ((Loadplanstepparalleltype) pExternalLoadPlanStep).getRestartType());
            type = LoadPlanStepType.PARALLEL;
            keywords = ((Loadplanstepparalleltype) pExternalLoadPlanStep).getKeywords();
            maxErrorChildCount = ((Loadplanstepparalleltype) pExternalLoadPlanStep).getMaxerrorchildcount();
            enabled = ((Loadplanstepparalleltype) pExternalLoadPlanStep).isEnabled();
            exceptionStep = ((Loadplanstepparalleltype) pExternalLoadPlanStep).getExceptionName();
            exceptionBehavior = ((Loadplanstepparalleltype) pExternalLoadPlanStep).getExceptionbehavior();
            variables = transform(((Loadplanstepparalleltype) pExternalLoadPlanStep).getVariables());
            timeout = ((Loadplanstepparalleltype) pExternalLoadPlanStep).getTimeout();
        } else if (pExternalLoadPlanStep instanceof Loadplanstepcasetype) {
            restartType = RestartType.NONE;
            type = LoadPlanStepType.CASE;
            keywords = ((Loadplanstepcasetype) pExternalLoadPlanStep).getKeywords();
            enabled = ((Loadplanstepcasetype) pExternalLoadPlanStep).isEnabled();
            exceptionStep = ((Loadplanstepcasetype) pExternalLoadPlanStep).getExceptionName();
            exceptionBehavior = ((Loadplanstepcasetype) pExternalLoadPlanStep).getExceptionbehavior();
            variables = transform(((Loadplanstepcasetype) pExternalLoadPlanStep).getVariables());
            timeout = ((Loadplanstepcasetype) pExternalLoadPlanStep).getTimeout();
            testVariable = ((Loadplanstepcasetype) pExternalLoadPlanStep).getTestVariable();
        } else if (pExternalLoadPlanStep instanceof Loadplanstepexceptiontype) {
            restartType = RestartType.NONE;
            type = LoadPlanStepType.EXCEPTION;
            keywords = ((Loadplanstepexceptiontype) pExternalLoadPlanStep).getKeywords();
            enabled = ((Loadplanstepexceptiontype) pExternalLoadPlanStep).isEnabled();
            exceptionBehavior = Exceptionbehavior.NONE;
        } else {
            String message = "The ExternalLoadplanstep is not found.";
            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
        // exception handling
        LoadPlanStep internalLoadPlanStep = new LoadPlanStep(pRoot, name, type, enabled, keywords,
                // exception handling
                restartType, timeout, exceptionStep, exceptionBehavior, priority, variables, maxErrorChildCount,
                scenario, scenarioVersion, testVariable, operator, value);
        LoadPlanTree<LoadPlanStep> rootTree = null;
        if (pRoot != null)
            rootTree = pRoot.child(internalLoadPlanStep);
        recurse(pExternalLoadPlanStep, rootTree);
        return internalLoadPlanStep;
    }

    private void recurse(Loadplansteptype pExternalLoadPlanStep, LoadPlanTree<LoadPlanStep> rootTree) {
        if (pExternalLoadPlanStep instanceof Loadplanstepparalleltype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepparalleltype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepserialtype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepserialtype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepcasetype) {
            for (Loadplanstepwhentype child : ((Loadplanstepcasetype) pExternalLoadPlanStep)
                    .getWhen()) {
                transform(child, rootTree);
            }
            if (((Loadplanstepcasetype) pExternalLoadPlanStep).getElse() != null) {
                transform(((Loadplanstepcasetype) pExternalLoadPlanStep)
                        .getElse(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepwhentype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepwhentype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepelsetype) {
            for (JAXBElement<? extends Loadplansteptype> child : ((Loadplanstepelsetype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepexceptiontype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepexceptiontype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
    }

    private void recurse(Loadplanbranchesteptype pExternalLoadPlanStep, LoadPlanTree<LoadPlanStep> rootTree) {
        if (pExternalLoadPlanStep instanceof Loadplanstepparalleltype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepparalleltype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepserialtype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepserialtype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepcasetype) {
            for (Loadplanstepwhentype child : ((Loadplanstepcasetype) pExternalLoadPlanStep)
                    .getWhen()) {
                transform(child, rootTree);
            }
            if (((Loadplanstepcasetype) pExternalLoadPlanStep)
                    .getElse() != null) {
                transform(((Loadplanstepcasetype) pExternalLoadPlanStep)
                        .getElse(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepwhentype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepwhentype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepelsetype) {
            for (JAXBElement<? extends Loadplansteptype> child : ((Loadplanstepelsetype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
        if (pExternalLoadPlanStep instanceof Loadplanstepexceptiontype) {
            for (JAXBElement<? extends Loadplancoresteptype> child : ((Loadplanstepexceptiontype) pExternalLoadPlanStep)
                    .getChildren().getLoadplancorestep()) {
                transform(child.getValue(), rootTree);
            }
        }
    }

    private LoadPlanStep transform(Loadplanbranchesteptype pExternalLoadPlanStep, LoadPlanTree<LoadPlanStep> pRoot) {
        String name = pExternalLoadPlanStep.getName();
        Boolean enabled = null;
        String exceptionStep = null;
        Exceptionbehavior exceptionBehavior;
        List<Variable> variables = null;
        Integer timeout = null;
        RestartType restartType;
        LoadPlanStepType type;
        Integer priority = 0;
        String keywords = null;
        Integer maxErrorChildCount = null;
        String scenario = null;
        Integer scenarioVersion = null;
        String testVariable = null;
        String operator = null;
        Object value = null;
        if (pExternalLoadPlanStep instanceof Loadplanstepwhentype) {
            restartType = RestartType.NONE;
            type = LoadPlanStepType.CASEWHEN;
            exceptionBehavior = Exceptionbehavior.NONE;
            operator = ((Loadplanstepwhentype) pExternalLoadPlanStep).getOperator();
            value = ((Loadplanstepwhentype) pExternalLoadPlanStep).getValue();
        } else if (pExternalLoadPlanStep instanceof Loadplanstepelsetype) {
            restartType = RestartType.NONE;
            type = LoadPlanStepType.CASEELSE;
            exceptionBehavior = Exceptionbehavior.NONE;
        } else {
            throw new UnsupportedOperationException();
        }
        // exception handling
        LoadPlanStep internalLoadPlanStep = new LoadPlanStep(pRoot, name, type, enabled, keywords,
                // exception handling
                restartType, timeout, exceptionStep, exceptionBehavior, priority, variables, maxErrorChildCount,
                scenario, scenarioVersion, testVariable, operator, value);
        LoadPlanTree<LoadPlanStep> rootTree = null;
        if (pRoot != null)
            rootTree = pRoot.child(internalLoadPlanStep);
        recurse(pExternalLoadPlanStep, rootTree);
        return internalLoadPlanStep;
    }

    private RestartType mapFromRestartTypeParallelType(Restarttypeparallel pRestartType) {
        if (pRestartType == null) {
            return RestartType.NONE;
        } else if (pRestartType.equals(Restarttypeparallel.ALL_CHILDREN)) {
            return RestartType.PARALLEL_STEP_ALL_CHILDREN;
        } else if (pRestartType.equals(Restarttypeparallel.FAILED_CHILDREN)) {
            return RestartType.PARALLEL_STEP_FAILED_CHILDREN;
        } else {
            String message = String.format("The Restarttypeparallel value was not found for '%1$s'.", pRestartType.value());
            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
    }

    private RestartType mapFromRestartTypeScenarioType(Restarttypescenario pRestartType) {
        if (pRestartType == null) {
            return RestartType.NONE;
        } else if (pRestartType.equals(Restarttypescenario.FROM_STEP)) {
            return RestartType.RUN_SCENARIO_FROM_STEP;
        } else if (pRestartType.equals(Restarttypescenario.FROM_TASK)) {
            return RestartType.RUN_SCENARIO_FROM_TASK;
        } else if (pRestartType.equals(Restarttypescenario.NEW_SESSION)) {
            return RestartType.RUN_SCENARIO_NEW_SESSION;
        } else {
            String message = String.format("The Restarttypescenario value was not found for '%1$s'.", pRestartType.value());
            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
    }

    private RestartType mapFromRestartTypeSerial(Restarttypeserial pRestartType) {
        if (pRestartType == null) {
            return RestartType.NONE;
        } else if (pRestartType.equals(Restarttypeserial.ALL_CHILDREN)) {
            return RestartType.SERIAL_STEP_ALL_CHILDREN;
        } else if (pRestartType.equals(Restarttypeserial.FROM_FAILURE)) {
            return RestartType.SERIAL_STEP_FROM_FAILURE;
        } else {
            String message = String.format("The Restarttypeserial value was not found for '%1$s'.", pRestartType.value());
            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
    }

    private List<Variable> transform(Variables pVariables) {
        List<Variable> internalVariables = new ArrayList<>();
        if (pVariables != null && pVariables.getVariable() != null) {
            internalVariables
                    .addAll(pVariables.getVariable().stream().map(this::transform)
                            .collect(Collectors.toList()));
        }
        return internalVariables;
    }

    public Variable transform(one.jodi.core.lpmodel.Variable pVariable) {
        return new Variable(pVariable.getName(), pVariable.isRefresh() == null ? false : pVariable.isRefresh(), pVariable.getValue());
    }

    /**
     * Builds an OdiPloadPlan from an internal model, The java visitor pattern
     * is used to visit all nodes of the model, and attach them to the parent.
     */
    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void build(List<LoadPlanTree<LoadPlanStep>> pInteralLoadPlans, final LoadPlanDetails loadPlanDetails) {
        assert loadPlanDetails != null;
        // we get a new instance so we make sure there is no state when processing new loadplan,
        // in a sense this is the reverse of getInstance() which returns a singleton,
        // this returns a new instance getNewInstance();
        // if we don't do this, we get an error saying that a variable step much be attached to the same loadplan,
        this.loadPlanValidationService.reset(loadPlanDetails.getLoadPlanName());
        logger.info("Building loadplan: "
                + loadPlanDetails.getLoadPlanName() + " size: "
                + pInteralLoadPlans.size());
        try {
            final boolean existedAlready;
            OdiLoadPlan odiLoadPlan = (OdiLoadPlan) odiLoadPlanAccessStrategy
                    .findLoadPlanByName(loadPlanDetails.getLoadPlanName());
            if (odiLoadPlan == null) {
                existedAlready = false;
                OdiScenarioFolder scenarioFolder = odiLoadPlanAccessStrategy.findScenarioFolder(loadPlanDetails.getFolderName());
                if (scenarioFolder == null) {
                    scenarioFolder = odiLoadPlanAccessStrategy.createScenarioFolder(loadPlanDetails.getFolderName());
                }
                odiLoadPlan = (OdiLoadPlan) odiLoadPlanAccessStrategy.createLoadPlan(loadPlanDetails.getLoadPlanName(),
                        loadPlanDetails.getFolderName());
            } else {
                // there is already a loadplan; so we remove all steps, and
                // reuse it.
                // this keeps the UID of the loadplan intact.
                odiLoadPlanAccessStrategy.removeAllSteps(odiLoadPlan);
                existedAlready = true;
            }

            List<OdiLoadPlanTree<Odiloadplanstep>> podiInteralLoadPlan = transform(pInteralLoadPlans, odiLoadPlan, loadPlanDetails);
            odiLoadPlanTransformationService.initialize(odiLoadPlan, loadPlanDetails, this.jodiProperties.getProjectCode());
            // tranform from internal model to odi model
            for (OdiLoadPlanTree<Odiloadplanstep> pInteralLoadPlan : podiInteralLoadPlan) {
//				OdiLoadPlanVisitor<Odiloadplanstep> visitor = new LevelPrintingVisitor();
//				pInteralLoadPlan.accept(visitor);
//				visitor.visit(pInteralLoadPlan);
                this.loadPlanValidationService.reset(loadPlanDetails.getLoadPlanName());
                //
                if (!this.loadPlanValidationService.validate(loadPlanDetails)) {
                    throw new OdiLoadPlanValidationException(
                            "The loadplandetails: " + loadPlanDetails.getLoadPlanName()
                                    + " transformed into internal model is not valid.");
                }
                if (!this.loadPlanValidationService.validate(pInteralLoadPlan)) {
                    throw new OdiLoadPlanValidationException(
                            "The loadplan: " + loadPlanDetails.getLoadPlanName()
                                    + " transformed into internal model is not valid.");
                }
                if (odiLoadPlanTransformationService == null) {
                    throw new OdiLoadPlanValidationException("Loadplanvistor may not be null.");
                }
                // for debug
                // create or find loadplan
                // for findbugs
                assert (odiLoadPlanTransformationService != null);
                pInteralLoadPlan.accept(this.odiLoadPlanTransformationService);
            }
            if (existedAlready) {
                odiLoadPlanAccessStrategy.merge(odiLoadPlan);
            } else {
                odiLoadPlanAccessStrategy.persist(odiLoadPlan);
            }
        } catch (Exception e) {
            logger.error(e);
            odiLoadPlanAccessStrategy.rollback();
            throw new OdiLoadPlanServiceException(e);
        }
        logger.info(String.format("Finished building loadplan: '%1$s' in folder '%2$s'",
                loadPlanDetails.getLoadPlanName(), loadPlanDetails.getFolderName()));
    }

    private List<OdiLoadPlanTree<Odiloadplanstep>> transform(
            final List<LoadPlanTree<LoadPlanStep>> pInteralLoadPlans,
            final OdiLoadPlan odiLoadPlan,
            final LoadPlanDetails loadPlanDetails) {
        List<OdiLoadPlanTree<Odiloadplanstep>> odiLoadPlanTrees = new ArrayList<OdiLoadPlanTree<Odiloadplanstep>>();
        for (LoadPlanTree<LoadPlanStep> loadPlanTree : pInteralLoadPlans) {
            Odiloadplanstep rootelelement = new Odiloadplanstep(null,
                    loadPlanTree.getRoot().getName(),
                    loadPlanTree.getRoot().getType(),
                    loadPlanTree.getRoot().isEnabled(),
                    loadPlanTree.getRoot().getKeywords(),
                    loadPlanTree.getRoot().getRestartType(),
                    loadPlanTree.getRoot().getTimeout(),
                    loadPlanTree.getRoot().getExceptionStep(),
                    loadPlanTree.getRoot().getExceptionBehavior(),
                    loadPlanTree.getRoot().getPriority(),
                    loadPlanTree.getRoot().getVariables(),
                    loadPlanTree.getRoot().getMaxErrorChildCount(),
                    loadPlanTree.getRoot().getScenario(),
                    loadPlanTree.getRoot().getScenarioVersion(),
                    loadPlanTree.getRoot().getTestVariable(),
                    loadPlanTree.getRoot().getOperator(),
                    loadPlanTree.getRoot().getValue());
            OdiLoadPlanTree<Odiloadplanstep> root = new OdiLoadPlanTree<Odiloadplanstep>(rootelelement, odiLoadPlan, loadPlanDetails, this.jodiProperties.getProjectCode());
            //logger.info("root variable: "+ root.getRoot().getVariables().size());
            transform(loadPlanTree, root, loadPlanDetails, this.jodiProperties.getProjectCode(), odiLoadPlan);
            odiLoadPlanTrees.add(root);
        }
        return odiLoadPlanTrees;
    }

    private void transform(final LoadPlanTree<LoadPlanStep> loadPlanTree,
                           OdiLoadPlanTree<Odiloadplanstep> parent,
                           final LoadPlanDetails loadPlanDetails,
                           final String projectCode,
                           final OdiLoadPlan odiLoadPlan) {
        for (LoadPlanTree<LoadPlanStep> child : loadPlanTree.getChildren()) {
            Odiloadplanstep olps = new Odiloadplanstep(parent,
                    child.getRoot().getName(),
                    child.getRoot().getType(),
                    child.getRoot().isEnabled(),
                    child.getRoot().getKeywords(),
                    child.getRoot().getRestartType(),
                    child.getRoot().getTimeout(),
                    child.getRoot().getExceptionStep(),
                    child.getRoot().getExceptionBehavior(),
                    child.getRoot().getPriority(),
                    child.getRoot().getVariables(),
                    child.getRoot().getMaxErrorChildCount(),
                    child.getRoot().getScenario(),
                    child.getRoot().getScenarioVersion(),
                    child.getRoot().getTestVariable(),
                    child.getRoot().getOperator(),
                    child.getRoot().getValue());
            OdiLoadPlanTree<Odiloadplanstep> olpt = new OdiLoadPlanTree<Odiloadplanstep>(olps, odiLoadPlan, loadPlanDetails, projectCode);
            parent.child(olpt);
            transform(child, olpt, loadPlanDetails, projectCode, odiLoadPlan);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void deleteLoadPlans() {
        odiLoadPlanAccessStrategy.deleteLoadPlans();
    }
}