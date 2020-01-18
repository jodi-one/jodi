package one.jodi.tools.impl;

import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.impl.TransformationImpl;
import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingType;
import oracle.odi.domain.mapping.Mapping;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransformationNameBuildingStepTest {

    String prefix = "PREFIX_";
    one.jodi.tools.impl.TransformationNameBuildingStep fixture = null;
    String transformationName = "MyMapping";

    @Mock
    Mapping mapping;
    @Mock
    JodiProperties properties;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fixture = new TransformationNameBuildingStep(prefix, properties);
        when(mapping.getName()).thenReturn(prefix + transformationName);
    }

    @Test
    public void testExplicit() {
        when(properties.getPropertyKeys()).thenReturn(
                Arrays.asList(
                        TransformationNameBuildingStep.option));
        when(properties.getProperty(TransformationNameBuildingStep.option)).thenReturn("true");

        TransformationImpl transformation = new TransformationImpl();

        MappingHolder mappingHolder = mock(MappingHolder.class);
        when(mappingHolder.getType()).thenReturn(MappingType.Indeterminate);

        fixture.processPreEnrichment(transformation, mapping, mappingHolder);

        assertEquals(transformationName, transformation.getName());
    }

    // Should leave transformation name unpopulated.
    @Test
    public void testDefault() {
        when(properties.getPropertyKeys()).thenReturn(new ArrayList<String>());

        TransformationImpl transformation = new TransformationImpl();

        MappingHolder mappingHolder = mock(MappingHolder.class);

        fixture.processPreEnrichment(transformation, mapping, mappingHolder);


        assertEquals(null, transformation.getName());
    }


}
