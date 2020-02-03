package one.jodi.core.datastore.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.service.schema.DataStoreNotInModelException;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.extensions.contexts.ModelNameExecutionContext;
import one.jodi.core.extensions.strategies.AmbiguousModelException;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;
import one.jodi.core.extensions.strategies.NoModelFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * This class implements the default logic to determine the selected model code
 * and implements the interface {@link ModelCodeStrategy}. This logic is always
 * executed before the custom plug-in is executed.
 * <p>
 * The class is a concrete strategy participating in the Strategy Pattern.
 */
public class ModelCodeDefaultStrategy implements ModelCodeStrategy {

    private final static String SELECTED_MODEL = "Model for data store '%1$s' is '%2$s'.";
    private final static String HEURISTICS_USED = "Heuristic was used to determine the model for data store '%1$s'.";
    private final static String DEFAULT_MODEL = "Selected default model '%2$s' for temporary table '%1$s'.";
    private final static String NO_POLICY_APPLIED = "Policy did not determine model for data store '%1$s'.";

    private final static String ERROR_MESSAGE_03100 = "Unable to determine a model for data store '%1$s'. It does not exist in any model. Check name of the specified data store.";
    private final static String ERROR_MESSAGE_03110 = "Unable to determine a model for data store '%1$s' in list of potential models '%2$s'. More than two potential models exist but logic is insufficient to determine one.";
    private final static String ERROR_MESSAGE_03120 = "Unable to determine a model for temporary table '%1$s' because no default model is defined. Consider adding model definitions in the Jodi configuration file.";
    private final static String ERROR_MESSAGE_03130 = "Data store '%1$s' is not in explicitly defined model '%2$s'. Check definition of data store and specified model.";

    private final static Logger logger = LogManager.getLogger(ModelCodeDefaultStrategy.class);
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public ModelCodeDefaultStrategy(
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    private ModelProperties getDefaultModel(
            final ModelNameExecutionContext execContext) {

        ModelProperties defaultModel = null;
        for (ModelProperties model : execContext.getConfiguredModels()) {
            if (model.isDefault()) {
                defaultModel = model;
                break;
            }
        }

        return defaultModel;
    }

    /*
     * Determines if data store is contained in explicitly defined model.
     */
    private boolean existsModel(final String explicitModelCode,
                                final ModelNameExecutionContext execContext) {
        boolean foundModel = false;
        for (DataStore desc : execContext.getMatchingDataStores()) {
            if (desc.getDataModel().getModelCode().equals(explicitModelCode)) {
                foundModel = true;
                break;
            }
        }
        return foundModel;
    }

    /*
     * Determines that this data store exists in the candidate model
     */

    @SuppressWarnings("deprecation")
    private String getModelByDataStore(
            final ModelNameExecutionContext execContext) {

        String modelCode = null;
        List<DataStore> descriptors = execContext.getMatchingDataStores();
        int count = descriptors.size();
        if (count == 0) {
            String msg = errorWarningMessages.formatMessage(3100,
                    ERROR_MESSAGE_03100, this.getClass(),
                    execContext.getDataStoreName());
            logger.debug(msg);
            throw new NoModelFoundException(msg);
        } else if (count == 1) {
            modelCode = descriptors.get(0).getDataModel().getModelCode();
        } else if (count > 1) {
            // Use heuristics to define precedence for data store using
            // order of model via model configuration and data store role,
            // which can used heuristically since most ETL goes from source
            // in the direction of the data mart.

            int notIgnored = 0;
            DataStore ignoredDataStore = null;
            for (DataStore ds : descriptors) {
                if (!ds.getDataModel().isModelIgnoredbyHeuristics()) {
                    notIgnored++;
                    ignoredDataStore = ds;
                }
            }

            if (notIgnored == 0) {
                String msg = errorWarningMessages.formatMessage(3100,
                        ERROR_MESSAGE_03100, this.getClass(),
                        execContext.getDataStoreName());
                logger.debug(msg);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new NoModelFoundException(msg);
            } else if (notIgnored == 1 && ignoredDataStore != null) {
                modelCode = ignoredDataStore.getDataModel().getModelCode();
                String friendlyMessage = String.format(HEURISTICS_USED,
                        execContext.getDataStoreName());
                logger.debug(friendlyMessage);
            } else {
                // without context information it is not possible to make a
                // decision
                // or formulate a reasonable policy
                // TODO exploit additional context information assignment of
                // interface
                // to a layering in the DW
                StringBuilder modelList = new StringBuilder();
                for (DataStore ds : descriptors) {
                    modelList.append(ds.getDataModel().getModelCode()).append(
                            ", ");
                }

                String msg = errorWarningMessages.formatMessage(3110,
                        ERROR_MESSAGE_03110, this.getClass(),
                        execContext.getDataStoreName(), modelList.toString());
                logger.debug(msg);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new AmbiguousModelException(msg);
            }
        }
        return modelCode;
    }

    /**
     * Determines if one of the model's prefixes matches the start of the
     * dataStoreName and one of the model's postfixes matches the end of the
     * dataStoreName.
     *
     * @param dataStoreName name of a data store for which a determination needs to be
     *                      made if is is considered part of the model based on the
     *                      defined naming conventions.
     * @param model         information about the model including pre- and postfixes that
     *                      are considered when attempting to match the data store name to
     *                      the model.
     * @return true if one prefix and one postfix in the model definition
     * matches the start and end of the data store name, otherwise
     * false. An undefined pre- or postfix is considered a match. If the
     * model does not define neither prefix nor postfix, a 'false' is
     * returned.
     */
    private boolean match(String dataStoreName, ModelProperties model) {
        boolean matched;
        if (model.getPrefix().isEmpty() && model.getPostfix().isEmpty()) {
            // models without pre- or postfix will not be matched
            matched = false;
        } else {
            boolean matchedPrefix = model.getPrefix().isEmpty();
            for (String prefix : model.getPrefix()) {
                if (dataStoreName.startsWith(prefix)) {
                    matchedPrefix = true;
                    break;
                }
            }

            boolean matchedPostfix = model.getPostfix().isEmpty();
            for (String postfix : model.getPostfix()) {
                if (dataStoreName.endsWith(postfix)) {
                    matchedPostfix = true;
                    break;
                }
            }
            matched = (matchedPrefix && matchedPostfix);
        }
        return matched;
    }

    /*
     * The business rules that assign a model are based on naming conventions
     * based on pre- and post-fixes of table names after the temporary table
     * identifier "_Sdd" was stripped.
     */
    private String getModelByConventions(final String dataStoreName,
                                         final ModelNameExecutionContext execContext) {

        List<ModelProperties> models = execContext.getConfiguredModels();
        String modelName = null;

        for (ModelProperties model : models) {
            if (match(dataStoreName, model)) {
                modelName = model.getCode();
                break;
            }
        }
        if (modelName == null) {
            logger.debug(String.format(NO_POLICY_APPLIED, dataStoreName));
        }
        return modelName;
    }

    private String deriveModelName(final ModelNameExecutionContext execContext) {
        String modelCode;
        String tableName = execContext.getDataStoreName();

        try {
            modelCode = getModelByDataStore(execContext);
        } catch (NoModelFoundException e) {
            if (execContext.isTemporaryTable()) {
                // Temporary tables may not exist in the data model and
                // consequently
                // it is desirable to determine the model they should belong to
                // by
                // applying some business rules.
                String coreTableName = tableName.substring(0,
                        tableName.length() - 4);
                modelCode = getModelByConventions(coreTableName, execContext);
                // if the model for the temporary table was not identified, we
                // default to the source model
                if (modelCode == null) {
                    ModelProperties defaultModelProperties = getDefaultModel(execContext);
                    if (defaultModelProperties != null) {
                        modelCode = defaultModelProperties.getCode();
                        logger.debug(String.format(DEFAULT_MODEL, tableName,
                                modelCode));
                    } else {
                        String msg = errorWarningMessages
                                .formatMessage(3120, ERROR_MESSAGE_03120,
                                        this.getClass(), tableName);
                        logger.debug(msg);
                        errorWarningMessages.addMessage(
                                errorWarningMessages.assignSequenceNumber(),
                                msg, MESSAGE_TYPE.ERRORS);
                        throw new AmbiguousModelException(msg);
                    }
                }
            } else {
                throw e;
            }
        }

        logger.debug(String.format(SELECTED_MODEL, tableName, modelCode));
        return modelCode;
    }

    /**
     * This method determines the model name using the execution context that is
     * created for this plug-in feature.
     * <p>
     * Temporary table models are determined by naming conventions as the tables
     * do not permanently exist in the data model and location cannot be
     * determined by searching all data models. If temporary table name does not
     * match any conventions, a default model is selected.
     * <p>
     * Standard tables are determined in a multi-step process: 1) Use explicitly
     * defined model and check that data store exists in it 2) Determine model
     * by naming convention if data store exists in model. 3) If conventions do
     * not apply to data store name (or do not exist), a model that contains the
     * data store name is selected using precedence rules if more than one
     * exists.
     *
     * @param explicitModelName - explicit model defined for data store in Transformation
     *                          specification; will be used if it exists
     * @param execContext       - offers a set of Jodi and encapsulated ODI information to
     *                          support the decision
     * @throws NoModelFoundException        if no model contains the non-temporary data store
     * @throws AmbiguousModelException      if multiple potential models exist and neither naming
     *                                      conventions or precedence rules can be applied to
     *                                      unambiguously identify a model. In some instances it is
     *                                      possible to improve the situation by defining models in the
     *                                      Jodi properties file.
     * @throws DataStoreNotInModelException if explicitly defined model does not contain the data store.
     */
    @Override
    public String getModelCode(final String explicitModelName,
                               final ModelNameExecutionContext execContext) {

        String modelName;

        if (explicitModelName != null) {
            // Validate that data store exists in explicit model and return
            // explicit model
            // Throw exception if model cannot be found for non-temporary tables
            if ((!execContext.isTemporaryTable())
                    && (!existsModel(explicitModelName, execContext))) {
                String msg = errorWarningMessages.formatMessage(3130,
                        ERROR_MESSAGE_03130, this.getClass(),
                        execContext.getDataStoreName(), explicitModelName);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                logger.debug(msg);
                throw new DataStoreNotInModelException(msg);
            }
            modelName = explicitModelName;
        } else {
            modelName = deriveModelName(execContext);
        }
        return modelName;
    }

}