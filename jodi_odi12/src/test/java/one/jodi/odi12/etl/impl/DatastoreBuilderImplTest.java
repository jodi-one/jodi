package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.FlowsBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.IModelObject;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.ReusableMapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.LookupComponent;
import oracle.odi.domain.mapping.component.ReusableMappingComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiTechnology;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DatastoreBuilderImplTest {

    final HashMap<String, MapRootContainer> mappingCache = new HashMap<>();
    @Mock
    LookupComponent lookupComponent;
    @Mock
    OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;
    @Mock
    JodiProperties properties;
    @Mock
    FlowsBuilder flowsBuilder;
    DatastoreBuilderImpl fixture;
    int packageSequence = 11;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        fixture = new DatastoreBuilderImpl(odiAccessStrategy, properties, mappingCache, flowsBuilder);
    }

    @Test
    public void testCreateFileComponent() throws AdapterException, MappingException {
        MapRootContainer mapping = createMockOdiMapping();
        OdiDataStore ds = createOdiDataStore("FILE");


        IMapComponent component = fixture.createComponent(mapping, ds, true);
        assert (component != null);
        verify(mapping).createComponent("FILE", ds, true);
    }

    @Test
    public void testCreateDatastoreComponent() throws AdapterException, MappingException {
        MapRootContainer mapping = createMockOdiMapping();
        OdiDataStore ds = createOdiDataStore("DATASTORE");


        IMapComponent component = fixture.createComponent(mapping, ds, true);
        assert (component != null);
        verify(mapping).createComponent("DATASTORE", ds, true);
    }

    @Test
    public void testCreateReusableMapping() throws AdapterException, MappingException {
        MapRootContainer mapping = createMockOdiMapping();
        OdiDataStore ds = createOdiDataStore("REUSABLEMAPPING");


        IMapComponent component = fixture.createComponent(mapping, ds.getModel(), true);
        assert (component != null);
        verify(mapping).createComponent("REUSABLEMAPPING", ds.getModel(), true);
    }

    private OdiDataStore createOdiDataStore(String type) {
        OdiDataStore boundObject = mock(OdiDataStore.class);
        OdiModel model = mock(OdiModel.class);
        when(boundObject.getModel()).thenReturn(model);
        OdiTechnology technology = mock(OdiTechnology.class);
        when(model.getTechnology()).thenReturn(technology);
        when(technology.getName()).thenReturn(type);
        return boundObject;
    }

    @SuppressWarnings("unused")
    private MapRootContainer createMockOdiMapping() throws AdapterException, MappingException {

        MapRootContainer mapping = mock(MapRootContainer.class);

        when(mapping.createComponent(any(String.class), any(IModelObject.class),
                any(Boolean.class))).
                thenAnswer((Answer<IMapComponent>) invocation -> {
                    Object[] arguments = invocation.getArguments();
                    String type = (String) arguments[0];
                    //IModelObject model = (IModelObject) arguments[1];
                    //boolean auto = (boolean) arguments[2];
                    IMapComponent component = null;
                    if (type == "REUSABLEMAPPING")
                        component = mock(ReusableMappingComponent.class);
                    else
                        component = mock(IMapComponent.class);

                    return component;
                });


        return mapping;
    }


    @Test
    public void testAddTemporaryDatasource() throws AdapterException, MappingException, ResourceNotFoundException, ResourceFoundAmbiguouslyException, TransformationAccessStrategyException {
        boolean journalized = false;
        MapRootContainer mapping = createMockOdiMapping();
        Source source = InputModelMockHelper.createMockETLSource("ALIAS", "NAME", "MODEL");
        when(source.isTemporary()).thenReturn(false);
        EtlOperators etlOperators = mock(EtlOperators.class);
        OdiDataStore ds = createOdiDataStore("DATASTORE");
        when(odiAccessStrategy.findDataStore(source.getName(), source.getModel())).thenReturn(ds);
        fixture.addDatasource(mapping, source, packageSequence, false, etlOperators);

        verify(flowsBuilder).addFlow(mapping, source, etlOperators, journalized);
        verify(mapping).createComponent("DATASTORE", ds, false);

    }

    @Test
    public void testAddDatasource() throws AdapterException, MappingException, ResourceNotFoundException, ResourceFoundAmbiguouslyException, TransformationAccessStrategyException {
        MapRootContainer mapping = createMockOdiMapping();
        String folder = "FOLDER";
        Transformation transformation = InputModelMockHelper.createMockETLTransformation("", new String[]{"ALIAS"}, new String[]{"NAME"}, new String[]{"MODEL"});
        when(transformation.getFolderName()).thenReturn(folder);
        Source source = InputModelMockHelper.createMockETLSource("ALIAS", "NAME", "MODEL");
        source = transformation.getDatasets().get(0).getSources().get(0);
        when(source.isTemporary()).thenReturn(true);
        EtlOperators etlOperators = mock(EtlOperators.class);
        ReusableMapping rm = mock(ReusableMapping.class);
        when(properties.getProjectCode()).thenReturn("PROJECT_CODE");
        when(odiAccessStrategy.findMappingsByName(source.getName(), folder, properties.getProjectCode())).thenReturn(rm);
        fixture.addDatasource(mapping, source, packageSequence, false, etlOperators);

        verify(mapping).createComponent("REUSABLEMAPPING", rm, false);
    }

}
