package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.impl.PivotImpl;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.DatastoreBuilder;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.FilterBuilder;
import one.jodi.odi12.etl.LookupBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.*;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.topology.OdiContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;

public class LookupBuilderImplTest {

    final Boolean devMode = false;
    final HashMap<String, MapRootContainer> mappingCache = new HashMap<>();
    @Mock
    LookupComponent lookupComponent;
    @Mock
    OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;
    @Mock
    JodiProperties properties;
    @Mock
    FilterBuilder filterBuilder;
    @Mock
    DatastoreBuilder datastoreBuilder;
    LookupBuilder fixture;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        fixture = new LookupBuilderImpl(odiAccessStrategy, mappingCache, properties, datastoreBuilder) {
            protected LookupComponent createLookupComponent(final MapRootContainer mapping, String name) throws MappingException {
                when(lookupComponent.getName()).thenReturn(name);
                when(lookupComponent.getMapRootContainer()).thenReturn(mapping);
                when(lookupComponent.getJoinConditionText()).thenReturn("1=1 AND");
                return lookupComponent;
            }
        };
    }


    @Test
    public void testWithFilter() throws AdapterException, ResourceNotFoundException, ResourceFoundAmbiguouslyException, MappingException, TransformationAccessStrategyException {
        MapRootContainer mapping = mock(MapRootContainer.class);
        EtlOperators etlOperators = mock(EtlOperators.class);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        JoinComponent joinComponent = mock(JoinComponent.class);
        List<JoinComponent> joiners = Arrays.asList(joinComponent);
        when(odiAccessStrategy.getDataSetNumberFromComponentName(joinComponent)).thenReturn(0);
        List<Lookup> lookups = InputModelMockHelper.createMockETLLookups("A.column = B.column");
        List<one.jodi.etl.internalmodel.Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"SOURCE1"}, new String[]{"SOURCE1"}, new String[]{"MODEL"});

        when(datasets.get(0).getSources().get(0).getLookups()).thenReturn(lookups);
        when(lookups.get(0).getAlias()).thenReturn("LookupAlias");
        Source source = datasets.get(0).getSources().get(0);
        when(lookups.get(0).getParent()).thenReturn(source);

        when(transformation.getDatasets()).thenReturn(datasets);

        FilterComponent filterComponent = mock(FilterComponent.class);
        when(filterComponent.getName()).thenReturn("FilterComponent");
        when(odiAccessStrategy.getComponentByName(mapping,
                ComponentPrefixType.FILTER.getAbbreviation() + "_" + datasets.get(0).getSources().get(0).getComponentName())).thenReturn(filterComponent);


        OdiDataStore lookupDataStore = mock(OdiDataStore.class);
        when(odiAccessStrategy.findDataStore(lookups.get(0).getLookupDataStore(), lookups.get(0).getModel())).thenReturn(lookupDataStore);

        IMapComponent second = mock(IMapComponent.class);
        when(datastoreBuilder.createComponent(mapping, lookupDataStore, false)).thenReturn(second);

        fixture.addLookups(mapping, datasets.get(0).getSources(), true, joiners, etlOperators);


    }

    @Test
    public void testWithoutFilter() throws AdapterException, ResourceNotFoundException, ResourceFoundAmbiguouslyException, MappingException, TransformationAccessStrategyException {
        MapRootContainer mapping = mock(MapRootContainer.class);
        EtlOperators etlOperators = mock(EtlOperators.class);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        JoinComponent joinComponent = mock(JoinComponent.class);
        List<JoinComponent> joiners = Arrays.asList(joinComponent);
        when(odiAccessStrategy.getDataSetNumberFromComponentName(joinComponent)).thenReturn(0);
        List<Lookup> lookups = InputModelMockHelper.createMockETLLookups("A.column = B.column");
        List<one.jodi.etl.internalmodel.Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"SOURCE1"}, new String[]{"SOURCE1"}, new String[]{"MODEL"});

        when(datasets.get(0).getSources().get(0).getLookups()).thenReturn(lookups);
        when(lookups.get(0).getAlias()).thenReturn("LookupAlias");
        Source source = datasets.get(0).getSources().get(0);
        when(source.getFilter()).thenReturn(null);
        DatastoreComponent sourceComponent = mock(DatastoreComponent.class);
        when(sourceComponent.getName()).thenReturn("SOURCE_DATASTORE");
        when(odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(),
                source)).thenReturn(sourceComponent);
        when(lookups.get(0).getParent()).thenReturn(source);

        when(transformation.getDatasets()).thenReturn(datasets);


        OdiDataStore lookupDataStore = mock(OdiDataStore.class);
        when(odiAccessStrategy.findDataStore(lookups.get(0).getLookupDataStore(), lookups.get(0).getModel())).thenReturn(lookupDataStore);


        IMapComponent second = mock(IMapComponent.class);
        when(datastoreBuilder.createComponent(mapping, lookupDataStore, false)).thenReturn(second);

        fixture.addLookups(mapping, datasets.get(0).getSources(), true, joiners, etlOperators);
        verify(etlOperators, times(1)).addLookup(lookupComponent);
        verify(odiAccessStrategy).findDataStore(lookups.get(0).getLookupDataStore(), lookups.get(0).getModel());


    }

    @Test
    public void testWithFlows() throws AdapterException, ResourceNotFoundException, ResourceFoundAmbiguouslyException, MappingException, TransformationAccessStrategyException {
        MapRootContainer mapping = mock(MapRootContainer.class);
        EtlOperators etlOperators = mock(EtlOperators.class);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        JoinComponent joinComponent = mock(JoinComponent.class);
        List<JoinComponent> joiners = Arrays.asList(joinComponent);
        when(odiAccessStrategy.getDataSetNumberFromComponentName(joinComponent)).thenReturn(0);
        List<Lookup> lookups = InputModelMockHelper.createMockETLLookups("A.column = B.column");
        List<one.jodi.etl.internalmodel.Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"SOURCE1"}, new String[]{"SOURCE1"}, new String[]{"MODEL"});

        when(datasets.get(0).getSources().get(0).getLookups()).thenReturn(lookups);
        when(lookups.get(0).getAlias()).thenReturn("LookupAlias");

        Source source = datasets.get(0).getSources().get(0);
        //firstComponent = etlOperators.getFlows(source).get(etlOperators.getFlows(source).size()-1);
        List<Flow> flows = Arrays.asList(new PivotImpl());
        PivotComponent pivotComponent = mock(PivotComponent.class);
        when(etlOperators.getFlows(source)).thenReturn(Arrays.asList(pivotComponent));
        when(source.getFlows()).thenReturn(flows);
        when(source.getFilter()).thenReturn(null);
        DatastoreComponent sourceComponent = mock(DatastoreComponent.class);
        when(sourceComponent.getName()).thenReturn("SOURCE_DATASTORE");
        when(odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(),
                source)).thenReturn(sourceComponent);
        when(lookups.get(0).getParent()).thenReturn(source);

        when(transformation.getDatasets()).thenReturn(datasets);


        OdiDataStore lookupDataStore = mock(OdiDataStore.class);
        when(odiAccessStrategy.findDataStore(lookups.get(0).getLookupDataStore(), lookups.get(0).getModel())).thenReturn(lookupDataStore);


        IMapComponent second = mock(IMapComponent.class);
        when(datastoreBuilder.createComponent(mapping, lookupDataStore, false)).thenReturn(second);

        fixture.addLookups(mapping, datasets.get(0).getSources(), true, joiners, etlOperators);
        verify(etlOperators, times(1)).addLookup(lookupComponent);


    }


}
