package one.jodi.core.targetcolumn.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.*;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.etl.internalmodel.KmType;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.TargetColumnExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * The class <code>FlagsContextImplTest</code> contains tests for the
 * class {@link FlagsContextImpl}
 *
 */
public class FlagsContextImplTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    FlagsStrategy customStrategy;
    @Mock
    FlagsDefaultStrategy defaultStrategy;
    @Mock
    SchemaMetaDataProvider etlProvider;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    ModelCodeContext modelCodeContext;
    @Mock
    KnowledgeModuleContext kmContext;
    @Mock
    JodiProperties properties;
    FlagsContextImpl fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Perform pre-test initialization
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fixture = new FlagsContextImpl(databaseMetadataService,
                defaultStrategy, customStrategy, errorWarningMessages, properties);
    }

    /**
     * Perform post-test clean up
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserDefinedFlags_NoCustomOverride() {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        final MappingsExtension mappingExtension = InputModelMockHelper.createMockMappingsExtension();
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getExtension()).thenReturn(mappingExtension);
        when(transformation.getMappings()).thenReturn(mappings);

        final String modelCode = "model";
        Map<String, Object> emptyObjMap = Collections.emptyMap();

        final DataStore ds = mock(DataStore.class);
        final DataModel dm = mock(DataModel.class);

        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(ds.getDataModel()).thenReturn(dm);
        when(dm.getModelCode()).thenReturn(modelCode);
        when(dm.getSolutionLayer()).thenReturn(ModelSolutionLayerType.UNKNOWN);
        when(ds.getDataStoreName()).thenReturn(InputModelMockHelper.TARGET_STORE);
        when(ds.getDataStoreType()).thenReturn(DataStoreType.UNKNOWN);
        when(dm.getModelFlexfields()).thenReturn(emptyObjMap);

        when(mappings.getModel()).thenReturn(modelCode);

        final Map<String, PropertyValueHolder> emptyMap = Collections.emptyMap();
        when(databaseMetadataService.getCoreProperties()).thenReturn(emptyMap);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(ds);

        final String ikmCode = "IKM Oracle Append";
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn(ikmCode);
        Map<String, String> kmOptions = new HashMap<String, String>();
        kmOptions.put("TRUNCATE", "true");
        when(kmType.getOptions()).thenReturn(kmOptions);
        when(transformation.getMappings().getIkm()).thenReturn(kmType);


        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        ds, new String[]{"colname", "col2", "col3"});
        when(ds.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(ds);
        for (Targetcolumn column : mappings.getTargetColumns()) {
            TargetColumnExtension ce = InputModelMockHelper.createMockTargetColumnExtension();
            when(column.getExtension()).thenReturn(ce);
        }

        when(defaultStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(customStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));

        when(defaultStrategy.getUserDefinedFlags(any(Set.class), any(FlagsDataStoreExecutionContext.class), any(UDFlagsTargetColumnExecutionContext.class))).thenAnswer(
                new Answer<Set<UserDefinedFlag>>() {

                    @Override
                    public Set<UserDefinedFlag> answer(InvocationOnMock invocation)
                            throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        FlagsDataStoreExecutionContext tableContext = (FlagsDataStoreExecutionContext) arguments[1];
                        UDFlagsTargetColumnExecutionContext columnContext = (UDFlagsTargetColumnExecutionContext) arguments[2];

                        assertNotNull(tableContext.getIKMCode());
                        assertNotNull(tableContext.getMappingsExtension());
                        assertNotNull(tableContext.getProperties());
                        assertNotNull(tableContext.getTargetDataStore());
                        assertNotNull(tableContext.getTransformationExtension());

                        assertNotNull(columnContext.getColumnDataType());
                        assertNotNull(columnContext.getTargetColumnName());
                        assertNotNull(columnContext.getColumnSCDType());
                        assertNotNull(columnContext.getTargetColumnFlags());
                        assertTrue(columnContext.getTargetColumnFlags().isInsert());
                        assertTrue(columnContext.getTargetColumnFlags().isUpdate());
                        assertFalse(columnContext.getTargetColumnFlags().isUpdateKey());
                        assertFalse(columnContext.getTargetColumnFlags().isMandatory());

                        assertEquals(tableContext.getIKMCode(), ikmCode);
                        assertEquals(tableContext.getProperties(), emptyMap);
                        assertEquals(tableContext.getTargetDataStore(), ds);

                        assertEquals(columnContext.getColumnDataType(), columnContext.getTargetColumnName() + "_DataType");
                        assertEquals(columnContext.getColumnSCDType(), SCDType.ADD_ROW_ON_CHANGE);

                        assertThat(tableContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                        assertThat(tableContext.getMappingsExtension(), SamePropertyValuesAs.<MappingsExtension>samePropertyValuesAs(mappingExtension));
                        return Collections.<UserDefinedFlag>emptySet();
                    }
                });
        when(customStrategy.getUserDefinedFlags(any(Set.class), any(FlagsDataStoreExecutionContext.class), any(UDFlagsTargetColumnExecutionContext.class))).thenAnswer(
                new Answer<Set<UserDefinedFlag>>() {

                    @Override
                    public Set<UserDefinedFlag> answer(InvocationOnMock invocation)
                            throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        FlagsDataStoreExecutionContext tableContext = (FlagsDataStoreExecutionContext) arguments[1];
                        FlagsTargetColumnExecutionContext columnContext = (FlagsTargetColumnExecutionContext) arguments[2];

                        assertNotNull(tableContext.getIKMCode());
                        assertNotNull(tableContext.getMappingsExtension());
                        assertNotNull(tableContext.getProperties());
                        assertNotNull(tableContext.getTargetDataStore());
                        assertNotNull(tableContext.getTransformationExtension());

                        assertNotNull(columnContext.getColumnDataType());
                        assertNotNull(columnContext.getTargetColumnName());
                        assertNotNull(columnContext.getColumnSCDType());

                        assertEquals(tableContext.getIKMCode(), ikmCode);
                        assertEquals(tableContext.getProperties(), emptyMap);
                        assertEquals(tableContext.getTargetDataStore(), ds);

                        assertEquals(columnContext.getColumnDataType(), columnContext.getTargetColumnName() + "_DataType");
                        assertEquals(columnContext.getColumnSCDType(), SCDType.ADD_ROW_ON_CHANGE);

                        assertThat(tableContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                        assertThat(tableContext.getMappingsExtension(), SamePropertyValuesAs.<MappingsExtension>samePropertyValuesAs(mappingExtension));
                        return Collections.<UserDefinedFlag>emptySet();
                    }
                });
        Map<String, Set<UserDefinedFlag>> result = fixture.getUserDefinedFlags(mappings);
        assertNotNull(result);
        assertEquals(3, result.size());

        assertNotNull(result.get("colname"));
        assertNotNull(result.get("col2"));
        assertNotNull(result.get("col3"));

        verify(databaseMetadataService, times(2)).getTargetDataStoreInModel(mappings);
        verify(databaseMetadataService, times(2)).getCoreProperties(); //deferred into inner class
        verify(defaultStrategy, times(3)).getUserDefinedFlags(any(Set.class), any(FlagsDataStoreExecutionContext.class), any(UDFlagsTargetColumnExecutionContext.class));
        verify(customStrategy, times(3)).getUserDefinedFlags(any(Set.class), any(FlagsDataStoreExecutionContext.class), any(UDFlagsTargetColumnExecutionContext.class));
    }


    /**
     * Run the Collection<UserDefinedFlag> getUserDefinedFlags(Targetcolumn)
     * method test
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserDefinedFlags_WithCustomOverride() {
        Targetcolumn column = mock(Targetcolumn.class);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        DataStore dataStore = mock(DataStore.class);
        MappingsExtension mappingsExtension = new MappingsExtension();
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        TargetColumnExtension columnextension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(columnextension);
        when(column.getParent()).thenReturn(mappings);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);

        final String ikmCode = "IKM Oracle Append";
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn(ikmCode);
        when(transformation.getMappings().getIkm()).thenReturn(kmType);

        when(defaultStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(customStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));

        Map<String, Set<UserDefinedFlag>> result = fixture.getUserDefinedFlags(mappings);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertNotNull(result.get("colname"));
        assertNotNull(result.get("col2"));
        assertNotNull(result.get("col3"));
        verify(databaseMetadataService, times(2)).getCoreProperties(); //deferred into inner class
        verify(customStrategy, times(3)).getUserDefinedFlags(any(Set.class), any(FlagsDataStoreExecutionContext.class), any(UDFlagsTargetColumnExecutionContext.class));
    }

    /**
     * Run the Collection<UserDefinedFlag> getUserDefinedFlags(Targetcolumn)
     * method test
     */
    @Test
    public void testGetUserDefinedFlags_FactTransformation() {
        Targetcolumn column = mock(Targetcolumn.class);
        when(column.getName()).thenReturn("colname");
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        DataStore dataStore = mock(DataStore.class);
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        TargetColumnExtension extension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(extension);
        when(column.getExplicitUserDefinedFlags()).thenReturn(Collections.emptySet());
        when(column.getParent()).thenReturn(mappings);
        when(mappings.getTargetColumns()).thenReturn(Arrays.asList(column));
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);

        final String ikmCode = "IKM Oracle Append";
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn(ikmCode);
        when(transformation.getMappings().getIkm()).thenReturn(kmType);

        when(defaultStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(customStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));

        Map<String, Set<UserDefinedFlag>> result = fixture.getUserDefinedFlags(mappings);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    /**
     * Run the Collection<UserDefinedFlag> getUserDefinedFlags(Targetcolumn)
     * method test
     */
    @Test
    public void testGetUserDefinedFlags_HelperTransformation() {
        Targetcolumn column = mock(Targetcolumn.class);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        DataStore dataStore = mock(DataStore.class);
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        TargetColumnExtension extension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(extension);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);
        when(column.getParent()).thenReturn(mappings);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        final String ikmCode = "IKM Oracle Append";
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn(ikmCode);
        when(transformation.getMappings().getIkm()).thenReturn(kmType);

        when(defaultStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(customStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));

        Map<String, Set<UserDefinedFlag>> result = fixture.getUserDefinedFlags(mappings);
        assertNotNull(result);
        assertEquals(3, result.size());
    }


    @Test
    public void testGetInsertUpdateFlagsWithExplicit() {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        final MappingsExtension mappingExtension = InputModelMockHelper.createMockMappingsExtension();
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getExtension()).thenReturn(mappingExtension);
        when(transformation.getMappings()).thenReturn(mappings);

        final String modelCode = "model";
        Map<String, Object> emptyObjMap = Collections.emptyMap();

        final DataStore ds = mock(DataStore.class);
        final DataModel dm = mock(DataModel.class);
        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(ds.getDataModel()).thenReturn(dm);
        when(dm.getModelCode()).thenReturn(modelCode);
        when(dm.getSolutionLayer()).thenReturn(ModelSolutionLayerType.UNKNOWN);
        when(ds.getDataStoreName()).thenReturn(InputModelMockHelper.TARGET_STORE);
        when(ds.getDataStoreType()).thenReturn(DataStoreType.UNKNOWN);
        when(dm.getModelFlexfields()).thenReturn(emptyObjMap);

        final Map<String, PropertyValueHolder> emptyMap = Collections.emptyMap();
        when(databaseMetadataService.getCoreProperties()).thenReturn(emptyMap);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(ds);

        final String ikmCode = "IKM Oracle Append";
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn(ikmCode);
        when(transformation.getMappings().getIkm()).thenReturn(kmType);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        ds, new String[]{"colname", "col2", "ROW_WID"});
        when(ds.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(ds);
        for (Targetcolumn column : mappings.getTargetColumns()) {
            TargetColumnExtension ce = InputModelMockHelper.createMockTargetColumnExtension();
            when(column.getExtension()).thenReturn(ce);
            when(column.isUpdateKey()).thenReturn(true);
            when(column.isMandatory()).thenReturn(false);
        }

        when(defaultStrategy.getTargetColumnFlags(any(TargetColumnFlags.class), any(FlagsDataStoreExecutionContext.class), any(FlagsTargetColumnExecutionContext.class))).thenAnswer(
                new Answer<TargetColumnFlags>() {

                    @Override
                    public TargetColumnFlags answer(InvocationOnMock invocation)
                            throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        FlagsDataStoreExecutionContext tableContext = (FlagsDataStoreExecutionContext) arguments[1];
                        FlagsTargetColumnExecutionContext columnContext = (FlagsTargetColumnExecutionContext) arguments[2];

                        assertNotNull(tableContext.getIKMCode());
                        assertNotNull(tableContext.getMappingsExtension());
                        assertNotNull(tableContext.getProperties());
                        assertNotNull(tableContext.getTargetDataStore());
                        assertNotNull(tableContext.getTransformationExtension());

                        assertNotNull(columnContext.getColumnDataType());
                        assertNotNull(columnContext.getTargetColumnName());
                        assertNotNull(columnContext.getColumnSCDType());

                        assertEquals(tableContext.getIKMCode(), ikmCode);
                        assertEquals(tableContext.getProperties(), emptyMap);
                        assertEquals(tableContext.getTargetDataStore(), ds);

                        assertEquals(columnContext.getColumnDataType(), columnContext.getTargetColumnName() + "_DataType");
                        assertEquals(columnContext.getColumnSCDType(), SCDType.ADD_ROW_ON_CHANGE);

                        assertThat(tableContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                        assertThat(tableContext.getMappingsExtension(), SamePropertyValuesAs.<MappingsExtension>samePropertyValuesAs(mappingExtension));
                        return createInsertUpdateFlag(true, true, false, false);
                    }
                });
        when(customStrategy.getTargetColumnFlags(any(TargetColumnFlags.class), any(FlagsDataStoreExecutionContext.class), any(FlagsTargetColumnExecutionContext.class))).thenAnswer(
                new Answer<TargetColumnFlags>() {

                    @Override
                    public TargetColumnFlags answer(InvocationOnMock invocation)
                            throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        FlagsDataStoreExecutionContext tableContext = (FlagsDataStoreExecutionContext) arguments[1];
                        FlagsTargetColumnExecutionContext columnContext = (FlagsTargetColumnExecutionContext) arguments[2];

                        assertNotNull(tableContext.getIKMCode());
                        assertNotNull(tableContext.getMappingsExtension());
                        assertNotNull(tableContext.getProperties());
                        assertNotNull(tableContext.getTargetDataStore());
                        assertNotNull(tableContext.getTransformationExtension());

                        assertNotNull(columnContext.getColumnDataType());
                        assertNotNull(columnContext.getTargetColumnName());
                        assertNotNull(columnContext.getColumnSCDType());

                        assertEquals(tableContext.getIKMCode(), ikmCode);
                        assertEquals(tableContext.getProperties(), emptyMap);
                        assertEquals(tableContext.getTargetDataStore(), ds);

                        assertEquals(columnContext.getColumnDataType(), columnContext.getTargetColumnName() + "_DataType");
                        assertEquals(columnContext.getColumnSCDType(), SCDType.ADD_ROW_ON_CHANGE);

                        assertThat(tableContext.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                        assertThat(tableContext.getMappingsExtension(), SamePropertyValuesAs.<MappingsExtension>samePropertyValuesAs(mappingExtension));
                        return createInsertUpdateFlag(true, true, false, false);
                    }
                });
        Map<String, TargetColumnFlags> result = fixture.getTargetColumnFlags(mappings);
        assertNotNull(result);
        assertEquals(3, result.size());

        assertNotNull(result.get("colname"));
        assertNotNull(result.get("col2"));
        assertNotNull(result.get("ROW_WID"));

        verify(databaseMetadataService).getTargetDataStoreInModel(mappings);
        verify(databaseMetadataService).getCoreProperties(); //deferred into inner class
        verify(defaultStrategy, times(3)).getTargetColumnFlags(any(TargetColumnFlags.class), any(FlagsDataStoreExecutionContext.class), any(FlagsTargetColumnExecutionContext.class));
        verify(customStrategy, times(3)).getTargetColumnFlags(any(TargetColumnFlags.class), any(FlagsDataStoreExecutionContext.class), any(FlagsTargetColumnExecutionContext.class));

    }


    @Test
    public void testGetInsertUpdateFlagsNoExplicitTargetColumns() {
        Targetcolumn column = mock(Targetcolumn.class);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        DataStore dataStore = mock(DataStore.class);
        MappingsExtension mappingsExtension = new MappingsExtension();
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        TargetColumnExtension columnextension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(columnextension);
        when(column.getParent()).thenReturn(mappings);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn("KM_NAME");
        when(mappings.getIkm()).thenReturn(kmType);

        when(defaultStrategy.getTargetColumnFlags(any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(customStrategy.getTargetColumnFlags(any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));

        Map<String, TargetColumnFlags> result = fixture.getTargetColumnFlags(mappings);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNotNull(result.get("colname"));
        assertNotNull(result.get("col2"));
        assertNotNull(result.get("col3"));
        verify(databaseMetadataService).getCoreProperties(); //deferred into inner class
        verify(defaultStrategy, times(3)).getTargetColumnFlags(any(TargetColumnFlags.class), any(FlagsDataStoreExecutionContext.class), any(FlagsTargetColumnExecutionContext.class));
        verify(customStrategy, times(3)).getTargetColumnFlags(any(TargetColumnFlags.class), any(FlagsDataStoreExecutionContext.class), any(FlagsTargetColumnExecutionContext.class));
    }

    private TargetColumnFlags createInsertUpdateFlag(final boolean isInsert,
                                                     final boolean isUpdate,
                                                     final boolean isUpdateKey,
                                                     final boolean isMandatory) {
        TargetColumnFlags defaultFlags = new TargetColumnFlags() {
            @Override
            public Boolean isInsert() {
                return isInsert;
            }

            @Override
            public Boolean isUpdate() {
                return isUpdate;
            }

            @Override
            public Boolean isUpdateKey() {
                return isUpdateKey;
            }

            @Override
            public Boolean isMandatory() {
                return isMandatory;
            }

            @Override
            public Boolean useExpression() {
                return true;
            }
        };
        return defaultFlags;
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserDefinedFlags_WithCustomOverrideException() {
        Targetcolumn column = mock(Targetcolumn.class);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        DataStore dataStore = mock(DataStore.class);
        MappingsExtension mappingsExtension = new MappingsExtension();
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        TargetColumnExtension columnextension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(columnextension);
        when(column.getParent()).thenReturn(mappings);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn("KM_NAME");
        when(mappings.getIkm()).thenReturn(kmType);

        when(
                defaultStrategy.getTargetColumnFlags(
                        any(TargetColumnFlags.class),
                        any(FlagsDataStoreExecutionContext.class),
                        any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(
                customStrategy.getTargetColumnFlags(
                        any(TargetColumnFlags.class),
                        any(FlagsDataStoreExecutionContext.class),
                        any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(
                customStrategy.getUserDefinedFlags(any(Set.class),
                        any(FlagsDataStoreExecutionContext.class),
                        any(UDFlagsTargetColumnExecutionContext.class)))
                .thenThrow(RuntimeException.class);

        thrown.expect(IncorrectCustomStrategyException.class);
        thrown.expectMessage("[02032] An unknown exception was raised in flags strategy customStrategy while determining flags for data store");
        fixture.getUserDefinedFlags(mappings);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserDefinedFlags_CustomOverrideNullResult() {
        Targetcolumn column = mock(Targetcolumn.class);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        DataStore dataStore = mock(DataStore.class);
        MappingsExtension mappingsExtension = new MappingsExtension();
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        TargetColumnExtension columnextension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(columnextension);
        when(column.getParent()).thenReturn(mappings);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn("KM_NAME");
        when(mappings.getIkm()).thenReturn(kmType);

        when(defaultStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(
                customStrategy.getTargetColumnFlags(
                        any(TargetColumnFlags.class),
                        any(FlagsDataStoreExecutionContext.class),
                        any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(customStrategy.getUserDefinedFlags(any(Set.class), any(FlagsDataStoreExecutionContext.class), any(UDFlagsTargetColumnExecutionContext.class)))
                .thenReturn(null);

        thrown.expect(IncorrectCustomStrategyException.class);
        thrown.expectMessage("Custom flags strategy customStrategy must return non-empty interface one.jodi.core.extensions.types.TargetColumnFlags object");
        fixture.getUserDefinedFlags(mappings);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserDefinedFlags_WithCustomTargetColumnException() {
        Targetcolumn column = mock(Targetcolumn.class);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        DataStore dataStore = mock(DataStore.class);
        MappingsExtension mappingsExtension = new MappingsExtension();
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        TargetColumnExtension columnextension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(columnextension);
        when(column.getParent()).thenReturn(mappings);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);
        KmType kmType = mock(KmType.class);
        when(kmType.getName()).thenReturn("KM_NAME");
        when(mappings.getIkm()).thenReturn(kmType);

        when(
                defaultStrategy.getTargetColumnFlags(
                        any(TargetColumnFlags.class),
                        any(FlagsDataStoreExecutionContext.class),
                        any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(
                customStrategy.getTargetColumnFlags(
                        any(TargetColumnFlags.class),
                        any(FlagsDataStoreExecutionContext.class),
                        any(FlagsTargetColumnExecutionContext.class)))
                .thenThrow(RuntimeException.class);

        thrown.expect(IncorrectCustomStrategyException.class);
        thrown.expectMessage("[02032] An unknown exception was raised in flags strategy customStrategy while determining flags for data store");
        fixture.getUserDefinedFlags(mappings);
    }


    @Test
    public void testGetUserDefinedFlags_CustomOverridNullTargetColumnResult() {
        Targetcolumn column = mock(Targetcolumn.class);
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = transformation.getMappings();
        KmType km = mock(KmType.class);
        when(mappings.getIkm()).thenReturn(km);
        when(km.getName()).thenReturn("km");
        DataStore dataStore = mock(DataStore.class);
        MappingsExtension mappingsExtension = new MappingsExtension();
        TransformationExtension transformationExtension = new TransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        TargetColumnExtension columnextension = new TargetColumnExtension();
        when(column.getExtension()).thenReturn(columnextension);
        when(column.getParent()).thenReturn(mappings);
        Map<String, DataStoreColumn> columnMd =
                InputModelMockHelper.createDataStoreColumns(
                        dataStore, new String[]{"colname", "col2", "col3"});
        when(dataStore.getColumns()).thenReturn(columnMd);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(dataStore);

        when(defaultStrategy.getTargetColumnFlags(
                any(TargetColumnFlags.class),
                any(FlagsDataStoreExecutionContext.class),
                any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(createInsertUpdateFlag(true, true, false, false));
        when(
                customStrategy.getTargetColumnFlags(
                        any(TargetColumnFlags.class),
                        any(FlagsDataStoreExecutionContext.class),
                        any(FlagsTargetColumnExecutionContext.class)))
                .thenReturn(null);

        thrown.expect(IncorrectCustomStrategyException.class);
        thrown.expectMessage("Custom flags strategy customStrategy must return non-empty interface one.jodi.core.extensions.types.TargetColumnFlags object");
        fixture.getUserDefinedFlags(mappings);
    }

    @Test
    public void testModelCreate() {

        InputModelMockHelper.createMockETLTransformation("", new String[]{"a"}, new String[]{"b"}, new String[]{"c"}, InputModelMockHelper.createMockETLMappings("one", "two", "three", 1));


        InputModelMockHelper.createMockETLTransformation();
    }
}
