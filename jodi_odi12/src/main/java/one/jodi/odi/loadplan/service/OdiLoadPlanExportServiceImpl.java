package one.jodi.odi.loadplan.service;

import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.lpmodel.Exceptionbehavior;
import one.jodi.core.lpmodel.impl.LoadplanImpl;
import one.jodi.etl.service.loadplan.LoadPlanExportService;
import one.jodi.etl.service.loadplan.internalmodel.*;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionstepType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.ViolatebehaviourType;
import one.jodi.odi.loadplan.*;
import one.jodi.odi.loadplan.internal.LevelPrintingVisitor;
import one.jodi.odi.loadplan.internal.ReverseEngingeerVisitor;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.runtime.loadplan.*;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan.SessionLogsBehavior;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan.SessionStepLogsBehavior;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStep.ExceptionBehavior;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * LoadPlanImportService service that builds internal model LoadPlanTree
 * LoadPlanStep from an OdiLoadPlan.
 * T = LoadPlanStep
 *
 * @param <T> loadplanstep
 */
public abstract class OdiLoadPlanExportServiceImpl<B extends IOdiEntity, T> implements LoadPlanExportService {
    private static final Logger logger = LogManager.getLogger(OdiLoadPlanExportServiceImpl.class);
    private final OdiLoadPlanAccessStrategy<OdiLoadPlan, B> odiLoadPlanAccessStrategy;
    private final String metaDataDirectory;
    private final OdiLoadPlanValidationService loadPlanValidationService;
    private final JodiProperties jodiProperties;

    public OdiLoadPlanExportServiceImpl(final OdiLoadPlanAccessStrategy<OdiLoadPlan, B> odiAccessStrategy,
                                        final String metadataFolder,
                                        final OdiLoadPlanValidationService loadPlanValidationService,
                                        final JodiProperties jodiProperties) {
        this.odiLoadPlanAccessStrategy = odiAccessStrategy;
        this.loadPlanValidationService = loadPlanValidationService;
        this.metaDataDirectory = metadataFolder + File.separator +
                JodiConstants.XMLLOADPLANLOC;
        this.jodiProperties = jodiProperties;
        if (!new File(this.metaDataDirectory).exists()) {
            if (!new File(this.metaDataDirectory).mkdirs()) {
                String message = "Couldn't create directories: " + this.metaDataDirectory;
                logger.error(message);
                throw new OdiLoadPlanServiceException(message);
            }
        }
    }

    /**
     * ImportLoadPlans builds an internal model from an OdiLoadPlan,
     * then it is transformed into external model (xml file specifications)
     * then it is spooled to a file.
     */
    @Override
    public void exportLoadPlans(final boolean useDefaultNames) {

        for (OdiLoadPlan loadPlan : odiLoadPlanAccessStrategy.findAllLoadPlans()) {
            logger.info("Processing loadplan: " + loadPlan.getName());
            buildPlan(loadPlan, useDefaultNames);
        }
    }

    private void buildPlan(OdiLoadPlan odiLoadPlan, final boolean useDefaultNames) {
        // build
        List<OdiLoadPlanTree<Odiloadplanstep>> trees = buildTree(odiLoadPlan, useDefaultNames);
        /**
         * Reverse eningeer it.
         */
        LoadPlanDetails loadPlanDetails = getLoadPlanDetailsFromOdi(odiLoadPlan);

        ReverseEngingeerVisitor re = new ReverseEngingeerVisitor(
                new File(this.metaDataDirectory, odiLoadPlan.getName() + ".xml"), new LoadplanImpl(),
                loadPlanDetails, 1, this.jodiProperties);
        for (OdiLoadPlanTree<Odiloadplanstep> tree : trees) {
            tree.accept(re);
            re.visit(tree);
        }
        re.write();
    }

    private List<OdiLoadPlanTree<LoadPlanStep>> transformTrees(List<LoadPlanTree<LoadPlanStep>> trees,
                                                               LoadPlanDetails loadPlanDetails, OdiLoadPlan odiLoadPlan, String projectCode) {
        List<OdiLoadPlanTree<LoadPlanStep>> odiTrees = new ArrayList<OdiLoadPlanTree<LoadPlanStep>>();
        for (LoadPlanTree<LoadPlanStep> tree : trees) {
            odiTrees.add(transform(tree, loadPlanDetails, odiLoadPlan, projectCode));
        }
        return odiTrees;
    }

    private OdiLoadPlanTree<LoadPlanStep> transform(LoadPlanTree<LoadPlanStep> tree,
                                                    LoadPlanDetails loadPlanDetails, OdiLoadPlan odiLoadPlan, String projectCode) {
        return new OdiLoadPlanTree<LoadPlanStep>(tree.getRoot(), odiLoadPlan, loadPlanDetails, projectCode);
    }

    private LoadPlanDetails getLoadPlanDetailsFromOdi(OdiLoadPlan odiLoadPlan) {
        final String pLoadPlanName = odiLoadPlan.getName();
        final String pFolderName = odiLoadPlan.getScenarioFolder().getName();
        final int keeplogHistory = odiLoadPlan.getLogHistoryRetainedNbDays();
        final LogsessionType logsessionType;
        if (odiLoadPlan.getSessionLogsDefaultBehaviorForScenarios().equals(SessionLogsBehavior.ALWAYS)) {
            logsessionType = LogsessionType.ALWAYS;
        } else if (odiLoadPlan.getSessionLogsDefaultBehaviorForScenarios().equals(SessionLogsBehavior.ERROR)) {
            logsessionType = LogsessionType.ERRORS;
        } else if (odiLoadPlan.getSessionLogsDefaultBehaviorForScenarios().equals(SessionLogsBehavior.NEVER)) {
            logsessionType = LogsessionType.NEVER;
        } else {
            logsessionType = LogsessionType.ALWAYS;
        }
        final LogsessionstepType logsessionstepType;
        if (odiLoadPlan.getSessionLogsDefaultBehaviorForScenarioSteps().equals(SessionStepLogsBehavior.BY_SCENARIO_SETTINGS)) {
            logsessionstepType = LogsessionstepType.BYSCENARIOSETTINGS;
        } else if (odiLoadPlan.getSessionLogsDefaultBehaviorForScenarioSteps().equals(SessionStepLogsBehavior.ERROR)) {
            logsessionstepType = LogsessionstepType.ERRORS;
        } else if (odiLoadPlan.getSessionLogsDefaultBehaviorForScenarioSteps().equals(SessionStepLogsBehavior.NEVER)) {
            logsessionstepType = LogsessionstepType.NEVER;
        } else {
            logsessionstepType = LogsessionstepType.BYSCENARIOSETTINGS;
        }
        final int sessiontaskloglevel = odiLoadPlan.getTaskLogLevel();
        final String keywords = odiLoadPlan.getSessionKeywordsAsString();
        // if limit concurrent execution is true then the max number of concurrentexecutions is set to 1 else 0.

        // ODI 12 specific
        final boolean limitconccurentexecutions = getLimitconcurrentExecutions(odiLoadPlan);
        final ViolatebehaviourType violatebehaviourType = getViolateBehaviorType(odiLoadPlan);
        final BigInteger waitpollinginterval = getWaitPollingInterval(odiLoadPlan);
        final Number numberOfConcurrentexecution = getNumberOfConcurrentexecution(odiLoadPlan);
        // end ODI 12 specific

        final String description = odiLoadPlan.getDescription();
        return new LoadPlanDetails(pLoadPlanName, pFolderName, keeplogHistory, logsessionType,
                logsessionstepType, sessiontaskloglevel, keywords, limitconccurentexecutions,
                numberOfConcurrentexecution, violatebehaviourType, waitpollinginterval, description);
    }

    public abstract Number getNumberOfConcurrentexecution(OdiLoadPlan odiLoadPlan);

    public abstract BigInteger getWaitPollingInterval(OdiLoadPlan odiLoadPlan);

    public abstract ViolatebehaviourType getViolateBehaviorType(OdiLoadPlan odiLoadPlan);

    public abstract boolean getLimitconcurrentExecutions(OdiLoadPlan odiLoadPlan);

    private List<OdiLoadPlanTree<Odiloadplanstep>> buildTree(OdiLoadPlan odiLoadPlan, final boolean useDefaultNames) {

        List<OdiLoadPlanTree<Odiloadplanstep>> exceptionsAndRootNode = new ArrayList<OdiLoadPlanTree<Odiloadplanstep>>();
        for (OdiLoadPlanException exception : odiLoadPlan.getExceptions()) {

            Odiloadplanstep exceptionStep = buildRootStep(exception);
            LoadPlanDetails details = getLoadPlanDetailsFromOdi(odiLoadPlan);
            OdiLoadPlanTree<Odiloadplanstep> loadPlan = new OdiLoadPlanTree<Odiloadplanstep>(exceptionStep, odiLoadPlan,
                    details, this.jodiProperties.getProjectCode());
            addChildren(loadPlan, exception.getRootStep().getChildrenSteps(), useDefaultNames);

            exceptionsAndRootNode.add(loadPlan);
        }
        Odiloadplanstep root = buildRootStep(odiLoadPlan.getRootStep());
        LoadPlanDetails details = getLoadPlanDetailsFromOdi(odiLoadPlan);
        OdiLoadPlanTree<Odiloadplanstep> loadPlan = new OdiLoadPlanTree<Odiloadplanstep>(root, odiLoadPlan,
                details, this.jodiProperties.getProjectCode());
        addChildren(loadPlan, odiLoadPlan.getRootStep().getChildrenSteps(), useDefaultNames);
        exceptionsAndRootNode.add(loadPlan);
        return exceptionsAndRootNode;
    }


    private void addChildren(OdiLoadPlanTree<Odiloadplanstep> root, List<? extends OdiLoadPlanStep> list, final boolean useDefaultNames) {
        for (OdiLoadPlanStep child : list) {
            OdiLoadPlanTree<Odiloadplanstep> childLoadPlanStep = buildStep(root, child, useDefaultNames);
            OdiLoadPlanTree<Odiloadplanstep> childOfRoot = root.child(childLoadPlanStep);
            if (child instanceof OdiLoadPlanStepParallel) {
                addChildren(childOfRoot, ((OdiLoadPlanStepParallel) child).getChildrenSteps(), useDefaultNames);
            } else if (child instanceof OdiLoadPlanStepSerial) {
                addChildren(childOfRoot, ((OdiLoadPlanStepSerial) child).getChildrenSteps(), useDefaultNames);
            } else if (child instanceof OdiLoadPlanStepCase) {
                if (((OdiLoadPlanStepCase) child).getCaseWhenList() == null || ((OdiLoadPlanStepCase) child).getCaseWhenList().isEmpty()) {
                    throw new OdiLoadPlanValidationException("The case step with name: " + child.getName() + " doesn't have one or mutiple when steps; this is not allowed.");
                } else {
                    addChildrenCase(childOfRoot, ((OdiLoadPlanStepCase) child).getCaseWhenList(), useDefaultNames);
                }
                if (((OdiLoadPlanStepCase) child).getCaseElse() != null) {
                    addChildrenCase(childOfRoot, ((OdiLoadPlanStepCase) child).getCaseElse(), useDefaultNames);
                }
            } else if (child instanceof OdiLoadPlanStepRunScenario) {
                logger.debug("Scenarios don't have children.");
            } else {
                logger.error("Child of type; " + child.getClass() + " not supported.");
                throw new UnsupportedOperationException();
            }
        }
    }

    private void addChildrenCase(OdiLoadPlanTree<Odiloadplanstep> root, List<OdiCaseWhen> list, final boolean useDefaultNames) {
        for (OdiCaseWhen child : list) {
            OdiLoadPlanTree<Odiloadplanstep> childLoadPlanStep = buildStep(root, child);
            OdiLoadPlanTree<Odiloadplanstep> childOfRoot = root.child(childLoadPlanStep);
            addChildren(childOfRoot, ((OdiCaseWhen) child).getRootStep().getChildrenSteps(), useDefaultNames);
        }
    }


    private void addChildrenCase(OdiLoadPlanTree<Odiloadplanstep> root, OdiCaseElse child, final boolean useDefaultNames) {
        assert (child != null) : "OdiCaseElse can't be null.";
        OdiLoadPlanTree<Odiloadplanstep> childLoadPlanStep = buildStep(root, child);
        OdiLoadPlanTree<Odiloadplanstep> childOfRoot = root.child(childLoadPlanStep);
        if (child.getRootStep() != null && child.getRootStep().getChildrenSteps() != null) {
            addChildren(childOfRoot, ((OdiCaseElse) child).getRootStep().getChildrenSteps(), useDefaultNames);
        }
    }

    private OdiLoadPlanTree<Odiloadplanstep> buildStep(OdiLoadPlanTree<Odiloadplanstep> root, OdiCaseElse step) {
        final String name = "ELSE step of " + step.getParentElement().getName();
        // name must be unique
        //TODO Make it unique
        final LoadPlanStepType type = LoadPlanStepType.CASEELSE;
        final int maxErrorChildCount = 0;
        final String scenario = "";
        final int scenarioVersion = 0;
        final boolean enabled = true;
        final String keywords = "";
        //@TODO keywords		step.get;
        // exception handling
        final RestartType restartType = RestartType.NONE;
        final int priority = 0;
        final int timeout = 0;
        final String exceptionStep = "";
        final Exceptionbehavior exceptionBehavior = Exceptionbehavior.NONE;
        final List<Variable> variables = Collections.emptyList();
        final String testVariable = "";
        final String operator = "";
        final Object value = null;
        //@TODO variables = buildVariables(step.get());
        Odiloadplanstep loadPlanStep = new Odiloadplanstep(root, name, type, enabled, keywords,
                // exception handling
                restartType, timeout, exceptionStep, exceptionBehavior,
                priority, variables, maxErrorChildCount, scenario, scenarioVersion, testVariable, operator, value);
        OdiLoadPlanTree<Odiloadplanstep> loadPlanTree = root.child(loadPlanStep);
        return loadPlanTree;
    }

    private OdiLoadPlanTree<Odiloadplanstep> buildStep(OdiLoadPlanTree<Odiloadplanstep> root, OdiCaseWhen step) {
        final String name = step.getName();
        final LoadPlanStepType type = LoadPlanStepType.CASEWHEN;
        final int maxErrorChildCount = 0;
        final String scenario = "";
        final int scenarioVersion = 0;
        final boolean enabled = step.isEnabled();
        final String keywords = "";
        //@TODO keywords		step.get;
        // exception handling
        final RestartType restartType = RestartType.NONE;
        final int priority = 0;
        final int timeout = 0;
        final String exceptionStep = "";
        final Exceptionbehavior exceptionBehavior = Exceptionbehavior.NONE;
        final List<Variable> variables = Collections.emptyList();
        final String testVariable;
        if (step.getParentElement() instanceof OdiLoadPlanStepCase) {
            testVariable = ((OdiLoadPlanStepCase) step.getParentElement()).getTestVariable().getName();
        } else {
            testVariable = "";
        }
        final String operator = step.getTestOperator().toString();
        final Object value = step.getValue();
        //@TODO variables = buildVariables(step.get());
        Odiloadplanstep loadPlanStep = new Odiloadplanstep(root, name, type, enabled, keywords,
                // exception handling
                restartType, timeout, exceptionStep, exceptionBehavior,
                priority, variables, maxErrorChildCount, scenario, scenarioVersion, testVariable, operator, value);
        OdiLoadPlanTree<Odiloadplanstep> loadPlanTree = root.child(loadPlanStep);
        return loadPlanTree;
    }

    private OdiLoadPlanTree<Odiloadplanstep> buildStep(OdiLoadPlanTree<Odiloadplanstep> root, OdiLoadPlanStep step, final boolean useDefaultNames) {
        final String name = step.getName();
        final LoadPlanStepType type;
        final int maxErrorChildCount;
        final String scenario;
        final int scenarioVersion;
        final String testVariable;
        if (step instanceof OdiLoadPlanStepSerial) {
            type = LoadPlanStepType.SERIAL;
            maxErrorChildCount = 0;
            testVariable = "";
        } else if (step instanceof OdiLoadPlanStepParallel) {
            type = LoadPlanStepType.PARALLEL;
            maxErrorChildCount = ((OdiLoadPlanStepParallel) step).getMaxErrorChildCount();
            testVariable = "";
        } else if (step instanceof OdiLoadPlanStepRunScenario) {
            type = LoadPlanStepType.SCENARIO;
            maxErrorChildCount = 0;
            testVariable = "";
        } else if (step instanceof OdiLoadPlanStepCase) {
            type = LoadPlanStepType.CASE;
            maxErrorChildCount = 0;
            testVariable = ((OdiLoadPlanStepCase) step).getTestVariable().getName();
        } else {
            throw new UnsupportedOperationException();
        }
        final boolean enabled = step.isEnabled();
        final String keywords = step.getSessionKeywordsAsString();
        // exception handling
        final RestartType restartType;
        if (step instanceof OdiLoadPlanStepSerial) {
            restartType = ((OdiLoadPlanStepSerial) step).getRestartType() != null
                    ? mapFrom((OdiLoadPlanStepSerial) step) : RestartType.NONE;
            scenario = "";
            scenarioVersion = 0;
        } else if (step instanceof OdiLoadPlanStepParallel) {
            restartType = ((OdiLoadPlanStepParallel) step).getRestartType() != null
                    ? mapFrom((OdiLoadPlanStepParallel) step) : RestartType.NONE;
            scenario = "";
            scenarioVersion = 0;
        } else if (step instanceof OdiLoadPlanStepRunScenario) {
            restartType = ((OdiLoadPlanStepRunScenario) step).getRestartType() != null
                    ? mapFrom((OdiLoadPlanStepRunScenario) step) : RestartType.NONE;
            if (useDefaultNames) {
                // we use default names; meaning the name of the underlying object in upper case.
                OdiScenario aOdiScenario = this.odiLoadPlanAccessStrategy
                        .findScenario(((OdiLoadPlanStepRunScenario) step).getScenarioTag().getName(),
                                Integer.parseInt(((OdiLoadPlanStepRunScenario) step).getScenarioTag().getVersion()));
                assert (aOdiScenario != null) : "Scenario with name :" + ((OdiLoadPlanStepRunScenario) step).getScenarioTag().getName() + " doesn't exist.";
                if (aOdiScenario.getSourceComponentClass().equals(oracle.odi.domain.project.OdiUserProcedure.class)) {
                    scenario = JodiConstants.getScenarioNameFromObject(this.odiLoadPlanAccessStrategy.findProcedure(aOdiScenario.getSourceComponentId()).getName(), false);
                } else if (aOdiScenario.getSourceComponentClass().equals(oracle.odi.domain.project.OdiVariable.class)) {
                    scenario = JodiConstants.getScenarioNameFromObject(this.odiLoadPlanAccessStrategy.findVariable(aOdiScenario.getSourceComponentId()).getName(), false);
                } else if (aOdiScenario.getSourceComponentClass().getName().equals("oracle.odi.domain.mapping.Mapping")) {
                    scenario = JodiConstants.getScenarioNameFromObject(this.odiLoadPlanAccessStrategy.findMapping(aOdiScenario.getSourceComponentId()).getName(), true);
                } else if (aOdiScenario.getSourceComponentClass().equals(oracle.odi.domain.project.OdiPackage.class)) {
                    scenario = JodiConstants.getScenarioNameFromObject(this.odiLoadPlanAccessStrategy.findPackage(aOdiScenario.getSourceComponentId()).getName(), false);
                } else {
                    throw new OdiLoadPlanServiceException(
                            String.format("Can't find scenario: '%s' of type '%s' with version '%s'.",
                                    ((OdiLoadPlanStepRunScenario) step).getScenarioTag().getName(), aOdiScenario.getSourceComponentClass().getName(), ((OdiLoadPlanStepRunScenario) step).getScenarioTag().getVersion()));
                }
            } else {
                scenario = ((OdiLoadPlanStepRunScenario) step).getScenarioTag().getName();
            }
            scenarioVersion = Integer.parseInt(((OdiLoadPlanStepRunScenario) step).getScenarioTag().getVersion());
        } else if (step instanceof OdiLoadPlanStepCase) {
            restartType = RestartType.NONE;
            scenario = "";
            scenarioVersion = 0;
        } else {
            throw new UnsupportedOperationException();
        }
        final int priority;
        if (step instanceof OdiLoadPlanStepRunScenario) {
            priority = ((OdiLoadPlanStepRunScenario) step).getPriority();
        } else {
            priority = 0;
        }
        final int timeout = step.getTimeout();
        final String exceptionStep = step.getException() != null ? step.getException().getName() : "";
        final Exceptionbehavior exceptionBehavior = step.getExceptionBehavior() != null ? mapFrom(step.getExceptionBehavior())
                : Exceptionbehavior.NONE;
        final List<Variable> variables = buildVariables(step.getLoadPlanStepVariables());
        final String operator = "";
        final Object value = null;
        Odiloadplanstep loadPlanStep = new Odiloadplanstep(root, name, type, enabled, keywords,
                // exception handling
                restartType, timeout, exceptionStep, exceptionBehavior, priority, variables,
                maxErrorChildCount, scenario, scenarioVersion, testVariable, operator, value);
        OdiLoadPlanTree<Odiloadplanstep> loadPlanTree = root.child(loadPlanStep);
        return loadPlanTree;
    }

    private Exceptionbehavior mapFrom(ExceptionBehavior exceptionBehavior) {
        if (exceptionBehavior.equals(ExceptionBehavior.RUN_EXCEPTION_AND_IGNORE_ERROR)) {
            return Exceptionbehavior.RUN_EXCEPTION_AND_IGNORE_ERROR;
        } else {
            return Exceptionbehavior.NONE;
        }
    }

    private RestartType mapFrom(OdiLoadPlanStepRunScenario step) {
        if (step.getRestartType().equals(
                oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepRunScenario.RestartType.RUN_SCENARIO_FROM_STEP)) {
            return RestartType.RUN_SCENARIO_FROM_STEP;
        } else if (step.getRestartType().equals(
                oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepRunScenario.RestartType.RUN_SCENARIO_FROM_TASK)) {
            return RestartType.RUN_SCENARIO_FROM_TASK;
        } else if (step.getRestartType().equals(
                oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepRunScenario.RestartType.RUN_SCENARIO_NEW_SESSION)) {
            return RestartType.RUN_SCENARIO_NEW_SESSION;
        } else {
            return RestartType.NONE;
        }
    }

    private RestartType mapFrom(OdiLoadPlanStepParallel step) {
        if (step.getRestartType().equals(
                oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepParallel.RestartType.PARALLEL_STEP_ALL_CHILDREN)) {
            return RestartType.PARALLEL_STEP_ALL_CHILDREN;
        } else if (step.getRestartType().equals(
                oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepParallel.RestartType.PARALLEL_STEP_FAILED_CHILDREN)) {
            return RestartType.PARALLEL_STEP_FAILED_CHILDREN;
        } else {
            return RestartType.NONE;
        }
    }

    /**
     * SERIAL_STEP_ALL_CHILDREN, SERIAL_STEP_FROM_FAILURE;
     *
     * @param step
     * @return
     */
    private RestartType mapFrom(OdiLoadPlanStepSerial step) {
        if (step.getRestartType().equals(
                oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepSerial.RestartType.SERIAL_STEP_ALL_CHILDREN)) {
            return RestartType.SERIAL_STEP_ALL_CHILDREN;
        } else if (step.getRestartType().equals(
                oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepSerial.RestartType.SERIAL_STEP_FROM_FAILURE)) {
            return RestartType.SERIAL_STEP_FROM_FAILURE;
        }
        return RestartType.NONE;
    }

    private List<Variable> buildVariables(Collection<OdiLoadPlanStepVariable> loadPlanStepVariables) {
        List<Variable> variables = new ArrayList<Variable>();
        for (OdiLoadPlanStepVariable odiLoadPlanStepVariable : loadPlanStepVariables) {
            variables.add(this.buildVariable(odiLoadPlanStepVariable));
        }
        return variables;
    }

    private Variable buildVariable(OdiLoadPlanStepVariable odiLoadPlanStepVariable) {
        String name = odiLoadPlanStepVariable.getLoadPlanVariable().getName();
        Object value = odiLoadPlanStepVariable.getValue();
        boolean refresh = odiLoadPlanStepVariable.isRefresh();
        Variable variable = new Variable(name, refresh, value);
        return variable;
    }

    private Odiloadplanstep buildRootStep(OdiLoadPlanException step) {
        logger.info("building root step : " + step.getName());
        final OdiLoadPlanTree<Odiloadplanstep> root1 = null;
        final String name = step.getName();
        final LoadPlanStepType type = LoadPlanStepType.EXCEPTION;
        final boolean enabled = step.isEnabled();
        final String keywords = step.getSessionKeywordsAsString();
        // exception handling
        final RestartType restartType = RestartType.NONE;
        final int timeout = 0;
        final String exceptionStep = "";
        final Exceptionbehavior exceptionBehavior = Exceptionbehavior.NONE;
        final List<Variable> variables = Collections.emptyList();
        final int priority = 0;
        final int maxErrorChildCount = 0;
        final String scenario = "";
        final int scenarioVersion = 0;
        final String testVariable = "";
        final String operator = "";
        final Object value = null;
        Odiloadplanstep loadPlanStep = new Odiloadplanstep(root1, name, type, enabled, keywords, restartType, timeout,
                exceptionStep, exceptionBehavior, priority, variables, maxErrorChildCount, scenario,
                scenarioVersion, testVariable, operator, value);
        return loadPlanStep;
    }


    private Odiloadplanstep buildRootStep(OdiLoadPlanStepSerial step) {
        final OdiLoadPlanTree<Odiloadplanstep> root1 = null;
        final String name = step.getName();
        final LoadPlanStepType type = LoadPlanStepType.SERIAL;
        final boolean enabled = step.isEnabled();
        final String keywords = step.getSessionKeywordsAsString();
        // exception handling
        final RestartType restartType = mapFrom(step);
        final int timeout = step.getTimeout();
        final String exceptionStep = step.getException() != null ? step.getException().getName() : "";
        final Exceptionbehavior exceptionBehavior = step.getExceptionBehavior() != null ? mapFrom(step.getExceptionBehavior())
                : Exceptionbehavior.NONE;
        final List<Variable> variables = buildVariables(step.getLoadPlanStepVariables());
        final int priority = 0;
        final int maxErrorChildCount = 0;
        final String scenario = "";
        final int scenarioVersion = 0;
        final String testVariable = "";
        final String operator = "";
        final Object value = null;
        Odiloadplanstep loadPlanStep = new Odiloadplanstep(root1, name, type, enabled, keywords, restartType, timeout,
                exceptionStep, exceptionBehavior, priority, variables, maxErrorChildCount, scenario,
                scenarioVersion, testVariable, operator, value);
        return loadPlanStep;
    }

    /**
     * This method is used to compare loadplans;
     * it merely prints out the details of a loadplan.
     */
    @Override
    public void printLoadPlans(final boolean useDefaultNames) {
        Collection<OdiLoadPlan> loadPlansUnsorted = odiLoadPlanAccessStrategy.findAllLoadPlans();
        List<OdiLoadPlan> list = new ArrayList<>(loadPlansUnsorted);
        Collections.sort(list,
                (o1, o2) -> o1.getName().compareTo(o2.getName()));
        for (OdiLoadPlan loadPlan : list) {
            printLoadPlan(loadPlan, useDefaultNames);
        }
    }

    private void printLoadPlan(OdiLoadPlan loadPlan, final boolean useDefaultNames) {
        // build
        this.loadPlanValidationService.reset(loadPlan.getName());
        int counterDoOnlyOnce = 0;
        // // print
        for (OdiLoadPlanTree<Odiloadplanstep> tree : buildTree(loadPlan, useDefaultNames)) {
            // LoadPlanTree<LoadPlanStep> tree = buildTree(loadPlan);
            if (!this.loadPlanValidationService.validate(tree.getLoadPlanDetails())) {
                throw new OdiLoadPlanValidationException("The loadplandetails: " + loadPlan.getName()
                        + " transformed into internal model is not valid.");
            }
            if (!this.loadPlanValidationService.validate(tree)) {
                throw new OdiLoadPlanValidationException(
                        "The loadplan: " + loadPlan.getName() + " transformed into internal model is not valid.");
            }
            if (counterDoOnlyOnce == 0) {
                // there may be multiple exception trees and regular trees,
                // but we do this only once.
                logger.info("Start Printing loadplan: " + loadPlan.getName());
                logger.info("Loadplan folder: " + tree.getLoadPlanDetails().getFolderName());
                logger.info("Loadplan Keeploghistory: " + tree.getLoadPlanDetails().getKeeplogHistory());
                logger.info("Loadplan Logsessions: " + tree.getLoadPlanDetails().getLogsessionType().name());
                logger.info("Loadplan Logsessionstep: " + tree.getLoadPlanDetails().getLogsessionstepType().name());
                logger.info("Loadplan Sessiontaskloglevel: " + tree.getLoadPlanDetails().getSessiontaskloglevel());
                logger.info("Loadplan Keywords: " + tree.getLoadPlanDetails().getKeywords());
                logger.info(
                        "Loadplan Limitconccurentexecutions: " + tree.getLoadPlanDetails().isLimitconcurrentexecutions());
                logger.info("Loadplan Violatebehaviour: " + tree.getLoadPlanDetails().getViolatebehaviourType().name());
                logger.info("Loadplan Waitpollinginterval: " + tree.getLoadPlanDetails().getWaitpollinginterval());
                logger.info("Loadplan Description: " + tree.getLoadPlanDetails().getDescription());
            }
            counterDoOnlyOnce++;
            OdiLoadPlanVisitor<Odiloadplanstep> visitor = new LevelPrintingVisitor();
            tree.accept(visitor);
            visitor.visit(tree);
        }
        logger.info("End Printing loadplan: " + loadPlan.getName());
    }
}