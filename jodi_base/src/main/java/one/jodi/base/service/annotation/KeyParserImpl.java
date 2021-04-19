package one.jodi.base.service.annotation;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.annotation.NameSpaceComponent.NameSpaceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class KeyParserImpl implements KeyParser {

    private static final Logger logger = LogManager.getLogger(KeyParserImpl.class);

    private static final String ERROR_MESSAGE_80600 = "Key '%1$s' is malformed.";

    private static final String ERROR_MESSAGE_80610 =
            "Key '%1$s' is malformed. Expected keyword '%2$s' in position %3$d.";

    private static final String ERROR_MESSAGE_80620 =
            "Key '%1$s' is malformed. Expected non-empty name in position %2$d.";

    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    KeyParserImpl(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    //
    // Parse input key
    //

    private NameSpaceComponent parseNameSpaceComponent(final String[] elements, final NameSpaceType type, final int idx,
                                                       final String keyName) {
        if (!elements[idx].equalsIgnoreCase(type.getComponentName())) {
            String msg =
                    errorWarningMessages.formatMessage(80610, ERROR_MESSAGE_80610, this.getClass(), keyName, type, idx);
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (elements[idx + 1].isEmpty()) {
            String msg =
                    errorWarningMessages.formatMessage(80620, ERROR_MESSAGE_80620, this.getClass(), keyName, idx + 1);
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        return new NameSpaceComponent(type, elements[idx + 1]);
    }

    /* (non-Javadoc)
     * @see one.jodi.core.service.annotation.KeyParserI#parseKey(java.lang.String)
     */
    @Override
    public Key parseKey(final String keyString) {
        String[] elements = keyString.split("\\.");
        if (elements.length != 4) {
            String msg = errorWarningMessages.formatMessage(80600, ERROR_MESSAGE_80600, this.getClass(), keyString);
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }

        List<NameSpaceComponent> ns = new ArrayList<>(3);
        ns.add(parseNameSpaceComponent(elements, NameSpaceType.SCHEMA, 0, keyString));
        ns.add(parseNameSpaceComponent(elements, NameSpaceType.TABLE, 2, keyString));

        if (elements[elements.length - 1].trim()
                                         .isEmpty()) {
            String msg = errorWarningMessages.formatMessage(80620, ERROR_MESSAGE_80620, this.getClass(), keyString,
                                                            elements.length - 1);
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Key(ns);
    }
}