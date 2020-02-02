package one.jodi.tools;

import one.jodi.core.model.Transformation;

import java.util.List;

public interface TransformationCache {

    /**
     * Register transformation and assign package sequence based on registration order.
     *
     * @param transformation transformation to register in the cache
     */
    public void registerTransformation(Transformation transformation);

    /**
     * Remove all transformations, reset package sequence counter.
     */
    public void clear();

    /**
     * Get list of registered transformations in order of registration
     *
     * @return transformations
     */
    public List<Transformation> getTransformations();

    /**
     * Maintain the association between a Transformation and the assigned package sequence.
     *
     * @param transformation transformation
     * @return package sequence
     */
    public int getPackageSequence(Transformation transformation);


}