package one.jodi.core.service.impl;

import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.Transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;


/**
 * Mock MetadataServiceProvider implementation
 *
 */
public class MockMetadataServiceProvider implements MetadataServiceProvider {
    private List<ETLPackage> etl;
    private List<Transformation> transformations =
            new ArrayList<Transformation>();

    /**
     * Creates a new MockMetadataServiceProvider instance.
     */
    public MockMetadataServiceProvider() {
    }

    /**
     * DOCUMENT ME!
     *
     * @param transformations DOCUMENT ME!
     */
    public void addTransformationMetadata(
            final Transformation... transformations) {
        this.transformations.addAll(Arrays.asList(transformations));
    }

    @Override
    public List<ETLPackage> getPackages(boolean journalized) {
        return etl;
    }

    /**
     * @see MetadataServiceProvider#provideTransformationMetadata(one.jodi.core.service.MetadataServiceProvider$TransformationMetadataHandler)
     */
    @Override
    public void provideTransformationMetadata(
            final TransformationMetadataHandler handler) {

        if ((transformations != null) && !transformations.isEmpty()) {
            handler.pre();
            handler.preASC();

            int idx = 0;

            for (Transformation t : transformations) {
                handler.handleTransformationASC(t, idx++);
            }

            handler.postASC();

            --idx;
            handler.preDESC();

            for (; idx >= 0; --idx) {
                handler.handleTransformationDESC(transformations.get(idx));
            }

            handler.postDESC();
            handler.post();
        }

    }

    public void setETLMetadata(final List<ETLPackage> etl) {
        this.etl = etl;
    }

    Transformation createTransformation() {
        return mock(Transformation.class);
    }

    @Override
    public List<ETLPackageHeader> getPackageHeaders(boolean journalized) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Transformation> getInternaTransformations() {
        // TODO Auto-generated method stub
        return Collections.EMPTY_LIST;
    }
}
