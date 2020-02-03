package one.jodi.core.datastore.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.extensions.contexts.ModelNameExecutionContext;
import one.jodi.core.extensions.strategies.AmbiguousModelException;
import one.jodi.core.extensions.strategies.ModelCodeStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Identity strategy that is used as a placeholder for a custom strategy.
 */
public class ModelCodeIDStrategy implements ModelCodeStrategy {

    private final static String ERROR_MESSAGE_03111 = "Unable to determine a model for data store '%1$s'.";
    private final static Logger logger = LogManager.getLogger(ModelCodeIDStrategy.class);
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public ModelCodeIDStrategy(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * Implements the identity (ID) strategy that returns the default model name
     * passed into this strategy method
     */
    @Override
    public String getModelCode(final String defaultModelName,
                               final ModelNameExecutionContext execContext) {

        if (defaultModelName == null) {
            String msg = errorWarningMessages.formatMessage(3111,
                    ERROR_MESSAGE_03111, this.getClass(), execContext.getDataStoreName());
            logger.error(msg);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new AmbiguousModelException(msg, true);
        }

        return defaultModelName;
    }

}