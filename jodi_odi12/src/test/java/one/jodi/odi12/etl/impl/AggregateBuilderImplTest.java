package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.AggregateComponent;
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class AggregateBuilderImplTest {

    @Mock
    OdiCommon odiCommon;
    @Mock
    MapRootContainer mapping;
    @Mock
    EtlOperators etlOperators;
    @Mock
    AggregateComponent aggregateComponent;
    AggregateBuilderImpl fixture;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        fixture = new AggregateBuilderImpl(odiCommon) {
            protected AggregateComponent createAggregateComponent(MapRootContainer mapping, String name) throws MappingException {
                when(aggregateComponent.getName()).thenReturn(name);
                return aggregateComponent;
            }
        };
    }

    @Test
    public void testCreate_noAggregates() throws AdapterException, MappingException {
        int dsn = 1;
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        fixture.addAggregate(mapping, transformation, etlOperators, dsn);

        verify(etlOperators, times(0)).addAggregate(any(AggregateComponent.class));

    }


    @Test
    public void testCreate() throws AdapterException, MappingException {
        int dsn = 1;
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        when(transformation.getMappings().isAggregateTransformation(dsn)).thenReturn(true);

        fixture.addAggregate(mapping, transformation, etlOperators, dsn);

        verify(etlOperators, times(1)).addAggregate(aggregateComponent);

    }


    @Test
    public void testSetAggregateSingleDataset() throws AdapterException, MappingException {
        testSetAggregate(1, false);
    }

    @Test
    public void testSetAggregateTwoDatasets() throws AdapterException, MappingException {
        testSetAggregate(2, false);
    }

    @Test
    public void testSetAggregateSingleDatasetWithAggregate() throws AdapterException, MappingException {
        testSetAggregate(1, true);
    }

    @Test
    public void testSetAggregateTwoDatasetsWithAggregate() throws AdapterException, MappingException {
        testSetAggregate(2, true);
    }


    private void testSetAggregate(int dsn, boolean isAggregateColumn) throws AdapterException, MappingException {

        Transformation transformation = InputModelMockHelper.createMockETLTransformation();

        String[] aliases = new String[dsn];
        String[] names = new String[dsn];
        String[] models = new String[dsn];
        for (int i = 0; i < dsn; i++) {
            aliases[i] = "ALIAS" + (i + 1);
            names[i] = "SOURCE" + (i + 1);
            models[i] = "MODEL";
        }
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, aliases, names, models);

        for (Dataset ds : datasets) {
            when(ds.translateExpression(any(String.class))).thenAnswer(
                    (Answer<String>) invocation -> {
                        String expression = (String) invocation.getArguments()[0];
                        return expression;
                    });
        }
        when(transformation.getDatasets()).thenReturn(datasets);
        when(transformation.getMappings().isAggregateTransformation(dsn)).thenReturn(true);

        fixture.addAggregate(mapping, transformation, etlOperators, dsn);
        List<AggregateComponent> aggregateComponents = Arrays.asList(aggregateComponent);

        for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
            when(tc.isAggregateColumn(dsn)).thenReturn(true);
            when(tc.getMappingExpressions()).thenReturn(Arrays.asList(names));
            when(tc.getDataType()).thenReturn("VARCHAR");
            when(tc.isAggregateColumn(dsn)).thenReturn(isAggregateColumn);
        }
        //odiCommon.getOdiModel(transformation.getMappings().getModel()).getLogicalSchema().getTechnology().getDataType(tc.getDataType())
        OdiModel odiModel = mock(OdiModel.class);
        when(odiCommon.getOdiModel(transformation.getMappings().getModel())).thenReturn(odiModel);
        OdiLogicalSchema odiLogicalSchema = mock(OdiLogicalSchema.class);
        when(odiModel.getLogicalSchema()).thenReturn(odiLogicalSchema);
        OdiTechnology odiTechnology = mock(OdiTechnology.class);
        when(odiLogicalSchema.getTechnology()).thenReturn(odiTechnology);
        OdiDataType odiDataType = mock(OdiDataType.class);
        when(odiTechnology.getDataType("VARCHAR")).thenReturn(odiDataType);

        MapAttribute mapAttribute = mock(MapAttribute.class);
        when(aggregateComponent.addAttribute(any(String.class), any(String.class), any(OdiDataType.class), any(Integer.class), any(Integer.class))).thenReturn(mapAttribute);
        fixture.setAggregate(transformation, aggregateComponents, dsn);
        verify(aggregateComponent).addAttribute("colname", names[dsn - 1], odiDataType, 0, 0);
        verify(aggregateComponent).setIsGroupByColumn(mapAttribute, !isAggregateColumn ? AggregateComponent.IsGroupByColumn.YES : AggregateComponent.IsGroupByColumn.NO);


    }
}
