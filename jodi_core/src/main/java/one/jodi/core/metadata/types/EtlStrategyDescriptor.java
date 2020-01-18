package one.jodi.core.metadata.types;

import one.jodi.etl.km.KnowledgeModuleType;

import java.util.Map;

/**
 * Represents a ETL Strategy descriptor to express how data is extracted,
 * transformed and inserted.
 *
 */
public interface EtlStrategyDescriptor {

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
     * Options associated with this strategy
     */
    Map<String, OptionType> getOptions();

    enum OptionType {CHECKBOX, SHORT_TEXT, LONG_TEXT, CHOICE}
}
