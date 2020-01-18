package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class VariableAnnotations extends ObjectAnnotation {

    private static final Logger logger = LogManager.getLogger(VariableAnnotations.class);

    private static final String ERROR_MESSAGE_80420 =
            "Unexpected result type for annotation with key '%1$s' in '%2$s'. " +
                    "Type '%3$s' was expected.";

    private static final String ERROR_MESSAGE_80430 =
            "Undefined Annotation Key '%1$s' being added in variable '%2$s'. " +
                    "This annotation will be ignored.";

    private static final String ERROR_MESSAGE_80440 =
            "A variable defintion has been identified without the required name.";
    protected final Map<String, Object> annotations = new HashMap<>();
    private final ErrorWarningMessageJodi errorWarningMessages;

    protected VariableAnnotations(final String name,
                                  final ErrorWarningMessageJodi errorWarningMessages) {
        super(name);
        this.errorWarningMessages = errorWarningMessages;
    }

    public boolean isEmpty() {
        return this.annotations.isEmpty();
    }

    public String getName() {
        return super.getName();
    }

    //
    // process variable annotations
    //
    protected void initializeAnnotations(final Map<String, Object> annotations) {
        assert (this.annotations.isEmpty()) :
                "Variable Annotations were already added for " + getName() + ".";
        this.annotations.putAll(getValidAnnotations(annotations, getKeyTypeMap()));
    }

    @Override
    public boolean isValid() {
        boolean isValid = true;
        // check name
        if (getName().isEmpty() || getName().trim().equals("")) {
            isValid = false;
            String msg = errorWarningMessages.formatMessage(80440,
                    ERROR_MESSAGE_80440, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
        }
        return isValid;
    }

    protected void reportUnexpectedType(final String key, final Class<?> keyTypes) {
        String msg = errorWarningMessages.formatMessage(80420,
                ERROR_MESSAGE_80420, this.getClass(),
                key, getName(), keyTypes.getSimpleName());
        errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                msg, MESSAGE_TYPE.ERRORS);
        logger.error(msg);
    }

    protected void reportUndefinedAnnotation(final String key) {
        String msg = errorWarningMessages.formatMessage(80430,
                ERROR_MESSAGE_80430, this.getClass(),
                key, getName());
        errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                MESSAGE_TYPE.WARNINGS);
        logger.warn(msg);
    }

}
