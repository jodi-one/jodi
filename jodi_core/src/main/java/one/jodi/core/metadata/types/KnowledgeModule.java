package one.jodi.core.metadata.types;

import one.jodi.etl.km.KnowledgeModuleType;

import java.util.Map;

/**
 * Metadata that describes the ODI KM.
 *
 */
public interface KnowledgeModule {
    /**
     * Obtain the type of KM represented.
     *
     * @return type
     */
    KnowledgeModuleType getType();

    /**
     * Fetch the name of the KM
     *
     * @return name
     */
    String getName();

    /**
     * Determine if the KM is multi-technology, e.g. is used as both LKM and IKM.
     *
     * @return multi-technology
     */
    boolean isMultiTechnology();

    /**
     *
     */
    Map<String, KMOptionType> getOptions();


    enum KMOptionType {
        CHECKBOX,
        SHORT_TEXT,
        LONG_TEXT,
        CHOICE
    }
}
