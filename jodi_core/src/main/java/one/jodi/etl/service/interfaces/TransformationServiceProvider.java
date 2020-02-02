package one.jodi.etl.service.interfaces;

import one.jodi.etl.internalmodel.Transformation;

import java.util.HashMap;

public interface TransformationServiceProvider {
    void deleteTransformation(String name, String folder) throws TransformationException;

    Transformation createTransformation(Transformation transformation,
                                        final boolean isJournalizedData, int packageSequence) throws TransformationException;

    /**
     * Truncates interfaces to be reused in merge.
     *
     * @param transformationName the transformation name
     * @param folderName the folder name
     * @throws TransformationException exception during truncating of interfaces
     */
    void truncateInterfaces(String transformationName, String folderName) throws TransformationException;

    HashMap<String, Transformation> getTransformationCache();

}
