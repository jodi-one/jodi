package one.jodi.etl.builder;

import one.jodi.etl.internalmodel.Transformation;

/**
 * This interfaces serves to communicate Jodi enriched information necessary for deleting a given Transformation.
 * 
 * See EnrichingBuilder.createDeleteContext(Transformation) for more information.
 * 
 * By calling {@link EnrichingBuilder#enrich(Transformation, boolean)}
 *
 */
public interface DeleteTransformationContext {
    /**
     * Fetch the ODI interface name for transformation to be deleted, e.g. Transformation.getName()
     *
     * @return transformation name
     */
    String getName();

    /**
     * Fetch the data store name, e.g. Transformation.getMappings().getDataStoreName();
     *
     * @return Data store name
     */
    String getDataStoreName();

    /**
     * Fetch the ODI model of transformation to be deleted, e.g. Transformation.getModel()
     *
     * @return model
     */
    String getModel();


    /**
     * Fetch package sequence used by Jodi for ordering
     *
     * @return package sequence
     */
    int getPackageSequence();

    /**
     * Determines if the interface built by Transformation is temporary
     *
     * @return flag indidicating temporary mappings
     */
    boolean isTemporary();

    /**
     * @return folderName
     */
    String getFolderName();
}