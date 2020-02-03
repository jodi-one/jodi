package one.jodi.core.km;


import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;

/**
 * Interface of the context/socket object for the strategy to configure ODI
 * Knowledge Modules.
 * <p>
 * It may be possible to condense the various KM calls into a single
 * <code>configure</code> function however it appears that in future refactoring
 * efforts KM type specific information will need to be provided.
 */
public interface KnowledgeModuleContext {

    /**
     * Configures an Loading Knowledge Module based on explicitly set LKM values and enriched model information.
     *
     * @param source      object for which to derive the appropriate LKM
     * @param dataSetName name of the originating xml dataset
     * @return knowledgeModuleConfiguration the name and options for the
     * selected load knowledge module
     */
    public KnowledgeModuleConfiguration getLKMConfig(
            final Source source,
            final String dataSetName);

    /**
     * Determines the staging model used for multi-technology KMs.
     *
     * @param Transformation
     * @return stagingModel
     */

    public String getStagingModel(Transformation transformation);

    /**
     * Configures an Check Knowledge Module based
     *
     * @param transformation transformation object that contains the specification
     * @return knowledgeModuleConfiguration the name and options for the
     * selected check knowledge module
     */
    public KnowledgeModuleConfiguration getCKMConfig(final Transformation transformation);

    /**
     * Configures an Integration Knowledge Module based on
     *
     * @param transformation transformation object that contains the specification
     * @return knowledgeModuleConfiguration the name and options for the
     * selected integration knowledge module
     */
    public KnowledgeModuleConfiguration getIKMConfig(final Transformation transformation);


}
