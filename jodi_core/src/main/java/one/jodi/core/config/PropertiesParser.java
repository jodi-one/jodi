package one.jodi.core.config;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses properties file and extracts properties, validates the settings and
 * constructs a list of class instances that capture the properties.
 * The getter and setter methods of the target class must reflect the named of
 * the properties in the configuration file to successfully parse the properties.
 * <p>
 * Group of patterns can be parsed as long as they follow the following naming
 * convention:   <topic>.<group>.<property> = <value> or list of <value>
 *
 * <topic>    identifies a larger topic for which properties are filtered.
 * For example, the topic could be "model", "km" or "odi"
 * and reflect various modules and core functionality within
 * the configuration.
 * <group>    identifies a group of property keys that describe a unit of
 * configuration information. For example, model.star could
 * identify the star model definitions or km.oracle may identify
 * KM properties related to Oracle technology.
 * <property> defines the name of a specific property that belongs in the
 * scope of the defined <topic>.<group>.
 * For example, model.star.name may define the name of a model that
 * contains the star schema in the ODI model definition.
 * <value>    may be a String without blank spaces that represents strings or
 * numbers. Comma-separated values are interpreted as a list of
 * properties and will be translated into a list of Strings or numbers.
 * Comma-separated values that have the form <key>:<value> will be transformed
 * into a map of objects of type String, Boolean or Integer.
 * <p>
 * Properties with the same topic and group are mapped to one class instance. Each
 * group must have a different name.
 * <p>
 * Topic, group and property names must start with a alpha character [a-zA-Z] and may
 * contain subsequently numbers and '_' characters.
 * <p>
 * Example:
 * <p>
 * The following example defines two source systems that belong to the SOURCE
 * layer and one data mart model that belongs to the STAR layer.
 * <p>
 * model.star.code       = GBU_DATA_MART_MODEL   # same name as in ODI
 * model.star.order      = 100
 * model.star.layer      = STAR
 * model.star.prefix     = W_
 * model.star.postfix    = _D, _F, _A, _H
 * <p>
 * The example is intended to highlight how the properties can be interpreted,
 * which however is out of the scope of this feature.
 * <p>
 * This configuration defines a star schema with ODI Code GBU_DATA_MART_MODEL that belongs
 * to the "STAR" layer of the architecture. Table named with a "W_" prefix and one of the
 * defined postfixes is considered part of the GBU_DATA_MART_MODEL. The order property
 * reflects ordering within the layer and between layers and will be used to offer
 * precedence rules.
 */
public class PropertiesParser<T> {

    private final static Logger logger = LogManager.getLogger(PropertiesParser.class);
    private final static String newLine = System.getProperty("line.separator");
    private final static String ERROR_MESSAGE_01140 =
            "The parameter definition string '%1$s' is not correctly formed.";
    private final static String ERROR_MESSAGE_01150 =
            "The mandatory property '%1$s' in property group '%2$s' is missing.";
    private final static String ERROR_MESSAGE_01151 =
            "Mandatory properties missing.";
    private final static String ERROR_MESSAGE_01160 =
            "Creating a bean instance for type '%1$s' failed.";
    private final static String ERROR_MESSAGE_01170 =
            "Population of the bean instance '%1$s' experienced an error.";
    private final static String ERROR_MESSAGE_06030 =
            "Failed to list property for property key = %s";
    private final static String ERROR_MESSAGE_06040 =
            "Failed to add item to map for property key = %s";
    private final static String ERROR_MESSAGE_06050 =
            "No value has been defined for property '%1$s' in property group '%2$s'.";
    private final static String ERROR_MESSAGE_06060 =
            "Property '%1$s' is undefined in property group '%2$s'. This property will be ignored.";
    private final static String CORE_PROP_PATTERN = "(([A-Za-z]+[A-Za-z0-9_]*).([A-Za-z]+[A-Za-z0-9_]*))";


    /*
     * parameter definition expresses which parameters are expected in the properties files and
     * it also conveys if information is mandatory (ends with '!') or a list (ends with '[]')
     */
    private final static int patternGroupIndex = 2;
    private final static int patternPropertyIndex = 3;
    //private final static String  PROPDEF_PATTERN = "([A-Za-z]+[A-Za-z0-9]*)(\\[\\])?!?";
    private final static String PROPDEF_PATTERN = "([A-Za-z]+[A-Za-z0-9_]*)(\\[\\]|\\{\\})?!?";
    private final static Pattern propDefPattern = Pattern.compile(PROPDEF_PATTERN);
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final JodiProperties wfProperties;

    public PropertiesParser(final JodiProperties wfProperties,
                            final ErrorWarningMessageJodi errorWarningMessages) {
        this.wfProperties = wfProperties;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * @param clazz defines the object type of the bean to be created
     */
    private T createBeanInstance(Class<T> clazz)
            throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }

    private boolean isMandatory(final String propertyName, final List<String> parameterDefs) {
        boolean isMandatory = false;


        for (String param : parameterDefs) {
            if (propertyName.equals(getName(param)) && (param.endsWith("!"))) {
                isMandatory = true;
                break;
            }
        }
        return isMandatory;
    }

    boolean isList(final String propertyName, final List<String> parameterDefs) {
        boolean isList = false;

        for (String param : parameterDefs) {
            String p = param.endsWith("!") ? param.substring(0, param.length() - 1)
                    : param;
            if (p.endsWith("[]") && propertyName.equals(p.substring(0, p.length() - "[]".length()))) {
                isList = true;
                break;
            }
        }
        return isList;
    }

    private boolean isMap(final String propertyName, final List<String> parameterDefs) {
        boolean isMap = false;

        for (String param : parameterDefs) {
            String p = param.endsWith("!") ? param.substring(0, param.length() - 1)
                    : param;
            if (p.endsWith("{}") && propertyName.equals(p.substring(0, p.length() - "{}".length()))) {
                isMap = true;
                break;
            }
        }
        return isMap;
    }

    private String getName(final String nameDef) {
        Matcher m = propDefPattern.matcher(nameDef);
        m.matches();
        assert (m.matches()) : "Wrong implementation.";
        return m.group(1);
    }

    private void updatePropertiesMap(final String group, final String propertyName,
                                     final Object value, final String groupFieldName,
                                     final Map<String, Map<String, Object>> map,
                                     String topic) {
        Map<String, Object> groupMap;
        if (map.containsKey(group)) {
            groupMap = map.get(group);
            groupMap.put(propertyName, value);
        } else {
            groupMap = new HashMap<>();
            groupMap.put(groupFieldName, topic + "." + group);
            groupMap.put(propertyName, value);
            map.put(group, groupMap);
        }
    }

    //Strip out all items that only contain an empty String
    private List<String> getCleanedList(String key) {
        List<String> cleanedList = new ArrayList<>();
        List<String> list = (List<String>) wfProperties.getPropertyList(key);
        for (String s : list) {
            if ((s != null) && (!s.equals(""))) {
                cleanedList.add(s);
            } else {
                String msg = errorWarningMessages.formatMessage(6030,
                        ERROR_MESSAGE_06030, this.getClass(), key);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.WARNINGS);
            }
        }
        return cleanedList;
    }

    // Attempt to build a map with key value pairs, attempting to create Booleans or Integers where recognized
    private Map<String, Object> getCleanedMap(String propertyKey) {
        Map<String, Object> cleanedMap = new HashMap<>();
        Map<String, String> map = wfProperties.getPropertyMap(propertyKey);
        for (Entry<String, String> entry : map.entrySet()) {
            String v = entry.getValue();
            String k = entry.getKey();
            if (k != null && !"".equals(k) && v != null && !"".equals(v)) {
                try {
                    cleanedMap.put(k, Integer.valueOf(v));
                } catch (NumberFormatException nfe) {
                    if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
                        cleanedMap.put(k, Boolean.valueOf(v));
                    } else {
                        cleanedMap.put(k, v);
                    }
                }
            } else {
                String msg = errorWarningMessages.formatMessage(6040,
                        ERROR_MESSAGE_06040, this.getClass(), propertyKey);
                logger.error(msg);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.WARNINGS);
            }
        }

        return cleanedMap;
    }

    private void addValueIntoHashMap(final String key,
                                     final String groupFieldName,
                                     final List<String> expectedParameters,
                                     final Pattern pattern,
                                     final Map<String, Map<String, Object>> map /* in-out structure */,
                                     String topic) {

        List<String> expectedNames =
                expectedParameters.stream().map(this::getName)
                        .collect(Collectors.toList());

        Matcher m = pattern.matcher(key);
        if (m.matches()) {
            String group = m.group(patternGroupIndex);
            String propertyName = m.group(patternPropertyIndex);
            if (expectedNames.contains(propertyName)) {
                // create new object or find existing object and set properties.
                Object value;
                if (isList(propertyName, expectedParameters)) {
                    value = getCleanedList(key);
                } else if (isMap(propertyName, expectedParameters)) {
                    value = getCleanedMap(key);
                } else {
                    value = wfProperties.getProperty(key);
                }
                if (!"".equals(value)) {
                    updatePropertiesMap(group, propertyName, value, groupFieldName, map, topic);
                } else {
                    String msg = errorWarningMessages.formatMessage(6050,
                            ERROR_MESSAGE_06050, this.getClass(), propertyName, group);
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(), msg,
                            MESSAGE_TYPE.WARNINGS);
                }
            } else {
                String msg = errorWarningMessages.formatMessage(6060,
                        ERROR_MESSAGE_06060, this.getClass(), propertyName, group);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.WARNINGS);
            }
        }
    }

    /*
     * validate that property names are syntactically correct
     */
    private void validateParameterDefinitions(final List<String> parameterDefs) {
        for (String param : parameterDefs) {
            Matcher m = propDefPattern.matcher(param);
            if (!m.matches()) {
                String msg = errorWarningMessages.formatMessage(1140,
                        ERROR_MESSAGE_01140, this.getClass(), param);
                logger.error(msg);

                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    private void checkforMandatoryProperties(final String groupFieldName,
                                             final List<String> parameterDefs,
                                             Map<String, Map<String, Object>> map) {
        // validate that values for all mandatory properties are defined

        //collects all mandatory property names
        List<String> mandatoryProperties = parameterDefs.stream()
                .filter(param -> isMandatory(getName(param),
                        parameterDefs))
                .map(this::getName)
                .collect(Collectors
                        .toList());

        List<String> errorMessages = new ArrayList<>();
        for (Map<String, Object> groupMap : map.values()) {
            mandatoryProperties.stream()
                    .filter(name -> !groupMap.containsKey(name))
                    .forEach(name -> {
                        String msg = errorWarningMessages
                                .formatMessage(1150,
                                        ERROR_MESSAGE_01150,
                                        this.getClass(),
                                        name,
                                        groupMap.get(groupFieldName));
                        errorWarningMessages.addMessage(
                                errorWarningMessages
                                        .assignSequenceNumber(),
                                msg,
                                MESSAGE_TYPE.ERRORS);
                        errorMessages.add(msg);
                    });
        }
        if (errorMessages.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String message : errorMessages) {
                sb.append(message).append(newLine);
            }
            logger.error(sb.toString());
            String msg = errorWarningMessages.formatMessage(1151,
                    ERROR_MESSAGE_01151, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
    }


    /**
     * Parses properties in a given topic and creates a list of objects of type
     * propertyBeanClazz. Each instance represents a groups of related
     * properties. The parser accepts a list of possible property names in a
     * parameter descriptor that also indicates if the required property
     * represents a list of properties or is mandatory.
     * <p>
     * Runtime Exceptions are thrown if the parameter descriptor is incorrectly
     * formed, the Java bean instance cannot be created, the Java bean cannot be
     * correctly updated with properties due to mismatches of getter and setter
     * methods with the names of properties defined in the parameter definition.
     *
     * @param topic             - defines the name of the topic that is leading prefix of all
     *                          keys that are parsed
     * @param groupFieldName    - name of the property in the Java bean defined in the
     *                          propertyBeanClazz class that contains the name of the group
     *                          name, which is the second
     * @param parameterDefs     - parameter descriptor, which is a list of property names that
     *                          may be used in this context. A property name that has '[]'
     *                          attached to the end of the name is a list (will be mapped to a
     *                          List in the bean). A property name that ends on '!' is
     *                          considered mandatory and an exception will be thrown if the
     *                          parameter is not defined for a given group.
     *                          <p>
     *                          Example: {"name!", "prefix[]", "postfix[]!", "order"}
     *                          implies that name and postfix are mandatory properties and
     *                          prefix and postfix are lists of properties.
     * @param propertyBeanClazz - class representing a Java bean with appropriate getter and
     *                          setter methods that align with the parameter definition. For
     *                          example, if the parameter definition contains "name" and
     *                          "order", the bean must implement the methods <type> getName(),
     *                          <type> getOrder(), setName(<type>) and setOrder(<type>). The
     *                          type can be primitive such as 'int' or 'String' or can be a
     *                          List of a primitive Java type.
     * @return List of Java beans of type propertyBeanClazz that capture the
     * properties with each object contains data for the same group of
     * properties as defined by the same group property key (second part
     * of the overall key)
     */
    public List<T> parseProperties(final String topic,
                                   final String groupFieldName,
                                   final List<String> parameterDefs,
                                   final Class<T> propertyBeanClazz) {

        validateParameterDefinitions(parameterDefs);

        Map<String, Map<String, Object>> map = new HashMap<>();
        Pattern pattern = Pattern.compile(topic + "\\." + CORE_PROP_PATTERN);
        for (String key : wfProperties.getPropertyKeys()) {
            addValueIntoHashMap(key, groupFieldName, parameterDefs, pattern, map, topic);
        }

        checkforMandatoryProperties(groupFieldName, parameterDefs, map);

        // at this point all properties have been split up in groups and inserted in a Map
        // we use BeanUtils to create new instances of the target object.
        List<T> properties = new ArrayList<>();
        for (Map<String, Object> groupMap : map.values()) {
            T property = null;

            for (String def : parameterDefs) {
                if ((def.endsWith("[]")) && (!groupMap.containsKey(def.substring(0, def.length() - "[]".length())))) {
                    groupMap.put(def.substring(0, def.length() - "[]".length()), Collections.emptyList());
                } else if (def.endsWith("{}") && (!groupMap.containsKey(def.substring(0, def.length() - "{}".length())))) {
                    groupMap.put(def.substring(0, def.length() - "{}".length()), Collections.emptyMap());
                }
            }


            try {
                property = createBeanInstance(propertyBeanClazz);
                BeanUtils.populate(property, groupMap);
                properties.add(property);
            } catch (InstantiationException e) {
                String msg = errorWarningMessages.formatMessage(1160,
                        ERROR_MESSAGE_01160, this.getClass(), propertyBeanClazz);
                logger.error(msg);

                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new RuntimeException(msg, e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                String msg = errorWarningMessages.formatMessage(1170,
                        ERROR_MESSAGE_01170, this.getClass(), property);
                logger.error(msg);

                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new RuntimeException(msg, e);
            }
        }
        return properties;
    }

    public List<T> parseProperties(final String topic,
                                   final List<String> parameterDefs,
                                   final Class<T> clazz) {

        return parseProperties(topic, topic + "ID", parameterDefs, clazz);
    }

}
