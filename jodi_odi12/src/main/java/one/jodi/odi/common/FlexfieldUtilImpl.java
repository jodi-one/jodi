package one.jodi.odi.common;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.flexfields.IFlexFieldUser;
import oracle.odi.domain.flexfields.IFlexFieldValue;
import oracle.odi.domain.topology.OdiFlexField;
import oracle.odi.domain.topology.finder.IOdiFlexFieldFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class FlexfieldUtilImpl<T extends IFlexFieldUser> implements FlexfieldUtil<T> {

    private static final Logger logger = LogManager.getLogger(FlexfieldUtilImpl.class);

    private static final String ERROR_MESSAGE_00340 =
            "ERROR: %s does not have required flex field %s: %s";
    private static final String ERROR_MESSAGE_00350 =
            "ERROR: %s does not have required flex field %s";

    private final OdiInstance odiInstance;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public FlexfieldUtilImpl(final OdiInstance odiInstance,
                             final ErrorWarningMessageJodi errorWarningMessages) {
        this.odiInstance = odiInstance;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Cached
    @Override
    public Map<String, Object> getFlexFieldValues(T odiObject) {
        IOdiFlexFieldFinder finder = ((IOdiFlexFieldFinder) odiInstance
                .getTransactionalEntityManager().getFinder(OdiFlexField.class));

        Map<String, Object> flex = new HashMap<>();
        odiObject.initFlexFields(finder);
        for (IFlexFieldValue fValue : odiObject.getFlexFieldsValues()) {
            flex.put(fValue.getCode(), fValue.getValue());
        }

        return flex;
    }

    @Cached
    private IFlexFieldValue getFlexFieldValueByName(final T odiObject,
                                                    final String flexFieldName) {
        IOdiFlexFieldFinder finder =
                ((IOdiFlexFieldFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiFlexField.class));
        odiObject.initFlexFields(finder);
        IFlexFieldValue flexfieldValue = null;
        for (IFlexFieldValue odiFlexFieldValue : odiObject.getFlexFieldsValues()) {
            if (odiFlexFieldValue.getCode().equals(flexFieldName)) {
                flexfieldValue = odiFlexFieldValue;
                logger.debug("FlexFieldValue found:" + flexfieldValue.getCode() +
                        " for " + odiObject.getName());
                break;
            }
        }
        return flexfieldValue;
    }

    @Override
    public boolean existsFlexfieldValueByName(T odiObject, String flexFieldName) {
        IFlexFieldValue value = getFlexFieldValueByName(odiObject, flexFieldName);
        return ((value != null) && (value.getValue() != null));
    }

    private Object getFlexFieldObjectValueByName(T odiObject, String flexFieldName) {
        Object value;
        try {
            value = getFlexFieldValueByName(odiObject, flexFieldName).getValue();
        } catch (NullPointerException npe) {
            String msg = errorWarningMessages.formatMessage(350, ERROR_MESSAGE_00350, this.getClass(),
                    odiObject, flexFieldName, npe.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return value;
    }

    @Cached
    @Override
    public String getFlexFieldStringValueByName(T odiObject, String flexFieldName) {
        return getFlexFieldObjectValueByName(odiObject, flexFieldName).toString();
    }

    @Cached
    @Override
    public long getFlexFieldLongValueByName(T odiObject, String flexFieldName) {
        return ((Number) getFlexFieldObjectValueByName(odiObject, flexFieldName)).longValue();
    }

    private void setFlexFieldsWithObject(T odiObject, String flexFieldName, Object value) {
        IFlexFieldValue flexfieldValue = getFlexFieldValueByName(odiObject, flexFieldName);
        if (flexfieldValue != null) {
            logger.debug(String.format("Setting flexfieldValue for object '%1$s' with value '%2$s'.", odiObject.getName(), value.toString()));
            flexfieldValue.setValue(value);
        } else {
            String msg = errorWarningMessages.formatMessage(340, ERROR_MESSAGE_00340, this.getClass(),
                    odiObject, flexFieldName);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
    }

    @Override
    public void setFlexFields(T odiObject, String flexFieldName, String value) {
        setFlexFieldsWithObject(odiObject, flexFieldName, value);
    }

    @Override
    public void setFlexFields(T odiObject, String flexFieldName, long value) {
        setFlexFieldsWithObject(odiObject, flexFieldName, value);
    }

}
