package one.jodi.core.service;

import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;

import java.io.InputStream;


/**
 * Service that provides Transformation management functionality.
 *
 */
public interface TransformationService {

    /**
     * First deletes then recreates all transformations that are injected by the
     * {@link MetadataServiceProvider}.
     *
     * @param journalized
     */
    void createOrReplaceTransformations(boolean journalized);

    /**
     * First deletes then recreates all transformations that are injected by the
     * {@link MetadataServiceProvider}.
     *
     * @param journalized
     */
    void createTransformations(boolean journalized);

    /**
     * Creates a {@link Transformation} from the metadata defined in the specified {@link InputStream}
     *
     * @param xmlFile
     */
    // jkm
    void createTransformation(InputStream xmlFile, int packageSequence, boolean journalized);
    // end jkm

    /**
     * Deletes the transformation that matches the provided name.
     *
     * @param name
     * @throws TransformationAccessStrategyException
     */
    void deleteTransformation(String name, String folder) throws Exception;

    /**
     * Deletes the provided transformation.
     *
     * @param  transformation
     */
    // This isnt used anywhere outside implementation of this interface and create dependency on Transformation
    //void deleteTransformation(Transformation transformation);

    /**
     * Deletes the {@link Transformation} identified by the metadata defined in the specified {@link InputStream}
     *
     * @param xmlFile
     */
    void deleteTransformation(InputStream xmlFile);

    /**
     * Deletes all transformations that are injected by the {@link
     * MetadataServiceProvider}.
     */
    void deleteTransformations(final boolean journalized);
    /**
     * Truncates all transformations that are injected by the {@link
     * MetadataServiceProvider}.
     */
    // jkm
    //void mergeTransformations(final Transformation transformation, final int packageSequence, final boolean journalized);

    /**
     * Truncates all transformations that are injected by the {@link
     * MetadataServiceProvider}.
     */
    void mergeTransformations(InputStream xmlFile, final int packageSequence, final boolean journalized);
    // end jkm 
}
