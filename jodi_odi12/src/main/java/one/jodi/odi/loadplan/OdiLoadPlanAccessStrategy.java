package one.jodi.odi.loadplan;

import com.google.inject.Inject;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.odi.loadplan.service.OdiLoadPlanServiceException;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.OdiUserProcedure;
import oracle.odi.domain.project.OdiVariable;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder;
import oracle.odi.domain.project.finder.IOdiVariableFinder;
import oracle.odi.domain.runtime.loadplan.OdiCaseElse;
import oracle.odi.domain.runtime.loadplan.OdiCaseWhen;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanException;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStep;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepCase;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepParallel;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepSerial;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanStepVariable;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlanVariable;
import oracle.odi.domain.runtime.loadplan.OdiVariableChangedDatatypeException;
import oracle.odi.domain.runtime.loadplan.finder.IOdiLoadPlanFinder;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.OdiScenarioFolder;
import oracle.odi.domain.runtime.scenario.Tag;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFolderFinder;
import oracle.odi.generation.OdiVariableTextGeneratorException;
import oracle.odi.generation.support.OdiVariableTextGeneratorDwgImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Strategy for CRUD operations on OdiLoadPlans and associated artifacts.
 *
 * <p>
 * A is OdiLoadplan
 * B extends OdiEntitiy
 */

public abstract class OdiLoadPlanAccessStrategy<A extends Object, B extends IOdiEntity> {

    private static final Logger logger = LogManager.getLogger(OdiLoadPlanAccessStrategy.class);

    private final OdiVariableAccessStrategy odiVariableService;
    private final OdiInstance odiInstance;

    @Inject
    public OdiLoadPlanAccessStrategy(final OdiInstance odiInstance,
                                     final OdiVariableAccessStrategy odiVariableService) {
        this.odiInstance = odiInstance;
        this.odiVariableService = odiVariableService;
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection<A> findAllLoadPlans() {
        IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) odiInstance.getTransactionalEntityManager()
                                                                            .getFinder(OdiLoadPlan.class);
        Collection<OdiLoadPlan> findAll = (Collection<OdiLoadPlan>) loadPlanFinder.findAll();
        return (Collection<A>) findAll;
    }

    @SuppressWarnings("unchecked")
    public A findLoadPlanByName(final String pName) {
        IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) odiInstance.getTransactionalEntityManager()
                                                                            .getFinder(OdiLoadPlan.class);
        return (A) loadPlanFinder.findByName(pName);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected void truncateLoadPlan(final A pOdiLoadPlan) {
        removeAllSteps(pOdiLoadPlan);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeAllSteps(final A pOdiLoadPlan) {
        while (((OdiLoadPlan) pOdiLoadPlan).getRootStep()
                                           .getChildrenSteps()
                                           .size() > 0) {
            removeSteps(pOdiLoadPlan);
        }
        for (int i = 0; i < ((OdiLoadPlan) pOdiLoadPlan).getExceptions()
                                                        .size(); i++) {
            OdiLoadPlanException[] exceptions = ((OdiLoadPlan) pOdiLoadPlan).getExceptions()
                                                                            .toArray(
                                                                                    new OdiLoadPlanException[((OdiLoadPlan) pOdiLoadPlan).getExceptions()
                                                                                                                                         .size()]);
            ((OdiLoadPlan) pOdiLoadPlan).removeException(exceptions[i]);
        }
    }

    public void removeSteps(final A pOdiLoadPlan) {
        List<OdiLoadPlanStep> children = ((OdiLoadPlan) pOdiLoadPlan).getRootStep()
                                                                     .getChildrenSteps();
        for (int i = 0; i < children.size(); i++) {
            ((OdiLoadPlan) pOdiLoadPlan).getRootStep()
                                        .removeStep(children.get(i));
        }
    }


    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public A createLoadPlan(final String pName, final String pFolder) {
        OdiLoadPlan odiLoadPlan = new OdiLoadPlan(pName);
        odiLoadPlan.setScenarioFolder(findScenarioFolder(pFolder));
        return (A) odiLoadPlan;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiScenarioFolder findScenarioFolder(final String pFolder) {
        IOdiScenarioFolderFinder loadPlanFinder = (IOdiScenarioFolderFinder) odiInstance.getTransactionalEntityManager()
                                                                                        .getFinder(
                                                                                                OdiScenarioFolder.class);
        OdiScenarioFolder odiScenarioFolder = loadPlanFinder.findByName(pFolder);
        return odiScenarioFolder;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OdiScenarioFolder createScenarioFolder(final String pName) {
        OdiScenarioFolder folder = new OdiScenarioFolder(pName);
        odiInstance.getTransactionalEntityManager()
                   .persist(folder);
        return folder;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void merge(final A pOdiLoadPlan) {
        odiInstance.getTransactionalEntityManager()
                   .merge(((OdiLoadPlan) pOdiLoadPlan));
    }

    public void rollback() {
        DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        odiInstance.getTransactionManager()
                   .rollback(txnStatus);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persist(A pOdiLoadPlan) {
        odiInstance.getTransactionalEntityManager()
                   .persist(((OdiLoadPlan) pOdiLoadPlan));
    }

    private String stripGlobal(final String varName) {
        return varName.replace(OdiLoadPlanTransformationService.GLOBAL_VAR_PREFIX_DOT, "");
    }

    OdiVariable findVariable(String pName, String pProjectCode) {
        String globalPrefixWoDot = OdiLoadPlanTransformationService.GLOBAL_VAR_PREFIX_DOT.substring(0,
                                                                                                    OdiLoadPlanTransformationService.GLOBAL_VAR_PREFIX_DOT.length() -
                                                                                                            1);

        // find project variable
        if (pProjectCode != null && !pProjectCode.startsWith(globalPrefixWoDot)) {
            Optional<OdiVariable> odiVariable = odiVariableService.findAllVariables()
                                                                  .stream()
                                                                  .filter(v -> pName.equals(
                                                                          pProjectCode + "." + v.getName()))
                                                                  .findFirst();
            if (odiVariable.isPresent()) {
                return odiVariable.get();
            }
        }

        // find global variable
        if (!pName.contains(".")) {
            Optional<OdiVariable> odiVariable = odiVariableService.findAllGlobalVariables()
                                                                  .stream()
                                                                  .filter(v -> pName.equals(v.getName()) ||
                                                                          pName.equals(stripGlobal(v.getName())))
                                                                  .findFirst();
            if (odiVariable.isPresent()) {
                return odiVariable.get();
            }
        }

        assert (false) : "Can't find variable  '" + pName + "'.";
        throw new OdiLoadPlanServiceException("Can't find variable  '" + pName + "'.");
    }

    OdiLoadPlanVariable addVariable(OdiLoadPlan odiLoadPlan, OdiVariable var) {
        OdiVariableTextGeneratorDwgImpl varTextGen = new OdiVariableTextGeneratorDwgImpl(odiInstance);
        OdiLoadPlanVariable odiLoadPlanVariable = null;
        try {
            odiLoadPlanVariable = odiLoadPlan.addVariable(var, varTextGen);
        } catch (OdiVariableChangedDatatypeException | OdiVariableTextGeneratorException e) {
            logger.error(e);
        }
        return odiLoadPlanVariable;
    }

    //
    // find by ID - OK to keep in this class
    //

    public OdiUserProcedure findProcedure(Serializable sourceComponentId) {
        IOdiUserProcedureFinder finder = (IOdiUserProcedureFinder) odiInstance.getTransactionalEntityManager()
                                                                              .getFinder(OdiUserProcedure.class);
        return (OdiUserProcedure) finder.findById(sourceComponentId);
    }

    public OdiVariable findVariable(Serializable sourceComponentId) {
        IOdiVariableFinder finder = (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiVariable.class);
        return (OdiVariable) finder.findById(sourceComponentId);

    }

    public abstract B findMapping(Serializable sourceComponentId);

    public OdiPackage findPackage(Serializable sourceComponentId) {
        IOdiPackageFinder finder = (IOdiPackageFinder) odiInstance.getTransactionalEntityManager()
                                                                  .getFinder(OdiPackage.class);
        return (OdiPackage) finder.findById(sourceComponentId);
    }

    public OdiScenario findScenario(String scenario, int scenarioVersion) {
        IOdiScenarioFinder mappingsFinder = (IOdiScenarioFinder) odiInstance.getTransactionalEntityManager()
                                                                            .getFinder(OdiScenario.class);
        String version = String.format("%03d", scenarioVersion);
        return mappingsFinder.findByTag(new Tag(scenario, version));
    }

    //
    //
    //

    public void verifyOdiLoadPlanPathExists(A odiLoadPlan, String path) {
        if (!(odiLoadPlan instanceof OdiLoadPlan)) {
            String msg = "Can't verify odiloadplan path since this is not an odiloadplan";
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        String[] pathPieces = path.split("/");
        OdiLoadPlanStep currentStep = null;
        List<OdiLoadPlanStep> children = null;
        List<OdiCaseWhen> whenchildren = null;
        OdiCaseElse elsechildren = null;
        for (int pathNumber = 1; pathNumber < pathPieces.length; pathNumber++) {
            String aPath = pathPieces[pathNumber];
            if (pathNumber == 1) {
                currentStep = ((OdiLoadPlan) odiLoadPlan).getRootStep();
                children = ((OdiLoadPlanStepSerial) currentStep).getChildrenSteps();
                if (!(currentStep.getName()
                                 .equals(aPath))) {
                    String msg = "Can't verify odiloadplan path since the rootpath isn't " + "equal to: '" + aPath +
                            "' but is: " + currentStep.getName() + ".";
                    logger.warn(msg);
                    throw new OdiLoadPlanServiceException(msg);
                }
            } else {
                boolean found = false;
                if (children == null) {
                    continue;
                }
                for (OdiLoadPlanStep child : children) {
                    if (child.getName()
                             .equals(aPath) && (child.getParentElement()
                                                     .getName()
                                                     .equals(currentStep.getName()) ||
                            currentStep instanceof OdiLoadPlanStepCase)) {
                        found = true;
                        currentStep = child;
                        if (child instanceof OdiLoadPlanStepSerial) {
                            children = ((OdiLoadPlanStepSerial) child).getChildrenSteps();
                        } else if (child instanceof OdiLoadPlanStepParallel) {
                            children = ((OdiLoadPlanStepParallel) child).getChildrenSteps();
                        }
                    } else if (child instanceof OdiLoadPlanStepCase) {
                        whenchildren = ((OdiLoadPlanStepCase) child).getCaseWhenList();
                        elsechildren = ((OdiLoadPlanStepCase) child).getCaseElse();
                        currentStep = child;
                    }
                }
                if (whenchildren != null) {
                    for (OdiCaseWhen when : whenchildren) {
                        if (when.getName()
                                .equals(aPath)) {
                            found = true;
                        }
                        children.addAll(when.getRootStep()
                                            .getChildrenSteps());
                    }
                }
                if (elsechildren != null) {
                    if (elsechildren.getName()
                                    .equals(aPath)) {
                        found = true;
                    }
                    children.addAll(elsechildren.getRootStep()
                                                .getChildrenSteps());
                }
                if (!found) {
                    String msg = "Can't verify odiloadplan path since this element is " + "not found: " + aPath + ".";
                    logger.warn(msg);
                    throw new OdiLoadPlanServiceException(msg);
                }
            }
        }
    }

    public boolean verifyOdiLoadPlanStepVariableValues(OdiLoadPlan odiLoadPlan, String variableName, String stepName,
                                                       Boolean refresh, Object value) {
        for (OdiLoadPlanException e : odiLoadPlan.getExceptions()) {
            for (OdiLoadPlanStep step : e.getRootStep()
                                         .getChildrenSteps()) {
                if (step.getName()
                        .equals(stepName)) {
                    return validateStep(step, variableName, refresh, value);
                }
            }
        }
        for (OdiLoadPlanStep step : odiLoadPlan.getRootStep()
                                               .getChildrenSteps()) {
            if (step.getName()
                    .equals(stepName)) {
                return validateStep(step, variableName, refresh, value);
            }
        }
        logger.warn(
                String.format("Did not found step %s to validate in loadplan %s.", stepName, odiLoadPlan.getName()));
        return false;
    }

    private boolean validateStep(OdiLoadPlanStep step, String variableName, Boolean refresh, Object value) {
        for (OdiLoadPlanStepVariable lpsv : step.getLoadPlanStepVariables()) {
            if (lpsv.getLoadPlanVariable()
                    .getName()
                    .replace(OdiLoadPlanTransformationService.GLOBAL_VAR_PREFIX_DOT, "")
                    .equals(variableName)) {
                return validateStepVariable(lpsv, refresh, value);
            }
        }
        logger.warn(String.format("Did not found variable %s to validate in loadplan %s for step %s.", variableName,
                                  step.getLoadPlan()
                                      .getName(), step.getName()));
        return false;
    }

    private boolean validateStepVariable(OdiLoadPlanStepVariable lpsv, Boolean refresh, Object value) {
        if (lpsv.isRefresh() != refresh.booleanValue()) {
            logger.warn(String.format("Refresh not equal for variable %s in loadplan %s for step %s.",
                                      lpsv.getLoadPlanVariable()
                                          .getName(), lpsv.getLoadPlan()
                                                          .getName(), lpsv.getLoadPlanStep()
                                                                          .getName()));
        }
        if (lpsv.getValue() == null && value == null) {
            return lpsv.isRefresh() == refresh.booleanValue();
        }
        return lpsv.isRefresh() == refresh.booleanValue() && lpsv.getValue()
                                                                 .equals(value);
    }

    OdiVariable findGlobalVariable(String name) {
        return findVariable(name.replace(OdiLoadPlanTransformationService.GLOBAL_VAR_PREFIX_DOT, ""), null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteLoadPlans() {
//        DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
//        ITransactionManager tm = odiInstance.getTransactionManager();
//        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) odiInstance.getTransactionalEntityManager()
                                                                            .getFinder(OdiLoadPlan.class);
        Collection<OdiLoadPlan> loadPlans = loadPlanFinder.findAll();
        logger.info("Found " + loadPlans.size() + " loadplans to delete.");
        loadPlans.stream()
                 .forEach(l -> {
                     logger.info("Trying to delete loadplan " + l.getName() + ".");
                     odiInstance.getTransactionalEntityManager()
                                .remove((OdiLoadPlan) l);
                 });
        //     tm.commit(txnStatus);
    }
}