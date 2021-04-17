package one.jodi;

import one.jodi.base.config.ConfigurationException;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.JodiPropertyValueHolder;
import one.jodi.core.config.PropertyValueHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;


public class TestProperties implements JodiProperties {
    public static final String LIST_SEPERATOR = ",";
    private final Properties properties;

    public TestProperties() {
        properties = new Properties();
    }

    public TestProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String getProjectCode() {
        return getProperty(JodiConstants.ODI_PROJECT_CODE);
    }

    @Override
    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public List<String> getPropertyList(String key) {
        String listProp = properties.getProperty(key);
        List<String> result = null;

        if (listProp != null) {

        }

        return result;
    }

    @Override
    public Map<String, String> getPropertyMap(String prop) {
        List<String> list = getPropertyList(prop);
        HashMap<String, String> map = new HashMap<>();
        if (list != null) {
            for (String s : list) {
                String[] keyValuePair = s.split(":", 2);
                if (keyValuePair.length != 2) {
                    throw new RuntimeException("Unsuccessful attempt to parse name value pair from list using odi property: " + prop + " (as a Map)");
                } else {
                    map.put(keyValuePair[0].trim(), keyValuePair[1].trim().toUpperCase());
                }
            }
        }
        return map;
    }

    @Override
    public List<String> getPropertyKeys() {
        Set<Object> propSet = properties.keySet();
        return propSet.stream().map(Object::toString).collect(Collectors.toList());
    }

    @Override
    public String getRowidColumnName() {
        return getProperty(JodiConstants.ROW_WID);
    }

    @Override
    public boolean isUpdateable() {
        boolean isUpdate = false;
        try {
            if ("true".equalsIgnoreCase(getProperty("jodi.update"))) {
                isUpdate = true;
            }
        } catch (Exception ex) {
            // no-op
        }
        return isUpdate;
    }

    @Override
    public boolean includeDetails() {
        boolean include = true;

        try {
            if ("false".equalsIgnoreCase(getProperty(JodiConstants.JODI_INCLUDE_DETAIL))) {
                include = false;
            }
        } catch (Exception ex) {
            // no-op
        }
        return include;
    }

    @Override
    public boolean hasDeprecateCDCProperty() {
        return false;
    }

    public Map<String, PropertyValueHolder> getAllProperties() {
        Map<String, PropertyValueHolder> allProperties = new HashMap<>();
        for (Object o : properties.keySet()) {
            String key = o.toString();
            allProperties.put(key,
                    new JodiPropertyValueHolder(key,
                            this));
        }

        return allProperties;
    }

    @Override
    public PropertyValueHolder getPropertyValueHolder(String key) {
        return new JodiPropertyValueHolder(key, this);
    }

    @Override
    public String getListAsString(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInputSchemaLocation() throws ConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPackageSchemaLocation() throws ConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTemporaryInterfacesRegex() {
        return "(_S){1,1}[0-9]{1,}";
    }
}
