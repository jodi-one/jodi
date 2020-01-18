package one.jodi.core.config;

import one.jodi.base.config.ConfigurationException;

import java.util.List;
import java.util.Map;

/**
 * Defines the interface for accessing application properties.
 *
 */
public interface JodiProperties {

    static final String DATASETSEPERATOR = "_";

    /**
     * Returns the value of the project code property.
     *
     * @return the value of the project code property.
     */
    String getProjectCode();

    /**
     * Returns the property value for the given property name.
     *
     * @param name
     * @return the property value for the given property name
     */
    String getProperty(String name);

    /**
     * Returns a list of String values associated with a property key.
     * values are separated with a comma in the properties file.
     *
     * @param prop
     * @return list of String values associated with a property key.
     */
    List<String> getPropertyList(final String prop);

    /**
     * Returns a map of string to string associated with property key.
     * Key value pairs are separated with a comma with the key and value separated by a colon.
     *
     * @return a map of string to string associated with property key
     */
    Map<String, String> getPropertyMap(final String prop);

    /**
     * Returns a list of properties that are defined in the properties file.
     *
     * @return list of keys in the properties file
     */
    List<String> getPropertyKeys();

    /**
     * Gets the rowid column name.
     *
     * @return the rowid column name
     */
    String getRowidColumnName();

    /**
     * Boolean value that indicates wether to update interfaces,
     * or drop and recreate them.
     *
     * @return updateIndication
     */
    boolean isUpdateable();

    boolean includeDetails();

    boolean hasDeprecateCDCProperty();

    PropertyValueHolder getPropertyValueHolder(String key);

    String getListAsString(String key);

    String getInputSchemaLocation() throws ConfigurationException;

    String getPackageSchemaLocation() throws ConfigurationException;

    String getTemporaryInterfacesRegex();
}