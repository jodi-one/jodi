package one.jodi.core.extensions.contexts;

import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.model.extensions.MappingsExtension;

import java.util.List;

/**
 * Contextual information used to determine application of which Integration
 * Knowledge Module to use.
 * <p>
 * Note that if Dataset-specific information is intended to be used the
 * information is provided in the {@link LoadKnowledgeModuleExecutionContext}
 * class.
 *
 */
public interface KnowledgeModuleExecutionContext extends DataStoreExecutionContext {

    /**
     * Gets the target data store description.
     *
     * @return returns a description of the data store with the name defined as
     * a target data store in the Mappings element of the input XSL.
     * The object describes the selected model  the data store is located and
     * and further details related to the physical location of the model.
     */
    public DataStore getTargetDataStore();

    /**
     * Access the properties based rules chain as wired in Jodi properties file.
     * The rule chain will contain at least one entry, however no rule may
     * match.
     *
     * @return list of Knowledge Module configurations that may apply to a given
     * model for the specific KM type
     */
    public List<KnowledgeModuleProperties> getConfigurations();


    /**
     * Gets mappings extension that is associated with the XML specification
     * mappings element.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations.
     */
    MappingsExtension getMappingsExtension();


    /**
     * Gets the metadata for all KMs loaded into ODI for all IKMs, CKMs and LKMs.
     *
     * @return KMs
     */
    List<KnowledgeModule> getKMs();

}

