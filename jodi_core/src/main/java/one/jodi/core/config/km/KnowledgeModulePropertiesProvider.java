package one.jodi.core.config.km;

import one.jodi.etl.km.KnowledgeModuleType;

import java.util.List;

/**
 * This interface defines the properties (aka rules) available for use in a configuration-based KM approach
 * as set in the Jodi properties file.
 *
 */
public interface KnowledgeModulePropertiesProvider {


    /**
     * Get a chain of rules describing the configuration for apply KM names and options.  If
     * there are configuration errors, the method will throw a Runtime Exception.  If caught
     * the client may obtain a list of exceptions with the <code>getErrors()</code> method below
     *
     * @param type the KM type the model List is desired for
     * @return list of models in ascending order
     */
    List<KnowledgeModuleProperties> getProperties(KnowledgeModuleType type);

    /**
     * This accesses the human readable error messages generated in formulating the configuration for KMs.
     *
     * @return list of error strings
     */
    List<String> getErrorMessages();


}
