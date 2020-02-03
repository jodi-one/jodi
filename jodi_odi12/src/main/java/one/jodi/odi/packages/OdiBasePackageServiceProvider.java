package one.jodi.odi.packages;

import com.sunopsis.dwg.function.SnpsFunctionBase;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.service.PackageServiceException;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.ModelStep.ModelActionType;
import one.jodi.etl.journalizng.JournalizingConfiguration;
import one.jodi.etl.service.packages.PackageServiceProvider;
import one.jodi.etl.service.packages.ProcedureNotFoundException;
import one.jodi.etl.service.procedure.ProcedureServiceProvider;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.OdiVariable.DataType;
import oracle.odi.domain.project.ProcedureOption.OptionType;
import oracle.odi.domain.project.StepVariable.EvaluateVariable;
import oracle.odi.domain.project.StepVariable.RefreshVariable;
import oracle.odi.domain.project.StepVariable.SetVariable;
import oracle.odi.domain.xrefs.expression.Expression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Provider is shared between 11g and 12c implementation. ODI 11g and 12c-specific
 * creation methods are factored out into a separate class.
 */
public abstract class OdiBasePackageServiceProvider<T extends IOdiEntity, S extends Step>
        extends PackageServiceProvider {
    public final static String ERROR_MESSAGE_80801 =
            "ODI variable %s has no logical schema.  Please set.";
    private final static Logger logger =
            LogManager.getLogger(OdiBasePackageServiceProvider.class);
    private final static String ERROR_MESSAGE_80150 =
            "Variable definition with name \"%1$s\" in package %2$s is " +
                    "incorrect: Variable Type Code \"SET\" and Set Operator \"INCREMENT\" " +
                    "was selected but variable %1$s is not of required Numeric type.";
    private final static String ERROR_MESSAGE_80160 =
            "There were no interfaces found with PackageListItem '%1$s' " +
                    "in project '%2$s'.";
    private final static String ERROR_MESSAGE_80170 =
            "LabeledSteps can't be null in Package %s";
    private final static String ERROR_MESSAGE_80180 =
            "Could not resolve a step for label GotoOnFinalSuccess %s in Package %s";
    private final static String ERROR_MESSAGE_80190 =
            "Null VariableTypeCode for variable %s";
    private final static String ERROR_MESSAGE_80611 =
            "Unknown VariableTypeCode %s";
    private final static String ERROR_MESSAGE_80621 =
            "Error creating Variable step %s. No matching variable found in ODI.";
    private final static String ERROR_MESSAGE_80630 =
            "Error creating package. Unknown Set variable operator %s";
    private final static String ERROR_MESSAGE_80640 =
            "Error creating procedure step. No option matching parameter %s";
    private final static String ERROR_MESSAGE_80650 =
            "Procedure with name %s not found.";
    private final static String ERROR_MESSAGE_80660 =
            "The command '%1$s' is not a valid command (%2$s).";
    private final static String ERROR_MESSAGE_80665 =
            "The package %1$s to be added as a scenario to package %2$s does not exist " +
                    "in the project. Please review the spelling of this package reference.";
    private final static String ERROR_MESSAGE_80680 =
            "%1$s packages with name %2$s exist in project %3$s. Please remove " +
                    "duplicate packages manually before proceeding.";
    private final static String ERROR_MESSAGE_80711 =
            "Unknown exception step type.";
    private final static String PACKAGE_STEP_COMMAND =
            "OdiStartScen -SCEN_NAME=%s -SCEN_VERSION=001 -SYNC_MODE=1";
    private final static String PACKAGE_STEP_COMMAND_ASYNC =
            "OdiStartScen -SCEN_NAME=%s -SCEN_VERSION=001 -SYNC_MODE=2";
    private final OdiInstance odiInstance;
    private final OdiPackageAccessStrategy<T, S> packageAccessStrategy;
    private final ProcedureServiceProvider procedureService;
    private final OdiVariableAccessStrategy odiVariableService;
    private final EtlSubSystemVersion etlSubSystemVersion;
    private final DatabaseMetadataService databaseMetadataService;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;

    protected OdiBasePackageServiceProvider(
            final OdiInstance odiInstance,
            final OdiPackageAccessStrategy<T, S> packageAccessStrategy,
            final ProcedureServiceProvider procedureService,
            final OdiVariableAccessStrategy odiVariableService,
            final JodiProperties properties,
            final ErrorWarningMessageJodi errorWarningMessages,
            final EtlSubSystemVersion etlSubSystemVersion,
            final JournalizingContext journalizingContext,
            final DatabaseMetadataService databaseMetadataService) {
        super(journalizingContext);
        this.odiInstance = odiInstance;
        this.packageAccessStrategy = packageAccessStrategy;
        this.procedureService = procedureService;
        this.odiVariableService = odiVariableService;
        this.properties = properties;
        this.errorWarningMessages = errorWarningMessages;
        this.etlSubSystemVersion = etlSubSystemVersion;
        this.databaseMetadataService = databaseMetadataService;
    }

    //
    // CRUD OPERATIONS for ODI Project, Folder, Variable etc.
    //

    @Override
    public boolean packageExists(final String packageName, final String folderPath) {
        assert (folderPath != null && !folderPath.trim().equals(""));
        return packageAccessStrategy.findPackage(packageName, folderPath,
                properties.getProjectCode()) != null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removePackage(final String packageName, final String folderPath) {
        if (properties.isUpdateable()) {
            return;
        }
        packageAccessStrategy.removePackage(packageName.toUpperCase(), folderPath,
                properties.getProjectCode());
    }

    protected Collection<OdiUserProcedure> findProcedures(final String procedureName,
                                                          final String projectCode) {
        return this.procedureService.findProcedures(procedureName, projectCode);
    }

    protected OdiVariable findProjectVariable(final String projectVariableName,
                                              final String projectCode) {
        OdiVariable odiVariable =
                odiVariableService.findProjectVariable(projectVariableName, projectCode);
        if (odiVariable != null && odiVariable.getLogicalSchema() == null) {
            String msg = errorWarningMessages.formatMessage(80801, ERROR_MESSAGE_80801,
                    this.getClass(),
                    projectVariableName);
            throw new RuntimeException(msg);
        }
        return odiVariable;
    }

    @Override
    public void removePackages(final List<ETLPackageHeader> headers,
                               final boolean raiseErrorOnFailure) {
        for (ETLPackageHeader header : headers) {
            try {
                removePackage(header.getPackageName().toUpperCase(),
                        header.getFolderCode());
            } catch (RuntimeException ex) {
                if (raiseErrorOnFailure)
                    throw ex;
            }
        }
    }

    // TODO - this constraint may be dropped in future
    protected void validatePackageIsNotMultiple(final String odiPackageString,
                                                final String folderPath,
                                                final String Project) {
        Collection<OdiPackage> odiPackages =
                this.packageAccessStrategy.findPackage(odiPackageString,
                        properties.getProjectCode());
        if (odiPackages.size() > 1) {
            String msg = errorWarningMessages.formatMessage(80680,
                    ERROR_MESSAGE_80680, this.getClass(),
                    odiPackages.size(), odiPackageString,
                    properties.getProjectCode());
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }
    }

    protected OdiUserProcedure findProjectProcedure(final String procedureName,
                                                    final String projectCode) {
        OdiUserProcedure found = null;
        for (OdiUserProcedure proc : findProcedures(procedureName, projectCode)) {
            found = proc;
            break;
        }
        return found;
    }

    @Override
    public Collection<String> getProcedureParameterNames(final String projectCode,
                                                         final String procedureName)
            throws ProcedureNotFoundException {
        Collection<OdiUserProcedure> procedureList =
                findProcedures(procedureName, projectCode);
        if (procedureList == null || procedureList.isEmpty()) {
            throw new ProcedureNotFoundException();
        }
        OdiUserProcedure odiProcedure = procedureList.iterator().next();
        return odiProcedure.getOptionNames();
    }

    //
    // Processing Related to Package Creation
    //

    @Override
    public void createPackages(final List<ETLPackage> jodiPackages,
                               final String projectCode,
                               final boolean raiseErrorOnFailure) {
        // Using jodiPackages in place of orderPackages
        for (ETLPackage jodiPackage : jodiPackages) {
            createPackage(jodiPackage, projectCode, raiseErrorOnFailure);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createPackage(final ETLPackage jodiPackage, final String projectCode,
                              boolean raiseErrorOnFailure) {
        // odifolder not found is always exception do not catch it.
        // check for 0 or 1 package
        validatePackageIsNotMultiple(jodiPackage.getPackageName(),
                jodiPackage.getFolderCode(), projectCode);
        // if it exists delete it
        try {
            boolean raiseErrorOnFailurePackage = false;
            removePackages(Arrays.asList(jodiPackage), raiseErrorOnFailurePackage);
        } catch (IllegalArgumentException | org.springframework.dao.DataAccessException e) {
            logger.info("Cannot delete packages: " + e.getMessage());
        }

        OdiPackage odiPackage =
                packageAccessStrategy.findPackage(jodiPackage.getPackageName().toUpperCase(),
                        jodiPackage.getFolderCode(), projectCode);
        if (odiPackage == null) {
            odiPackage = packageAccessStrategy.createPackage(
                    jodiPackage.getPackageName().toUpperCase(),
                    jodiPackage.getFolderCode(), projectCode);
        }
        odiPackage.setDescription((jodiPackage.getComments() != null ? jodiPackage.getComments() : "") + "\n" + jodiPackage.getDescription());
        OdiFolder odiFolder = odiPackage.getParentFolder();

        Map<String, Step> labeledSteps = jodiPackage.getGoToOnFinalSuccess() != null
                ? new HashMap<>() : null;

        @SuppressWarnings("unchecked")
        List<T> odiTransforms = (List<T>) odiFolder.getMappings();
        String firstItem = jodiPackage.getPackageListItems().isEmpty()
                ? null
                : jodiPackage.getTargetPackageList().iterator().next();
        if (odiTransforms != null && odiTransforms.isEmpty() &&
                raiseErrorOnFailure &&
                !jodiPackage.getPackageListItems().isEmpty() &&
                firstItem != null &&
                firstItem.trim() != null &&
                !firstItem.trim().equals("") &&
                firstItem.trim().length() > 0) {
            String msg = errorWarningMessages.formatMessage(80160,
                    ERROR_MESSAGE_80160, this.getClass(), StringUtils.joinStrings(
                            jodiPackage.getTargetPackageList(), ", "), projectCode);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }

        Step currentStep = addSteps(odiPackage, odiFolder, projectCode,
                jodiPackage.getFirstStep(), labeledSteps,
                odiTransforms);

        if (jodiPackage.getGoToOnFinalSuccess() != null && currentStep != null) {
            if (labeledSteps == null) {
                String msg = errorWarningMessages.formatMessage(80170,
                        ERROR_MESSAGE_80170, this.getClass(), jodiPackage.getPackageName());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new OdiPackageException(msg);
            }
            Step gotoStep = labeledSteps.get(jodiPackage.getGoToOnFinalSuccess()
                    .getLabel());

            if (gotoStep != null) {
                currentStep.setNextStepAfterSuccess(gotoStep);
            } else {
                // rollBackTransaction(transaction);
                String msg = errorWarningMessages.formatMessage(80180,
                        ERROR_MESSAGE_80180, this.getClass(), jodiPackage.getGoToOnFinalSuccess().getLabel(),
                        jodiPackage.getPackageName());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new OdiPackageException(msg);

            }
        }
    }

    protected Map<String, T> createTMap(
            Collection<T> odiTransforms) {
        Map<String, T> interfaceMap = new HashMap<>(
                odiTransforms.size());

        for (T odiTransform : odiTransforms) {
            interfaceMap
                    .put(JodiConstants.getScenarioNameFromObject(odiTransform.getName().toUpperCase(), true), odiTransform);
        }

        return interfaceMap;
    }

    protected Step addSteps(OdiPackage odiPackage, OdiFolder odiFolder,
                            String projectCode, ETLStep firstETLStep,
                            Map<String, Step> labeledSteps,
                            Collection<T> odiTransforms) {
        Step nextOdiStep = null;
        ETLStep currentETLStep = firstETLStep;
        Map<ETLStep, Step> failureFlowCache = new HashMap<>();
        Map<String, T> interfaceMap = createTMap(odiTransforms);
        while (currentETLStep != null) {
            Step failureFlow = currentETLStep.getNextStepOnFailure() == null ? null
                    : failureFlowCache.get(currentETLStep.getNextStepOnFailure());

            if (failureFlow == null
                    && currentETLStep.getNextStepOnFailure() != null) {
                // Assumes that if the first step in the failure flow is the
                // same then the entire flow is the same
                failureFlow = createFailureStepChain(odiPackage, odiFolder,
                        currentETLStep.getNextStepOnFailure(), projectCode,
                        interfaceMap);

                failureFlowCache.put(currentETLStep.getNextStepOnFailure(),
                        failureFlow);
            }

            /*
             * Reusable mappings are not "stand-alone" mappings anymore,
             * and can't be executed by itself.
             * So we skip the mapping if it is temporary.
             */
            String regex = this.properties.getTemporaryInterfacesRegex();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            Matcher matcher = pattern.matcher(currentETLStep.getName());
            boolean found = matcher.find();
            if (found && !etlSubSystemVersion.isVersion11()) {
                currentETLStep = currentETLStep.getNextStepOnSuccess();
                continue;
            }

            nextOdiStep = createNextStep(odiPackage, odiFolder, nextOdiStep,
                    currentETLStep, projectCode, failureFlow, interfaceMap);

            if (nextOdiStep == null) {
                //TODO - Temporary commented out.
                // changed to commented: 20191129
                throw new PackageServiceException("Error creating step");
            } else if (labeledSteps != null
                    && StringUtils.hasLength(currentETLStep.getLabel())) {
                labeledSteps.put(currentETLStep.getLabel(), nextOdiStep);
            }

            currentETLStep = currentETLStep.getNextStepOnSuccess();
        }

        return nextOdiStep;
    }

    protected Step createFailureStepChain(OdiPackage odiPackage,
                                          OdiFolder odiFolder, ETLStep firstETLStep, String projectCode,
                                          Map<String, T> interfaceMap) {
        Step firstStep = null;

        if (firstETLStep != null) {
            Step lastStep = null;
            ETLStep currentETLStep = firstETLStep;

            while (currentETLStep != null) {
                Step step = createOdiStep(odiPackage, odiFolder,
                        currentETLStep, projectCode, null, interfaceMap);

                if (firstStep == null) {
                    firstStep = step;
                }

                if (lastStep != null) {
                    lastStep.setNextStepAfterSuccess(step);
                }

                lastStep = step;

                currentETLStep = currentETLStep.getNextStepOnSuccess();
            }
        }
        return firstStep;
    }

    protected Step createNextStep(OdiPackage odiPackage, OdiFolder odiFolder,
                                  Step previousStep, ETLStep jodiStep, String projectCode,
                                  Step failureFlow, Map<String, T> interfaceMap) {
        Step nextStep = createOdiStep(odiPackage, odiFolder, jodiStep,
                projectCode, failureFlow, interfaceMap);

        if (nextStep != null) {
            if (previousStep == null) {
                odiPackage.setFirstStep(nextStep);
            } else {
                previousStep.setNextStepAfterSuccess(nextStep);
            }
        }

        return nextStep;
    }

    protected Step createOdiStep(OdiPackage odiPackage, OdiFolder odiFolder,
                                 ETLStep jodiStep, String projectCode, Step failureFlow,
                                 Map<String, T> interfaceMap) {
        Step step = null;
        if (jodiStep instanceof VariableStep) {
            step = createVariableStep((VariableStep) jodiStep, projectCode,
                    odiPackage, failureFlow);
        } else if (jodiStep instanceof ProcedureStep) {
            step = createProcedureStep((ProcedureStep) jodiStep, projectCode,
                    odiPackage, failureFlow);
        } else if (jodiStep instanceof PackageStep) { // verified
            Boolean async = ((PackageStep) jodiStep).executeAsynchronously();
            step = createPackageStep(odiPackage, jodiStep.getName(),
                    getStepLabel(jodiStep), (async == null ? false : async),
                    failureFlow);
        } else if (jodiStep instanceof CommandStep) { // verified
            CommandStep commandType = (CommandStep) jodiStep;

            List<StepParameter> parameters = commandType.getParameters();

            step = createCommandStep(odiPackage, commandType.getName(),
                    getStepLabel(commandType), parameters, failureFlow);

        } else if (jodiStep instanceof ModelStep) {
            step = createModelStep((ModelStep) jodiStep, odiFolder, odiPackage,
                    false, failureFlow);
        } else if (jodiStep instanceof InterfaceStep) {
            InterfaceStep etlInterfaceStep = (InterfaceStep) jodiStep;
            T odiTransform = interfaceMap.get(JodiConstants.getScenarioNameFromObject(etlInterfaceStep.getName(), true));

            if (odiTransform == null) {

                throw new
                        PackageServiceException("Could not find interface " + JodiConstants.getScenarioNameFromObject(etlInterfaceStep.getName(), true));
            } else {
                step = createInterfaceStep(etlInterfaceStep, odiPackage,
                        odiTransform, projectCode, failureFlow);

            }
        } else {
            String msg = errorWarningMessages.formatMessage(80711, ERROR_MESSAGE_80711, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }

        return step;
    }

    protected StepModel createModelStep(ModelStep modelType, OdiFolder odiFolder,
                                        OdiPackage odiPackage, boolean isJournalized, Step failureFlow) {
        boolean createSubscribers = getBooleanValue(
                modelType.getCreateSubscribers(), false);
        boolean dropSubscribers = getBooleanValue(
                modelType.getDropSubscribers(), false);
        boolean extendWindow = getBooleanValue(modelType.getExtendWindow(),
                false);
        boolean installJournalization = getBooleanValue(
                modelType.getInstallJournalization(), false);
        boolean lockSubscribers = getBooleanValue(
                modelType.getLockSubscribers(), false);
        boolean purgeJournal = getBooleanValue(modelType.getPurgeJournal(),
                false);
        boolean uninstallJournalization = getBooleanValue(
                modelType.getUninstallJournalization(), false);
        boolean unlockSubscribers = getBooleanValue(
                modelType.getUnlockSubscribers(), false);
        ModelActionType action = (modelType.getActionType() == null ? ModelActionType.JOURNALIZE
                : modelType.getActionType());

        StepModel result = null;
        StepModel lastStep = null;
        List<JournalizingConfiguration> config = getJournalizingConfigurationFromContext();
        IOdiModelFinder finder = ((IOdiModelFinder) odiInstance
                .getTransactionalEntityManager().getFinder(OdiModel.class));
        for (JournalizingConfiguration options : config) {
            if (options.getModelCode().equals(
                    properties.getProperty(modelType.getModel()))) {
                OdiModel odiModel = finder.findByCode(options.getModelCode());
                StepModel stepModel = new StepModel(odiPackage, odiModel,
                        getStepLabel(modelType));

                if (failureFlow != null) {
                    stepModel.setNextStepAfterFailure(failureFlow);
                }

                // add subscribers - by default use subscribers associated with
                // CDC infrastructure
                String[] subscribers = null;
                if ((modelType.getSubscriber() == null)
                        || (modelType.getSubscriber().equals(""))) {
                    subscribers = options.getSubscribers().toArray(
                            new String[0]);
                } else {
                    subscribers = new String[1];
                    subscribers[0] = modelType.getSubscriber().trim();
                }

                switch (action) {
                    case JOURNALIZE:
                        if (odiModel.getJKM().isConsistentJournalize()) {
                            stepModel
                                    .setAction(new StepModel.ConsistentJournalizeModel(
                                            createSubscribers, dropSubscribers,
                                            installJournalization,
                                            uninstallJournalization, subscribers,
                                            extendWindow, purgeJournal,
                                            lockSubscribers, unlockSubscribers));
                        } else {
                            stepModel
                                    .setAction(new StepModel.SimpleJournalizeModel(
                                            createSubscribers, dropSubscribers,
                                            installJournalization,
                                            uninstallJournalization, subscribers));
                        }
                        break;
                    case CONTROL:
                        stepModel
                                .setAction(new StepModel.ControlModel(false, true));
                        break;
                    case REVERSE_ENGINEER:
                        stepModel.setAction(new StepModel.ReverseModel());
                        break;
                }

                if (result == null) {
                    result = lastStep = stepModel;
                } else {
                    lastStep.setNextStepAfterSuccess(stepModel);
                }

                lastStep = stepModel;
            }
        }

        return result;
    }

    protected boolean getBooleanValue(Boolean source, boolean defaultValue) {
        return (source == null ? defaultValue : source);
    }

    protected StepOdiCommand createCommandStep(OdiPackage odiPackage,
                                               String commandName, String label, List<StepParameter> parameters,
                                               Step failureFlow) {
        StringBuilder commandStr = new StringBuilder(commandName);

        if (parameters != null) {
            for (StepParameter p : parameters) {
                String cmd = " \"-" + p.getName() + "=" + p.getValue() + "\"";
                commandStr.append(cmd);
            }
        }
        StepOdiCommand odicmnd = new StepOdiCommand(odiPackage, label);
        if (etlSubSystemVersion.isVersion11()) {
            validateCommand(commandStr.toString());
        }
        odicmnd.setCommandExpression(new Expression(commandStr.toString(),
                null, Expression.SqlGroupType.NONE));

        if (failureFlow != null) {
            odicmnd.setNextStepAfterFailure(failureFlow);
        }
        return odicmnd;
    }

    protected StepVariable createVariableStep(VariableStep jodiVariable,
                                              String projectCode, OdiPackage odiPackage, Step failureFlow) {
        StepVariable result = null;

        if (jodiVariable.getStepType() == null) {
            String msg = errorWarningMessages.formatMessage(80190,
                    ERROR_MESSAGE_80190, this.getClass(), jodiVariable.getName());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new OdiPackageException(msg);
        }

        switch (jodiVariable.getStepType()) {
            case REFRESH:
                result = createRefreshVariableStep(jodiVariable, projectCode,
                        odiPackage);

                if (failureFlow != null) {
                    result.setNextStepAfterFailure(failureFlow);
                }
                break;
            case EVALUATE:
                result = createEvaluateVariableStep(jodiVariable, projectCode,
                        odiPackage);
                break;
            case SET:
                result = createSetVariableStep(jodiVariable, projectCode, odiPackage);
                break;
            case DECLARE:
                result = createDeclareVariableStep(jodiVariable, projectCode,
                        odiPackage);
                break;
            default:
                String msg = errorWarningMessages.formatMessage(80611,
                        ERROR_MESSAGE_80611, this.getClass(), jodiVariable.getStepType());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new OdiPackageException(msg);
        }
        return result;
    }

    private StepVariable createDeclareVariableStep(VariableStep jodiVariable,
                                                   String projectCode,
                                                   OdiPackage odiPackage) {
        OdiVariable odiVariable = findProjectVariable(
                jodiVariable.getName(), projectCode);
        StepVariable stepRefVar = null;
        if (odiVariable != null) {
            stepRefVar = new StepVariable(odiPackage, odiVariable,
                    odiVariable.getName());
            StepVariable.DeclareVariable RefVar = new StepVariable.DeclareVariable(
                    stepRefVar);
            stepRefVar.setAction(RefVar);
        } else {
            String msg = errorWarningMessages.formatMessage(80621,
                    ERROR_MESSAGE_80621, this.getClass(), jodiVariable.getName());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new OdiPackageException(msg);
        }

        return stepRefVar;
    }

    protected StepVariable createRefreshVariableStep(VariableStep jodiVariable,
                                                     String projectCode, OdiPackage odiPackage) {
        OdiVariable odiVariable = findProjectVariable(
                jodiVariable.getName(), projectCode);
        StepVariable stepRefVar = null;
        if (odiVariable != null) {
            stepRefVar = new StepVariable(odiPackage, odiVariable,
                    odiVariable.getName());
            RefreshVariable RefVar = new StepVariable.RefreshVariable(
                    stepRefVar);
            stepRefVar.setAction(RefVar);
        } else {
            String msg = errorWarningMessages.formatMessage(80621,
                    ERROR_MESSAGE_80621, this.getClass(), jodiVariable.getName());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new OdiPackageException(msg);
        }

        return stepRefVar;
    }

    protected StepVariable createSetVariableStep(VariableStep variable,
                                                 String projectCode, OdiPackage odiPackage) {
        StepVariable result = null;
        OdiVariable odiVariable = findProjectVariable(variable.getName(),
                projectCode);
        if (odiVariable != null) {
            result = new StepVariable(odiPackage, odiVariable,
                    getStepLabel(variable));
            SetVariable setVariable = null;

            switch (variable.getSetOperator()) {
                case ASSIGN:
                    setVariable = new StepVariable.SetVariable(variable.getValue());
                    break;
                case INCREMENT:
                    // TODO MUST be moved to validation package
                    if (!(odiVariable.getDataType() == DataType.NUMERIC)) {
                        String msg = errorWarningMessages.formatMessage(80150,
                                ERROR_MESSAGE_80150, this.getClass(), odiVariable.getName(),
                                odiPackage.getName());
                        logger.error(msg);
                        throw new UnRecoverableException(msg);
                    }
                    Integer increment = variable.getIncrementBy();
                    setVariable = new StepVariable.SetVariable(
                            increment == null ? Integer.valueOf(1) : increment);
                    break;
                default:
                    String msg = errorWarningMessages.formatMessage(80630,
                            ERROR_MESSAGE_80630, this.getClass(), variable.getSetOperator());
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(), msg,
                            MESSAGE_TYPE.ERRORS);
                    throw new OdiPackageException(msg);
            }
            result.setAction(setVariable);
        } else {
            String msg = errorWarningMessages.formatMessage(80621,
                    ERROR_MESSAGE_80621, this.getClass(), variable.getName());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new OdiPackageException(msg);
        }

        return result;
    }

    protected StepProcedure createProcedureStep(ProcedureStep execProcedure,
                                                String projectCode, OdiPackage odiPackage, Step failureFlow) {
        OdiUserProcedure odiProcedure = findProjectProcedure(
                execProcedure.getName(), projectCode);
        StepProcedure stepProcedure = null;

        if (odiProcedure != null) {
            stepProcedure = new StepProcedure(odiPackage, odiProcedure,
                    getStepLabel(execProcedure));
            Collection<StepParameter> parameters = execProcedure
                    .getParameters();
            if (parameters != null) {
                Map<String, IOptionValue> procOptions = createOptionValueMap(stepProcedure
                        .getProcedureOptions());

                for (StepParameter p : parameters) {
                    IOptionValue value = procOptions.get(p.getName());

                    if ((value != null)
                            && (value.getOptionType() == OptionType.CHECKBOX)) {
                        value.setValue(Boolean.valueOf(p.getValue()));
                    } else if (value != null) {
                        value.setValue(p.getValue());
                    } else {
                        String msg = errorWarningMessages.formatMessage(80640,
                                ERROR_MESSAGE_80640, this.getClass(), p.getName());
                        errorWarningMessages.addMessage(
                                errorWarningMessages.assignSequenceNumber(),
                                msg, MESSAGE_TYPE.ERRORS);
                        throw new OdiPackageException(msg);
                    }
                }
            }

            if (failureFlow != null) {
                stepProcedure.setNextStepAfterFailure(failureFlow);
            }
        } else {
            String msg =
                    errorWarningMessages.formatMessage(80650,
                            ERROR_MESSAGE_80650, this.getClass(),
                            execProcedure.getName());
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }
        return stepProcedure;
    }

    protected Map<String, IOptionValue> createOptionValueMap(
            List<IOptionValue> procedureOptions) {
        Map<String, IOptionValue> result = new HashMap<>(
                procedureOptions.size());

        for (IOptionValue ov : procedureOptions) {
            result.put(ov.getName(), ov);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    protected S createInterfaceStep(InterfaceStep jodiStep,
                                    OdiPackage odiPackage, T odiTransform,
                                    String projectCode, Step failureFlow) {
        S stepTransformation = null;

        if (odiTransform != null) {
            if (jodiStep.executeAsynchronously() || jodiStep.useScenario()) {
                logger.info("Generating scenario for step ----> " + jodiStep.getName());
                // OdiStartScen "-SCEN_NAME=LOAD_UP_TO_DATE" "-SCEN_VERSION=001" "-SYNC_MODE=2"
                String commandName = "OdiStartScen";
                String label = jodiStep.getName();
                List<StepParameter> parameters = new ArrayList<StepParameter>();
                parameters.add(new StepParameter() {

                    @Override
                    public String getValue() {
                        return JodiConstants.getScenarioNameFromObject(odiTransform.getName(), true);
                    }

                    @Override
                    public String getName() {
                        return "SCEN_NAME";
                    }
                });
                parameters.add(new StepParameter() {

                    @Override
                    public String getValue() {
                        return "001";
                    }

                    @Override
                    public String getName() {
                        return "SCEN_VERSION";
                    }
                });
                parameters.add(new StepParameter() {

                    @Override
                    public String getValue() {
                        return jodiStep.executeAsynchronously() ? "2" : "1";
                    }

                    @Override
                    public String getName() {
                        return "SYNC_MODE";
                    }
                });
                stepTransformation = (S) createCommandStep(odiPackage,
                        commandName, label, parameters,
                        failureFlow);


            } else {
                stepTransformation = packageAccessStrategy.newStep(odiPackage,
                        odiTransform, jodiStep.getLabel());
            }
            if (failureFlow != null) {
                stepTransformation.setNextStepAfterFailure(failureFlow);
            }
        } else {
            logger.info("Couldn't find step ----> " + jodiStep.getName());
        }

        return stepTransformation;
    }

    protected StepOdiCommand createPackageStep(OdiPackage odiPackage, String packageName,
                                               String label, boolean isAsynchronous,
                                               Step failureFlow) {
        StepOdiCommand odicmnd = new StepOdiCommand(odiPackage, label);
        if (this.packageAccessStrategy.findPackage(packageName,
                this.properties.getProjectCode())
                .isEmpty()) {
            String msg = errorWarningMessages.formatMessage(80665, ERROR_MESSAGE_80665,
                    this.getClass(),
                    label, odiPackage.getName());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
            logger.warn(msg);
        }
        String command =
                isAsynchronous ? String.format(PACKAGE_STEP_COMMAND_ASYNC, packageName)
                        : String.format(PACKAGE_STEP_COMMAND, packageName);
        validateCommand(command);
        odicmnd.setCommandExpression(new Expression(command, null,
                Expression.SqlGroupType.NONE));
        if (failureFlow != null) {
            odicmnd.setNextStepAfterFailure(failureFlow);
        }
        return odicmnd;
    }

    protected StepVariable createEvaluateVariableStep(VariableStep jodiVariable,
                                                      String projectCode, OdiPackage odiPackage) {
        OdiVariable odiVariable = findProjectVariable(
                jodiVariable.getName(), projectCode);

        StepVariable stepVariable = new StepVariable(odiPackage, odiVariable,
                getStepLabel(jodiVariable));
        EvaluateVariable evaluateVariable = new StepVariable.EvaluateVariable(
                jodiVariable.getOperator(), jodiVariable.getValue());
        stepVariable.setAction(evaluateVariable);

        return stepVariable;
    }

    protected String getStepLabel(ETLStep jodiStep) {
        return (StringUtils.hasLength(jodiStep.getLabel()) ? jodiStep.getLabel()
                : jodiStep.getName());
    }

    protected void validateCommand(String command) {
        try {
            SnpsFunctionBase tool = SnpsFunctionBase.getCoreOdiTool(command);
            tool.verifyProperty();
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            String msg = errorWarningMessages.formatMessage(80660,
                    ERROR_MESSAGE_80660, this.getClass(), command, message);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    @Override
    public boolean projectVariableExists(String projectCode, String variable) {
        return databaseMetadataService.projectVariableExists(projectCode, variable);
    }

}
