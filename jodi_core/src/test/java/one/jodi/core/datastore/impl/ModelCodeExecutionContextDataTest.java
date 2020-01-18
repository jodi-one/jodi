package one.jodi.core.datastore.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.extensions.contexts.ModelNameExecutionContext;
import one.jodi.core.extensions.contexts.ModelNameExecutionContext.DataStoreRole;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.impl.*;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ModelCodeExecutionContextDataTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    JodiProperties properties;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    ModelCodeDefaultStrategy defaultStrategy;
    @Mock
    ModelCodeIDStrategy idStrategy;
    ModelCodeContextImpl fixture;
    @Mock
    ETLValidator validator;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new ModelCodeContextImpl(properties, databaseMetadataService, defaultStrategy, idStrategy, validator, errorWarningMessages);
    }

    @Test
    public void testGetModelCode_Mappings() throws Exception {
        final TransformationImpl transformation = new TransformationImpl();
        final MappingsImpl mappings = new MappingsImpl();
        transformation.setMappings(mappings);
        mappings.setParent(transformation);
        mappings.setTargetDataStore("targetDataStore");
        final TransformationExtension transformationExtension = new TransformationExtension();
        transformation.setExtension(transformationExtension);
        final MappingsExtension mappingsExtension = new MappingsExtension();
        mappings.setExtension(mappingsExtension);
        final DataStore dataStore = mock(DataStore.class);
        final ModelProperties modelProperties = mock(ModelProperties.class);
        @SuppressWarnings("unchecked") final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);

        when(databaseMetadataService.getCoreProperties()).thenReturn(coreProperties);
        when(databaseMetadataService.getConfiguredModels()).thenReturn(Collections.<ModelProperties>singletonList(modelProperties));
        when(databaseMetadataService.findDataStoreInAllModels(mappings.getTargetDataStore())).thenReturn(Collections.<DataStore>singletonList(dataStore));
        when(defaultStrategy.getModelCode(anyString(), any(ModelNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ModelNameExecutionContext context = (ModelNameExecutionContext) args[1];
                assertNotNull(context);
                assertNotNull(context.getConfiguredModels());
                assertNotNull(context.getDataStoreRole());
                assertNotNull(context.getMatchingDataStores());
                assertNotNull(context.getProperties());

                assertThat(context.getConfiguredModels(), IsIterableContainingInAnyOrder.containsInAnyOrder(modelProperties));
                assertEquals(mappings.getTargetDataStore(), context.getDataStoreAlias());
                assertEquals(mappings.getTargetDataStore(), context.getDataStoreName());
                assertEquals(DataStoreRole.TARGET, context.getDataStoreRole());
                assertThat(context.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                assertThat(context.getMappingsExtension(), SamePropertyValuesAs.<MappingsExtension>samePropertyValuesAs(mappingsExtension));
                assertEquals(coreProperties, context.getProperties());
                return "default_modelname";
            }
        });

        when(idStrategy.getModelCode(anyString(), any(ModelNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ModelNameExecutionContext context = (ModelNameExecutionContext) args[1];
                assertNotNull(context);
                assertNotNull(context.getConfiguredModels());
                assertNotNull(context.getDataStoreAlias());
                assertNotNull(context.getDataStoreName());
                assertNotNull(context.getDataStoreRole());
                assertNotNull(context.getMappingsExtension());
                assertNotNull(context.getMatchingDataStores());
                assertNotNull(context.getProperties());
                assertNotNull(context.getTransformationExtension());

                assertThat(context.getConfiguredModels(), IsIterableContainingInAnyOrder.containsInAnyOrder(modelProperties));
                assertEquals(mappings.getTargetDataStore(), context.getDataStoreAlias());
                assertEquals(mappings.getTargetDataStore(), context.getDataStoreName());
                assertEquals(DataStoreRole.TARGET, context.getDataStoreRole());
                assertThat(context.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                assertThat(context.getMappingsExtension(), SamePropertyValuesAs.<MappingsExtension>samePropertyValuesAs(mappingsExtension));
                assertEquals(coreProperties, context.getProperties());
                return "id_modelname";
            }
        });

        String result = fixture.getModelCode(mappings);

        assertNotNull(result);
        assertEquals("id_modelname", result);
        verify(databaseMetadataService, times(4)).getCoreProperties();
        verify(databaseMetadataService, times(4)).getConfiguredModels();
        verify(defaultStrategy).getModelCode(anyString(), any(ModelNameExecutionContext.class));
        verify(idStrategy).getModelCode(anyString(), any(ModelNameExecutionContext.class));
    }

    @Test
    public void testGetModelCode_Source() throws Exception {
        final TransformationImpl transformation = new TransformationImpl();
        final DatasetImpl dataset = new DatasetImpl();
        transformation.addDataset(dataset);
        dataset.setParent(transformation);
        final SourceImpl source = new SourceImpl();
        dataset.addSource(source);
        source.setParent(dataset);
        source.setAlias("source_alias");
        source.setName("source_name");
        final SourceExtension sourceExtension = new SourceExtension();
        source.setExtension(sourceExtension);
        final TransformationExtension transformationExtension = new TransformationExtension();
        transformation.setExtension(transformationExtension);

        final ModelProperties modelProperties = mock(ModelProperties.class);
        @SuppressWarnings("unchecked") final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);

        when(databaseMetadataService.getCoreProperties()).thenReturn(coreProperties);
        when(databaseMetadataService.getConfiguredModels()).thenReturn(Collections.<ModelProperties>singletonList(modelProperties));
        when(defaultStrategy.getModelCode(anyString(), any(ModelNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ModelNameExecutionContext context = (ModelNameExecutionContext) args[1];
                assertNotNull(context);
                assertNotNull(context.getConfiguredModels());
                assertNotNull(context.getDataStoreAlias());
                assertNotNull(context.getDataStoreName());
                assertNotNull(context.getDataStoreRole());
                assertNotNull(context.getSourceExtension());
                assertNotNull(context.getMatchingDataStores());
                assertNotNull(context.getProperties());
                assertNotNull(context.getTransformationExtension());

                assertThat(context.getConfiguredModels(), IsIterableContainingInAnyOrder.containsInAnyOrder(modelProperties));
                assertEquals("source_alias", context.getDataStoreAlias());
                assertEquals("source_name", context.getDataStoreName());
                assertEquals(DataStoreRole.SOURCE, context.getDataStoreRole());
                assertThat(context.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                assertThat(context.getSourceExtension(), SamePropertyValuesAs.<SourceExtension>samePropertyValuesAs(sourceExtension));
                assertEquals(coreProperties, context.getProperties());
                return "default_modelname";
            }
        });

        when(idStrategy.getModelCode(anyString(), any(ModelNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ModelNameExecutionContext context = (ModelNameExecutionContext) args[1];
                assertNotNull(context);
                assertNotNull(context.getConfiguredModels());
                assertNotNull(context.getDataStoreAlias());
                assertNotNull(context.getDataStoreName());
                assertNotNull(context.getDataStoreRole());
                assertNotNull(context.getSourceExtension());
                assertNotNull(context.getMatchingDataStores());
                assertNotNull(context.getProperties());
                assertNotNull(context.getTransformationExtension());

                assertThat(context.getConfiguredModels(), IsIterableContainingInAnyOrder.containsInAnyOrder(modelProperties));
                assertEquals("source_alias", context.getDataStoreAlias());
                assertEquals("source_name", context.getDataStoreName());
                assertEquals(DataStoreRole.SOURCE, context.getDataStoreRole());
                assertThat(context.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                assertThat(context.getSourceExtension(), SamePropertyValuesAs.<SourceExtension>samePropertyValuesAs(sourceExtension));
                assertEquals(coreProperties, context.getProperties());
                return "id_modelname";
            }
        });

        String result = fixture.getModelCode(source);

        assertNotNull(result);
        assertEquals("id_modelname", result);
        verify(databaseMetadataService, times(4)).getCoreProperties();
        verify(databaseMetadataService, times(4)).getConfiguredModels();
        verify(defaultStrategy).getModelCode(anyString(), any(ModelNameExecutionContext.class));
        verify(idStrategy).getModelCode(anyString(), any(ModelNameExecutionContext.class));
    }

    @Test
    public void testGetModelCode_Lookup() throws Exception {
        final TransformationImpl transformation = new TransformationImpl();
        final DatasetImpl dataset = new DatasetImpl();
        transformation.addDataset(dataset);
        dataset.setParent(transformation);
        final SourceImpl source = new SourceImpl();
        dataset.addSource(source);
        source.setParent(dataset);
        source.setAlias("source_alias");
        source.setName("source_name");
        final SourceExtension sourceExtension = new SourceExtension();
        source.setExtension(sourceExtension);
        final TransformationExtension transformationExtension = new TransformationExtension();
        transformation.setExtension(transformationExtension);
        final LookupImpl lookup = new LookupImpl();
        lookup.setParent(source);
        source.addLookup(lookup);
        lookup.setLookupDatastore("lookup_datastore");
        lookup.setAlias("lookup_alias");

        final ModelProperties modelProperties = mock(ModelProperties.class);
        @SuppressWarnings("unchecked") final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);

        when(databaseMetadataService.getCoreProperties()).thenReturn(coreProperties);
        when(databaseMetadataService.getConfiguredModels()).thenReturn(Collections.<ModelProperties>singletonList(modelProperties));
        when(defaultStrategy.getModelCode(anyString(), any(ModelNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ModelNameExecutionContext context = (ModelNameExecutionContext) args[1];
                assertNotNull(context);
                assertNotNull(context.getConfiguredModels());
                assertNotNull(context.getDataStoreAlias());
                assertNotNull(context.getDataStoreName());
                assertNotNull(context.getDataStoreRole());
                assertNotNull(context.getSourceExtension());
                assertNotNull(context.getMatchingDataStores());
                assertNotNull(context.getProperties());
                assertNotNull(context.getTransformationExtension());

                assertThat(context.getConfiguredModels(), IsIterableContainingInAnyOrder.containsInAnyOrder(modelProperties));
                assertEquals("lookup_alias", context.getDataStoreAlias());
                assertEquals("lookup_datastore", context.getDataStoreName());
                assertEquals(DataStoreRole.LOOKUP, context.getDataStoreRole());
                assertThat(context.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                assertThat(context.getSourceExtension(), SamePropertyValuesAs.<SourceExtension>samePropertyValuesAs(sourceExtension));
                assertEquals(coreProperties, context.getProperties());
                return "default_modelname";
            }
        });

        when(idStrategy.getModelCode(anyString(), any(ModelNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ModelNameExecutionContext context = (ModelNameExecutionContext) args[1];
                assertNotNull(context);
                assertNotNull(context.getConfiguredModels());
                assertNotNull(context.getDataStoreAlias());
                assertNotNull(context.getDataStoreName());
                assertNotNull(context.getDataStoreRole());
                assertNotNull(context.getSourceExtension());
                assertNotNull(context.getMatchingDataStores());
                assertNotNull(context.getProperties());
                assertNotNull(context.getTransformationExtension());

                assertThat(context.getConfiguredModels(), IsIterableContainingInAnyOrder.containsInAnyOrder(modelProperties));
                assertEquals("lookup_alias", context.getDataStoreAlias());
                assertEquals("lookup_datastore", context.getDataStoreName());
                assertEquals(DataStoreRole.LOOKUP, context.getDataStoreRole());
                assertThat(context.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                assertThat(context.getSourceExtension(), SamePropertyValuesAs.<SourceExtension>samePropertyValuesAs(sourceExtension));
                assertEquals(coreProperties, context.getProperties());
                return "id_modelname";
            }
        });

        String result = fixture.getModelCode(lookup);

        assertNotNull(result);
        assertEquals("id_modelname", result);
        verify(databaseMetadataService, times(4)).getCoreProperties();
        verify(databaseMetadataService, times(4)).getConfiguredModels();
        verify(defaultStrategy).getModelCode(anyString(), any(ModelNameExecutionContext.class));
        verify(idStrategy).getModelCode(anyString(), any(ModelNameExecutionContext.class));
    }


}
