package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.etl.internalmodel.*;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.DistinctComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.topology.OdiDataType;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.OdiTechnology;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class DistinctBuilderImplTest {

    @Mock
    OdiCommon odiCommon;
    @Mock
    MapRootContainer mapping;
    @Mock
    EtlOperators etlOperators;
    @Mock
    DistinctComponent distinctComponent;
    DistinctBuilderImpl fixture;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        fixture = new DistinctBuilderImpl(odiCommon) {
            @Override
            protected DistinctComponent createDistinctComponent(MapRootContainer mapping) throws MappingException {
                return distinctComponent;
            }
        };
    }


    @Test
    public void testNoDistinct() throws MappingException {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        EtlOperators etlOperators = mock(EtlOperators.class);
        fixture.addDistinct(mapping, transformation, etlOperators);
        verify(etlOperators, times(0)).addDistinctComponents(any(DistinctComponent.class));

    }

    @Test
    public void testDistinct() throws MappingException {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        when(transformation.getMappings().isDistinct()).thenReturn(true);
        EtlOperators etlOperators = mock(EtlOperators.class);
        fixture.addDistinct(mapping, transformation, etlOperators);
        verify(etlOperators).addDistinctComponents(distinctComponent);
    }


    @Test
    public void testSetDistinctUnset() throws MappingException {
        boolean useExpressions = false;
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        when(mappings.isDistinct()).thenReturn(false);
        fixture.setDistinct(transformation, Arrays.asList(distinctComponent),
                useExpressions);
        verify(distinctComponent, times(0)).addAttribute(any(String.class),
                any(String.class), any(OdiDataType.class), any(Integer.class),
                any(Integer.class));
    }


    @Test
    public void testSetNotDistinct() throws MappingException {
        testSetDistinct(1, true, false);
    }

    @Test
    public void testSetSingleDatasetExpressions() throws MappingException {
        testSetDistinct(1, true, true);
    }

    @Test
    public void testSetSingleDatasetNoExpressions() throws MappingException {
        testSetDistinct(1, false, true);
    }


    @Test
    public void testSetMultipleDatasetExpressions() throws MappingException {
        testSetDistinct(2, true, true);
    }

    @Test
    public void testSetMultipleDatasetNoExpressions() throws MappingException {
        testSetDistinct(2, false, true);
    }


    private void testSetDistinct(int numberDatasets, boolean useExpressions, boolean isDistinct) throws AdapterException, MappingException {
        int n = numberDatasets + 1;
        List<String> aliases = IntStream.range(1, n).mapToObj(i -> "alias" + i).collect(Collectors.toList());
        List<String> names = IntStream.range(1, n).mapToObj(i -> "source" + i).collect(Collectors.toList());
        List<String> models = IntStream.range(1, n).mapToObj(i -> "model").collect(Collectors.toList());
        Transformation transformation = InputModelMockHelper
                .createMockETLTransformation(aliases.toArray(new String[]{}),
                        names.toArray(new String[]{}),
                        models.toArray(new String[]{}));
        when(transformation.getMaxDatasetNumber()).thenReturn(numberDatasets);
        when(transformation.useExpressions()).thenReturn(useExpressions);
        when(transformation.getMappings().isDistinct()).thenReturn(isDistinct);
        for (Dataset ds : transformation.getDatasets()) {
            when(ds.translateExpression(any(String.class))).thenAnswer(
                    (Answer<String>) invocation -> invocation.getArgumentAt(0, String.class));
        }
        OdiModel odiModel = mock(OdiModel.class);
        when(odiCommon.getOdiModel(transformation.getMappings().getModel())).thenReturn(odiModel);
        OdiLogicalSchema odiLogicalSchema = mock(OdiLogicalSchema.class);
        when(odiModel.getLogicalSchema()).thenReturn(odiLogicalSchema);
        OdiTechnology odiTechnology = mock(OdiTechnology.class);
        when(odiLogicalSchema.getTechnology()).thenReturn(odiTechnology);
        OdiDataType odiDataType = mock(OdiDataType.class);
        when(odiTechnology.getDataType(any(String.class))).thenReturn(odiDataType);

        fixture.setDistinct(transformation, Arrays.asList(distinctComponent),
                useExpressions);
        // verify each expression is added to aggregate.
        int i = 0;

        if (isDistinct) {
            for (Dataset ds : transformation.getDatasets()) {
                for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
                    String expression = "";
                    if (numberDatasets > 1)
                        expression = ComponentPrefixType.SETCOMPONENT.getAbbreviation() + "." + tc.getName();
                    else if (useExpressions)
                        expression = ComponentPrefixType.TARGET_EXPRESSIONS.getAbbreviation() + "." + tc.getName();
                    else
                        expression = tc.getMappingExpressions().get(i);
                    verify(distinctComponent).addAttribute(tc.getName(),
                            expression, odiDataType, 0, 0);
                }
                i++;
            }
        } else {
            verify(distinctComponent, times(0)).addAttribute(any(String.class), any(String.class), any(OdiDataType.class), any(Integer.class), any(Integer.class));
        }

    }

}
