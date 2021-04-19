package one.jodi.etl.builder.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.base.util.Version;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.context.packages.TransformationCacheItem;
import one.jodi.core.etlmodel.ExecCommandType;
import one.jodi.core.etlmodel.ExecPackageType;
import one.jodi.core.etlmodel.ExecProcedureType;
import one.jodi.core.etlmodel.ModelType;
import one.jodi.core.etlmodel.ModelTypeActionEnum;
import one.jodi.core.etlmodel.Package;
import one.jodi.core.etlmodel.Parameter;
import one.jodi.core.etlmodel.SetOperatorEnum;
import one.jodi.core.etlmodel.StepType;
import one.jodi.core.etlmodel.Steps;
import one.jodi.core.etlmodel.VariableType;
import one.jodi.core.etlmodel.VariableTypeCodeEnum;
import one.jodi.etl.builder.PackageBuilder;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLStep;
import one.jodi.etl.internalmodel.InterfaceStep;
import one.jodi.etl.internalmodel.ModelStep;
import one.jodi.etl.internalmodel.StepParameter;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.VariableStep;
import one.jodi.etl.internalmodel.impl.CommandStepImpl;
import one.jodi.etl.internalmodel.impl.ETLPackageImpl;
import one.jodi.etl.internalmodel.impl.ETLStepImpl;
import one.jodi.etl.internalmodel.impl.InterfaceStepImpl;
import one.jodi.etl.internalmodel.impl.ModelStepImpl;
import one.jodi.etl.internalmodel.impl.PackageStepImpl;
import one.jodi.etl.internalmodel.impl.ProcedureStepImpl;
import one.jodi.etl.internalmodel.impl.StepParameterImpl;
import one.jodi.etl.internalmodel.impl.VariableStepImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PackageBuilderImpl implements PackageBuilder {

    private static final String ERROR_MESSAGE_03351 = "Unrecognized VariableTypeCodeEnum value %s";
    private static final String ERROR_MESSAGE_03360 = "Unrecognized VariableSetOperatorType value %s";
    private static final String ERROR_MESSAGE_03370 = "Unrecognized ModelActionType value %s";

    private static final Logger logger = LogManager.getLogger(PackageBuilderImpl.class);
    private static final String INTERFACE_STEP_NAME = "Step %d %s";
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final JodiProperties properties;
    private final PackageCache packageCache;

    @Inject
    public PackageBuilderImpl(final JodiProperties properties, final PackageCache packageCache,
                              final ErrorWarningMessageJodi errorWarningMessages) {
        this.properties = properties;
        this.packageCache = packageCache;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public ETLPackage transmute(final Package pPackage) {
        return createETLPackage(pPackage);
    }

    public void addTransformationToPackage(final ETLPackage etlPackage, final Transformation transformation) {
    }

    //TODO - this method needs to be streamlined a bit with regards to hooking ETLSteps up
    private ETLPackage createETLPackage(Package pPackage) {
        ETLStep firstFailureStep = createSteps(pPackage.getFailure(), null, null);
        List<InterfaceStep> interfaceSteps = createInterfaceSteps(pPackage, firstFailureStep);

        ETLStep firstBeforeStep = createSteps(pPackage.getBefore(), firstFailureStep,
                                              ((!interfaceSteps.isEmpty()) ? interfaceSteps.get(0) : null));
        ETLStep firstAfterStep = createSteps(pPackage.getAfter(), firstFailureStep, null);
        ETLStep finalSuccessStep = findStepByLabel(pPackage.getGotoOnFinalSuccess(), firstBeforeStep, firstAfterStep);


        if (firstBeforeStep != null) {
            ETLStep lastBeforerStep = firstBeforeStep;
            // find last ETLStep starting with Before Step
            while (lastBeforerStep.getNextStepOnSuccess() != null) {
                lastBeforerStep = lastBeforerStep.getNextStepOnSuccess();
            }
            ((ETLStepImpl) lastBeforerStep).setNextStepOnSuccess(firstAfterStep);
        } else if (!interfaceSteps.isEmpty()) {
            ETLStepImpl lastStep = (ETLStepImpl) interfaceSteps.get(interfaceSteps.size() - 1);
            lastStep.setNextStepOnSuccess(firstAfterStep);
        }
        ETLStep firstStep = (firstBeforeStep != null ? firstBeforeStep
                                                     : ((interfaceSteps != null && !interfaceSteps.isEmpty())
                                                        ? interfaceSteps.get(0) : firstAfterStep));

        StringBuilder description = new StringBuilder();
        if (properties.includeDetails()) {
            String newLine = System.getProperty("line.separator");
            description.append("Bulk creation operation for package '");
            description.append(pPackage.getPackageName());
            description.append("'.  Imported by ");
            description.append(System.getProperty("user.name"));
            description.append(" at ");
            DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            description.append(dateFormat.format(new Date()));
            description.append(newLine);
            description.append("Created by Jodi version ");
            description.append(Version.getProductVersion());
            description.append(" with build date ");
            description.append(Version.getBuildDate());
            description.append(" ");
            description.append(Version.getBuildTime());
        }


        ETLPackageImpl result =
                new ETLPackageImpl(pPackage.getPackageName(), pPackage.getFolderCode(), properties.getProjectCode(),
                                   firstBeforeStep, firstAfterStep, firstFailureStep, finalSuccessStep,
                                   pPackage.getPackageListItem(), pPackage.getExtension(), firstStep,
                                   description.toString(), interfaceSteps, pPackage.getComments());

        return result;
    }

    private ETLStep createSteps(Steps steps, ETLStep firstFailureStep, ETLStep afterLastStep) {
        ETLStepImpl firstStep = null;
        ETLStepImpl lastStep = null;

        if (steps != null && steps.getStep() != null) {
            for (JAXBElement<? extends StepType> element : steps.getStep()) {
                StepType st = element.getValue();
                ETLStep nextStep = null;

                if (st instanceof ExecPackageType) {
                    nextStep = createPackageStep((ExecPackageType) st);
                } else if (st instanceof ExecProcedureType) {
                    nextStep = createProcedureStep((ExecProcedureType) st);
                } else if (st instanceof VariableType) {
                    nextStep = createVariableStep((VariableType) st);
                } else if (st instanceof ModelType) {
                    nextStep = createModelStep((ModelType) st);
                } else if (st instanceof ExecCommandType) {
                    nextStep = createCommandStep((ExecCommandType) st);
                }
                if (nextStep == null) {
                    // unknown step type
                    throw new RuntimeException("Next step can't be null.");
                }


                ((ETLStepImpl) nextStep).setNextStepOnFailure(firstFailureStep);
                if (firstStep == null) {
                    firstStep = lastStep = (ETLStepImpl) nextStep;
                } else {
                    if (lastStep == null) {
                        // unknown step type
                        throw new RuntimeException("Last step can't be null.");
                    }
                    lastStep.setNextStepOnSuccess(nextStep);
                    lastStep = (ETLStepImpl) nextStep;
                }
            }

            if (lastStep != null) {
                lastStep.setNextStepOnSuccess(afterLastStep);
            }
        }
        return firstStep;
    }


    private List<InterfaceStep> createInterfaceSteps(final Package pPackage, final ETLStep firstFailureStep) {
        final List<InterfaceStep> interfaceSteps = new ArrayList<>();
        List<TransformationCacheItem> transformations =
                packageCache.getTransformationsForPackage(pPackage.getPackageListItem(), pPackage.getFolderCode());
        if (transformations == null) {
            logger.warn(
                    "No transformations from cache for package: " + pPackage.getPackageName() + " and packageList: " +
                            pPackage.getPackageListItem());
            return interfaceSteps;
        }
        for (TransformationCacheItem transformation : transformations) {
            createInterfaceStep(transformation.getName(), firstFailureStep, interfaceSteps,
                                transformation.getPackageSequence(), transformation.isAsynchronous());
        }
        return interfaceSteps;
    }

    private InterfaceStep createInterfaceStep(String transformation, ETLStep firstFailureStep,
                                              List<InterfaceStep> interfaceSteps, int packageSequence,
                                              boolean asynchronous) {
        String stepName = String.format(INTERFACE_STEP_NAME, packageSequence, transformation);


        final boolean useScenario = this.properties.getPropertyKeys()
                                                   .contains(JodiConstants.USE_SCENARIOS_INSTEAD_OF_MAPPINGS)
                                    ? Boolean.valueOf(
                this.properties.getProperty(JodiConstants.USE_SCENARIOS_INSTEAD_OF_MAPPINGS))
                                             .booleanValue() : false;

        InterfaceStepImpl step =
                new InterfaceStepImpl(transformation, stepName, null, firstFailureStep, packageSequence, asynchronous,
                                      useScenario);

        if (!interfaceSteps.isEmpty()) {
            ETLStepImpl lastStep = (ETLStepImpl) interfaceSteps.get(interfaceSteps.size() - 1);
            lastStep.setNextStepOnSuccess(step);
        }

        interfaceSteps.add(step);
        return step;
    }

    private ETLStep createProcedureStep(ExecProcedureType step) {
        ProcedureStepImpl result = new ProcedureStepImpl(step.getName(), step.getLabel());

        if (step.getParameters() != null && step.getParameters()
                                                .getParameter() != null && !step.getParameters()
                                                                                .getParameter()
                                                                                .isEmpty()) {
            for (Parameter p : step.getParameters()
                                   .getParameter()) {
                result.addParameter(createStepParameter(p));
            }
        }
        return result;
    }

    private ETLStep createPackageStep(ExecPackageType step) {
        PackageStepImpl result = new PackageStepImpl(step.getName(), step.getLabel());
        boolean async = step.isAsynchronous() != null ? step.isAsynchronous() : false;
        result.setExecuteAsynchronously(async);
        // TODO: setSourceFolderCode
        return result;
    }

    private ETLStep createVariableStep(VariableType step) {
        VariableStepImpl result = new VariableStepImpl(step.getName(), step.getLabel());

        result.setIncrementBy(step.getIncrementBy());
        result.getSetOperator(step.getOperator());
        result.setSetOperator(map(step.getSetOperator()));
        result.setStepType(map(step.getType()));
        result.setValue(step.getValue());
        return result;
    }

    private ETLStep createCommandStep(ExecCommandType step) {
        CommandStepImpl result = new CommandStepImpl(step.getName(), step.getLabel());

        if (step.getParameters() != null) {
            for (Parameter p : step.getParameters()
                                   .getParameter()) {
                result.addParameter(createStepParameter(p));
            }
        }
        return result;
    }

    private ETLStep createModelStep(ModelType step) {
        ModelStepImpl result = new ModelStepImpl(step.getName(), step.getLabel());

        result.setActionType(map(step.getActionType()));
        result.setCreateSubscribers(step.isCreateSubscribers());
        result.setDropSubscribers(step.isDropSubscribers());
        result.setExtendWindow(step.isExtendWindow());
        result.setInstallJournalization(step.isInstallJournalization());
        result.setLockSubscribers(step.isLockSubscribers());
        result.setModel(step.getModel());
        result.setPurgeJournal(step.isPurgeJournal());
        result.setSubscriber(step.getSubscriber());
        result.setUninstallJournalization(step.isUninstallJournalization());
        result.setUnlockSubscribers(step.isUnlockSubscribers());
        return result;
    }

    private StepParameter createStepParameter(Parameter parameter) {
        return new StepParameterImpl(parameter.getName(), parameter.getValue());
    }

    private ETLStep findStepByLabel(String targetLabel, ETLStep... firstSteps) {
        ETLStep result = null;

        if (StringUtils.hasLength(targetLabel) && firstSteps != null && firstSteps.length > 0) {
            for (ETLStep current : firstSteps) {
                do {
                    if (current == null) {
                        break; // SKIP to next item in firstSteps list
                    } else if (StringUtils.equalsIgnoreCase(targetLabel, current.getLabel())) {
                        result = current;
                        break;
                    }
                    current = current.getNextStepOnSuccess();
                } while (current != null);

                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    private VariableStep.VariableStepType map(VariableTypeCodeEnum orig) {
        VariableStep.VariableStepType result = null;

        if (orig != null) {
            switch (orig) {
                case EVALUATE:
                    result = VariableStep.VariableStepType.EVALUATE;
                    break;
                case REFRESH:
                    result = VariableStep.VariableStepType.REFRESH;
                    break;
                case SET:
                    result = VariableStep.VariableStepType.SET;
                    break;
                case DECLARE:
                    result = VariableStep.VariableStepType.DECLARE;
                    break;
                default:
                    String msg = errorWarningMessages.formatMessage(3351, ERROR_MESSAGE_03351, this.getClass(), orig);
                    logger.error(msg);
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                    MESSAGE_TYPE.ERRORS);
                    throw new IllegalArgumentException(msg);
            }
        }
        return result;
    }

    private VariableStep.VariableSetOperatorType map(SetOperatorEnum orig) {
        VariableStep.VariableSetOperatorType result = null;

        if (orig != null) {
            switch (orig) {
                case ASSIGN:
                    result = VariableStep.VariableSetOperatorType.ASSIGN;
                    break;
                case INCREMENT:
                    result = VariableStep.VariableSetOperatorType.INCREMENT;
                    break;
                default:
                    String msg = errorWarningMessages.formatMessage(3360, ERROR_MESSAGE_03360, this.getClass(), orig);
                    logger.error(msg);
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                    MESSAGE_TYPE.ERRORS);
                    throw new IllegalArgumentException(msg);
            }
        }
        return result;
    }

    private ModelStep.ModelActionType map(ModelTypeActionEnum orig) {
        ModelStep.ModelActionType result = null;

        if (orig != null) {
            switch (orig) {
                case CONTROL:
                    result = ModelStep.ModelActionType.CONTROL;
                    break;
                case JOURNALIZE:
                    result = ModelStep.ModelActionType.JOURNALIZE;
                    break;
                case REVERSE_ENGINEER:
                    result = ModelStep.ModelActionType.REVERSE_ENGINEER;
                    break;
                default:
                    String msg = errorWarningMessages.formatMessage(3370, ERROR_MESSAGE_03370, this.getClass(), orig);
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                    MESSAGE_TYPE.ERRORS);
                    logger.error(msg);
                    throw new IllegalArgumentException(msg);
            }

        }
        return result;
    }
}
