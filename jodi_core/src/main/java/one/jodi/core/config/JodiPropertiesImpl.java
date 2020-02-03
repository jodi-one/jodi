package one.jodi.core.config;

import com.google.inject.Inject;
import one.jodi.base.annotations.PropertyFileName;
import one.jodi.base.config.JodiPropertyNotFoundException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.io.File;
import java.util.*;


/**
 * Implements the {@link JodiProperties} interface using a
 * properties file.
 */
@Singleton
public class JodiPropertiesImpl implements JodiProperties {
    public static final String XSD_INTERFACES = "xml.xsd.interfaces";
    public static final String XSD_PACKAGES = "xml.xsd.packages";
    private final static Logger logger = LogManager.getLogger(JodiPropertiesImpl.class);
    private final static String ERROR_MESSAGE_01090 =
            "The Jodi ODI property file is not found.";

    private final static String ERROR_MESSAGE_01100 =
            "The property '%1$s' is a list, use getPropertyList(String name) "
                    + "instead of \"getProperty\".";

    private final static String ERROR_MESSAGE_01110 =
            "Unsuccessful attempt to parse name value pair from list using odi "
                    + "property: %s (as a Map)";

    private final static String ERROR_MESSAGE_01120 =
            "Errors encountered attempting to load Jodi properties %s.";

    private final static String ERROR_MESSAGE_01240 =
            "The required configuration property '%1$s' is not found. Please "
                    + "consult the user manual.";

    private final static String ERROR_MESSAGE_06010 =
            "Exception in isUpdateable %s.";

    private final static String ERROR_MESSAGE_06020 =
            "Exception in includeDetails %s.";

    private final ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiImpl.getInstance();

    private Configuration config;

    /**
     * Creates a new JodiPropertiesImpl instance.
     *
     * @param propFile properties file to initialize
     */
    @Inject
    protected JodiPropertiesImpl(final @PropertyFileName String propFile) {
        init(propFile);
    }

    /**
     * @see JodiProperties#getInputSchemaLocation()
     */
    @Override
    public String getInputSchemaLocation() {
        String schemaLocation = getProperty(XSD_INTERFACES);

        return schemaLocation;
    }

    /**
     * @see JodiProperties#getPackageSchemaLocation()
     */
    @Override
    public String getPackageSchemaLocation() {
        String schemaLocation = getProperty(XSD_PACKAGES);

        return schemaLocation;
    }

    /**
     * @see JodiProperties#getProjectCode()
     */
    @Override
    public String getProjectCode() {
        return getProperty(JodiConstants.ODI_PROJECT_CODE);
    }

    /**
     * @see JodiProperties#getProperty(String)
     */
    public String getProperty(final String prop) {
        assert (!prop.equals(JodiConstants.DATA_MART_PREFIX)) :
                errorWarningMessages.formatMessage(1100, ERROR_MESSAGE_01100,
                        this.getClass(), JodiConstants.DATA_MART_PREFIX);

        String property = (String) config.getProperty(prop);
        if (property == null && prop != XSD_INTERFACES && prop != XSD_PACKAGES) {

            String msg = errorWarningMessages.formatMessage(1240,
                    ERROR_MESSAGE_01240, this.getClass(), prop);
            logger.error(msg);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            throw new JodiPropertyNotFoundException(msg, prop);
        }
        return property;
    }

    @Override
    public List<String> getPropertyList(final String prop) {
        return new ArrayList<>(Arrays.asList(config.getStringArray(prop)));
    }

    @Override
    public Map<String, String> getPropertyMap(final String prop) {
        List<String> list = getPropertyList(prop);
        HashMap<String, String> map = new HashMap<>();
        for (String s : list) {
            String[] keyValuePair = s.split(":", 2);
            if (keyValuePair.length != 2) {
                String msg = errorWarningMessages.formatMessage(1110,
                        ERROR_MESSAGE_01110, this.getClass(), prop);
                logger.error(msg);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                throw new JodiPropertyNotFoundException(msg, prop);
            } else {
                map.put(keyValuePair[0].trim(), upCase(keyValuePair[1].trim()));
            }
        }

        return map;
    }


    /**
     * @see JodiProperties#getPropertyKeys()
     */
    @Override
    public List<String> getPropertyKeys() {

        List<String> keys = new ArrayList<>();
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            keys.add((String) iter.next());
        }
        return keys;
    }

    /**
     * Initialized the singleton <code>JodiPropertiesImpl</code> instance by
     * loading the properties in the specified file.
     *
     * @param fileName properties file
     * @throws RuntimeException
     * @throws IllegalArgumentException
     */
    private void init(final String fileName) {
        if ((new File(fileName)).exists()) {
            logger.debug("Loading Jodi properties file " + fileName);
            Parameters params = new Parameters();
            File propertiesFile = new File(fileName);
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                            .configure(params.fileBased()
                                    .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                                    .setFile(propertiesFile));
            builder.setAutoSave(true);
            try {
                config = builder.getConfiguration();
            } catch (ConfigurationException ce) {
                String msg = errorWarningMessages.formatMessage(1120,
                        ERROR_MESSAGE_01120, this.getClass(), ce);
                logger.error(msg);

                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                throw new RuntimeException(msg, ce);
            }
        } else {
            String msg = errorWarningMessages.formatMessage(1090,
                    ERROR_MESSAGE_01090, this.getClass());
            logger.error(msg);

            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
    }

    public boolean isUpdateable() {
        boolean isUpdate = false;
        try {
            if ("true".equalsIgnoreCase(config.getString("jodi.update"))) {
//    		 if ("true".equalsIgnoreCase(config.getProperty("jodi.update"))){
                isUpdate = true;
            }
        } catch (ConversionException ex) {
            String msg = errorWarningMessages.formatMessage(6010,
                    ERROR_MESSAGE_06010, this.getClass(), ex);
            logger.error(msg, ex);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
        }
        return isUpdate;
    }

    @Override
    public boolean hasDeprecateCDCProperty() {
        String test;
        try {
            test = config.getString("odi.cdc.subscriber");
        } catch (ConversionException e) {
            test = null;
        }
        return (test != null) ? true : false;
    }

    @Override
    public boolean includeDetails() {
        boolean include = true;
        try {
            if ("false".equalsIgnoreCase(config.getString(JodiConstants.JODI_INCLUDE_DETAIL))) {
                include = false;
            }
        } catch (ConversionException ex) {
            String msg = errorWarningMessages.formatMessage(6020,
                    ERROR_MESSAGE_06020, this.getClass(), ex);
            logger.error(msg, ex);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
        }
        return include;
    }

    @Override
    public String getRowidColumnName() {
        return getProperty(JodiConstants.ROW_WID);
    }

    private String upCase(String target) {
        return (target == null ? null : target);
    }

    public Map<String, PropertyValueHolder> getAllProperties() {
        Map<String, PropertyValueHolder> properties = new HashMap<>();
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();
            properties.put(key, new JodiPropertyValueHolder(key, this));
        }

        return properties;
    }

    public PropertyValueHolder getPropertyValueHolder(String key) {
        if (!config.containsKey(key)) {
            String msg = errorWarningMessages.formatMessage(1240,
                    ERROR_MESSAGE_01240, this.getClass(), key);
            logger.error(msg);

            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }

        return new JodiPropertyValueHolder(key, this);
    }

    @Override
    public String getListAsString(String key) {
        StringBuilder value = new StringBuilder();
        for (String piece : config.getStringArray(key)) {
            value.append(piece);
            value.append(",");
        }
        assert (value.toString().contains(","));
        return value.toString().substring(0, value.toString().length() - 1);
    }

    @Override
    public String getTemporaryInterfacesRegex() {
        if (getPropertyKeys().contains(JodiConstants.TEMPORARY_MAPPING_REGEX_PROPERTY)) {
            return getProperty(JodiConstants.TEMPORARY_MAPPING_REGEX_PROPERTY);
        } else {
            return "(_S)[0-9]{2,2}$";
        }
    }
}
