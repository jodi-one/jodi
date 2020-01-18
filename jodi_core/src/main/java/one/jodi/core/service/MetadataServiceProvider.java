package one.jodi.core.service;

import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.Transformation;

import java.util.List;

/**
 * This interface defines the API's that must be implemented by a
 * {@link Transformation} metadata provider. The
 * <code>MetadataServiceProvider</code> implementation will send {@link ETL} and
 * {@link Transformation} metadata to the application via the via the
 * {@link #getETLMetaData()} method and the callbacks on the
 * {@link ScenarioServiceImpl} interface respectively
 *
 */
public interface MetadataServiceProvider {

    /**
     * Returns an instance of the {@link ETL} metadata object.
     *
     * @param journalized
     * @return Packages metadata instance
     */
    List<ETLPackage> getPackages(boolean journalized);

    List<ETLPackageHeader> getPackageHeaders(boolean journalized);

    /**
     * Initiates the processing of metadata and injection into the application
     * via the provided <code>TransformationMetadataHandler</code>
     * implementation.
     *
     * @param handler <code>TransformationMetadataHandler</code> implementation that
     *                receives callbacks
     */
    void provideTransformationMetadata(TransformationMetadataHandler handler);

    List<Transformation> getInternaTransformations();

    /**
     * Callback interface for the handler that will receive
     * {@link Transformation} metadata.
     *
     */
    public interface TransformationMetadataHandler {

        // TODO: The following handleTransformation interface is the more
        // permanent approach and is
        // being added now to facilitate the streaming metadata provider

        /**
         * Called with each {@link Transformation} metadata instance.
         *
         * @param transformation {@link Transformation} metadata instance
         */
        void handleTransformation(Transformation transformation);

        // TODO: Two separate methods are a hack until we determine an approach
        // for ordering actions

        /**
         * Called with each {@link Transformation} metadata instance in
         * ascending dependency order.
         *
         * @param transformation  {@link Transformation} metadata instance
         * @param packageSequence sequence
         */
        void handleTransformationASC(Transformation transformation,
                                     int packageSequence);

        /**
         * Called with each {@link Transformation} metadata instance metadata
         * instance in descending dependency order.
         *
         * @param transformation {@link Transformation} metadata instance
         */
        void handleTransformationDESC(Transformation transformation);

        /**
         * Called after all processing.
         */
        void post();

        /**
         * Called after ASC processing.
         */
        void postASC();

        /**
         * Called after DESC processing.
         */
        void postDESC();

        /**
         * Called before all processing.
         */
        void pre();

        /**
         * Called before ASC processing.
         */
        void preASC();

        /**
         * Called before DESC processing.
         */
        void preDESC();
    }

}
