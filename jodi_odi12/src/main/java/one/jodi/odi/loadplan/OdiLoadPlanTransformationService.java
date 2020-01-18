package one.jodi.odi.loadplan;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails.LogsessionstepType;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStepType;
import one.jodi.etl.service.loadplan.internalmodel.Variable;
import oracle.odi.domain.project.OdiVariable;
import oracle.odi.domain.runtime.loadplan.*;
import oracle.odi.domain.runtime.loadplan.OdiCaseWhen.ComparisonOperator;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan.SessionLogsBehavior;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan.SessionStepLogsBehavior;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepRunScenario.RestartType;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.OdiScenarioFolder;
import oracle.odi.domain.runtime.scenario.OdiScenarioVariable;
import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiLogicalAgent;
import oracle.odi.generation.OdiVariableTextGeneratorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class OdiLoadPlanTransformationService implements OdiLoadPlanVisitor<Odiloadplanstep> {
    public final static String GLOBAL_VAR_PREFIX_DOT = "GLOBAL.";
    private final static Logger logger = LogManager.getLogger(OdiLoadPlanTransformationService.class);
    private final static String ERROR_MESSAGE_08001 = "Couldn't find the exception '%1$s' of loadplan '%2$s'.";
    private final Map<String, OdiLoadPlanStep> odiLoadPlanStepDB = new HashMap<>();
    private final Map<String, Odiloadplanstep> internalModelStepDB = new HashMap<>();
    private final Set<OdiVariable> variables = new HashSet<>();
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final Set<Odiloadplanstep> children = new LinkedHashSet<Odiloadplanstep>();
    @SuppressWarnings("rawtypes")
    private final OdiLoadPlanAccessStrategy loadPlanAccessStrategy;
    private OdiLoadPlan pOdiLoadPlan;
    private LoadPlanDetails loadPlanDetails;
    private String projectCode;

    @SuppressWarnings("rawtypes")
    public OdiLoadPlanTransformationService(
            final OdiLoadPlanAccessStrategy loadPlanAccessStrategy,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.loadPlanAccessStrategy = loadPlanAccessStrategy;
        this.errorWarningMessages = errorWarningMessages;
    }

    public void initialize(final OdiLoadPlan pOdiLoadPlan,
                           final LoadPlanDetails loadPlanDetails,
                           final String projectCode) {
        this.pOdiLoadPlan = pOdiLoadPlan;
        this.loadPlanDetails = loadPlanDetails;
        this.projectCode = projectCode;
    }

    @Override
    public OdiLoadPlanVisitor<Odiloadplanstep> visit(OdiLoadPlanTree<Odiloadplanstep> tree) {
        return this;
    }

    @Override
    public Collection<Odiloadplanstep> getChildren() {
        return children;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void visit(Odiloadplanstep pLoadPlanStep) {
        if (pLoadPlanStep == null) {
            return;
        }
        if (pLoadPlanStep.getParent() != null &&
                pLoadPlanStep.getParent().getRoot() != null &&
                pLoadPlanStep.getParent().getRoot().getName() != null
        ) {
            // attach to parent node
            final OdiLoadPlanStep parent = getParentByName(pLoadPlanStep.getParent().getRoot().getName());
            //assert(parent != null);
            OdiLoadPlanStep child;
            try {
                assert loadPlanDetails != null;
                child = populateNode(pLoadPlanStep, parent, pOdiLoadPlan,
                        loadPlanDetails, projectCode);
            } catch (OdiVariableChangedDatatypeException | OdiVariableTextGeneratorException e) {
                logger.error(e);
                throw new OdiLoadPlanTransformationException(e);
            }
            final String key;
            if (pLoadPlanStep.getType().equals(LoadPlanStepType.CASEELSE) || pLoadPlanStep.getType().equals(LoadPlanStepType.CASEWHEN)
                    || pLoadPlanStep.getType().equals(LoadPlanStepType.EXCEPTION)) {
                key = child.getParentElement().getName();
            } else {
                key = child.getName();
            }
            odiLoadPlanStepDB.put(key, child);
            if (pLoadPlanStep.getVariables() != null &&
                    pLoadPlanStep.getVariables().size() != 0
            ) {
                populateVariables(child, pLoadPlanStep.getVariables());
            }
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.SERIAL)) {
            // is parent (root node)
            OdiLoadPlanStep root = populateSerialNode(pLoadPlanStep, null, pOdiLoadPlan,
                    loadPlanDetails, projectCode);
            odiLoadPlanStepDB.put(root.getName(), root);
            if (pLoadPlanStep.getVariables() != null &&
                    pLoadPlanStep.getVariables().size() != 0
            ) {
                populateVariables(root, pLoadPlanStep.getVariables());
            }
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.EXCEPTION)) {
            // is parent (root node)
            OdiLoadPlanException root = populateExceptionNode(pLoadPlanStep, pOdiLoadPlan,
                    loadPlanDetails, projectCode);
            String key = root.getRootStep().getName();
            odiLoadPlanStepDB.put(key, root.getRootStep());
        } else {
            throw new UnsupportedOperationException();
        }
        internalModelStepDB.put(pLoadPlanStep.getName(), pLoadPlanStep);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void populateVariables(OdiLoadPlanStep child, List<Variable> ivs) {
        for (final Variable internalVariable : ivs) {
            OdiLoadPlanVariable variable = getLoadPlanVar(internalVariable.getName());
            assert (variable != null) : "Variable can't be null for internal variable: " + internalVariable.getName();
            boolean found = false;
            OdiLoadPlanStepVariable stepVariable = null;
            for (OdiLoadPlanStepVariable lpsv : child.getLoadPlanStepVariables()) {
                if (lpsv.getLoadPlanVariable().getName().replace(GLOBAL_VAR_PREFIX_DOT, "").equals(internalVariable.getName())) {
                    found = true;
                    stepVariable = lpsv;
                }
            }
            if (!found) {
                stepVariable = child.addVariable(variable);
            }
            assert (stepVariable != null);
            stepVariable
                    .setRefresh(internalVariable
                            .isRefresh());
            if (internalVariable.getValue() != null) {
                stepVariable
                        .setValue(internalVariable
                                .getValue());
            }
            if (!internalVariable.isRefresh() && internalVariable
                    .getValue() == null) {
                stepVariable
                        .setValue(null);
            }
        }
    }

    private OdiLoadPlanVariable getLoadPlanVar(String name) {
        logger.info("Get loadplan var: " + name);
        final OdiVariable odiVariable;
        if (name.contains(".") && !name.startsWith(GLOBAL_VAR_PREFIX_DOT)) {
            odiVariable = this.loadPlanAccessStrategy.findVariable(name, projectCode);
        } else {
            odiVariable = this.loadPlanAccessStrategy.findGlobalVariable(name);
        }
        return this.loadPlanAccessStrategy.addVariable(pOdiLoadPlan, odiVariable);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiLoadPlanStep populateNode(Odiloadplanstep pLoadPlanStep, OdiLoadPlanStep parent, final OdiLoadPlan pOdiLoadPlan,
                                        final LoadPlanDetails loadPlanDetails, final String projectCode)
            throws OdiVariableChangedDatatypeException, OdiVariableTextGeneratorException {
        if (pLoadPlanStep.getType().equals(LoadPlanStepType.CASEWHEN)) {
            return populateStepWhen(pLoadPlanStep, parent, pOdiLoadPlan,
                    loadPlanDetails, projectCode);
        }
        if (pLoadPlanStep.getType().equals(LoadPlanStepType.CASEELSE)) {
            return populateStepElse(pLoadPlanStep, parent);
        }
        if (pLoadPlanStep.getType().equals(LoadPlanStepType.CASE)) {
            OdiLoadPlanStepCase odiLoadPlanStepCase = populateStepCase(pLoadPlanStep, parent, pOdiLoadPlan,
                    loadPlanDetails, projectCode);
            return odiLoadPlanStepCase;
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.PARALLEL)) {
            return populateStepParallel(pLoadPlanStep, parent, pOdiLoadPlan,
                    loadPlanDetails, projectCode);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.SCENARIO)) {
            return populateScenarioNode(pLoadPlanStep, parent, pOdiLoadPlan,
                    loadPlanDetails, projectCode);
        } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.SERIAL)) {
            return populateSerialNode(pLoadPlanStep, parent, pOdiLoadPlan,
                    loadPlanDetails, projectCode);
        } else {
            throw new UnsupportedOperationException("Can't find type; " + pLoadPlanStep.getType() + " in odiloadplan: " + pOdiLoadPlan.getName());
        }
    }

    public OdiLoadPlanStepRunScenario populateScenarioNode(Odiloadplanstep pLoadPlanStep, OdiLoadPlanStep pParent, final OdiLoadPlan pOdiLoadPlan,
                                                           final LoadPlanDetails loadPlanDetails, final String projectCode)
            throws OdiVariableChangedDatatypeException, OdiVariableTextGeneratorException {
        final OdiLoadPlanStepRunScenario aScenarioNode;
        String pStepName = pLoadPlanStep.getName();
        OdiScenario pScenario = loadPlanAccessStrategy.findScenario(pLoadPlanStep.getScenario(), pLoadPlanStep.getScenarioVersion());
        if (pScenario == null) {
            String message = String.format(
                    "In the loadplan %1$s the scenario %2$s is not found, please create or regenerate it.",
                    loadPlanDetails.getLoadPlanName(), pLoadPlanStep.getName());
            logger.error(message);
            throw new OdiLoadPlanTransformationException(message);
        }
        OdiLogicalAgent pLogicalAgent = null;
        OdiContext pContext = null;
        if (pParent instanceof OdiLoadPlanStepSerial) {
            aScenarioNode = ((OdiLoadPlanStepSerial) pParent).addStepRunScenario(pStepName, pScenario, pLogicalAgent,
                    pContext);
        } else if (pParent instanceof OdiLoadPlanStepParallel) {
            aScenarioNode = ((OdiLoadPlanStepParallel) pParent).addStepRunScenario(pStepName, pScenario, pLogicalAgent,
                    pContext);
        } else {
            throw new UnsupportedOperationException();
        }
        aScenarioNode.setEnabled(pLoadPlanStep.isEnabled());
        //this.odiLoadPlan.getRootStep().setException(pException);
        //aScenarioNode.setExceptionBehavior(pLoadPlanStep.getExceptionBehavior());
        aScenarioNode.setName(pLoadPlanStep.getName());
        aScenarioNode.setRestartType(mapFromRestartTypeScenario(pLoadPlanStep.getRestartType()));
        aScenarioNode.setSessionKeywordsAsString(pLoadPlanStep.getKeywords());
        aScenarioNode.setTimeout(pLoadPlanStep.getTimeout());
        aScenarioNode.setPriority(pLoadPlanStep.getPriority());
        for (final OdiScenarioVariable odiScenarioVariable : pScenario.getScenarioVariables()) {
            OdiVariable var = loadPlanAccessStrategy.findVariable(odiScenarioVariable.getName(), projectCode);
            assert (var != null);
            OdiLoadPlanVariable retVal;
            if (!variables.contains(var)) {
                retVal = loadPlanAccessStrategy.addVariable(pOdiLoadPlan, var);
                assert (retVal != null) : " variable : " + var.getProject().getCode() + "." + var.getName() + " not found.";
            } else {
                assert (pOdiLoadPlan != null);
                retVal = pOdiLoadPlan
                        .getVariableIfExistsInLoadPlan(var.getProject().getCode() + "." + var.getName());
                if (retVal == null) {
                    retVal = loadPlanAccessStrategy.addVariable(pOdiLoadPlan, var);
                }
                assert (retVal != null) : " variable : " + var.getProject().getCode() + "." + var.getName() + " not found.";
            }
            Variable externalVar = getVariableByName(var.getProject().getCode() + "." + var.getName(),
                    pLoadPlanStep.getVariables());
            if (externalVar != null) {
                OdiLoadPlanStepVariable loadPlanStepVar = aScenarioNode.addVariable(retVal);
                loadPlanStepVar.setRefresh(externalVar.isRefresh());
                loadPlanStepVar.setValue(externalVar.getValue());
            }
            variables.add(var);
        }
        if (pLoadPlanStep.getExceptionStep() != null && pLoadPlanStep.getExceptionStep().length() > 0) {
            aScenarioNode.setException(findExceptionByName(pOdiLoadPlan, pLoadPlanStep.getExceptionStep()));
        }
        return aScenarioNode;
    }


    public OdiLoadPlanException findExceptionByName(OdiLoadPlan odiLoadPlan, String name) {
        for (OdiLoadPlanException odiLoadPlanException : odiLoadPlan.getExceptions()) {
            if (odiLoadPlanException.getName().equals(name)) {
                return odiLoadPlanException;
            }
        }
        String message = errorWarningMessages.formatMessage(8001, ERROR_MESSAGE_08001, this.getClass(),
                name, odiLoadPlan.getName());
        errorWarningMessages.addMessage(-1, message, MESSAGE_TYPE.ERRORS);
        logger.error(message);
        throw new OdiLoadPlanTransformationException(message);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiLoadPlanStepParallel populateStepParallel(Odiloadplanstep pLoadPlanStep, OdiLoadPlanStep pParent, final OdiLoadPlan pOdiLoadPlan,
                                                        final LoadPlanDetails loadPlanDetails, final String projectCode) {
        assert (pParent != null);
        final OdiLoadPlanStepParallel aParallelNode;
        if (pParent instanceof OdiLoadPlanStepSerial) {
            aParallelNode = ((OdiLoadPlanStepSerial) pParent).addStepParallel(pLoadPlanStep.getName());
        } else if (pParent instanceof OdiLoadPlanStepParallel) {
            aParallelNode = ((OdiLoadPlanStepParallel) pParent).addStepParallel(pLoadPlanStep.getName());
        } else {
            throw new UnsupportedOperationException("Partent of type " + pParent.getClass().getName() + " not allowed.");
        }
        aParallelNode.setEnabled(pLoadPlanStep.isEnabled());
        // this.odiLoadPlan.getRootStep().setException(pException);
        // this.odiLoadPlan.getRootStep().setExceptionBehavior(exceptionBehavior);
        aParallelNode.setName(pLoadPlanStep.getName());
        aParallelNode.setRestartType(mapFromRestartTypeParallel(pLoadPlanStep.getRestartType()));
        aParallelNode.setSessionKeywordsAsString(pLoadPlanStep.getKeywords());
        aParallelNode.setTimeout(pLoadPlanStep.getTimeout());
        aParallelNode.setMaxErrorChildCount(pLoadPlanStep.getMaxErrorChildCount());
        if (pLoadPlanStep.getExceptionStep() != null && pLoadPlanStep.getExceptionStep().length() > 0) {
            aParallelNode.setException(findExceptionByName(pOdiLoadPlan, pLoadPlanStep.getExceptionStep()));
        }
        return aParallelNode;
    }

    private OdiLoadPlanVariable getLoadPlanVariableByName(String name, final OdiLoadPlan pOdiLoadPlan,
                                                          final LoadPlanDetails loadPlanDetails, final String projectCode) {
        for (OdiLoadPlanVariable odiLoadPlanVariable : pOdiLoadPlan.getVariables()) {
            if (odiLoadPlanVariable.getName().equals(name)) {
                return odiLoadPlanVariable;
            }
        }
        assert (false) : "Loadplanvariable " + name + " not found in project : " + projectCode;
        return null;
    }

    public OdiLoadPlanStepCase populateStepCase(Odiloadplanstep pLoadPlanStep, OdiLoadPlanStep pParent, final OdiLoadPlan pOdiLoadPlan,
                                                final LoadPlanDetails loadPlanDetails, final String projectCode) {
        final OdiLoadPlanStepCase aCaseNode;
        if (pParent instanceof OdiLoadPlanStepSerial) {
            loadPlanAccessStrategy.addVariable(pOdiLoadPlan, (loadPlanAccessStrategy.findVariable(pLoadPlanStep.getTestVariable(), projectCode)));
            OdiLoadPlanVariable odiLoadPlanVariable = getLoadPlanVariableByName(pLoadPlanStep.getTestVariable(), pOdiLoadPlan,
                    loadPlanDetails, projectCode);
            aCaseNode = ((OdiLoadPlanStepSerial) pParent).addStepCase(pLoadPlanStep.getName(), odiLoadPlanVariable);
        } else if (pParent instanceof OdiLoadPlanStepParallel) {
            loadPlanAccessStrategy.addVariable(pOdiLoadPlan, (loadPlanAccessStrategy.findVariable(pLoadPlanStep.getTestVariable(), projectCode)));
            OdiLoadPlanVariable odiLoadPlanVariable = getLoadPlanVariableByName(pLoadPlanStep.getTestVariable(), pOdiLoadPlan,
                    loadPlanDetails, projectCode);
            aCaseNode = ((OdiLoadPlanStepParallel) pParent).addStepCase(pLoadPlanStep.getName(), odiLoadPlanVariable);
        } else {
            throw new UnsupportedOperationException();
        }
        aCaseNode.setName(pLoadPlanStep.getName());
        aCaseNode.setEnabled(pLoadPlanStep.isEnabled());
        aCaseNode.setSessionKeywordsAsString(pLoadPlanStep.getKeywords());
        aCaseNode.setTimeout(pLoadPlanStep.getTimeout());
        if (pLoadPlanStep.getExceptionStep() != null && pLoadPlanStep.getExceptionStep().length() > 0) {
            aCaseNode.setException(findExceptionByName(pOdiLoadPlan, pLoadPlanStep.getExceptionStep()));
        }
        return aCaseNode;
    }

    private OdiLoadPlanStepSerial populateStepWhen(Odiloadplanstep pLoadPlanStep, OdiLoadPlanStep pParent, final OdiLoadPlan pOdiLoadPlan,
                                                   final LoadPlanDetails loadPlanDetails, final String projectCode) {
        assert (pParent != null);
        final OdiCaseWhen odiCaseWhen;
        if (pParent instanceof OdiLoadPlanStepCase) {
            odiCaseWhen = ((OdiLoadPlanStepCase) pParent).addCaseWhen(mapFromOperator(pLoadPlanStep.getOperator()), pLoadPlanStep.getValue());
        } else {
            throw new UnsupportedOperationException("Parent of type: " + pParent.getClass().getName() + " not supported.");
        }
        odiCaseWhen.setName(pLoadPlanStep.getName());
        //odiCaseWhen.setOrder(pLoadPlanStep.getOrder());
        if (pLoadPlanStep.getValue() != null) {
            odiCaseWhen.setValue(pLoadPlanStep.getValue());
        }
        return odiCaseWhen.getRootStep();
    }

    /*
     * LESS_THAN, /* 61 LESS_THAN_OR_EQUAL, /* 65 DIFFERENT, /* 69 EQUALS, /* 73
     * GREATER_THAN, /* 77 GREATER_THAN_OR_EQUAL, /* 81 IS_NOT_NULL, /* 85
     * IS_NULL;
     *
     */
    private ComparisonOperator mapFromOperator(String operator) {
        switch (operator) {
            case "LESS_THAN":
                return ComparisonOperator.LESS_THAN;
            case "LESS_THAN_OR_EQUAL":
                return ComparisonOperator.LESS_THAN_OR_EQUAL;
            case "DIFFERENT":
                return ComparisonOperator.DIFFERENT;
            case "EQUALS":
                return ComparisonOperator.EQUALS;
            case "GREATER_THAN":
                return ComparisonOperator.GREATER_THAN;
            case "GREATER_THAN_OR_EQUAL":
                return ComparisonOperator.GREATER_THAN_OR_EQUAL;
            case "IS_NOT_NULL":
                return ComparisonOperator.IS_NOT_NULL;
            case "IS_NULL":
                return ComparisonOperator.IS_NULL;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public OdiLoadPlanStepSerial populateStepElse(Odiloadplanstep pLoadPlanStep, OdiLoadPlanStep pParent) {
        final OdiCaseElse aElseNode;
        if (pParent instanceof OdiLoadPlanStepCase) {
            ((OdiLoadPlanStepCase) pParent).setCaseElse(pLoadPlanStep.getName());
            aElseNode = ((OdiLoadPlanStepCase) pParent).getCaseElse();
            aElseNode.setName(pLoadPlanStep.getName());
        } else {
            throw new UnsupportedOperationException();
        }
        aElseNode.setEnabled(pLoadPlanStep.isEnabled());
        return aElseNode.getRootStep();
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiLoadPlanStepSerial populateSerialNode(Odiloadplanstep pLoadPlanStep, OdiLoadPlanStep pParent, final OdiLoadPlan pOdiLoadPlan,
                                                    final LoadPlanDetails loadPlanDetails, final String projectCode) {
        final OdiLoadPlanStepSerial aSerialNode;
        if (pParent == null) {
            if (pLoadPlanStep.getType().equals(LoadPlanStepType.SERIAL)) {
                // this is the root node
                assert (pOdiLoadPlan != null);
                aSerialNode = pOdiLoadPlan.getRootStep();
                LoadPlanDetails details = loadPlanDetails;
                assert (details != null);
                pOdiLoadPlan.setName(details.getLoadPlanName());
                OdiScenarioFolder pScenarioFolder = this.loadPlanAccessStrategy.findScenarioFolder(details.getFolderName());
                pOdiLoadPlan.setScenarioFolder(pScenarioFolder);
                pOdiLoadPlan.setLogHistoryRetainedNbDays(details.getKeeplogHistory());
                pOdiLoadPlan.setSessionLogsDefaultBehaviorForScenarios(mapFrom(details.getLogsessionType()));
                pOdiLoadPlan.setSessionLogsDefaultBehaviorForScenarioSteps(mapFrom(details.getLogsessionstepType()));
                pOdiLoadPlan.setTaskLogLevel(details.getSessiontaskloglevel());
                pOdiLoadPlan.setSessionKeywordsAsString(details.getKeywords());
                // this.odiLoadPlan.setCecLprBehavior(mapFrom ( details.getViolatebehaviourType() ));
                // this.odiLoadPlan.setCecLprPollIntv(details.getWaitpollinginterval());
                pOdiLoadPlan.setDescription(details.getDescription());
                //this.odiLoadPlan.setMaxCecLpr(details.isLimitconccurentexecutions() ? 1 : 0);
            } else if (pLoadPlanStep.getType().equals(LoadPlanStepType.EXCEPTION)) {
                throw new UnsupportedOperationException();
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (pParent instanceof OdiLoadPlanStepSerial) {
            aSerialNode = ((OdiLoadPlanStepSerial) pParent).addStepSerial(pLoadPlanStep.getName());
        } else if (pParent instanceof OdiLoadPlanStepParallel) {
            aSerialNode = ((OdiLoadPlanStepParallel) pParent).addStepSerial(pLoadPlanStep.getName());
        } else {
            throw new UnsupportedOperationException();
        }
        aSerialNode.setEnabled(pLoadPlanStep.isEnabled());
        // this.odiLoadPlan.getRootStep().setException(pException);
        // this.odiLoadPlan.getRootStep().setExceptionBehavior(exceptionBehavior);
        aSerialNode.setName(pLoadPlanStep.getName());
        aSerialNode.setRestartType(mapFromRestartTypeSerial(pLoadPlanStep.getRestartType()));
        aSerialNode.setSessionKeywordsAsString(pLoadPlanStep.getKeywords());
        aSerialNode.setTimeout(pLoadPlanStep.getTimeout());
        if (pLoadPlanStep.getExceptionStep() != null && pLoadPlanStep.getExceptionStep().length() > 0) {
            aSerialNode.setException(findExceptionByName(pOdiLoadPlan, pLoadPlanStep.getExceptionStep()));
        }
        return aSerialNode;
    }

    private OdiLoadPlanException populateExceptionNode(Odiloadplanstep pLoadPlanStep, final OdiLoadPlan pOdiLoadPlan,
                                                       final LoadPlanDetails loadPlanDetails, final String projectCode) {
        final OdiLoadPlanException aExceptionNode;
        // this is the root node
        assert (pOdiLoadPlan != null);
        aExceptionNode = pOdiLoadPlan.addException(pLoadPlanStep.getName());
        aExceptionNode.setEnabled(pLoadPlanStep.isEnabled());
        aExceptionNode.setSessionKeywordsAsString(pLoadPlanStep.getKeywords());
        return aExceptionNode;
    }

    public SessionStepLogsBehavior mapFrom(LogsessionstepType logsessionstepType) {
        final SessionStepLogsBehavior sessionStepLogsBehavior;
        if (logsessionstepType.equals(LogsessionstepType.BYSCENARIOSETTINGS)) {
            sessionStepLogsBehavior = SessionStepLogsBehavior.BY_SCENARIO_SETTINGS;
        } else if (logsessionstepType.equals(LogsessionstepType.ERRORS)) {
            sessionStepLogsBehavior = SessionStepLogsBehavior.ERROR;
        } else if (logsessionstepType.equals(LogsessionstepType.NEVER)) {
            sessionStepLogsBehavior = SessionStepLogsBehavior.NEVER;
        } else {
            throw new UnsupportedOperationException();
        }
        return sessionStepLogsBehavior;
    }

    public SessionLogsBehavior mapFrom(LogsessionType logsessionType) {
        final SessionLogsBehavior sessionLogsBehavior;
        if (logsessionType.equals(LogsessionType.NEVER)) {
            sessionLogsBehavior = SessionLogsBehavior.NEVER;
        } else if (logsessionType.equals(LogsessionType.ALWAYS)) {
            sessionLogsBehavior = SessionLogsBehavior.ALWAYS;
        } else if (logsessionType.equals(LogsessionType.ERRORS)) {
            sessionLogsBehavior = SessionLogsBehavior.ERROR;
        } else {
            throw new UnsupportedOperationException();
        }
        return sessionLogsBehavior;
    }

    private Variable getVariableByName(String name, List<Variable> externalVariables) {
        for (final Variable v : externalVariables) {
            if (v.getName().equals(name)) {
                return v;
            }
        }
        return null;
    }

    private RestartType mapFromRestartTypeScenario(one.jodi.etl.service.loadplan.internalmodel.RestartType restartType) {
        if (restartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.RUN_SCENARIO_FROM_STEP)) {
            return OdiLoadPlanStepRunScenario.RestartType.RUN_SCENARIO_FROM_STEP;
        } else if (restartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.RUN_SCENARIO_FROM_TASK)) {
            return OdiLoadPlanStepRunScenario.RestartType.RUN_SCENARIO_FROM_TASK;
        } else if (restartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.RUN_SCENARIO_NEW_SESSION)) {
            return OdiLoadPlanStepRunScenario.RestartType.RUN_SCENARIO_NEW_SESSION;
        } else if (restartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.NONE)) {
            return null;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    //
    public oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepSerial.RestartType mapFromRestartTypeSerial(
            one.jodi.etl.service.loadplan.internalmodel.RestartType pRestartType) {
        if (pRestartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.SERIAL_STEP_ALL_CHILDREN)) {
            return oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepSerial.RestartType.SERIAL_STEP_ALL_CHILDREN;
        } else if (pRestartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.SERIAL_STEP_FROM_FAILURE)) {
            return oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepSerial.RestartType.SERIAL_STEP_FROM_FAILURE;
        } else if (pRestartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.NONE)) {
            return null;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepParallel.RestartType mapFromRestartTypeParallel(
            one.jodi.etl.service.loadplan.internalmodel.RestartType pRestartType) {
        if (pRestartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.PARALLEL_STEP_ALL_CHILDREN)) {
            return oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepParallel.RestartType.PARALLEL_STEP_ALL_CHILDREN;
        } else if (pRestartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.PARALLEL_STEP_FAILED_CHILDREN)) {
            return oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepParallel.RestartType.PARALLEL_STEP_FAILED_CHILDREN;
        } else if (pRestartType.equals(one.jodi.etl.service.loadplan.internalmodel.RestartType.NONE)) {
            return null;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public OdiLoadPlanStep getParentByName(String name) {
        return odiLoadPlanStepDB.get(name);
    }

    public Odiloadplanstep getLoadPlanStepByName(String name) {
        return internalModelStepDB.get(name);
    }

    @SuppressWarnings("rawtypes")
    public OdiLoadPlanAccessStrategy getLoadPlanAccessStrategy() {
        return loadPlanAccessStrategy;
    }
}
