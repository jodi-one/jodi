package one.jodi.core.transformation.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.TransformationNameExecutionContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.KmType;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.TransformationImpl;
import one.jodi.model.extensions.TransformationExtension;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * The class <code>TransformationNameContextImplTest</code> contains tests for the class <code>{@link TransformationNameContextImpl}</code>.
 */
public class TransformationNameContextImplTest {
    @Mock
    SchemaMetaDataProvider etlProvider;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    TransformationNameDefaultStrategy defaultStrategy;
    @Mock
    TransformationNameIDStrategy customStrategy;
    @Mock
    KmType km;
    @Mock
    ETLValidator validator;
    TransformationNameContextImpl fixture;
    String prefix;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(TransformationNameContextImplTest.class);
    }

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp()
            throws Exception {
        MockitoAnnotations.initMocks(this);
        prefix = "PREFIX";
        fixture = new TransformationNameContextImpl(prefix, databaseMetadataService,
                defaultStrategy, customStrategy,
                validator, errorWarningMessages);
        when(km.getName()).thenReturn("KMNAME");
    }

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown()
            throws Exception {
    }

    @Test
    public void testGetTransformationName()
            throws Exception {

        TransformationImpl transformation = new TransformationImpl();

        Transformation mockTransformation = InputModelMockHelper.createMockETLTransformation();
        transformation.setMappings(mockTransformation.getMappings());
        final String modelName = "MODEL_NAME";
        final String targetDataStoreName = transformation.getMappings().getTargetDataStore();
        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        Mappings mappings = mock(Mappings.class);
        final DataStore dataStore = mock(DataStore.class);
        @SuppressWarnings("unchecked") final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);

        when(databaseMetadataService.getCoreProperties()).thenReturn(coreProperties);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(dataStore);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
        when(mappings.getModel()).thenReturn(modelName);

        when(mappings.getIkm()).thenReturn(km);
        //when(transformation.getMappings()).thenReturn(mappings);
        transformation.setMappings(mappings);
        //when(transformation.getExtension()).thenReturn(transformationExtension);
        transformation.setExtension(transformationExtension);
        when(defaultStrategy.getTransformationName(anyString(), any(TransformationNameExecutionContext.class))).thenReturn("name");
        when(customStrategy.getTransformationName(eq("name"), any(TransformationNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                TransformationNameExecutionContext executionContext = (TransformationNameExecutionContext) arguments[1];

                assertNotNull(executionContext.getPrefix());
                assertNotNull(executionContext.getProperties());
                assertNotNull(executionContext.getTargetDataStore());
                assertNotNull(executionContext.getTransformationExtension());

                assertEquals(prefix, executionContext.getPrefix());
                assertEquals(coreProperties, executionContext.getProperties());
                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertThat(executionContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));

                return "customName";
            }
        });
        String result = fixture.getTransformationName(transformation);

        assertNotNull(result);
        assertEquals("customName", result);
        verify(defaultStrategy).getTransformationName(anyString(), any(TransformationNameExecutionContext.class));
        verify(customStrategy).getTransformationName(eq("name"), any(TransformationNameExecutionContext.class));
        verify(databaseMetadataService, times(2)).getCoreProperties();
        verify(databaseMetadataService).getTargetDataStoreInModel(mappings);
    }

	
	/*
	@Test
	public void testGetTransformationName_EmptyCustomValue()
		throws Exception {
		
		one.jodi.etl.internalmodel.Transformation transformation = InputModelMockHelper.createMockETLTransformation();
		final String modelName = "MODEL_NAME";
		final String targetDataStoreName = transformation.getMappings().getTargetDataStore();
		final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
		one.jodi.etl.internalmodel.Mappings mappings = mock(one.jodi.etl.internalmodel.Mappings.class);
		final DataStore dataStore = mock(DataStore.class);
		@SuppressWarnings("unchecked")
		final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);
		
		when(databaseMetadataService.isTemporaryTransformation(targetDataStoreName)).thenReturn(false);
		when(databaseMetadataService.getCoreProperties()).thenReturn(coreProperties);
		when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(dataStore);
		when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
		when(mappings.getModel()).thenReturn(modelName);
		when(mappings.getIkm()).thenReturn(km);
		when(transformation.getMappings()).thenReturn(mappings);
		when(transformation.getExtension()).thenReturn(transformationExtension);
		when(defaultStrategy.getTransformationName(anyString(), any(TransformationNameExecutionContext.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				TransformationNameExecutionContext executionContext = (TransformationNameExecutionContext)arguments[1]; 
				
				assertNotNull(executionContext.getPrefix());
				assertNotNull(executionContext.getProperties());
				assertNotNull(executionContext.getTargetDataStore());
				assertNotNull(executionContext.getTransformationExtension());
				
				assertEquals(prefix, executionContext.getPrefix());
				assertEquals(coreProperties, executionContext.getProperties());
				assertEquals(dataStore, executionContext.getTargetDataStore());
				assertThat(executionContext.getTransformationExtension(),SamePropertyValuesAs.<TransformationExtension> samePropertyValuesAs(transformationExtension));

				return "name";
			}});
		when(customStrategy.getTransformationName(anyString(), any(TransformationNameExecutionContext.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				TransformationNameExecutionContext executionContext = (TransformationNameExecutionContext)arguments[1]; 
				
				assertNotNull(executionContext.getPrefix());
				assertNotNull(executionContext.getProperties());
				assertNotNull(executionContext.getTargetDataStore());
				assertNotNull(executionContext.getTransformationExtension());
				
				assertEquals(prefix, executionContext.getPrefix());
				assertEquals(coreProperties, executionContext.getProperties());
				assertEquals(dataStore, executionContext.getTargetDataStore());
				assertThat(executionContext.getTransformationExtension(),SamePropertyValuesAs.<TransformationExtension> samePropertyValuesAs(transformationExtension));

				return "";
			}});
		thrown.expect(IncorrectCustomStrategyException.class);
		fixture.getTransformationName(transformation);
	}
*/

    @Test
    public void testGetTransformationName_TemporaryTransformation()
            throws Exception {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        final String modelName = "MODEL_NAME";
        final String targetDataStoreName = transformation.getMappings().getTargetDataStore();
        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        Mappings mappings = mock(Mappings.class);
        final DataStore dataStore = mock(DataStore.class);
        @SuppressWarnings("unchecked") final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);

        when(databaseMetadataService.isTemporaryTransformation(targetDataStoreName)).thenReturn(true);
        when(databaseMetadataService.getCoreProperties()).thenReturn(coreProperties);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(dataStore);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
        when(transformation.getMappings()).thenReturn(mappings);
        when(mappings.getIkm()).thenReturn(km);
        when(mappings.getModel()).thenReturn(modelName);
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(defaultStrategy.getTransformationName(anyString(), any(TransformationNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                TransformationNameExecutionContext executionContext = (TransformationNameExecutionContext) arguments[1];

                assertNotNull(executionContext.getPrefix());
                assertNotNull(executionContext.getProperties());
                assertNotNull(executionContext.getTargetDataStore());
                assertNotNull(executionContext.getTransformationExtension());

                assertEquals(prefix, executionContext.getPrefix());
                assertEquals(coreProperties, executionContext.getProperties());
                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertThat(executionContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));

                return "name";
            }
        });
        when(customStrategy.getTransformationName(anyString(), any(TransformationNameExecutionContext.class))).thenReturn("customName");
        String result = fixture.getTransformationName(transformation);

        assertNotNull(result);
        assertEquals("name", result);
        verify(defaultStrategy).getTransformationName(anyString(), any(TransformationNameExecutionContext.class));
        verify(customStrategy, never()).getTransformationName(eq("name"), any(TransformationNameExecutionContext.class));
        verify(databaseMetadataService).getTargetDataStoreInModel(mappings);
    }

    @Test
    public void testGetTransformationName_NullCustomValue()
            throws Exception {
        int packageSequence = 11;
        TransformationImpl transformation = new TransformationImpl();
        transformation.setPackageSequence(packageSequence);
        @SuppressWarnings("unused")
        Transformation mockTransformation = InputModelMockHelper.createMockETLTransformation();
        final String modelName = "MODEL_NAME";
        final String targetDataStoreName = "targetDataStore"; //transformation.getMappings().getTargetDataStore();
        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        transformation.setExtension(transformationExtension);
        Mappings mappings = mock(Mappings.class);
        transformation.setMappings(mappings);
        final DataStore dataStore = mock(DataStore.class);
        @SuppressWarnings("unchecked") final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);

        when(databaseMetadataService.isTemporaryTransformation(targetDataStoreName)).thenReturn(false);
        when(databaseMetadataService.getCoreProperties()).thenReturn(coreProperties);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(dataStore);
        when(mappings.getIkm()).thenReturn(km);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
        when(mappings.getModel()).thenReturn(modelName);
        //when(transformation.getMappings()).thenReturn(mappings);
        //when(transformation.getExtension()).thenReturn(transformationExtension);

        when(defaultStrategy.getTransformationName(anyString(), any(TransformationNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                TransformationNameExecutionContext executionContext = (TransformationNameExecutionContext) arguments[1];

                assertNotNull(executionContext.getPrefix());
                assertNotNull(executionContext.getProperties());
                assertNotNull(executionContext.getTargetDataStore());
                assertNotNull(executionContext.getTransformationExtension());

                assertEquals(prefix, executionContext.getPrefix());
                assertEquals(coreProperties, executionContext.getProperties());
                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertThat(executionContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));

                return "name";
            }
        });
        when(customStrategy.getTransformationName(anyString(), any(TransformationNameExecutionContext.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                TransformationNameExecutionContext executionContext = (TransformationNameExecutionContext) arguments[1];

                assertNotNull(executionContext.getPrefix());
                assertNotNull(executionContext.getProperties());
                assertNotNull(executionContext.getTargetDataStore());
                assertNotNull(executionContext.getTransformationExtension());

                assertEquals(prefix, executionContext.getPrefix());
                assertEquals(coreProperties, executionContext.getProperties());
                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertThat(executionContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));

                return null;
            }
        });

        //thrown.expect(IncorrectCustomStrategyException.class);
        fixture.getTransformationName(transformation);
        verify(validator).validateTransformationName(transformation);
    }
}
