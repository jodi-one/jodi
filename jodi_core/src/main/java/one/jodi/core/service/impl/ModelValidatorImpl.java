package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreKey;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.ModelValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the {@link ModelValidator}interface.
 */
public class ModelValidatorImpl implements ModelValidator {

    private static final String ERROR_MESSAGE_60000 = "No key: %s_U1 exists in datastore %s for model %s.";

    private static final Logger logger = LogManager.getLogger(ModelValidatorImpl.class);

    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new ModelValidationImpl instance
     *
     * @param properties
     */
    @Inject
    public ModelValidatorImpl(final JodiProperties properties, final ErrorWarningMessageJodi errorWarningMessages) {
        this.properties = properties;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * Perform a check for an alternate key within the input data store.
     *
     * @param dataStore
     * @param properties
     * @return valid or not
     */
    @Override
    public boolean doCheck(DataStore dataStore) {
        boolean hasAlternativeKey = false;
        StringBuilder logMessages = new StringBuilder("");

        // or future use
        /**
         for(String columnName : dataStore.getColumns().keySet()) {
         if ( StringUtils.endsWithIgnoreCase(columnName, properties.
         getRowidColumnName()) && columnName.length() > 7) {
         logMessages.append(properties.getRowidColumnName() + dataStore.
         getDataStoreName() + ":" + columnName + " exists\n");
         }
         }
         */

        //TODO extract U1 and P1 constraint post fix into properties file
        for (DataStoreKey dataStoreKey : dataStore.getDataStoreKeys()) {
            if (dataStoreKey.getName()
                            .equals(dataStore.getDataStoreName() + "_U1")) {
                hasAlternativeKey = true;
                break;
            }
        }

        boolean hasDataMartPrefix = false;
        for (String dmp : properties.getPropertyList(JodiConstants.DATA_MART_PREFIX)) {
            if (dataStore.getDataStoreName()
                         .startsWith(dmp)) {
                hasDataMartPrefix = true;
            }
        }
        if (!hasAlternativeKey && dataStore.getDataStoreType() != null && dataStore.getDataStoreType()
                                                                                   .equals(DataStoreType.FACT) &&
                hasDataMartPrefix) {
            String message = errorWarningMessages.formatMessage(60000, ERROR_MESSAGE_60000, this.getClass(),
                                                                dataStore.getDataStoreName(),
                                                                dataStore.getDataStoreName(), dataStore.getDataModel()
                                                                                                       .getDataBaseServiceName());
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.WARNINGS);
        } else {
            logger.debug("[OK]  Tablename: " + dataStore.getDataStoreName());
            logger.debug(logMessages.toString());
        }
        return hasAlternativeKey;
    }
}
