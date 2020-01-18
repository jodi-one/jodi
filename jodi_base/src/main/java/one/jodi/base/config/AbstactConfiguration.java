package one.jodi.base.config;

import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

public abstract class AbstactConfiguration {

    public static final String ORACLE_OBJECT_NAME_REGEX =
            "[A-Za-z]{1}[A-Za-z0-9_$#]{0,}";
    private static final Logger logger =
            LogManager.getLogger(AbstactConfiguration.class);
    // all unicode characters with exception of space characters and *, ', and ?
    // we may add the constraint if we learn of possible side effects.
    private static final String UNICODE_START_END = "[\\S]"; //[\\S&&[^*'?]]"
    // as before but allows ' ' (space)
    private static final String UNICODE_MIDDLE = "[ \\S]";   //[ \\S&&[^*'?]]"

    public static final String BI_LOGICAL_OBJECT_NAME_REGEX =
            UNICODE_START_END + "(" + UNICODE_MIDDLE + "*" + UNICODE_START_END + ")?";
    private static final String ERROR_MESSAGE_80100 =
            "The property file has not been found.";
    private static final String ERROR_MESSAGE_80110 =
            "Errors encountered attempting to load properties file: %s";
    private final static String ERROR_MESSAGE_01110 =
            "Unsuccessful attempt to parse name value pair from list using odi " +
                    "property: %s (as a Map)";
    private static final String FRIENDLY_MESSAGE =
            "The required configuration property '%1$s' is not found. Please " +
                    "consult the user manual, it will be skipped and defaults will be used.";
    private final ErrorWarningMessageJodi errorWarningMessages;
    // TODO - values cached. Not multi-threaded or multi-tenant if used as Singleton
    private Configuration config;

    protected AbstactConfiguration(final ErrorWarningMessageJodi errorWarningMessages,
                                   final String propFile,
                                   final boolean usedForTesting) {
        this.errorWarningMessages = errorWarningMessages;
        if (!usedForTesting) {
            assert (propFile != null);
            init(propFile);
        }
    }

    /**
     * Initialized the singleton <code>JodiPropertiesImpl</code> instance by
     * loading the properties in the specified file.
     *
     * @param fileName properties file
     * @throws RuntimeException
     * @throws IllegalArgumentException
     */
    protected void init(final String fileName) {
        if ((new File(fileName)).exists()) {
            logger.debug("Loading Jodi properties file " + fileName);
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<PropertiesConfiguration>(
                            PropertiesConfiguration.class)
                            .configure(params.fileBased()
                                    .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                                    .setFileName(fileName));
            builder.setAutoSave(true);
            try {
                PropertiesConfiguration pConfig = builder.getConfiguration();
                setConfig(pConfig.interpolatedConfiguration());
            } catch (ConfigurationException ce) {
                String msg = errorWarningMessages.formatMessage(80110,
                        ERROR_MESSAGE_80110,
                        this.getClass(),
                        ce.getMessage());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.fatal(msg, ce);
                throw new UnRecoverableException(msg);
            }

        } else {
            String msg = errorWarningMessages.formatMessage(80100,
                    ERROR_MESSAGE_80100, this.getClass());
            logger.error(msg);

            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
    }

    // looks for strings that start and end with a quotation mark to
    // allow for Strings with leading or training spaces.
    private String decodeProperty(final String prop) {
        String embedded = prop.trim();
        if (embedded.length() > 1 && embedded.startsWith("\"") &&
                embedded.endsWith("\"")) {
            // remove leading and trailing quotation marks
            embedded = embedded.substring(1, embedded.length() - 1);
        }
        return embedded;
    }

    @Cached
    public String getProperty(final String prop) {
        if (getConfig() == null) {
            return null;
        }
        Object property = getConfig().getProperty(prop);
        if (property == null) {
            String friendlyMessage = String.format(FRIENDLY_MESSAGE, prop);
            logger.debug(friendlyMessage);
            return null;
        }
        String tempProperty = property.toString();
        if (tempProperty != null) {
            tempProperty = decodeProperty(tempProperty);
        }
        return tempProperty;
    }

    public List<String> getPropertyList(final String prop) {
        if (getConfig() == null) {
            return new ArrayList<>();
        }
        String[] property = getConfig().getStringArray(prop);
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(property)));
    }

    private String upCase(String target) {
        return (target == null ? null : target);
    }

    public Map<String, String> getPropertyMap(final String prop) {
        List<String> list = getPropertyList(prop);
        HashMap<String, String> map = new HashMap<>();
        for (String s : list) {
            String[] keyValuePair = s.split(":", 2);
            if (keyValuePair.length != 2) {
                String msg = errorWarningMessages.formatMessage(1110, ERROR_MESSAGE_01110,
                        this.getClass(), prop);
                logger.error(msg);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                throw new JodiPropertyNotFoundException(msg, prop);
            } else {
                map.put(keyValuePair[0].trim(), upCase(keyValuePair[1].trim()));
            }
        }
        return map;
    }

    public List<String> getPropertyKeys() {

        List<String> keys = new ArrayList<>();
        Iterator<String> iter = getConfig().getKeys();
        while (iter.hasNext()) {
            keys.add((String) iter.next());
        }
        return keys;
    }

    protected List<String> getTokenList(String[] tokens) {
        return Collections.unmodifiableList(Arrays.asList(tokens));
    }

    protected Configuration getConfig() {
        return config;
    }

    protected void setConfig(Configuration config) {
        this.config = config;
    }
}
