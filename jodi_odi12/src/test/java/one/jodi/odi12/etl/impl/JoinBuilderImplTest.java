package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.JoinTypeEnum;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.*;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.*;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.topology.OdiContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;

public class JoinBuilderImplTest {
    final HashMap<String, MapRootContainer> mappingCache = new HashMap<>();
    @Mock
    JodiProperties properties;
    @Mock
    LookupComponent lookupComponent;
    @Mock
    OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;
    @Mock
    FlowsBuilder flowsBuilder;
    @Mock
    FilterBuilder filterBuilder;
    @Mock
    LookupBuilder lookupBuilder;
    @Mock
    DatastoreBuilder datastoreBuilder;
    @Mock
    JoinComponent joinComponent;
    @Mock
    MapRootContainer mapping;
    @Mock
    EtlOperators etlOperators;
    @Mock
    IMapComponent firstComponent;
    @Mock
    IMapComponent secondComponent;

    JoinBuilderImpl fixture;
    int packageSequence = 11;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        fixture = new JoinBuilderImpl(odiAccessStrategy, properties, mappingCache, filterBuilder, datastoreBuilder, lookupBuilder, flowsBuilder) {
            @Override
            protected JoinComponent createJoinComponent(MapRootContainer mapping, int i, int j, String s) {
                when(joinComponent.getName()).thenReturn(ComponentPrefixType.DATASET.getAbbreviation()
                        + i + "_" + ComponentPrefixType.JOIN.getAbbreviation() + j);
                return joinComponent;
            }
        };
    }
	
	/*
	 * public void addDatasets(final MapRootContainer mapping, final Transformation transformation,
			final boolean journalized, final List<FilterComponent> filterComponentsss, final EtlOperators etlOperators,
			final boolean useExpressions) throws AdapterException, MapPhysicalException, MappingException,
					TransformationAccessStrategyException, TransformationException, ResourceNotFoundException,
					ResourceFoundAmbiguouslyException {
		
		if (transformation.getComments() != null) {
			mapping.setDescription(transformation.getComments());
		}
		List<Dataset> dataSets = odiAccessStrategy.findDatasets(mapping);
		logger.debug(transformation.getPackageSequence() + " DataSetSize:" + dataSets.size());
		
		for (int currentDatasetIdx = 0; currentDatasetIdx < transformation.getDatasets().size(); currentDatasetIdx++) {
			one.jodi.etl.internalmodel.Dataset ds = transformation.getDatasets().get(currentDatasetIdx);
			datastoreBuilder.addDatasource(mapping, ds.getDriverSourceInDataset(), transformation.getPackageSequence(), journalized, etlOperators);
			
			// add joined tables and joins
			for (Source joinedTable : ds.findJoinedSourcesInDataset()) {
				String model = joinedTable.getModel();
				// jkm
				if (joinedTable.isTemporary()) {
					ReusableMapping joinedreusableMapping = (ReusableMapping) odiAccessStrategy
							.findMappingsByName(properties.getProjectCode(), joinedTable.getName(), mappingCache);
					String alias = joinedTable.getComponentName();
					ReusableMappingComponent sourceComponent = (ReusableMappingComponent) datastoreBuilder.createComponent(mapping,
							joinedreusableMapping, false);
					sourceComponent.setName(alias);
					logger.debug("joined temp: " + joinedTable.getName() + ": in model" + model);
				} else {
					OdiDataStore boundTo = odiAccessStrategy.findDataStore(joinedTable.getName(),
							joinedTable.getModel());
					assert(boundTo != null);
					logger.debug("Creating joined datastore " + joinedTable.getAlias());
					DatastoreComponent dsc = (DatastoreComponent) datastoreBuilder.createComponent(mapping, boundTo, false);
					dsc.setName(joinedTable.getComponentName());
					if (transformation.isTemporary()) {
						((ReusableMapping) mapping).addComponent(dsc);
					} else {
						((Mapping) mapping).addComponent(dsc);
					}
				}
								
				flowBuilder.addFlow(mapping, joinedTable, etlOperators, journalized);
			}
			
			addJoins(mapping, transformation, journalized, ds, currentDatasetIdx, etlOperators.getFilterComponents(currentDatasetIdx+1),
					etlOperators);
			lookupBuilder.addLookups(mapping, ds.getSources(), journalized, etlOperators.getJoiners(currentDatasetIdx+1), etlOperators);
		}
	}
	 */

    @Test
    public void testWithFilter() throws Exception {
        test(JoinTypeEnum.INNER, new String[]{"DATASTORE", "DATASTORE"}, "FILTER", new Boolean[]{false, false});
    }

    @Test
    public void testWithoutFilter() throws Exception {
        test(JoinTypeEnum.INNER, new String[]{"DATASTORE", "DATASTORE"}, "", new Boolean[]{false, true});
    }

    @Test
    public void testLEFTOUTER() throws Exception {
        test(JoinTypeEnum.LEFT_OUTER, new String[]{"DATASTORE", "DATASTORE"}, "", new Boolean[]{false, true});
    }

    @Test
    public void testFULL() throws Exception {
        test(JoinTypeEnum.FULL, new String[]{"DATASTORE", "DATASTORE"}, "", new Boolean[]{false, true});
    }

    @Test
    public void testCROSS() throws Exception {
        test(JoinTypeEnum.CROSS, new String[]{"DATASTORE", "DATASTORE"}, "", new Boolean[]{false, true});
    }

    @Test
    public void testNATURAL() throws Exception {
        test(JoinTypeEnum.NATURAL, new String[]{"DATASTORE", "DATASTORE"}, "", new Boolean[]{false, true});
    }


    private void test(JoinTypeEnum joinType, String[] componentTypeNames, String filter, Boolean[] hasFlows) throws Exception {

        String joinText = "JOINTEXT";
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        when(transformation.getComments()).thenReturn("COMMENTS");
        FilterComponent filterComponent = mock(FilterComponent.class);

        one.jodi.etl.internalmodel.Dataset dataset = transformation.getDatasets().get(0);
        when(transformation.getDatasets()).thenReturn(Arrays.asList(dataset));

        List<Source> sources = new ArrayList<>();
        sources.add(InputModelMockHelper.createMockETLSource("SOURCE1", "SOURCE2", "MODEL"));
        sources.add(InputModelMockHelper.createMockETLSource("SOURCE1", "SOURCE2", "MODEL"));
        for (Source s : sources) {
            when(s.getParent()).thenReturn(dataset);
            when(s.getJoinType()).thenReturn(joinType);
            when(s.getFilter()).thenReturn(filter);
            OdiDataStore datastore = mock(OdiDataStore.class);
            when(odiAccessStrategy.findDataStore(s.getName(), s.getModel())).thenReturn(datastore);
            if (s.isTemporary()) {
                ReusableMappingComponent rm = mock(ReusableMappingComponent.class);
                when(datastoreBuilder.createComponent(mapping, datastore, false)).thenReturn(rm);
            } else {
                DatastoreComponent dsc = mock(DatastoreComponent.class);
                when(datastoreBuilder.createComponent(mapping, datastore, false)).thenReturn(dsc);
            }
        }

        when(firstComponent.getComponentTypeName()).thenReturn(componentTypeNames[0]);
        when(secondComponent.getComponentTypeName()).thenReturn(componentTypeNames[1]);

        if (filter.length() > 1) {
            when(etlOperators.getFilterComponentsByKey(FilterBuilder.getFilterKeyForSource(sources.get(0), false))).thenReturn(filterComponent);
            when(filterComponent.getComponentTypeName()).thenReturn(componentTypeNames[1]);
            secondComponent = filterComponent;
        }
        when(dataset.getSources()).thenReturn(sources);

        for (int i = 0; i < hasFlows.length; i++) {
            if (hasFlows[i]) {
                PivotComponent pivotComponent = mock(PivotComponent.class);

                when(etlOperators.getFlows(sources.get(i))).thenReturn(Arrays.asList(pivotComponent));
            }
        }
        when(joinComponent.getJoinConditionText()).thenReturn(joinText);

        when(odiAccessStrategy.findSourceComponent(mapping, transformation, sources.get(0))).thenReturn(firstComponent);
        when(odiAccessStrategy.findSourceComponent(mapping, transformation, sources.get(1))).thenReturn(secondComponent);


        fixture.addDatasets(mapping, transformation, false, Arrays.asList(filterComponent), etlOperators, true);

        if (filter.length() > 1)
            verify(joinComponent).addJoinSource(filterComponent, "1=1");
        else if (joinType == JoinTypeEnum.CROSS)
            verify(joinComponent).addJoinSource(firstComponent, null);
        else
            verify(joinComponent).addJoinSource(firstComponent, "1=1");

        verify(mapping).setDescription(transformation.getComments());

    }

}
