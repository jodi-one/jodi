package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.*;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.DatastoreBuilder;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.*;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiDataType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class FlowsBuilderImplTest {
    @Mock
    OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;

    @Mock
    PivotComponent pivotComponent;
    @Mock
    UnpivotComponent unpivotComponent;
    @Mock
    IMapComponent sourceComponent;
    @Mock
    MapRootContainer mapping;
    @Mock
    EtlOperators etlOperators;
    @Mock
    OdiDataType odiType;
    @Mock
    DatastoreBuilder datastoreBuilder;
    @Mock
    JodiProperties properties;

    FlowsBuilderImpl fixture;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(odiType.getName()).thenReturn("VARCHAR");

        fixture = new FlowsBuilderImpl(datastoreBuilder, odiAccessStrategy, properties) {
            @Override
            protected PivotComponent createPivotComponent(
                    final MapRootContainer mapping, String key)
                    throws MappingException {
                return pivotComponent;
            }

            @Override
            protected UnpivotComponent createUnpivotComponent(
                    final MapRootContainer mapping, String key)
                    throws MappingException {
                return unpivotComponent;
            }
        };
    }

    @Test
    public void testAddPivotWithFilter() throws Exception {
        testAddPivot("filter");
    }

    @Test
    public void testAddPivotWithoutFilter() throws Exception {
        testAddPivot("");
    }

    private void testAddPivot(String filter) throws Exception {
        String pivotName = "PIVOT";
        String rowLocator = "name.COL1";
        String aggregateFunction = "MIN";
        String[] attributeNames = {"ATTR1", "ATTR2"};
        String[] attributeValues = {"V1", "V2"};
        String[] attributeExpressions = {"EXPR1", "EXPR2"};

        Pivot pivot = InputModelMockHelper.createMockPivot(pivotName, rowLocator, aggregateFunction, attributeNames, attributeValues, attributeExpressions);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Source source = transformation.getDatasets().get(0).getSources().get(0);
        when(source.getFilter()).thenReturn(filter);
        when(source.getFlows()).thenReturn(Arrays.asList(pivot));
        when(pivot.getParent()).thenReturn(source);
        when(odiAccessStrategy.findSourceComponent(mapping, transformation, source)).thenReturn(sourceComponent);
        when(odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(), source)).thenReturn(sourceComponent);


        when(source.getParent().translateExpression(any(String.class))).thenAnswer(
                (Answer<String>) invocation -> {
                    String expression = (String) invocation.getArguments()[0];
                    return expression;
                });

        addAttributes(sourceComponent, new String[]{"COL1"});

        ArrayList<OutputAttribute> outputAttributes = new ArrayList<>();
        for (String attributeName : attributeNames) {
            OutputAttribute oa = mock(OutputAttribute.class);
            when(oa.getName()).thenReturn(attributeName);
            HashMap<String, String> map = new HashMap<>();
            for (int j = 0; j < attributeValues.length; j++) {
                map.put(attributeValues[j], attributeExpressions[j]);
            }
            when(oa.getExpressions()).thenReturn(map);
            outputAttributes.add(oa);

        }

        for (int i = 0; i < attributeNames.length; i++) {
            MapAttribute mapAttribute = mock(MapAttribute.class);
            when(pivotComponent.addAttribute(attributeNames[i], attributeExpressions[i], odiType, null, null)).thenReturn(mapAttribute);
        }

        fixture.addFlow(mapping, source, etlOperators, false);

        if (filter.length() < 2) {
            verify(sourceComponent).connectTo(pivotComponent);
        }

        verify(pivotComponent).setName(pivotName);
        verify(pivotComponent).setAggregateFunction(aggregateFunction);
        for (int i = 0; i < attributeNames.length; i++) {
            verify(pivotComponent).addAttribute(attributeNames[i], attributeExpressions[i], odiType, null, null);
        }
        verify(etlOperators).addFlowItem(source, pivotComponent);

    }

    private List<MapExpression> addAttributes(IMapComponent component, String[] attrs) throws AdapterException, MappingException {

        ArrayList<MapExpression> list = new ArrayList<>();
        when(component.getAttributeExpressions()).thenReturn(list);
        for (String attr : attrs) {
            MapExpression expression = mock(MapExpression.class);
            list.add(expression);
            MapAttribute mapAttribute = mock(MapAttribute.class);
            when(mapAttribute.getDataType()).thenReturn(odiType);
            when(expression.getOwningAttribute()).thenReturn(mapAttribute);
            when(mapAttribute.getName()).thenReturn(attr);
        }
        return list;

    }


    @Test
    public void testAddUnpivot() throws Exception {
        testAddUnpivot("");
    }

    private void testAddUnpivot(String filter) throws Exception {
        String pivotName = "PIVOT";
        String rowLocator = "C1";
        String[] attributeNames = {"ATTR1", "ATTR2"};
        String[] attributeValues = {"V1", "V2"};
        String[] attributeExpressions = {"EXPR1", "EXPR2"};

        UnPivot unpivot = InputModelMockHelper.createMockUnPivot(pivotName, rowLocator, attributeNames, attributeValues, attributeExpressions);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Source source = transformation.getDatasets().get(0).getSources().get(0);
        when(source.getFilter()).thenReturn(filter);
        when(source.getFlows()).thenReturn(Arrays.asList(unpivot));
        when(unpivot.getParent()).thenReturn(source);
        when(odiAccessStrategy.findSourceComponent(mapping, transformation, source)).thenReturn(sourceComponent);
        when(odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(), source)).thenReturn(sourceComponent);
        when(transformation.getDatasets().get(0).translateExpression(any(String.class))).thenAnswer(
                (Answer<String>) invocation -> invocation.getArgumentAt(0, String.class));

        addAttributes(sourceComponent, new String[]{"C1", "C2"});


        for (int i = 0; i < attributeNames.length; i++) {
            MapAttribute mapAttribute = mock(MapAttribute.class);

            when(unpivotComponent.addAttribute(attributeNames[i], attributeExpressions[i], odiType, null, null)).thenReturn(mapAttribute);
        }

        fixture.addFlow(mapping, source, etlOperators, false);

        if (filter.length() < 2) {
            verify(sourceComponent).connectTo(unpivotComponent);
        }

        verify(unpivotComponent).setName(pivotName);
        for (int i = 0; i < attributeNames.length; i++) {
            verify(unpivotComponent).addAttribute(attributeNames[i], attributeExpressions[i], odiType, null, null);
        }
        verify(etlOperators).addFlowItem(source, unpivotComponent);
    }

    @Test
    public void testSetFlows() throws Exception {
        String pivotName = "PIVOT";
        String rowLocator = "name.COL1";
        String aggregateFunction = "MIN";
        String[] attributeNames = {"ATTR1", "ATTR2"};
        String[] attributeValues = {"V1", "V2"};
        String[] attributeExpressions = {"EXPR1", "EXPR2"};
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Source source = transformation.getDatasets().get(0).getSources().get(0);
        when(source.getComponentName()).thenReturn("name");
        Pivot pivot = InputModelMockHelper.createMockPivot(pivotName, rowLocator, aggregateFunction, attributeNames, attributeValues, attributeExpressions);
        when(source.getFlows()).thenReturn(Arrays.asList(pivot));
        when(pivot.getParent()).thenReturn(source);
        when(odiAccessStrategy.findSourceComponent(mapping, transformation, source)).thenReturn(sourceComponent);
        when(odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(), source)).thenReturn(sourceComponent);

        PivotComponent pivotComponent = mock(PivotComponent.class);
        when(pivotComponent.getName()).thenReturn(ComponentPrefixType.PIVOT.getAbbreviation() + "_name");
        HashMap<Source, List<IMapComponent>> map = new HashMap<>();
        map.put(source, Arrays.asList(pivotComponent));
        fixture.setFlows(transformation, map, true);
    }

}
