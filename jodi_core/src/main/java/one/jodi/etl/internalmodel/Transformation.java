package one.jodi.etl.internalmodel;


import one.jodi.model.extensions.TransformationExtension;

import java.util.List;

/**
 * The Transformation class is the top level object in the Jodi hierarchy which describes how Jodi will create interfaces and packages in ODI.
 * This object hierarchy contains both information specified by the input model via XML but also "enriched" information, that is, information
 * computed by Jodi by recognition of patterns in the source and target data models.
 */
public interface Transformation {
    /**
     * Determine the package sequence associated with Transformation
     *
     * @return sequence number
     */
    int getPackageSequence();

    /**
     * Indicator if the table is temporary and generated through ODI interface.  This information is computed by Jodi.
     *
     * @return table is temporary.
     */
    boolean isTemporary();

    /**
     * Gets comments as defined by input model e.g. <code>Transformation/Comments</code>
     *
     * @return comments
     */
    String getComments();

    /**
     * Gets name of transformation / the ODI interface name.  The interface may be explicitly specified in the input model's <code>Transformation/Name</code>
     * node; or otherwise derived via the target data store name in the mapping.  This behavior may be extended through plugin TODO.
     *
     * @return transformation name
     */
    String getName();

    String getOriginalFolderPath();

    /**
     * Gets the list of <code>Dataset</code> objects which define the sources of information used in the transformation.
     * <p>
     * This will not return null, if no Datasets are in the collection this will return an empty list, however a validated
     * Transformation will contain at least one dataset.
     *
     * @return datasets
     */
    List<Dataset> getDatasets();

    /**
     * Get the list of Mapping objects which are define how the datasets are written to target data store.
     *
     * @return mapping
     */
    Mappings getMappings();

    /**
     * Get the name of the ODI folder the transformation/ODI interface is written to.  This information is derived by Jodi, and by
     * default is determined from model of the target data store.  This behavior may be changed using the Folder Name plugin.
     *
     * @return ODI folder name
     */
    String getFolderName();

    /**
     * Get the package list definition supplied in the input model.
     * <p>
     * This is a comma separated value.
     */
    String getPackageList();

    /**
     * Fetch the extension as defined by Jodi customization.
     *
     * @return extension
     */
    TransformationExtension getExtension();

    /**
     * @return Return the number of datasets present.
     */
    int getMaxDatasetNumber();

    /**
     * Indicating use of expressions in transformationserviceprovider odi12transformationserviceprovider.
     */
    boolean useExpressions();

    /**
     * @return A MappingCommand to be executed at the beginning of a mapping.
     */
    MappingCommand getBeginMappingCommand();

    /**
     * @return A MappingCommand to be executed at the end of a mapping.
     */
    MappingCommand getEndMappingCommand();

    boolean isAsynchronous();

}

