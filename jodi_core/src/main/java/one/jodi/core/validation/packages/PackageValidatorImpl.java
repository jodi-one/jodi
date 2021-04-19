package one.jodi.core.validation.packages;

import one.jodi.base.annotations.XmlFolderName;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.core.annotations.JournalizedData;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.context.packages.TransformationCacheItem;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.VariableStep.VariableSetOperatorType;
import one.jodi.etl.journalizng.JournalizingConfiguration;
import one.jodi.etl.service.packages.PackageServiceProvider;
import one.jodi.etl.service.packages.ProcedureNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

/**
 * The Class PackageValidatorImpl provides an implementation of the
 * PackageValidator interface.
 */
public class PackageValidatorImpl implements PackageValidator {
    private static final String ERROR_MESSAGE_80000 =
            "Package with name %s was already defined in folder %s. "
                    + "Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80100 =
            "Variable definition with name %s that is referenced in package %s"
                    + " located in folder %s is not found. Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80110 =
            "Variable definition with name %s in package %s located in folder %s "
                    + "is incorrect: Variable Type Code \"REFRESH\" was selected but "
                    + "options %s are set as well. The additional options are ignored. (%s)";

    private static final String ERROR_MESSAGE_80111 =
            "Variable definition with name %s in package %s located in folder %s "
                    + "is incorrect: Variable Type Code \"DECLARE\" was selected but "
                    + "options %s are set as well. The additional options are ignored. (%s)";

    private static final String ERROR_MESSAGE_80120 =
            "Variable definition with name %s in package %s located in folder %s"
                    + " is incorrect: Variable Type Code \"EVALUATE\" was selected but "
                    + "required options %s are missing. Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80121 = "Variable definition with"
            + " name %s in package %s located in folder %s is incorrect: Variable"
            + " Type Code \"EVALUATE\" was selected but options %s are set as well."
            + " The additional options are ignored. (%s)";

    private static final String ERROR_MESSAGE_80130 =
            "Variable definition with name %s in package %s located in folder %s"
                    + " is incorrect: Variable Type Code \"SET\" was selected but required"
                    + " options %s are missing. Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80140 = "Variable definition with name"
            + " %s in package %s located in folder %s is incorrect: Variable "
            + "Type Code \"SET\" was selected but options %s are set as well."
            + " The additional options are ignored. (%s)";

    private static final String ERROR_MESSAGE_80200 =
            "Package with name %s is called within package %s located in folder "
                    + "%s that is not found. Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80300 =
            "Procedure with name %s is called within package %s located in folder"
                    + " %s that is not found in ODI. Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80310 =
            "Procedure with name %s is called within package %s located in folder"
                    + " %s with unknown parameter name %s. Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80500 =
            "Model Check (a.k.a. KnowledgeModule) for model %s is called within "
                    + "package %s located in folder %s with unknown model parameter."
                    + " Package %s was not created. (%s)";

    private static final String ERROR_MESSAGE_80600 =
            "No transformation definition was found that targets package %s in project " +
                    "folder %s with PackageListItem value %s. Its definition in file '%s' " +
                    "is either not used or the values of PackageListItem or FolderCode " +
                    "elements do not align with the values set in Transformation defintions " +
                    "that are intended to target this package.";
    private static final Logger logger = LogManager.getLogger(PackageValidatorImpl.class);
    private final JournalizingContext journalizingContext;
    private final JodiProperties properties;
    private final PackageServiceProvider packageService;
    private final PackageCache packageCache;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final String xmlFolderName;
    private final boolean journalized;
    private final DatabaseMetadataService databaseMetadataService;

    /**
     * Instantiates a new PackageValidatorImpl.
     *
     * @param properties           the JodiProperties
     * @param journalizingContext  the JournalizingContext
     * @param packageService,
     * @param packageCache,
     * @param errorWarningMessages
     */
    @Inject
    protected PackageValidatorImpl(final JodiProperties properties,
                                   final JournalizingContext journalizingContext,
                                   final PackageServiceProvider packageService,
                                   final PackageCache packageCache,
                                   final ErrorWarningMessageJodi errorWarningMessages,
                                   final @XmlFolderName String xmlFolderName,
                                   final @JournalizedData String journalized,
                                   final DatabaseMetadataService databaseMetadataService) {
        this.journalizingContext = journalizingContext;
        this.properties = properties;
        this.packageService = packageService;
        this.packageCache = packageCache;
        this.errorWarningMessages = errorWarningMessages;
        this.xmlFolderName = xmlFolderName;
        this.journalized = Boolean.parseBoolean(journalized);
        this.databaseMetadataService = databaseMetadataService;
    }

    @Override
    public List<PackageValidationResult> validatePackages(final List<ETLPackage> packages) {
        Set<String> allPreviousPackages = new HashSet<>(packages.size());
        List<PackageValidationResult> validationResults = new ArrayList<>(packages.size());
        for (ETLPackage p : packages) {
            PackageValidationResult validationResult = new PackageValidationResult(p);
            validationResults.add(validationResult);

            String folderCode = p.getFolderCode();
            if (isPreviousPackage(p.getPackageName(), allPreviousPackages) ||
                    (packageService.packageExists(p.getPackageName(), folderCode) &&
                            !properties.isUpdateable())) {
                String msg =
                        errorWarningMessages.formatMessage(80000, ERROR_MESSAGE_80000,
                                this.getClass(), p.getPackageName(),
                                folderCode, p.getPackageName(),
                                get0xml());
//            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                validationResult.setValid(false);
                validationResult.addValidationMessage(msg);
                continue;
            }

            boolean failed = this.validatePackageListItem(validationResult);
            if (failed) {
                logger.warn("validatePackageListItem failed for package; " +
                        p.getPackageName());
                continue;
            }

            failed = validateVariables(validationResult);
            if (failed) {
                logger.warn("validateVariables failed for package; " + p.getPackageName());
                validationResult.setValid(false);
                continue;
            }

            failed = validateExecProcedures(validationResult);
            if (failed) {
                logger.warn("validateExecProcedures failed for package; " +
                        p.getPackageName());
                validationResult.setValid(false);
                continue;
            }

            failed = validateModels(validationResult);
            if (failed) {
                logger.warn("validateModels failed for package; " + p.getPackageName());
                validationResult.setValid(false);
                continue;
            }

            failed = validateAsynchronousFailed(validationResult);
            if (failed) {
                logger.warn(String.format("validateAsynchronous failed for package %s. " +
                                "A mapping is set to execute asynchronous and " +
                                "following mappings are set to execute synchronous.",
                        p.getPackageName()));
                validationResult.setValid(false);
                continue;
            }

            /**
             * We don't validate execpackages to be in the same folder, since we allow
             * execpackages be in a different folder.
             */
            addPreviousPackage(p.getPackageName(), allPreviousPackages);
            resolvePackageDependencies(p.getPackageName(), validationResults);
            logger.debug("validatePackages succeeded.");

        }

        failPackagesWithInvalidDependencies(validationResults);
        return validationResults;
    }

    private boolean validateAsynchronousFailed(PackageValidationResult validationResult) {
        boolean failed = false;
        boolean isAsynchronous = false;
        outer:
        for (InterfaceStep s : validationResult.getTargetPackage().getInterfaceSteps()) {

            if (s.executeAsynchronously()) {
                isAsynchronous = true;
            }
            if (isAsynchronous && !s.executeAsynchronously()) {
                failed = true;
                break outer;
            }
        }
        return failed;
    }

    private String get0xml() {
        if (!journalized)
            return new File(xmlFolderName, "0.xml").getAbsolutePath();
        else
            return new File(xmlFolderName, "1.xml").getAbsolutePath();
    }

    /**
     * Validate all ExecProcedureType instances.
     *
     * @param validationResult the PackageValidationResult that will be updated based on
     *                         validation results
     * @return true, if the instance failed validation and the Package should
     * not be created
     */
    private boolean validateExecProcedures(
            PackageValidationResult validationResult) {
        boolean fail = false;

        ETLPackage targetPackage = validationResult.getTargetPackage();
        List<ProcedureStep> procedures = getAllSteps(ProcedureStep.class,
                targetPackage.getFirstStep());
        String folderCode = targetPackage.getFolderCode();

        for (ProcedureStep execProcedure : procedures) {
            try {
                Collection<String> parameters = packageService
                        .getProcedureParameterNames(
                                properties.getProjectCode(),
                                execProcedure.getName());

                if (execProcedure.getParameters() != null
                        && !execProcedure.getParameters().isEmpty()) {
                    Set<String> paramSet = new HashSet<>(parameters);

                    for (StepParameter p : execProcedure.getParameters()) {
                        if (!paramSet.contains(p.getName())) {
                            String msg = errorWarningMessages.formatMessage(80310,
                                    ERROR_MESSAGE_80310, this.getClass(), execProcedure.getName(),
                                    targetPackage.getPackageName(),
                                    folderCode, p.getName(),
                                    targetPackage.getPackageName(), get0xml());
                            validationResult.addValidationMessage(msg);
                            fail = true;
                            break;
                        }
                    }
                }
            } catch (ProcedureNotFoundException e) {
                String msg = errorWarningMessages.formatMessage(80300,
                        ERROR_MESSAGE_80300, this.getClass(), execProcedure.getName(),
                        targetPackage.getPackageName(), folderCode,
                        targetPackage.getPackageName(), get0xml());
                errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                // DO NOT DISABLE THIS ERROR MESSAGE
                logger.error(msg);
                validationResult.addValidationMessage(msg);
                fail = true;
                break;
            }
        }

        return fail;
    }

    private boolean validatePackageListItem(PackageValidationResult validationResult) {
        boolean fail = false;

        ETLPackage targetPackage = validationResult.getTargetPackage();
        for (String packageListItem : targetPackage.getTargetPackageList()) {
            if (packageListItem.trim().length() > 0) {
                List<TransformationCacheItem> transformations =
                        packageCache.getTransformationsForPackage(packageListItem,
                                targetPackage.getFolderCode());
                if (transformations.isEmpty()) {
                    String msg =
                            errorWarningMessages.formatMessage(80600, ERROR_MESSAGE_80600,
                                    this.getClass(),
                                    targetPackage.getPackageName(),
                                    targetPackage.getFolderCode(),
                                    packageListItem, get0xml());
//               errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    validationResult.addValidationMessage(msg);
                    fail = true;
                }
            }
        }

        if (fail) {
            validationResult.setValid(false);
        }

        return fail;
    }

    /**
     * Validate all VariableType instances.
     *
     * @param validationResult the PackageValidationResult that will be updated based on
     *                         validation results
     * @return true, if the instance failed validation and the Package should
     * not be created
     */
    private boolean validateVariables(PackageValidationResult validationResult) {
        boolean fail = false;

        ETLPackage targetPackage = validationResult.getTargetPackage();
        List<VariableStep> variables = getAllSteps(VariableStep.class,
                targetPackage.getFirstStep());
        String folderCode = targetPackage.getFolderCode();

        for (VariableStep variable : variables) {
            if (!databaseMetadataService.projectVariableExists(
                    properties.getProjectCode(), variable.getName())) {
                String msg = errorWarningMessages.formatMessage(80100,
                        ERROR_MESSAGE_80100, this.getClass(), variable.getName(),
                        targetPackage.getPackageName(), folderCode,
                        targetPackage.getPackageName(), get0xml());
//            	errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                validationResult.addValidationMessage(msg);
                validationResult.setValid(false);
                fail = true;
                break;
            }
            switch (variable.getStepType()) {
                case DECLARE:
                    fail = validateDeclareVariable(variable, validationResult);
                    if (fail) {
                        logger.warn("For " + validationResult.getTargetPackage() + " validation fails for declare variable " + variable.getName() + ".");
                    }
                    break;
                case REFRESH:
                    fail = validateRefreshVariable(variable, validationResult);
                    if (fail) {
                        logger.warn("For " + validationResult.getTargetPackage() + " validation fails for refresh variable " + variable.getName() + ".");
                    }
                    break;
                case EVALUATE:
                    fail = validateEvaluateVariable(variable, validationResult);
                    if (fail) {
                        logger.warn("For " + validationResult.getTargetPackage() + " validation fails for evalutate variable " + variable.getName() + ".");
                    }
                    break;
                case SET:
                    fail = validateSetVariable(variable, validationResult);
                    if (fail) {
                        logger.warn("For " + validationResult.getTargetPackage() + " validation fails for set variable " + variable.getName() + ".");
                    }
                    break;
            }

            if (fail) {
                break;
            }
        }

        validationResult.setValid(!fail);

        return fail;
    }

    /**
     * Validate all ModelType instances.
     *
     * @param validationResult the PackageValidationResult that will be updated based on
     *                         validation results
     * @return true, if the instance failed validation and the Package should
     * not be created
     */
    private boolean validateModels(PackageValidationResult validationResult) {
        boolean fail = false;

        ETLPackage targetPackage = validationResult.getTargetPackage();
        List<ModelStep> models = getAllSteps(ModelStep.class,
                targetPackage.getFirstStep());
        String folderCode = targetPackage.getFolderCode();

        List<JournalizingConfiguration> config = journalizingContext
                .getJournalizingConfiguration();

        for (ModelStep model : models) {
            if (!validateModel(model, config)) {
                String msg = errorWarningMessages.formatMessage(80500,
                        ERROR_MESSAGE_80500, this.getClass(), model.getName(),
                        targetPackage.getPackageName(), folderCode, get0xml());
//            	errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                validationResult.addValidationMessage(msg);
                validationResult.setValid(false);
                fail = true;
                break;
            }
        }

        if (fail) {
            validationResult.setValid(false);
        }

        return fail;
    }

    /**
     * Validate a ModelType instance.
     *
     * @param model  the ModelType to be validated
     * @param config the config
     * @return true, if the instance failed validation and the Package should
     * not be created
     */
    private boolean validateModel(ModelStep model,
                                  List<JournalizingConfiguration> config) {
        boolean valid = true;

        return valid;
    }

    /**
     * Validate refresh VariableType.
     *
     * @param variable         the refresh variable
     * @param validationResult the PackageValidationResult that will be updated based on
     *                         validation results
     * @return true, if the instance failed validation and the Package should
     * not be created
     */
    private boolean validateRefreshVariable(VariableStep variable,
                                            PackageValidationResult validationResult) {
        boolean fail = false;
        List<String> strList = new ArrayList<>(4);

        if (StringUtils.hasLength(variable.getOperator())) {
            strList.add("Operator");
        }
        if (StringUtils.hasLength(variable.getValue())) {
            strList.add("Value");
        }
        if (variable.getSetOperator() != null) {
            strList.add("SetOperator");
        }
        if (variable.getIncrementBy() != null) {
            strList.add("IncrementBy");
        }

        if (!strList.isEmpty()) {
            String msg = StringUtils.joinStrings(strList, ",", " ", "");
            String message = errorWarningMessages.formatMessage(80110,
                    ERROR_MESSAGE_80110, this.getClass(), variable.getName(), validationResult
                            .getTargetPackage().getPackageName(),
                    validationResult.getTargetPackage().getFolderCode(), msg, get0xml());
//        	errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
            validationResult.addValidationMessage(message);
        }
        return fail;
    }

    private boolean validateDeclareVariable(VariableStep variable,
                                            PackageValidationResult validationResult) {
        boolean fail = false;
        List<String> strList = new ArrayList<>(4);

        if (StringUtils.hasLength(variable.getOperator())) {
            strList.add("Operator");
        }
        if (StringUtils.hasLength(variable.getValue())) {
            strList.add("Value");
        }
        if (variable.getSetOperator() != null) {
            strList.add("SetOperator");
        }
        if (variable.getIncrementBy() != null) {
            strList.add("IncrementBy");
        }

        if (!strList.isEmpty()) {
            String msg = StringUtils.joinStrings(strList, ",", " ", "");
            String message = errorWarningMessages.formatMessage(80111,
                    ERROR_MESSAGE_80111, this.getClass(), variable.getName(), validationResult
                            .getTargetPackage().getPackageName(),
                    validationResult.getTargetPackage().getFolderCode(), msg,
                    get0xml());
//        	errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
            validationResult.addValidationMessage(message);
        }
        return fail;
    }


    /**
     * Validate evaluate VariableType.
     *
     * @param variable         the evaluate variable
     * @param validationResult the PackageValidationResult that will be updated based on
     *                         validation results
     * @return true, if the instance failed validation and the Package should
     * not be created
     */
    private boolean validateEvaluateVariable(VariableStep variable,
                                             PackageValidationResult validationResult) {
        boolean fail = false;
        List<String> strList = new ArrayList<>(2);
        if (variable.getOperator() == null) {
            strList.add("Operator");
            fail = true;
        }
        if (variable.getValue() == null) {
            strList.add("Value");
            fail = true;
        }

        ETLPackage targetPackage = validationResult.getTargetPackage();
        if (!strList.isEmpty()) {
            String msg = StringUtils.joinStrings(strList, ",", " ", "");
            String message = errorWarningMessages.formatMessage(80120,
                    ERROR_MESSAGE_80120, this.getClass(), variable.getName(),
                    targetPackage.getPackageName(),
                    targetPackage.getFolderCode(), msg,
                    targetPackage.getPackageName(),
                    get0xml());
//        	errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
            validationResult.addValidationMessage(message);
            fail = true;
        }

        if (!fail) {
            strList = new ArrayList<>(2);
            if (variable.getSetOperator() != null) {
                strList.add("SetOperator");
            }
            if (variable.getIncrementBy() != null) {
                strList.add("IncrementBy");
            }

            if (!strList.isEmpty()) {
                String msg = StringUtils.joinStrings(strList, ",", " ", "");
                String message = errorWarningMessages.formatMessage(80121,
                        ERROR_MESSAGE_80121, this.getClass(), variable.getName(),
                        targetPackage.getPackageName(),
                        targetPackage.getFolderCode(), msg,
                        get0xml());
//            	errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
                validationResult.addValidationMessage(message);
            }
        }

        return fail;
    }

    /**
     * Validate set VariableType.
     *
     * @param variable         the set variable
     * @param validationResult the PackageValidationResult that will be updated based on
     *                         validation results
     * @return true, if the instance failed validation and the Package should
     * not be created
     */
    private boolean validateSetVariable(VariableStep variable,
                                        PackageValidationResult validationResult) {
        boolean fail = false;
        List<String> strList = new ArrayList<>(4);

        if (variable.getSetOperator() == null) {
            strList.add("SetOperator");
        } else {
            switch (variable.getSetOperator()) {
                case ASSIGN:
                    if (!StringUtils.hasLength(variable.getValue())) {
                        strList.add("Value");
                    }
                    break;
                case INCREMENT:
                    if (variable.getIncrementBy() == null) {
                        strList.add("IncrementBy");
                    }
                    break;
            }
        }
        ETLPackage targetPackage = validationResult.getTargetPackage();
        if (!strList.isEmpty()) {
            String msg = StringUtils.joinStrings(strList, ",", " ", "");
            String message = errorWarningMessages.formatMessage(80130,
                    ERROR_MESSAGE_80130, this.getClass(), variable.getName(),
                    targetPackage.getPackageName(),
                    targetPackage.getFolderCode(), msg,
                    targetPackage.getPackageName(),
                    get0xml());
//        	errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
            validationResult.addValidationMessage(message);
            fail = true;
        }

        if (!fail) {
            if (variable.getOperator() != null) {
                strList.add("Operator");
            }
            if (variable.getSetOperator() == VariableSetOperatorType.ASSIGN
                    && variable.getIncrementBy() != null) {
                strList.add("IncrementBy");
            }
            if (variable.getSetOperator() == VariableSetOperatorType.INCREMENT
                    && StringUtils.hasLength(variable.getValue())) {
                strList.add("value");
            }

            if (!strList.isEmpty()) {
                String msg = StringUtils.joinStrings(strList, ",", " ", "");
                String message = errorWarningMessages.formatMessage(80140,
                        ERROR_MESSAGE_80140, this.getClass(), variable.getName(),
                        targetPackage.getPackageName(),
                        targetPackage.getFolderCode(), msg,
                        get0xml());
//            	errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
                validationResult.addValidationMessage(message);
            }
        }
        return fail;
    }

    /**
     * Creates a parameertized message.
     *
     * @param message
     *            the message string
     * @param parameters
     *            the message parameters
     * @return the string

    private String createMessage(String message, Object... parameters) {
    return String.format(message, parameters);
    } */

    /**
     * Gets the steps of a specific type.
     *
     * @param <T>             the generic step type
     * @param type            the target step type
     * @param stepsCollection the steps collection
     * @return the steps matching the specified type
     */
    private <T> List<T> getAllSteps(Class<T> type, ETLStep... stepsCollection) {
        List<T> result = new ArrayList<>();

        for (ETLStep steps : stepsCollection) {

            if (steps != null) {
                result.addAll(getSteps(type, steps));
                ETLStep currentStep = steps;
                while (currentStep != null) {
                    if (currentStep.getNextStepOnFailure() != null) {
                        result.addAll(getSteps(type, currentStep.getNextStepOnFailure()));
                    }
                    currentStep = currentStep.getNextStepOnSuccess();
                }
            }
        }

        return new ArrayList<>(new HashSet<>(result));
    }

    private <T> List<T> getSteps(Class<T> type, ETLStep startingStep) {
        List<T> result = new ArrayList<>();
        ETLStep currentStep = startingStep;

        while (currentStep != null) {
            if (type.isAssignableFrom(currentStep.getClass())) {
                result.add(type.cast(currentStep));
            }

            currentStep = currentStep.getNextStepOnSuccess();

        }

        return result;
    }

    /**
     * Checks if the specified package was previously processed.
     *
     * @param packageName      the package name
     * @param previousPackages collection that tracks previously validated packages
     * @return true, if the package was previously processed
     */
    private boolean isPreviousPackage(String packageName,
                                      Set<String> previousPackages) {
        return previousPackages.contains(packageName.toUpperCase());
    }

    private void addPreviousPackage(String packageName,
                                    Set<String> previousPackages) {
        previousPackages.add(packageName.toUpperCase());
    }

    private void resolvePackageDependencies(String validatedPackageName,
                                            List<PackageValidationResult> validationResults) {
        for (PackageValidationResult vResult : validationResults) {
            vResult.removePendingDependency(validatedPackageName);
        }
    }

    private void failPackagesWithInvalidDependencies(
            List<PackageValidationResult> validationResults) {

        validationResults.stream().filter(vResult -> vResult.isValid() &&
                vResult.hasPendingDependencies()).forEach(vResult -> {
            ETLPackage targetPackage = vResult.getTargetPackage();
            String msg = errorWarningMessages.formatMessage(80200,
                    ERROR_MESSAGE_80200,
                    this.getClass(),
                    vResult.getDependencyNames(),
                    targetPackage.getPackageName(),
                    targetPackage.getFolderCode(),
                    targetPackage.getPackageName(),
                    get0xml());
//          errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            vResult.addValidationMessage(msg);
            vResult.setValid(false);
        });

    }
}
