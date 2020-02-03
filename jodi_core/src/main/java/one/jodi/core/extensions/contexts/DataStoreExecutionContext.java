package one.jodi.core.extensions.contexts;

import one.jodi.core.config.PropertyValueHolder;
import one.jodi.model.extensions.TransformationExtension;

import java.util.Map;

/**
 * This interface is used to provide shared contextual information across
 * source, lookup or target execution context objects.
 */
public interface DataStoreExecutionContext {

    /**
     * Gets the transformation extension, which is a child of the Transformation
     * XML specification.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations.
     */
    public TransformationExtension getTransformationExtension();

    /**
     * Gets properties defined in the properties file.
     *
     * @return Key-value pairs of the properties in the Jodi properties files
     * with exception of those that relate to the underlying ETL tool or
     * DB tool, including user names
     */
    public Map<String, PropertyValueHolder> getProperties();

}