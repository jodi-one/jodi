package one.jodi.tools.impl;

import one.jodi.base.ListAppender;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.tools.InputModelMockHelper;
import one.jodi.tools.dependency.MappingHolder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression.ExecuteOnLocation;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutionLocationBuildingStepTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    JodiProperties properties;
    @Mock
    Mapping mapping;
    ExecutionLocationBuildingStep fixture = null;
    ListAppender listAppender = null;

    public ExecutionLocationBuildingStepTest() {
        listAppender = new ListAppender(this.getClass().getName());
        Logger logger = (Logger) LogManager.getLogger(ExecutionLocationBuildingStep.class);
        logger.addAppender(listAppender);
        logger.setLevel(org.apache.logging.log4j.Level.INFO);
        fixture = new ExecutionLocationBuildingStep(properties);

    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mapping.getName()).thenReturn("TestMapping");

    }


    //@Test
    public void testGenerateWarning() throws AdapterException, MappingException {

        IMapComponent target = mock(IMapComponent.class);
        MapAttribute ma1 = mock(MapAttribute.class);
        MapAttribute ma2 = mock(MapAttribute.class);
        when(target.getAttributes()).thenReturn(Arrays.asList(ma1, ma2));
        when(ma1.getName()).thenReturn("C1");
        when(ma1.getExecuteOnHint()).thenReturn(ExecuteOnLocation.TARGET);
        when(ma2.getName()).thenReturn("C2");
        when(ma2.getExecuteOnHint()).thenReturn(ExecuteOnLocation.SOURCE);
        List<IMapComponent> targets = Arrays.asList(target);

        when(mapping.getTargets()).thenReturn(targets);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation("Transformation1");
        Mappings mappings = InputModelMockHelper.createMockETLMappings("targetDS", new String[]{"C1", "C2"}, "INT", "model");
        when(transformation.getMappings()).thenReturn(mappings);
        for (Targetcolumn tc : mappings.getTargetColumns()) {
            List<ExecutionLocationtypeEnum> el = Arrays.asList(ExecutionLocationtypeEnum.SOURCE);
            when(tc.getExecutionLocations()).thenReturn(el);
        }

        MappingHolder mappingHolder = mock(MappingHolder.class);

        fixture.processPostEnrichment(null, transformation, mapping, mappingHolder);

        if (!listAppender.contains(Level.WARN, "Mapping 'TestMapping' and target column 'C1' specifies 'TARGET' but strategy set 'SOURCE'")) {
            Assert.fail("Test needed to generate error message for column C1");
        }


    }


    @Test
    public void testGenerateODIWarning() throws AdapterException, MappingException {

        IMapComponent target = mock(IMapComponent.class);

        when(target.getAttributes()).thenThrow(new MappingException("in get attributes."));
        List<IMapComponent> targets = Arrays.asList(target);

        when(mapping.getTargets()).thenReturn(targets);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation("Transformation1");
        Mappings mappings = InputModelMockHelper.createMockETLMappings("targetDS", new String[]{"C1", "C2"}, "INT", "model");
        when(transformation.getMappings()).thenReturn(mappings);
        for (Targetcolumn tc : mappings.getTargetColumns()) {
            List<ExecutionLocationtypeEnum> el = Arrays.asList(ExecutionLocationtypeEnum.SOURCE);
            when(tc.getExecutionLocations()).thenReturn(el);
        }

        MappingHolder mappingHolder = mock(MappingHolder.class);

        try {
            thrown.expect(RuntimeException.class);
            fixture.processPostEnrichment(null, transformation, mapping, mappingHolder);
        } catch (RuntimeException re) {
            if (!listAppender.contains(Level.ERROR, "ODI exception")) {
                Assert.fail("Test needed to generate error message for column C1");
            }
            throw new RuntimeException(re);
        }
    }

}
