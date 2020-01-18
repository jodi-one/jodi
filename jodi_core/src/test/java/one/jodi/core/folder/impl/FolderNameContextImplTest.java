package one.jodi.core.folder.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.FolderNameExecutionContext;
import one.jodi.core.extensions.strategies.FolderNameStrategy;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.TransformationImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class FolderNameContextImplTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    DatabaseMetadataService databaseMetadataService;

    @Mock
    FolderNameStrategy defaultStrategy;
    @Mock
    FolderNameIDStrategy idStrategy;
    FolderNameContextImpl fixture;
    @Mock
    ETLValidator validator;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp()
            throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new FolderNameContextImpl(databaseMetadataService,
                defaultStrategy, idStrategy, validator, errorWarningMessages);
    }


    @Test
    public void testGetFolderFromTable() throws Exception {
        // TODO add in transformation extensions!

        final String tableName = "test";
        final String prefix = "folder";
        final String modelCode = "model";
        TransformationImpl transformation = new TransformationImpl();
        MappingsImpl mappings = new MappingsImpl();
        mappings.setModel(modelCode);
        mappings.setTargetDataStore(tableName);
        mappings.setParent(transformation);
        transformation.setMappings(mappings);
        when(idStrategy.getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean())).thenCallRealMethod();
        final DataStore ds = MockDatastoreHelper.createMockDataStore(tableName, modelCode);

        when(validator.validateFolderName(transformation, idStrategy)).thenReturn(true);


        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(ds);
        final Map<String, PropertyValueHolder> properties = new HashMap<String, PropertyValueHolder>();
        properties.put(JodiConstants.INITIAL_LOAD_FOLDER, InputModelMockHelper.createMockPropertyValueHolder(JodiConstants.INITIAL_LOAD_FOLDER, prefix));
        when(databaseMetadataService.getCoreProperties()).thenReturn(properties);

        when(defaultStrategy.getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                FolderNameExecutionContext executionContext = (FolderNameExecutionContext) arguments[1];

                assertNotNull(executionContext.getProperties());
                assertNotNull(executionContext.getTargetDataStore());

                assertEquals(executionContext.getProperties(), properties);
                assertEquals(executionContext.getTargetDataStore(), ds);
                return prefix + modelCode;
            }
        });
        String result = fixture.getFolderName(transformation, false);
        assertNotNull(result);
        assertTrue(result.startsWith(prefix));
        assertEquals(prefix + modelCode, result);
        verify(defaultStrategy).getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean());
        verify(validator).validateFolderName(transformation, idStrategy);
    }

    @Test
    public void testGetFolderFromTable_invalidFoldreName() throws Exception {
        // TODO add in transformation extensions!

        final String tableName = "test";
        final String prefix = "folder";
        final String modelCode = "model";
        TransformationImpl transformation = new TransformationImpl();
        MappingsImpl mappings = new MappingsImpl();
        mappings.setModel(modelCode);
        mappings.setTargetDataStore(tableName);
        mappings.setParent(transformation);
        transformation.setMappings(mappings);
        when(idStrategy.getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean())).thenCallRealMethod();
        final DataStore ds = MockDatastoreHelper.createMockDataStore(tableName, modelCode);

        when(validator.validateFolderName(transformation, idStrategy)).thenReturn(false);


        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(ds);
        final Map<String, PropertyValueHolder> properties = new HashMap<String, PropertyValueHolder>();
        properties.put(JodiConstants.INITIAL_LOAD_FOLDER, InputModelMockHelper.createMockPropertyValueHolder(JodiConstants.INITIAL_LOAD_FOLDER, prefix));
        when(databaseMetadataService.getCoreProperties()).thenReturn(properties);

        when(defaultStrategy.getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                FolderNameExecutionContext executionContext = (FolderNameExecutionContext) arguments[1];

                assertNotNull(executionContext.getProperties());
                assertNotNull(executionContext.getTargetDataStore());

                assertEquals(executionContext.getProperties(), properties);
                assertEquals(executionContext.getTargetDataStore(), ds);
                return prefix + modelCode;
            }
        });
        thrown.expect(UnRecoverableException.class);
        String result = fixture.getFolderName(transformation, false);
        assertNotNull(result);
        assertTrue(result.startsWith(prefix));
        assertEquals(prefix + modelCode, result);
        verify(defaultStrategy).getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean());
        verify(validator).validateFolderName(transformation, idStrategy);
    }

    @Test
    public void testGetFolderFromTable_exceptionThrown() throws Exception {
        // TODO add in transformation extension
        final String tableName = "test";
        final String prefix = "folder";
        final String modelCode = "model";
        TransformationImpl transformation = new TransformationImpl();
        MappingsImpl mappings = new MappingsImpl();
        mappings.setParent(transformation);
        transformation.setMappings(mappings);
        when(validator.validateFolderName(transformation, idStrategy)).thenReturn(false);
        mappings.setTargetDataStore(tableName);
        mappings.setModel(modelCode);

        //Mappings etlMappings = InputModelMockHelper.createMockETLMappings(tableName, "dataType", modelCode);
        final RuntimeException exception = new RuntimeException("");
        when(idStrategy.getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean())).thenThrow(exception);
        final DataStore ds = MockDatastoreHelper.createMockDataStore(tableName, modelCode);

        when(validator.validateFolderName(transformation, idStrategy)).thenReturn(false);

        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(ds);

        final Map<String, PropertyValueHolder> properties = new HashMap<String, PropertyValueHolder>();
        properties.put(JodiConstants.INITIAL_LOAD_FOLDER, InputModelMockHelper.createMockPropertyValueHolder(JodiConstants.INITIAL_LOAD_FOLDER, prefix));
        when(databaseMetadataService.getCoreProperties()).thenReturn(properties);

        when(defaultStrategy.getFolderName(anyString(), any(FolderNameExecutionContext.class), anyBoolean())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "folderName";
            }
        });

        thrown.expect(RuntimeException.class);
        try {
            fixture.getFolderName(transformation, false);
        } catch (RuntimeException e) {
            verify(validator).handleFolderName(exception, transformation);
            throw e;
        }

    }


}