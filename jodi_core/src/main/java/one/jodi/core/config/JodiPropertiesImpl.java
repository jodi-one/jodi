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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Implements the {@link JodiProperties} interface using a
 * properties file.
 */
@Singleton
public class JodiPropertiesImpl implements JodiProperties {
   public static final String XSD_INTERFACES = "xml.xsd.interfaces";
   public static final String XSD_PACKAGES = "xml.xsd.packages";

   private static final Logger LOGGER = LogManager.getLogger(JodiPropertiesImpl.class);

   private static final String ERROR_MESSAGE_01090 = "The Jodi ODI property file is not found.";

   private static final String ERROR_MESSAGE_01100 =
           "The property '%1$s' is a list, use getPropertyList(String name) " + "instead of \"getProperty\".";

   private static final String ERROR_MESSAGE_01110 =
           "Unsuccessful attempt to parse name value pair from list using odi " + "property: %s (as a Map)";

   private static final String ERROR_MESSAGE_01120 = "Errors encountered attempting to load Jodi properties %s.";

   private static final String ERROR_MESSAGE_01240 =
           "The required configuration property '%1$s' is not found. Please " + "consult the user manual.";

   private static final String ERROR_MESSAGE_06010 = "Exception in isUpdateable %s.";

   private static final String ERROR_MESSAGE_06020 = "Exception in includeDetails %s.";

   private final ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();

   private final Configuration config;

   /**
    * Creates a new JodiPropertiesImpl instance.
    *
    * @param propFile properties file to initialize
    */
   @Inject
   protected JodiPropertiesImpl(final @PropertyFileName String propFile) {
      this.config = initConfiguration(propFile);
   }

   /**
    * @see JodiProperties#getInputSchemaLocation()
    */
   @Override
   public String getInputSchemaLocation() {
      return getProperty(XSD_INTERFACES);
   }

   /**
    * @see JodiProperties#getPackageSchemaLocation()
    */
   @Override
   public String getPackageSchemaLocation() {
      return getProperty(XSD_PACKAGES);
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
   @Override
   public String getProperty(final String prop) {
      assert (!prop.equals(JodiConstants.DATA_MART_PREFIX)) : errorWarningMessages.formatMessage(1100,
                                                                                                 ERROR_MESSAGE_01100,
                                                                                                 this.getClass(),
                                                                                                 JodiConstants.DATA_MART_PREFIX);

      String property = (String) config.getProperty(prop);
      if (property == null && !prop.equals(XSD_INTERFACES) && !prop.equals(XSD_PACKAGES)) {

         String msg = errorWarningMessages.formatMessage(1240, ERROR_MESSAGE_01240, this.getClass(), prop);
         LOGGER.error(msg);
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
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
            String msg = errorWarningMessages.formatMessage(1110, ERROR_MESSAGE_01110, this.getClass(), prop);
            LOGGER.error(msg);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
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
         keys.add(iter.next());
      }
      return keys;
   }

   /**
    * Initialized the singleton <code>JodiPropertiesImpl</code> instance by
    * loading the properties in the specified file.
    *
    * @param fileName properties file
    * @throws RuntimeException When stuff goes awry
    */
   private Configuration initConfiguration(final String fileName) {
      if ((new File(fileName)).exists()) {
         LOGGER.debug("Loading Jodi properties file " + fileName);
         Parameters params = new Parameters();
         File propertiesFile = new File(fileName);
         FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                 new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(params.fileBased()
                                                                                                    .setListDelimiterHandler(
                                                                                                            new DefaultListDelimiterHandler(
                                                                                                                    ','))
                                                                                                    .setFile(
                                                                                                            propertiesFile));
         builder.setAutoSave(true);
         try {
            return builder.getConfiguration();
         } catch (ConfigurationException ce) {
            String msg = errorWarningMessages.formatMessage(1120, ERROR_MESSAGE_01120, this.getClass(), ce);
            LOGGER.error(msg);

            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg, ce);
         }
      } else {
         String msg = errorWarningMessages.formatMessage(1090, ERROR_MESSAGE_01090, this.getClass());
         LOGGER.error(msg);

         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
         throw new RuntimeException(msg);
      }
   }

   @Override
   public boolean isUpdateable() {
      boolean isUpdate = false;
      try {
         if ("true".equalsIgnoreCase(config.getString("jodi.update"))) {
            isUpdate = true;
         }
      } catch (ConversionException ex) {
         String msg = errorWarningMessages.formatMessage(6010, ERROR_MESSAGE_06010, this.getClass(), ex);
         LOGGER.error(msg, ex);
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.WARNINGS);
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
      return test != null;
   }

   @Override
   public boolean includeDetails() {
      boolean include = true;
      try {
         if ("false".equalsIgnoreCase(config.getString(JodiConstants.JODI_INCLUDE_DETAIL))) {
            include = false;
         }
      } catch (ConversionException ex) {
         String msg = errorWarningMessages.formatMessage(6020, ERROR_MESSAGE_06020, this.getClass(), ex);
         LOGGER.error(msg, ex);
         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.WARNINGS);
      }
      return include;
   }

   @Override
   public String getRowidColumnName() {
      return getProperty(JodiConstants.ROW_WID);
   }

   private String upCase(String target) {
      return (target);
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

   @Override
   public PropertyValueHolder getPropertyValueHolder(String key) {
      if (!config.containsKey(key)) {
         String msg = errorWarningMessages.formatMessage(1240, ERROR_MESSAGE_01240, this.getClass(), key);
         LOGGER.error(msg);

         errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
         throw new RuntimeException(msg);
      }

      return new JodiPropertyValueHolder(key, this);
   }

   @Override
   public String getListAsString(String key) {

      String value = String.join(",", config.getStringArray(key));
      assert (value.length() > 0);
      return value;
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
