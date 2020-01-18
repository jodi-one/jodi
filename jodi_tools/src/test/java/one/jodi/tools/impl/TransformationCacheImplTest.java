package one.jodi.tools.impl;

import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.impl.TransformationImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


public class TransformationCacheImplTest {

    @Mock
    JodiProperties properties;
    one.jodi.tools.impl.TransformationCacheImpl fixture = null;

    @Before
    public void setUp() throws java.lang.Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPackageSequenceAssignmentSpecified() {

        when(properties.getPropertyKeys()).thenReturn(
                Arrays.asList(
                        one.jodi.tools.impl.TransformationCacheImpl.INITIAL_PACKAGE_SEQUENCE_PROPERTY,
                        one.jodi.tools.impl.TransformationCacheImpl.PACKAGE_SEQUENCE_STEP_PROPERTY));
        when(properties.getProperty(one.jodi.tools.impl.TransformationCacheImpl.INITIAL_PACKAGE_SEQUENCE_PROPERTY)).thenReturn("100");
        when(properties.getProperty(one.jodi.tools.impl.TransformationCacheImpl.PACKAGE_SEQUENCE_STEP_PROPERTY)).thenReturn("100");

        fixture = new one.jodi.tools.impl.TransformationCacheImpl(properties);


        TransformationImpl transformation1 = new TransformationImpl();
        TransformationImpl transformation2 = new TransformationImpl();

        fixture.registerTransformation(transformation1);
        fixture.registerTransformation(transformation2);

        //assertEquals(100, fixture.getPackageSequence(transformation1));
        assertEquals(200, fixture.getPackageSequence(transformation2));
    }


    @Test
    public void testGetPackageSequenceAssignmentDefault() {
        fixture = new one.jodi.tools.impl.TransformationCacheImpl(properties);

        TransformationImpl transformation1 = new TransformationImpl();
        TransformationImpl transformation2 = new TransformationImpl();

        fixture.registerTransformation(transformation1);
        fixture.registerTransformation(transformation2);

        assertEquals(1000000, fixture.getPackageSequence(transformation1));
        assertEquals(1000100, fixture.getPackageSequence(transformation2));
    }


    @Test
    public void testGetTransformations() {
        fixture = new TransformationCacheImpl(properties);

        TransformationImpl transformation1 = new TransformationImpl();
        TransformationImpl transformation2 = new TransformationImpl();

        fixture.registerTransformation(transformation1);
        fixture.registerTransformation(transformation2);

        fixture.getTransformations().containsAll(Arrays.asList(transformation1, transformation1));
    }

    @Test
    public void testClear() {
        fixture = new TransformationCacheImpl(properties);

        TransformationImpl transformation1 = new TransformationImpl();
        TransformationImpl transformation2 = new TransformationImpl();

        fixture.registerTransformation(transformation1);
        fixture.registerTransformation(transformation2);

        fixture.clear();

        assert (fixture.getTransformations().size() == 0);

        TransformationImpl transformation3 = new TransformationImpl();
        fixture.registerTransformation(transformation3);

        assertEquals(1000000, fixture.getPackageSequence(transformation3));
    }
}
