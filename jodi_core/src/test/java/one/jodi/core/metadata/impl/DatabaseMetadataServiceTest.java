package one.jodi.core.metadata.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.context.Context;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.model.types.DataStoreForeignReference;
import one.jodi.base.model.types.DataStoreKey;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.model.types.ModelSolutionLayerType;
import one.jodi.base.model.types.SCDType;
import one.jodi.base.service.metadata.DataModelDescriptor;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.metadata.DatabaseMetadataServiceImpl;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.service.SubsystemServiceProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseMetadataServiceTest {
    // Specification model
    @Mock
    ModelCodeContext modelCodeContext;
    @Mock
    JodiProperties mockWfProperties;
    @Mock
    SchemaMetaDataProvider mockEtlProvider;
    @Mock
    Context context;
    @Mock
    ModelPropertiesProvider modelPropertiesProvider;
    @Mock
    SubsystemServiceProvider subsystemServiceProvider;

    DatabaseMetadataServiceImpl fixture;
    private final ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp() throws Exception {
        List<String> exclude = Arrays.asList("odi.master.repo.url", "odi.repo.db.driver", "odi.login.username");
        when(subsystemServiceProvider.getPropertyNameExclusionList()).thenReturn(exclude);
        when(mockWfProperties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");
        fixture = new DatabaseMetadataServiceImpl(mockEtlProvider,
                subsystemServiceProvider, context, mockWfProperties,
                modelPropertiesProvider, errorWarningMessages);
    }

    @Test
    public void getCoreProperties_Success() {
        when(mockWfProperties.getPropertyKeys()).thenReturn(Arrays.asList("a", "b", "odi.master.repo.url",
                "odi.repo.db.driver", "odi.master.repos.password", "odi.login.username"));
        PropertyValueHolder pvhMock = mock(PropertyValueHolder.class);

        when(pvhMock.getString()).thenReturn("Some_Value");

        when(mockWfProperties.getPropertyValueHolder(anyString())).thenReturn(pvhMock);

        Map<String, PropertyValueHolder> props = fixture.getCoreProperties();

        assertEquals(3, props.size());
        assertEquals("Some_Value", props.get("a").getString());
        assertEquals("Some_Value", props.get("b").getString());
        assertEquals("Some_Value", props.get("odi.master.repos.password").getString());
        assertNull(props.get("odi.repo.db.driver"));
    }

    @Test
    public void testGetConfiguredModels() {

        String[] models = {"m1", "m2", "m3"};

        List<ModelProperties> modelPropList = setupModelProperties(models);

        List<ModelProperties> mps = fixture.getConfiguredModels();
        assertNotNull(mps);
        assertEquals(models.length, mps.size());
        assertTrue(mps.containsAll(modelPropList));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFindDataStoreInAllModels() {

        DatabaseMetadataService fixtureSpy = spy(fixture);
        String[] models = {"m1", "m2", "m3"};
        String dataStore = "target";

        setupModelProperties(models);

        String lastModel = models[models.length - 1];
        when(mockEtlProvider.getModelCodes()).thenReturn(Arrays.asList(models));
        for (String model : models) {
            Map<String, DataStoreDescriptor> dataStoreDesc;
            if (!model.equals(lastModel)) {
                dataStoreDesc = MockDatastoreHelper.createMockDSDescriptor(dataStore, model, false);
            } else {
                // assume that data store is not in last available models
                dataStoreDesc = Collections.emptyMap();
            }
            when(mockEtlProvider.getDataStoreDescriptorsInModel(model)).thenReturn(dataStoreDesc);
            doReturn(DataStoreType.UNKNOWN).when(fixtureSpy).getDataStoreType(anyString());
        }

        List<DataStore> result = fixtureSpy.findDataStoreInAllModels(dataStore);

        assertNotNull(result);
        assertEquals(2, result.size());
        try {
            result.add(MockDatastoreHelper.createMockDataStore(dataStore, "m", false));
            fail("collection must be unmodifiable");
        } catch (Throwable e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }

        // cover base meta data
        DataStore ds = result.get(0);
        assertFalse(ds.isTemporary());
        assertEquals(dataStore, ds.getDataStoreName());
        assertEquals(DataStoreType.UNKNOWN, ds.getDataStoreType());
        assertNotNull(ds.getDataStoreFlexfields());
        assertEquals(0, ds.getDataStoreFlexfields().size());
        assertEquals(MockDatastoreHelper.DB_SERVER, ds.getDataModel().getDataServerName());
        assertEquals(MockDatastoreHelper.PHYSICAL_SERVER_NAME, ds.getDataModel().getPhysicalDataServerName());
        assertEquals("Oracle", ds.getDataModel().getDataServerTechnology());

        assertEquals(models[0], ds.getDataModel().getModelCode());
        assertNotNull(ds.getDataModel().getModelFlexfields());
        assertEquals(0, ds.getDataModel().getModelFlexfields().size());
        assertTrue(ds.getDataModel().isModelIgnoredbyHeuristics());
        assertEquals(ModelSolutionLayerType.UNKNOWN, ds.getDataModel().getSolutionLayer());
    }

    @Test
    public void testGetSourceDataStoreInModel_DoesNotExist() {
        String sourceDataStore = "source";
        String sourceModel = "sourceModel";

        final Map<String, DataStoreDescriptor> emptyMap = Collections.emptyMap();
        when(mockEtlProvider.getDataStoreDescriptorsInModel(sourceModel)).thenReturn(emptyMap);
        DataStore ds = fixture.getSourceDataStoreInModel(sourceDataStore, sourceModel);
        assertNull(ds);
    }


    @Test
    public void testGetTargetDataStore_UseCache() {

        String targetDataStore = "target_S01";
        String targetModel = "targetModel";
        String[] columnList = {"col1", "col2", "col3"};

        setupModelProperties(new String[]{targetModel});

        List<DataModelDescriptor> modelDescriptors = new ArrayList<>();
        modelDescriptors.add(MockDatastoreHelper.createMockDataModelDescriptor(targetModel));
        when(mockEtlProvider.getDataModelDescriptors()).thenReturn(modelDescriptors);

        Map<String, DataStoreDescriptor> dataStoreDesc;
        dataStoreDesc = MockDatastoreHelper.createMockDSDescriptor(targetDataStore, targetModel, false);
        when(mockEtlProvider.getDataStoreDescriptorsInModel(targetModel)).thenReturn(dataStoreDesc);

        Mappings mappings = InputModelMockHelper.createMockETLMappings(targetDataStore, columnList, "TYPE", targetModel);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);

        DataStore result = fixture.getTargetDataStoreInModel(mappings);

        // cover base meta data
        assertNotNull(result);
//    assertFalse(result.isTemporary());
        assertEquals(targetDataStore, result.getDataStoreName());
        // indicates that cache was missed and data store is added to cache
        verify(context).getDataStore(targetDataStore, targetModel);
        verify(context).addDataStore(result);

        //ensure that data store is not created more than once and uses cached value
        when(context.getDataStore(targetDataStore, targetModel)).thenReturn(result);
        DataStore result2 = fixture.getTargetDataStoreInModel(mappings);
        assertNotNull(result2);
        assertEquals(result, result2);
        // please note that getDataStore() was called again
        verify(context, times(2)).getDataStore(targetDataStore, targetModel);
        // please note that the addDataStoreMethod was not called again,
        // implying with previous check that data store in cache is found
        verify(context).addDataStore(any(DataStore.class));
    }

    @Test
    public void testGetTwoDataStoresInSameModel_UseCache() {

        String targetDataStore = "targetDataStore";
        String targetDataStore2 = "targetDataStore2";
        String targetModel = "targetModel";
        String[] columnList = {"col1", "col2", "col3"};

        setupModelProperties(new String[]{targetModel});

        List<DataModelDescriptor> modelDescriptors = new ArrayList<>();
        modelDescriptors.add(MockDatastoreHelper.createMockDataModelDescriptor(targetModel));
        when(mockEtlProvider.getDataModelDescriptors()).thenReturn(modelDescriptors);

        Map<String, DataStoreDescriptor> dataStoreDesc;
        dataStoreDesc = MockDatastoreHelper.createMockDSDescriptor(targetDataStore, targetModel, false);
        dataStoreDesc.putAll(MockDatastoreHelper.createMockDSDescriptor(targetDataStore2, targetModel, false));
        when(mockEtlProvider.getDataStoreDescriptorsInModel(targetModel)).thenReturn(dataStoreDesc);

        Mappings mappings = InputModelMockHelper.createMockETLMappings(targetDataStore, columnList, "TYPE", targetModel);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        DataStore result = fixture.getTargetDataStoreInModel(mappings);
        // will return model from existing data store when another data store is added
        when(context.getDataModel(targetModel)).thenReturn(result.getDataModel());
        mappings = InputModelMockHelper.createMockETLMappings(targetDataStore2, columnList, "TYPE", targetModel);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore2);
        DataStore result2 = fixture.getTargetDataStoreInModel(mappings);

        // cover base meta data
        assertNotNull(result);
        assertNotNull(result2);
        assertEquals(targetDataStore, result.getDataStoreName());
        assertEquals(targetDataStore2, result2.getDataStoreName());
        assertEquals(result.getDataModel(), result2.getDataModel());
        // indicates that cache was missed and data store is added to cache
        verify(context).getDataStore(targetDataStore, targetModel);
        verify(context).getDataStore(targetDataStore2, targetModel);
        verify(context).addDataStore(result);
        verify(context).addDataStore(result2);
        //indicates that model was retrieved exactly twice from cache for the expected case
        verify(context, times(2)).getDataModel(targetModel);
        verify(context, times(2)).getDataModel(anyString());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetTargetDataStoreInModel_TemporaryTable() {
        DatabaseMetadataService fixtureSpy = spy(fixture);

        String targetDataStore = "target_S01";
        String targetModel = "targetModel";
        String[] columnList = {"col1", "col2", "col3"};

        setupModelProperties(new String[]{targetModel});

        List<DataModelDescriptor> modelDescriptors = new ArrayList<>();
        modelDescriptors.add(MockDatastoreHelper.createMockDataModelDescriptor("other"));
        modelDescriptors.add(MockDatastoreHelper.createMockDataModelDescriptor(targetModel));
        when(mockEtlProvider.getDataModelDescriptors()).thenReturn(modelDescriptors);

        Map<String, DataStoreDescriptor> dataStoreDesc;
        dataStoreDesc = MockDatastoreHelper.createMockDSDescriptor(targetDataStore, targetModel, true);

        when(mockEtlProvider.getDataStoreDescriptorsInModel(targetModel)).thenReturn(dataStoreDesc);
        doReturn(DataStoreType.UNKNOWN).when(fixtureSpy).getDataStoreType(anyString());

        Mappings mappings = InputModelMockHelper.createMockETLMappings(targetDataStore, columnList, "TYPE", targetModel);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        doReturn(true).when(fixtureSpy).isTemporaryTransformation(targetDataStore);
        DataStore result = fixtureSpy.getTargetDataStoreInModel(mappings);

        assertNotNull(result);

        // cover base meta data
        assertTrue(result.isTemporary());
        assertEquals(targetDataStore, result.getDataStoreName());
        assertEquals(DataStoreType.UNKNOWN, result.getDataStoreType());
        assertNotNull(result.getDataStoreFlexfields());
        assertEquals(0, result.getDataStoreFlexfields().size());
        assertEquals(MockDatastoreHelper.DB_SERVER, result.getDataModel().getDataServerName());
        assertEquals(MockDatastoreHelper.PHYSICAL_SERVER_NAME, result.getDataModel().getPhysicalDataServerName());
        assertEquals("Oracle", result.getDataModel().getDataServerTechnology());

        assertEquals(targetModel, result.getDataModel().getModelCode());
        assertNotNull(result.getDataModel().getModelFlexfields());
        assertEquals(0, result.getDataModel().getModelFlexfields().size());
        assertTrue(result.getDataModel().isModelIgnoredbyHeuristics());
        assertEquals(ModelSolutionLayerType.UNKNOWN, result.getDataModel().getSolutionLayer());
    }

    private void testDataStoreKeys(String[] columnList, DataStoreKey.KeyType expectedType) {
        String sourceDataStore = "source";
        String sourceModel = "sourceModel";
        String[] keyList = Arrays.copyOf(columnList, 2);

        setupModelProperties(new String[]{sourceModel});

        Map<String, DataStoreDescriptor> dataStoreDesc = MockDatastoreHelper
                .createMockDSDescriptor(sourceDataStore, sourceModel, false, columnList, keyList);
        when(mockEtlProvider.getDataStoreDescriptorsInModel(sourceModel)).thenReturn(dataStoreDesc);

        DataStore result = fixture.getSourceDataStoreInModel(sourceDataStore, sourceModel);

        List<DataStoreKey> keys = result.getDataStoreKeys();

        assertNotNull(result.getColumns());
        assertNotNull(keys);
        assertEquals(1, keys.size());

        DataStoreKey key = keys.get(0);
        assertNotNull(key);
        assertEquals(columnList[0].substring(0, 1).toUpperCase() + "_key", key.getName());
        assertEquals(expectedType, key.getType());
        assertEquals(2, key.getColumns().size());
        assertEquals(columnList[0], key.getColumns().get(0));
        assertEquals(columnList[1], key.getColumns().get(1));
        assertTrue(key.isEnabledInDatabase());
        assertTrue(key.existsInDatabase());
    }

    @Test
    public void testGetColumnMetaData_For_Dastore() {
        when(mockWfProperties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");

        String targetDataStore = "target";
        String targetModel = "targetModel";
        String[] columnList = {"col1", "col2", "col3"};

        Mappings mappings = mock(Mappings.class);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        when(mappings.getModel()).thenReturn(targetModel);

        setupModelProperties(new String[]{targetModel});

        Map<String, DataStoreDescriptor> dataStoreDesc = MockDatastoreHelper
                .createMockDSDescriptor(targetDataStore, targetModel, false, columnList);
        when(mockEtlProvider.getDataStoreDescriptorsInModel(targetModel)).thenReturn(dataStoreDesc);

        DataStore result = fixture.getTargetDataStoreInModel(mappings);
        Map<String, DataStoreColumn> columns = result.getColumns();

        assertNotNull(columns);
        assertEquals(3, columns.size());
        assertTrue(Arrays.asList(columnList).containsAll(columns.keySet()));
        int position = 1;
        for (String columnName : columnList) {
            int count = Integer.parseInt(columnName.substring(columnName.length() - 1));
            assertEquals(columnName, columns.get(columnName).getName());
            assertEquals(columnName + "_DataType", columns.get(columnName).getColumnDataType());
            assertEquals(SCDType.ADD_ROW_ON_CHANGE, columns.get(columnName).getColumnSCDType());
            assertEquals((count % 2) == 0, columns.get(columnName).hasNotNullConstraint());
            assertEquals(position++, columns.get(columnName).getPosition());
        }
//		verify(mockEtlProvider, times(1)).isTemporaryTransformation(targetDataStore);
    }

    @Test
    public void testGetColumnMetaData_For_TempTable() {
        DatabaseMetadataService fixtureSpy = spy(fixture);

        String targetDataStore = "target_S01";
        String targetModel = "targetModel";
        String[] columnList = {"col1", "col2", "col3"};

        setupModelProperties(new String[]{targetModel});

        Mappings mappings = InputModelMockHelper.createMockETLMappings(targetDataStore, columnList, "TYPE", targetModel);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        when(mappings.getModel()).thenReturn(targetModel);

        List<DataModelDescriptor> modelDescriptors = new ArrayList<>();
        modelDescriptors.add(MockDatastoreHelper.createMockDataModelDescriptor(targetModel));
        when(mockEtlProvider.getDataModelDescriptors()).thenReturn(modelDescriptors);

        when(fixtureSpy.isTemporaryTransformation(targetDataStore)).thenReturn(true);

        DataStore result = fixtureSpy.getTargetDataStoreInModel(mappings);
        Map<String, DataStoreColumn> columns = result.getColumns();

        assertNotNull(columns);
        assertEquals(3, columns.size());
        int position = 1;
        for (String columnName : columnList) {
            int count = Integer.parseInt(columnName.substring(columnName.length() - 1));
            boolean b = count % 2 == 0;

            assertEquals(columnName, columns.get(columnName).getName());
            assertEquals("TYPE", columns.get(columnName).getColumnDataType());
            assertNull(columns.get(columnName).getColumnSCDType());
            assertEquals(b, columns.get(columnName).hasNotNullConstraint());
            assertEquals(position++, columns.get(columnName).getPosition());
        }
        verify(mockEtlProvider, never()).getDataStoreDescriptorsInModel(targetModel);
    }
	
	/*
	@Test
	public void testgetTempTableMetadata() {
		String targetDataStore = "target_S01";
		String targetModel     = "targetModel";
		String[] columnList    = {"col1", "col2", "col3"};
		
		Mappings mappings = InputModelMockHelper.createMockETLMappings(targetDataStore, columnList, "TYPE", targetModel);
		
		when(mockEtlProvider.isTemporaryTransformation(targetDataStore)).thenReturn(true);
		DataStore ds = fixture.getTargetDataStoreInModel(mappings);
		
		verify(context, times(1)).addTemporaryTableDataStore(ds);
		verify(context, times(1)).getTemporaryTableDataStore(mappings.getTargetDataStore());
		
		//uses cached Mapping information
		when(context.getTemporaryTableDataStore(targetDataStore)).thenReturn(ds);
		DataStoreWithAlias ds2 = fixture.getSourceDataStoreInModel(targetDataStore, targetModel, "Alias");
		
		assertEquals(targetDataStore, ds2.getDataStoreName());
		assertEquals("Alias", ds2.getAlias());
		assertNotNull(ds2.getColumns());
		assertEquals(3, ds2.getColumns().size());
		verify(context, times(2)).getTemporaryTableDataStore(targetDataStore);
		// following implies that no additional data store is inserted as it is already present in cache
		verify(context, times(1)).addTemporaryTableDataStore(any(DataStore.class));
	}
	*/

    @SuppressWarnings("deprecation")
    @Test
    public void testGetTempTableMetadata() {
        DatabaseMetadataService fixtureSpy = spy(fixture);
        String targetDataStore = "target_S01";
        String targetModel = "targetModel";
        String[] columnList = {"col1", "col2", "col3"};

        setupModelProperties(new String[]{targetModel, "other"});

        Mappings mappings = InputModelMockHelper.createMockETLMappings(targetDataStore, columnList, "TYPE", targetModel);

        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        when(fixtureSpy.isTemporaryTransformation(targetDataStore)).thenReturn(true);
        List<DataModelDescriptor> modelDescriptors = new ArrayList<>();
        modelDescriptors.add(MockDatastoreHelper.createMockDataModelDescriptor("other"));
        modelDescriptors.add(MockDatastoreHelper.createMockDataModelDescriptor(targetModel));
        when(mockEtlProvider.getDataModelDescriptors()).thenReturn(modelDescriptors);

        DataStore ds = fixture.getTargetDataStoreInModel(mappings);

        verify(context, times(1)).addDataStore(ds);
        verify(context, times(1)).getDataStore(mappings.getTargetDataStore(), targetModel);

        //uses cached Mapping information
        when(context.getDataStore(targetDataStore, targetModel)).thenReturn(ds);
        DataStore ds2 = fixtureSpy.getSourceDataStoreInModel(targetDataStore, targetModel);

        assertEquals(targetDataStore, ds2.getDataStoreName());
        assertNotNull(ds2.getColumns());
        assertEquals(3, ds2.getColumns().size());

        assertNotNull(ds2.getDataStoreForeignReference());
        assertEquals(0, ds2.getDataStoreForeignReference().size());

        // cover base meta data
        assertTrue(ds2.isTemporary());
        assertEquals(DataStoreType.UNKNOWN, ds2.getDataStoreType());
        assertNotNull(ds2.getDataStoreFlexfields());
        assertEquals(0, ds2.getDataStoreFlexfields().size());
        assertEquals(MockDatastoreHelper.DB_SERVER, ds2.getDataModel().getDataServerName());
        assertEquals("Oracle", ds2.getDataModel().getDataServerTechnology());

        assertEquals(targetModel, ds2.getDataModel().getModelCode());
        assertNotNull(ds2.getDataModel().getModelFlexfields());
        assertEquals(0, ds2.getDataModel().getModelFlexfields().size());
        assertTrue(ds2.getDataModel().isModelIgnoredbyHeuristics());
        assertEquals(ModelSolutionLayerType.UNKNOWN, ds2.getDataModel().getSolutionLayer());


        //Ensure that key is empty list
        assertNotNull(ds2.getDataStoreKeys());
        assertEquals(0, ds2.getDataStoreKeys().size());
        verify(context, times(2)).getDataStore(targetDataStore, targetModel);
        // following implies that no additional data store is inserted as it is already present in cache
        verify(context, times(1)).addDataStore(any(DataStore.class));
    }

    @Test
    public void testGetDataStoreKey() {
        String[] columnList = {"col1", "col2", "col3"};

        for (DataStoreKey.KeyType expectedType : DataStoreKey.KeyType.values()) {
            columnList[0] = expectedType.toString().substring(0, 1).toLowerCase() + columnList[0].substring(1);
            testDataStoreKeys(columnList, expectedType);
        }

//    verify(mockEtlProvider, times(columnList.length)).isTemporaryTransformation(anyString());
    }

    @Test
    public void testGetDataStoreFkKey() {

        String sourceDataStore = "source";
        String sourceModel = "sourceModel";
        String[] sourceColumnList = {"col1", "key_fk", "col3"};

        String primaryDataStore = "primarySource";
        String primaryModel = "primary_sourceModel";
        String[] primaryColumnList = {"key_pk", "col2", "col3"};
        String[] fkColums = {"key"};

        setupModelProperties(new String[]{sourceModel, primaryModel});

        //setup data store for primary key table
        Map<String, DataStoreDescriptor> primaryDataStoreDesc = MockDatastoreHelper
                .createMockDSDescriptor(primaryDataStore, primaryModel, false, primaryColumnList, new String[]{});
        when(mockEtlProvider.getDataStoreDescriptorsInModel(primaryModel)).thenReturn(primaryDataStoreDesc);

        // setup test case for foreign key table
        Map<String, DataStoreDescriptor> dataStoreDesc = MockDatastoreHelper
                .createMockDSDescriptor(sourceDataStore, sourceModel, false, sourceColumnList, new String[]{},
                        primaryDataStore, primaryModel, fkColums);
        when(mockEtlProvider.getDataStoreDescriptorsInModel(sourceModel)).thenReturn(dataStoreDesc);

        DataStore result = fixture.getSourceDataStoreInModel(sourceDataStore, sourceModel);
        List<DataStoreForeignReference> refList = result.getDataStoreForeignReference();

        assertNotNull(refList);
        assertEquals(1, refList.size());
        assertEquals("source_primarySource_FK", refList.get(0).getName());
        assertNotNull(refList.get(0).getForeignKeyDataStore());
        assertEquals(sourceDataStore, refList.get(0).getForeignKeyDataStore().getDataStoreName());
        assertNotNull(refList.get(0).getPrimaryKeyDataStore());
        assertEquals(primaryDataStore, refList.get(0).getPrimaryKeyDataStore().getDataStoreName());
        assertTrue(refList.get(0).isEnabledInDatabase());

        assertNotNull(refList.get(0).getReferenceColumns());
        assertEquals(1, refList.get(0).getReferenceColumns().size());
        for (DataStoreForeignReference.DataStoreReferenceColumn ref : refList.get(0).getReferenceColumns()) {
            assertEquals("key", ref.getForeignKeyColumnName());
            assertEquals("P_PK", ref.getPrimaryKeyColumnName());
        }
    }

    @Test
    public void testGetAllDataStoresInModel() {
        String[] models = {"m1", "m2", "m3"};
        String dataStore = "target";

        setupModelProperties(models);

        when(mockEtlProvider.getModelCodes()).thenReturn(Arrays.asList(models));
        int i = 1;
        for (String model : models) {
            Map<String, DataStoreDescriptor> dataStoreDesc;
            dataStoreDesc = MockDatastoreHelper.createMockDSDescriptor(dataStore + i + "_1", model, false);
            dataStoreDesc.putAll(MockDatastoreHelper.createMockDSDescriptor(dataStore + i + "_2", model, false));
            when(mockEtlProvider.getDataStoreDescriptorsInModel(model)).thenReturn(dataStoreDesc);
            i++;
        }

        Map<String, DataStore> result = fixture.getAllDataStoresInModel(models[1]);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get(models[1] + "." + dataStore + 2 + "_1"));
        assertNotNull(result.get(models[1] + "." + dataStore + 2 + "_2"));
    }

    @Test
    public void testIsDimensionFalse() {

        //when(mockWfProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)).thenReturn("foo");
        DataStoreType result = fixture.getDataStoreType("W_TARGETDATASTORE_D");

        assertNotEquals(DataStoreType.DIMENSION, result);

    }

    @Test
    public void testIsDimensionTrue() {
        List<String> prefix = new ArrayList<>();
        prefix.add("W_");
        when(mockWfProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)).thenReturn(prefix);
        when(mockWfProperties.getProperty(JodiConstants.DIMENSION_SUFFIX)).thenReturn("_D");
        DataStoreType result = fixture.getDataStoreType("W_TARGETDATASTORE_D");
        System.out.println(result);
        assertEquals(DataStoreType.DIMENSION, result);
    }

    @Test
    public void testIsFactFalse() {
//		when(mockWfProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)).thenReturn("foo");
        DataStoreType result = fixture.getDataStoreType("W_TARGETDATASTORE_F");

        assertNotEquals(DataStoreType.FACT, result);

    }

    @Test
    public void testIsFactTrue() {
        List<String> prefix = new ArrayList<>();
        prefix.add("W_");
        when(mockWfProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)).thenReturn(prefix);
        List<String> list = new ArrayList<>();
        list.add("_F");
        when(mockWfProperties.getPropertyList(JodiConstants.FACT_SUFFIX)).thenReturn(list);
        DataStoreType result = fixture.getDataStoreType("W_TARGETDATASTORE_F");

        assertEquals(DataStoreType.FACT, result);
    }

    @Test
    public void testIsHelperFalse() {
        when(mockWfProperties.getProperty(JodiConstants.DATA_MART_PREFIX)).thenReturn("foo");
        DataStoreType result = fixture.getDataStoreType("W_TARGETDATASTORE_H");

        assertNotEquals(DataStoreType.HELPER, result);

    }

    @Test
    public void testIsHelperTrue() {
        List<String> prefix = new ArrayList<>();
        prefix.add("W_");
        when(mockWfProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)).thenReturn(prefix);
        when(mockWfProperties.getProperty(JodiConstants.HELPER_SUFFIX)).thenReturn("_H");
        DataStoreType result = fixture.getDataStoreType("W_TARGETDATASTORE_H");

        assertEquals(DataStoreType.HELPER, result);
    }


    @Test
    public void testIsTemporaryTransformationTrue() {
        String tableName = "test_S04";

        boolean result = fixture.isTemporaryTransformation(tableName);

        assertTrue(result);
    }

    @Test
    public void testIsTemporaryTransformationFalse_1() {
        String tableName = "test";

        boolean result = fixture.isTemporaryTransformation(tableName);

        assertFalse(result);
    }

    @Test
    public void testIsTemporaryTransformationFalse_2() {
        String tableName = "test_S04_test";
        when(mockWfProperties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");
        boolean result = fixture.isTemporaryTransformation(tableName);

        assertFalse(result);
    }

    @Test
    public void testIsTemporaryTransformationFalse_3() {
        String tableName = "test_X04";

        boolean result = fixture.isTemporaryTransformation(tableName);

        assertFalse(result);
    }

    @Test
    public void testIsTemporaryTransformationException() {
        String tableName = "_S04";
        when(mockWfProperties.getTemporaryInterfacesRegex()).thenReturn("(_S)([0-9]{1,}){1,1}");
        assertTrue(fixture.isTemporaryTransformation(tableName));
    }


    @Test
    public void testIsConnectorModel() {
        String modelCode = "test";

        ModelProperties modelProperties = mock(ModelProperties.class);
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(Collections.singletonList(modelProperties));
        when(modelProperties.getCode()).thenReturn(modelCode);
        when(modelProperties.getLayer()).thenReturn("edw_SDS");

        boolean result = fixture.isConnectorModel(modelCode);

        assertTrue(result);
    }

    @Test
    public void testIsConnectorModelFalse() {
        String modelCode = "test";

        ModelProperties modelProperties = mock(ModelProperties.class);
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(Collections.singletonList(modelProperties));
        when(modelProperties.getCode()).thenReturn(modelCode);
        when(modelProperties.getLayer()).thenReturn("something_else");

        boolean result = fixture.isConnectorModel(modelCode);

        assertFalse(result);
    }

    @Test
    public void testIsSourceModel() {
        String modelCode = "test";
        ModelProperties modelProperties = mock(ModelProperties.class);
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(Collections.singletonList(modelProperties));
        when(modelProperties.getCode()).thenReturn(modelCode);
        when(modelProperties.getLayer()).thenReturn("source");

        boolean result = fixture.isSourceModel(modelCode);

        assertTrue(result);
    }

    @Test
    public void testIsSourceModelFalse() {
        String modelCode = "test";

        ModelProperties modelProperties = mock(ModelProperties.class);
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(Collections.singletonList(modelProperties));
        when(modelProperties.getCode()).thenReturn(modelCode);
        when(modelProperties.getLayer()).thenReturn("something_else");

        boolean result = fixture.isSourceModel(modelCode);

        assertFalse(result);
    }
    //
    //  UTILITY methods
    //

    @SuppressWarnings("deprecation")
    private List<ModelProperties> setupModelProperties(String[] models) {
        List<ModelProperties> modelPropList = new ArrayList<>();
        boolean ignore = true;
        for (String model : models) {
            ModelProperties modelProp = mock(ModelProperties.class);
            when(modelProp.getCode()).thenReturn(model);
            when(modelProp.getLayer()).thenReturn(ModelSolutionLayerType.UNKNOWN.toString());
            when(modelProp.isIgnoredByHeuristics()).thenReturn(ignore);
            modelPropList.add(modelProp);
            ignore = false;
        }
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropList);
        return modelPropList;
    }

}