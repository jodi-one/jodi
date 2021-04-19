package one.jodi.core.common;

import com.rits.cloning.Cloner;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClonerUtil<T> {

    private static final String ERROR_MESSAGE_01001 = "Exception during cloning of '%1$s'.";
    private static final Logger log = LogManager.getLogger(ClonerUtil.class);

    private final ErrorWarningMessageJodi errorWarningMessages;

    public ClonerUtil(ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.errorWarningMessages = errorWarningMessages;
    }

    public T clone(T original, Class<?>... exceptions) {
        Cloner cloner = new Cloner();
        cloner.dontClone(exceptions);
        T clone = null;
        if (original != null) {
            try {
                clone = cloner.deepClone(original);
            } catch (RuntimeException e) {
                String msg = errorWarningMessages.formatMessage(1001, ERROR_MESSAGE_01001, this.getClass(),
                                                                original.toString());
                log.error(msg);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                throw new RuntimeException(msg, e);
            }
        }
        return clone;
    }

    public T clone(T original) {
        return clone(original, new Class<?>[]{});
    }

}
