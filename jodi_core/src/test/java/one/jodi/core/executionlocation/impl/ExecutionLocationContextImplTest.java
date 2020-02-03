package one.jodi.core.executionlocation.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.*;
import one.jodi.core.extensions.strategies.ExecutionLocationStrategy;
import one.jodi.core.extensions.strategies.IncorrectCustomStrategyException;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.*;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TargetColumnExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.hamcrest.collection.IsIn;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.After;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * The class <code>ExecutionLocationContextImplTest</code> contains tests for
 * the class <code>{@link ExecutionLocationContextImpl}</code>.
 */
@RunWith(JUnit4.class)
public class ExecutionLocationContextImplTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    JodiProperties properties;
    @Mock
    ExecutionLocationStrategy customStrategy;
    @Mock
    ExecutionLocationStrategy defaultStrategy;
    @Mock
    SchemaMetaDataProvider etlProvider;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    ExecutionLocationContextImpl fixture;
    @Mock
    ETLValidator validator;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new ExecutionLocationContextImpl(customStrategy,
                defaultStrategy, databaseMetadataService, validator,
                errorWarningMessages);
    }

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown() throws Exception {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetExecutionLocation_1() throws Exception {
        final int datasetIndex = 0;
        final String modelName = "MODEL_NAME";
        final String colName = "colname";
        final String targetDataStoreName = "TARGET_DATASTORE";
        Mappings mappings = mock(Mappings.class);
        Transformation transformation = mock(Transformation.class);
        Targetcolumn column = InputModelMockHelper.createMockETLTargetcolumn(
                colName, "expr1", "expr2", "expr3");
        final String datasetName = "ds1";
        final TransformationExtension transformationExtension = InputModelMockHelper
                .createMockTransformationExtension();
        final MappingsExtension mappingExtension = InputModelMockHelper
                .createMockMappingsExtension();
        final TargetColumnExtension columnExtension = InputModelMockHelper
                .createMockTargetColumnExtension();
        final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);
        final List<String> sqlExpressions = Arrays.asList("sql1", "sql2",
                "sql3");
        final String[] mdColumnList = new String[]{"colname"};
        final DataStore dataStore = mock(DataStore.class);
        final DataModel dataModel = mock(DataModel.class);

        when(dataStore.getDataModel()).thenReturn(dataModel);
        when(dataModel.getModelCode()).thenReturn(modelName);
        when(column.getMappingExpressions()).thenReturn(sqlExpressions);
        when(databaseMetadataService.getCoreProperties()).thenReturn(
                coreProperties);
        when(transformation.getMappings()).thenReturn(mappings);
        when(column.getParent()).thenReturn(mappings);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
        when(mappings.getExtension()).thenReturn(mappingExtension);
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(column.getExtension()).thenReturn(columnExtension);
        when(mappings.getModel()).thenReturn(modelName);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings))
                .thenReturn(dataStore);
        Map<String, DataStoreColumn> columnMd = InputModelMockHelper
                .createDataStoreColumns(dataStore, mdColumnList);
        when(dataStore.getColumns()).thenReturn(columnMd);
        List<Targetcolumn> colList = Collections
                .singletonList(InputModelMockHelper.createMockETLTargetcolumn(
                        "colname", "alias.colname"));
        when(mappings.getTargetColumns()).thenReturn(colList);
        for (Targetcolumn c : mappings.getTargetColumns()) {
            TargetColumnExtension ce = InputModelMockHelper
                    .createMockTargetColumnExtension();
            when(c.getExtension()).thenReturn(ce);
        }
        when(
                defaultStrategy
                        .getTargetColumnExecutionLocation(
                                any(ExecutionLocationType.class),
                                any(ExecutionLocationDataStoreExecutionContext.class),
                                any(ExecutionLocationTargetColumnExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        validateContextData(datasetIndex, colName, datasetName,
                                transformationExtension, mappingExtension,
                                columnExtension, coreProperties, mdColumnList,
                                dataStore, invocation);
                        return ExecutionLocationType.SOURCE;
                    }

                });
        when(
                customStrategy
                        .getTargetColumnExecutionLocation(
                                any(ExecutionLocationType.class),
                                any(ExecutionLocationDataStoreExecutionContext.class),
                                any(ExecutionLocationTargetColumnExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        validateContextData(datasetIndex, colName, datasetName,
                                transformationExtension, mappingExtension,
                                columnExtension, coreProperties, mdColumnList,
                                dataStore, invocation);
                        return ExecutionLocationType.WORK;
                    }

                });
        Map<String, ExecutionLocationType> result = fixture
                .getTargetColumnExecutionLocation(mappings, datasetName,
                        datasetIndex);

        assertNotNull(result);
        assertEquals(mdColumnList.length, result.size());
        for (String c : mdColumnList) {
            assertThat(result,
                    hasEntry(equalTo(c), equalTo(ExecutionLocationType.WORK)));
        }
        verify(defaultStrategy, times(mdColumnList.length))
                .getTargetColumnExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationDataStoreExecutionContext.class),
                        any(ExecutionLocationTargetColumnExecutionContext.class));
        verify(customStrategy, times(mdColumnList.length))
                .getTargetColumnExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationDataStoreExecutionContext.class),
                        any(ExecutionLocationTargetColumnExecutionContext.class));
        // verify(modelCodeContext).getModelCode(mappings);

        // TODO: REMOVE COMMENTED BELOW -
        // verify(databaseMetadataService).getTargetDataStoreInModel(mappings);
    }

    private ExecutionLocationType converteEnumType(
            ExecutionLocationtypeEnum enumValue) {
        ExecutionLocationType converted = null;
        switch (enumValue) {
            case SOURCE:
                converted = ExecutionLocationType.SOURCE;
                break;
            case WORK:
                converted = ExecutionLocationType.WORK;
                break;
            case TARGET:
                converted = ExecutionLocationType.TARGET;
                break;
        }
        return converted;
    }

    @SuppressWarnings("unchecked")
    private void testTemplateGetJoinExecutionLocation_internalmodel(
            final ExecutionLocationtypeEnum expectedExecLocType,
            final String alt_alias, final Class<? extends Throwable> exception,
            final String errorMessage,
            final ExecutionLocationType defaultLocation,
            final ExecutionLocationType customLocation) {
        final String modelName = "MODEL_NAME";
        final String targetDataStoreName = "TARGET_DATASTORE";
        Source source = InputModelMockHelper.createMockETLSource(
                "source_alias", "source", "model");
        Source s1 = mock(Source.class);
        Dataset ds = mock(Dataset.class);
        // Datasets dss = mock(Datasets.class);
        final TransformationExtension transformationExtension = InputModelMockHelper
                .createMockTransformationExtension();
        Transformation transformation = mock(Transformation.class);
        final SourceExtension sourceExtension = InputModelMockHelper
                .createMockSourceExtension();
        final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);
        Mappings mappings = mock(Mappings.class);
        final DataStore targetDataStore = mock(DataStore.class);
        final DataModel dataModel = mock(DataModel.class);
        when(targetDataStore.getDataModel()).thenReturn(dataModel);
        final DataStore sourceDataStore1 = mock(DataStore.class);
        final DataStoreWithAlias dataStore_wAlias1 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias1.getAlias()).thenReturn("source_alias");
        when(dataStore_wAlias1.getDataStore()).thenReturn(sourceDataStore1);
        final DataStore sourceDataStore2 = mock(DataStore.class);
        final DataStoreWithAlias dataStore_wAlias2 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias2.getDataStore()).thenReturn(sourceDataStore2);
        when(dataStore_wAlias2.getAlias()).thenReturn("s1");
        final String join = "source_alias.col1 = value and source_alias2.col2=s1.col1";

        // additional code for test case of two aliases for same data store
        Source source_alt = InputModelMockHelper.createMockETLSource(alt_alias,
                "source", "model");
        final DataStoreWithAlias dataStore_wAlias1_1 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias1_1.getDataStore()).thenReturn(sourceDataStore1);
        if (alt_alias != null) {
            when(dataStore_wAlias1_1.getAlias()).thenReturn(alt_alias);
        }

        // //////// Variation of Test Case
        when(source.getJoinExecutionLocation()).thenReturn(expectedExecLocType);

        // ////////
        when(s1.getName()).thenReturn("s1");
        when(s1.getAlias()).thenReturn("s1");
        when(source.getJoin()).thenReturn(join);
        if (alt_alias == null) {
            when(ds.getSources()).thenReturn(Arrays.asList(source, s1));
        } else {
            // alternate code for test case of two aliases for same data store
            when(ds.getSources()).thenReturn(
                    Arrays.asList(source, source_alt, s1));
        }
        when(transformation.getMappings()).thenReturn(mappings);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
        // when(modelCodeContext.getModelCode(mappings)).thenReturn(modelName);
        when(mappings.getModel()).thenReturn(modelName);
        // when(modelCodeContext.getModelCode(source)).thenReturn("source_model_name");
        when(source.getModel()).thenReturn("source_model_name");
        // when(modelCodeContext.getModelCode(s1)).thenReturn("s1_model_name");
        when(s1.getModel()).thenReturn("s1_model_name");
        when(databaseMetadataService.getCoreProperties()).thenReturn(
                coreProperties);
        // when(dss.getParent()).thenReturn(transformation);
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(source.getExtension()).thenReturn(sourceExtension);
        when(ds.getParent()).thenReturn(transformation);
        when(source.getParent()).thenReturn(ds);
        // when(dss.getModel()).thenReturn("model");
        // when(mappings.getModel()).thenReturn("model");
        when(source.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings))
                .thenReturn(targetDataStore);
        // when(modelCodeContext.getModelCode(source)).thenReturn("source_model_name");
        // when(modelCodeContext.getModelCode(s1)).thenReturn("s1_model_name");
        when(
                databaseMetadataService.getSourceDataStoreInModel("source",
                        "source_model_name")).thenReturn(sourceDataStore1);
        when(
                databaseMetadataService.getSourceDataStoreInModel("s1",
                        "s1_model_name")).thenReturn(sourceDataStore2);

        // additional code for test case of two aliases for same data store
        if (alt_alias != null) {
            // when(modelCodeContext.getModelCode(source_alt)).thenReturn("source_model_name");
            when(source_alt.getModel()).thenReturn("source_model_name");
            when(source_alt.getExtension()).thenReturn(null);
            when(source_alt.getParent()).thenReturn(ds);
        }

        when(
                defaultStrategy.getJoinExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationJoinExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];
                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            assertEquals(converteEnumType(expectedExecLocType),
                                    defaultExecLoc);
                        }
                        // ////////
                        DataStoreWithAlias[] ds;
                        // additional code for test case of two aliases for same
                        // data store
                        if (alt_alias == null) {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias2};
                        } else {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias1_1, dataStore_wAlias2};
                        }
                        validateContextDataForJoins(join, targetDataStore, ds,
                                transformationExtension, sourceExtension,
                                coreProperties, arguments);
                        return defaultLocation;
                    }

                });

        when(
                customStrategy.getJoinExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationJoinExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];
                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            assertEquals(defaultLocation, defaultExecLoc);
                        }
                        // ////////
                        DataStoreWithAlias[] ds;
                        // additional code for test case of two aliases for same
                        // data store
                        if (alt_alias == null) {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias2};
                        } else {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias1_1, dataStore_wAlias2};
                        }
                        validateContextDataForJoins(join, targetDataStore, ds,
                                transformationExtension, sourceExtension,
                                coreProperties, arguments);
                        return customLocation;
                    }

                });

        if (exception != null) {
            thrown.expect(exception);
            thrown.expectMessage(errorMessage);
        }
        ExecutionLocationType result = fixture.getJoinExecutionLocation(source);

        assertNotNull(result);
        assertEquals(result, ExecutionLocationType.SOURCE);
        verify(defaultStrategy).getJoinExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationJoinExecutionContext.class));
        verify(customStrategy).getJoinExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationJoinExecutionContext.class));

        verify(databaseMetadataService, times(2)).getTargetDataStoreInModel(
                mappings);
    }

    //@Test
    public void testGetJoinExecutionLocation_NULL_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(null, null, null,
                null, ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    //@Test
    public void testGetJoinExecutionLocation_SOURCE_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.SOURCE, null, null, null,
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    //@Test
    public void testGetJoinExecutionLocation_WORK_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.WORK, null, null, null,
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetJoinExecutionLocation_Multiple_Alias_One_Source_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.WORK, "source_alias2", null, null,
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetJoinExecutionLocation_TARGET_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.TARGET,
                null,
                RuntimeException.class,
                "The explcitly defined join execution location can only be values SOURCE or WORK.",
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetJoinExecutionLocation_NullDefaultLocation_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.SOURCE, null, AssertionError.class,
                "The join execution location must only return SOURCE or WORK.",
                null, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetJoinExecutionLocation_TargetDefaultLocation_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.SOURCE, null, AssertionError.class,
                "The join execution location must only return SOURCE or WORK.",
                ExecutionLocationType.TARGET, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetJoinExecutionLocation_NullCustomLocation_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.SOURCE,
                null,
                IncorrectCustomStrategyException.class,
                "The join execution location strategy customStrategy must only return values SOURCE or WORK.",
                ExecutionLocationType.SOURCE, null);
    }

    // @Test
    public void testGetJoinExecutionLocation_TargetCustomLocation_internalmodel() {
        testTemplateGetJoinExecutionLocation_internalmodel(
                ExecutionLocationtypeEnum.SOURCE,
                null,
                IncorrectCustomStrategyException.class,
                "The join execution location strategy customStrategy must only return values SOURCE or WORK.",
                ExecutionLocationType.SOURCE, ExecutionLocationType.TARGET);
    }

    @SuppressWarnings("unchecked")
    private void testTemplateGetLookupExecutionLocation(
            final ExecutionLocationtypeEnum expectedExecLocType,
            final Class<? extends Throwable> exception,
            final String errorMessage,
            final ExecutionLocationType defaultLocation,
            final ExecutionLocationType customLocation) {
        final String modelName = "MODEL_NAME";
        final String lookupModelName = "LOOKUP_MODEL_NAME";
        final String lookupDataStoreName = "LOOKUP_DATASTORE";
        final String mappingsDataStoreName = "MAPPINGS_DATASTORE";
        Lookup lookup = mock(Lookup.class);
        Source source = InputModelMockHelper.createMockETLSource(
                "source_alias", "source", "model");
        when(lookup.getParent()).thenReturn(source);
        Dataset ds = mock(Dataset.class);
        when(source.getParent()).thenReturn(ds);
        final TransformationExtension transformationExtension = InputModelMockHelper
                .createMockTransformationExtension();
        Transformation transformation = mock(Transformation.class);
        when(ds.getParent()).thenReturn(transformation);
        final SourceExtension sourceExtension = InputModelMockHelper
                .createMockSourceExtension();
        final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);
        Mappings mappings = mock(Mappings.class);
        final DataStore targetDataStore = mock(DataStore.class);
        final DataModel dataModel = mock(DataModel.class);
        when(targetDataStore.getDataModel()).thenReturn(dataModel);

        final DataStoreWithAlias sourceDataStore = mock(DataStoreWithAlias.class);
        final DataStore sourceDS = mock(DataStore.class);
        when(sourceDataStore.getDataStore()).thenReturn(sourceDS);
        when(sourceDataStore.getAlias()).thenReturn("source_alias");

        final DataStoreWithAlias lookupDataStore = mock(DataStoreWithAlias.class);
        final DataStore lookupDS = mock(DataStore.class);
        when(lookupDataStore.getDataStore()).thenReturn(lookupDS);
        when(lookupDataStore.getAlias()).thenReturn(lookupDataStoreName);

        // //////// Variation of Test Case
        when(lookup.getJoinExecutionLocation()).thenReturn(expectedExecLocType);
        // ////////
        when(lookup.getJoin()).thenReturn("lookup.join");
        when(lookup.getLookupType()).thenReturn(LookupTypeEnum.SCALAR);
        when(lookup.getLookupDataStore()).thenReturn(lookupDataStoreName);
        when(transformation.getMappings()).thenReturn(mappings);
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(source.getParent()).thenReturn(ds);
        when(source.getExtension()).thenReturn(sourceExtension);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn(mappingsDataStoreName);
        when(databaseMetadataService.getCoreProperties()).thenReturn(
                coreProperties);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings))
                .thenReturn(targetDataStore);
        when(
                databaseMetadataService.getSourceDataStoreInModel("source",
                        "source_model_name")).thenReturn(sourceDS);
        when(
                databaseMetadataService.getSourceDataStoreInModel(
                        lookupDataStoreName, lookupModelName)).thenReturn(
                lookupDS);
        when(source.getModel()).thenReturn("source_model_name");
        when(mappings.getModel()).thenReturn(modelName);
        when(lookup.getModel()).thenReturn(lookupModelName);

        when(
                defaultStrategy.getLookupExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationLookupExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];

                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            assertEquals(converteEnumType(expectedExecLocType),
                                    defaultExecLoc);
                        }
                        // ////////

                        ExecutionLocationLookupExecutionContext context = (ExecutionLocationLookupExecutionContext) arguments[1];

                        assertNotNull(context.getJoinCondition());
                        assertNotNull(context.getLookupDataStore());
                        assertNotNull(context.getLookupType());
                        assertNotNull(context.getProperties());
                        assertNotNull(context.getSourceDataStore());
                        assertNotNull(context.getTargetDataStore());
                        assertNotNull(context.getTransformationExtension());

                        assertEquals("lookup.join", context.getJoinCondition());
                        assertEquals(lookupDataStore.getAlias(), context
                                .getLookupDataStore().getAlias());
                        assertEquals(lookupDataStore.getDataStore(), context
                                .getLookupDataStore().getDataStore());
                        assertEquals(LookupTypeEnum.SCALAR,
                                context.getLookupType());
                        assertEquals(coreProperties, context.getProperties());
                        assertEquals(sourceDataStore.getAlias(), context
                                .getSourceDataStore().getAlias());
                        assertEquals(sourceDataStore.getDataStore(), context
                                .getSourceDataStore().getDataStore());
                        // assertThat(context.getSourceExtension(),SamePropertyValuesAs.<SourceExtension>
                        // samePropertyValuesAs(sourceExtension));
                        assertThat(
                                context.getSourceDataStore()
                                        .getSourceExtension(),
                                SamePropertyValuesAs
                                        .<SourceExtension>samePropertyValuesAs(sourceExtension));

                        assertEquals(targetDataStore,
                                context.getTargetDataStore());
                        assertThat(
                                context.getTransformationExtension(),
                                SamePropertyValuesAs
                                        .<TransformationExtension>samePropertyValuesAs(transformationExtension));

                        return defaultLocation;
                    }

                });

        when(
                customStrategy.getLookupExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationLookupExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];

                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            assertEquals(defaultLocation, defaultExecLoc);
                        }
                        // ////////
                        ExecutionLocationLookupExecutionContext context = (ExecutionLocationLookupExecutionContext) arguments[1];

                        assertNotNull(context.getJoinCondition());
                        assertNotNull(context.getLookupDataStore());
                        assertNotNull(context.getLookupType());
                        assertNotNull(context.getProperties());
                        assertNotNull(context.getSourceDataStore());
                        // assertNotNull(context.getSourceExtension());
                        assertNotNull(context.getSourceDataStore()
                                .getSourceExtension());
                        assertNotNull(context.getTargetDataStore());
                        assertNotNull(context.getTransformationExtension());

                        assertEquals("lookup.join", context.getJoinCondition());
                        assertEquals(lookupDataStore.getAlias(), context
                                .getLookupDataStore().getAlias());
                        assertEquals(lookupDataStore.getDataStore(), context
                                .getLookupDataStore().getDataStore());
                        assertEquals(LookupTypeEnum.SCALAR,
                                context.getLookupType());
                        assertEquals(coreProperties, context.getProperties());
                        assertEquals(sourceDataStore.getAlias(), context
                                .getSourceDataStore().getAlias());
                        assertEquals(sourceDataStore.getDataStore(), context
                                .getSourceDataStore().getDataStore());
                        // assertThat(context.getSourceExtension(),SamePropertyValuesAs.<SourceExtension>
                        // samePropertyValuesAs(sourceExtension));
                        assertThat(
                                context.getSourceDataStore()
                                        .getSourceExtension(),
                                SamePropertyValuesAs
                                        .<SourceExtension>samePropertyValuesAs(sourceExtension));
                        assertEquals(targetDataStore,
                                context.getTargetDataStore());
                        assertThat(
                                context.getTransformationExtension(),
                                SamePropertyValuesAs
                                        .<TransformationExtension>samePropertyValuesAs(transformationExtension));
                        // assertTrue(context.isSameModelInTransformation());
                        return customLocation;
                    }

                });

        if (exception != null) {
            thrown.expect(exception);
            thrown.expectMessage(errorMessage);
        }
        ExecutionLocationType result = fixture
                .getLookupExecutionLocation(lookup);

        assertNotNull(result);
        assertEquals(result, ExecutionLocationType.SOURCE);
        verify(defaultStrategy).getLookupExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationLookupExecutionContext.class));
        verify(customStrategy).getLookupExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationLookupExecutionContext.class));
        verify(source).getExtension();
        verify(transformation).getExtension();
        verify(databaseMetadataService, times(2)).getTargetDataStoreInModel(
                mappings);
        verify(databaseMetadataService).getSourceDataStoreInModel("source",
                "source_model_name");
        verify(databaseMetadataService).getSourceDataStoreInModel(
                lookupDataStoreName, lookupModelName);
        verify(databaseMetadataService).getCoreProperties();
    }

    /*
     * testTemplateGetLookupExecutionLocation( final ExecutionLocationtypeEnum
     * expectedExecLocType, final Class<? extends Throwable> exception, final
     * String errorMessage, final ExecutionLocationType defaultLocation, final
     * ExecutionLocationType customLocation)
     */
    // @Test
    public void testGetLookupExecutionLocation_SOURCE() {
        testTemplateGetLookupExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE, null, null,
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetLookupExecutionLocation_WORK() {
        testTemplateGetLookupExecutionLocation(ExecutionLocationtypeEnum.WORK,
                null, null, ExecutionLocationType.WORK,
                ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetLookupExecutionLocation_TARGET() {
        testTemplateGetLookupExecutionLocation(
                ExecutionLocationtypeEnum.TARGET,
                RuntimeException.class,
                "The explcitly defined lookup execution location can only be values SOURCE or WORK.",
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetLookupExecutionLocation_NULL() {
        testTemplateGetLookupExecutionLocation(null, null, null,
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    @Test
    public void testGetLookupExecutionLocation_NullDefaultLocation() {

        testTemplateGetLookupExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                RuntimeException.class,
                "[03180] The explicitly defined lookup execution location can only be values SOURCE or WORK.",
                null, ExecutionLocationType.SOURCE);
    }

    @Test
    public void testGetLookupExecutionLocation_TargetDefaultLocation() {
        testTemplateGetLookupExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                RuntimeException.class,
                "[03180] The explicitly defined lookup execution location can only be values SOURCE or WORK.",
                ExecutionLocationType.TARGET, ExecutionLocationType.SOURCE);
    }

    @Test
    public void testGetLookupExecutionLocation_NullCustomLocation() {
        testTemplateGetLookupExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                RuntimeException.class,
                "[03180] The explicitly defined lookup execution location can only be values SOURCE or WORK.",
                ExecutionLocationType.SOURCE, null);
    }

    @Test
    public void testGetLookupExecutionLocation_TargetCustomLocation() {
        testTemplateGetLookupExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                RuntimeException.class,
                "[03180] The explicitly defined lookup execution location can only be values SOURCE or WORK.",
                ExecutionLocationType.SOURCE, ExecutionLocationType.TARGET);
    }

    // //
    // TODO - ideally method is merged with testTemplateGetJoinExecutionLocation
    // since the overall
    // logic flow is exactly the same. The main differences are attributed to
    // the different
    // context execution context objects used for filter vs. join scenario and
    // the different
    // method calls.
    @SuppressWarnings("unchecked")
    private void testTemplateGetFilterExecutionLocation(
            final ExecutionLocationtypeEnum expectedExecLocType,
            final String alt_alias, final Class<? extends Throwable> exception,
            final String errorMessage,
            final ExecutionLocationType defaultLocation,
            final ExecutionLocationType customLocation) {
        final String modelName = "MODEL_NAME";
        final String targetDataStoreName = "TARGET_DATASTORE";
        Source source = InputModelMockHelper.createMockETLSource(
                "source_alias", "source", "model");
        Source s1 = mock(Source.class);
        Dataset ds = mock(Dataset.class);
        Transformation transformation = mock(Transformation.class);
        final TransformationExtension transformationExtension = InputModelMockHelper
                .createMockTransformationExtension();
        final SourceExtension sourceExtension = InputModelMockHelper
                .createMockSourceExtension();
        final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);
        Mappings mappings = mock(Mappings.class);
        final DataStore targetDataStore = mock(DataStore.class);
        final DataModel dataModel = mock(DataModel.class);
        when(targetDataStore.getDataModel()).thenReturn(dataModel);
        final DataStore sourceDataStore1 = mock(DataStore.class);
        final DataStoreWithAlias dataStore_wAlias1 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias1.getAlias()).thenReturn("source_alias");
        when(dataStore_wAlias1.getDataStore()).thenReturn(sourceDataStore1);
        final DataStore sourceDataStore2 = mock(DataStore.class);
        final DataStoreWithAlias dataStore_wAlias2 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias2.getDataStore()).thenReturn(sourceDataStore2);
        when(dataStore_wAlias2.getAlias()).thenReturn("s1");

        final String filter = "source_alias.col1 = value and source_alias2.col2=s1.col1";

        // additional code for test case of two aliases for same data store
        Source source_alt = InputModelMockHelper.createMockETLSource(alt_alias,
                "source", "model");
        final DataStoreWithAlias dataStore_wAlias1_1 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias1_1.getDataStore()).thenReturn(sourceDataStore1);
        if (alt_alias != null) {
            when(dataStore_wAlias1_1.getAlias()).thenReturn(alt_alias);
        }

        // //////// Variation of Test Case
        when(source.getFilterExecutionLocation()).thenReturn(
                expectedExecLocType);
        // ////////
        when(s1.getName()).thenReturn("s1");
        when(s1.getAlias()).thenReturn("s1");
        when(source.getFilter()).thenReturn(filter);
        if (alt_alias == null) {
            when(ds.getSources()).thenReturn(Arrays.asList(source, s1));
        } else {
            // alternate code for test case of two aliases for same data store
            when(ds.getSources()).thenReturn(
                    Arrays.asList(source, source_alt, s1));
        }
        when(transformation.getMappings()).thenReturn(mappings);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
        when(mappings.getModel()).thenReturn(modelName);
        when(source.getModel()).thenReturn("source_model_name");
        when(s1.getModel()).thenReturn("s1_model_name");

        when(databaseMetadataService.getCoreProperties()).thenReturn(
                coreProperties);
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(source.getExtension()).thenReturn(sourceExtension);
        when(ds.getParent()).thenReturn(transformation);
        when(source.getParent()).thenReturn(ds);
        // when(source.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings))
                .thenReturn(targetDataStore);
        when(source.getModel()).thenReturn("source_model_name");
        when(s1.getModel()).thenReturn("s1_model_name");
        when(
                databaseMetadataService.getSourceDataStoreInModel("source",
                        "source_model_name")).thenReturn(sourceDataStore1);
        when(
                databaseMetadataService.getSourceDataStoreInModel("s1",
                        "s1_model_name")).thenReturn(sourceDataStore2);

        // additional code for test case of two aliases for same data store
        if (alt_alias != null) {
            when(source_alt.getModel()).thenReturn("source_model_name");
            when(source_alt.getExtension()).thenReturn(null);
            when(source_alt.getParent()).thenReturn(ds);
        }

        when(
                defaultStrategy.getFilterExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationFilterExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];
                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            assertEquals(converteEnumType(expectedExecLocType),
                                    defaultExecLoc);
                        }
                        // ////////
                        DataStoreWithAlias[] ds;
                        // additional code for test case of two aliases for same
                        // data store
                        if (alt_alias == null) {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias2};
                        } else {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias1_1, dataStore_wAlias2};
                        }
                        validateContextDataForFilter(filter, targetDataStore,
                                ds, transformationExtension, sourceExtension,
                                coreProperties, arguments);
                        return defaultLocation;
                    }

                });

        when(
                customStrategy.getFilterExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationFilterExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];
                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            // assertEquals(ExecutionLocationType.WORK,
                            // defaultExecLoc);
                            assertEquals(defaultLocation, defaultExecLoc);
                        }
                        // ////////
                        DataStoreWithAlias[] ds;
                        // additional code for test case of two aliases for same
                        // data store
                        if (alt_alias == null) {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias2};
                        } else {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias1_1, dataStore_wAlias2};
                        }
                        validateContextDataForFilter(filter, targetDataStore,
                                ds, transformationExtension, sourceExtension,
                                coreProperties, arguments);
                        return customLocation;
                    }

                });
        if (exception != null) {
            thrown.expect(exception);
            thrown.expectMessage(errorMessage);
        }
        ExecutionLocationType result = fixture
                .getFilterExecutionLocation(source);

        assertNotNull(result);
        assertEquals(result, ExecutionLocationType.SOURCE);
        verify(defaultStrategy).getFilterExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationFilterExecutionContext.class));
        verify(customStrategy).getFilterExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationFilterExecutionContext.class));

        verify(databaseMetadataService, atLeastOnce())
                .getTargetDataStoreInModel(mappings);
    }

    // @Test
    public void testGetFilterExecutionLocation_SOURCE() {
        testTemplateGetFilterExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE, null, null, null,
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetFilterExecutionLocation_WORK() {
        testTemplateGetFilterExecutionLocation(ExecutionLocationtypeEnum.WORK,
                null, null, null, ExecutionLocationType.WORK,
                ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetFilterExecutionLocation_Multiple_Alias_One_Source() {
        testTemplateGetFilterExecutionLocation(ExecutionLocationtypeEnum.WORK,
                "source_alias2", null, null, ExecutionLocationType.WORK,
                ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetFilterExecutionLocation_TARGET() {
        testTemplateGetFilterExecutionLocation(
                ExecutionLocationtypeEnum.TARGET,
                null,
                RuntimeException.class,
                "The explcitly defined filter execution location can only be values SOURCE or WORK.",
                ExecutionLocationType.WORK, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetFilterExecutionLocation_NullDefaultLocation() {
        testTemplateGetFilterExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                null,
                AssertionError.class,
                "The filter execution location must only return SOURCE or WORK.",
                null, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetFilterExecutionLocation_TargetDefaultLocation() {
        testTemplateGetFilterExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                null,
                AssertionError.class,
                "The filter execution location must only return SOURCE or WORK.",
                ExecutionLocationType.TARGET, ExecutionLocationType.SOURCE);
    }

    // @Test
    public void testGetFilterExecutionLocation_NullCustomLocation() {
        testTemplateGetFilterExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                null,
                IncorrectCustomStrategyException.class,
                "The filter execution location strategy customStrategy must only return values SOURCE or WORK.",
                ExecutionLocationType.SOURCE, null);
    }

    // @Test
    public void testGetFilterExecutionLocation_TargetCustomLocation() {
        testTemplateGetFilterExecutionLocation(
                ExecutionLocationtypeEnum.SOURCE,
                null,
                IncorrectCustomStrategyException.class,
                "The filter execution location strategy customStrategy must only return values SOURCE or WORK.",
                ExecutionLocationType.SOURCE, ExecutionLocationType.TARGET);
    }

    /**
     * Run the ExecutionLocationType
     * getExecutionLocation(Targetcolumn,String,int,ETLTransformation) method
     * test.
     *
     * @throws Exception
     */
    /*
     * //@Test public void testGetExecutionLocation_MultipleColumns() throws
     * Exception {
     *
     * final String modelName = "MODEL_NAME"; final String targetDataStoreName =
     * "TARGET_DATASTORE";
     *
     * ETLTransformation etlTransformation = mock(ETLTransformation.class);
     * Targetcolumn column =
     * InputModelMockHelper.createMockTargetcolumn("colname", "expr1", "expr2",
     * "expr3"); String datasetName = "ds1"; int dsindex = 1;
     * TransformationExtension transformationExtension = new
     * TransformationExtension(); MappingsExtension mappingExtension = new
     * MappingsExtension(); TargetColumnExtension columnExtension = new
     * TargetColumnExtension(); Transformation transformation =
     * mock(Transformation.class); final String[] mdColumnList = new
     * String[]{"colname", "col2", "col3"}; final DataStore dataStore =
     * mock(DataStore.class);
     * when(dataStore.getModelCode()).thenReturn(modelName);
     *
     * Mappings mappings = mock(Mappings.class);
     * when(column.getParent()).thenReturn(mappings);
     * when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
     * when(mappings.getParent()).thenReturn(transformation);
     * when(mappings.getExtension()).thenReturn(mappingExtension);
     * when(transformation.getExtension()).thenReturn(transformationExtension);
     * when(column.getExtension()).thenReturn(columnExtension);
     * when(transformation.getMappings()).thenReturn(mappings);
     *
     * when(modelCodeContext.getModelCode(mappings)).thenReturn(modelName);
     * when(databaseMetadataService.getTargetDataStoreInModel(mappings,
     * modelName)).thenReturn(dataStore);
     *
     * when(customStrategy.getTargetColumnExecutionLocation(any(
     * ExecutionLocationType.class),
     * any(ExecutionLocationDataStoreExecutionContext.class),
     * any(ExecutionLocationTargetColumnExecutionContext
     * .class))).thenReturn(ExecutionLocationType.TARGET); Map<String,
     * DataStoreColumn> columnMd =
     * InputModelMockHelper.createDataStoreColumns(mdColumnList);
     * when(dataStore.getColumns()).thenReturn(columnMd); for (Targetcolumn c :
     * mappings.getTargetColumn()) { TargetColumnExtension ce =
     * InputModelMockHelper.createMockTargetColumnExtension();
     * when(c.getExtension()).thenReturn(ce); }
     *
     * Map<String, ExecutionLocationType> result =
     * fixture.getTargetColumnExecutionLocation(mappings, datasetName, dsindex);
     * assertNotNull(result); assertEquals(mdColumnList.length, result.size());
     * for (String c : mdColumnList) { assertThat(result, hasEntry(equalTo(c),
     * equalTo(ExecutionLocationType.TARGET))); } verify(defaultStrategy,
     * times(mdColumnList
     * .length)).getTargetColumnExecutionLocation(any(ExecutionLocationType
     * .class), any(ExecutionLocationDataStoreExecutionContext.class),
     * any(ExecutionLocationTargetColumnExecutionContext.class));
     * verify(customStrategy,
     * times(mdColumnList.length)).getTargetColumnExecutionLocation
     * (any(ExecutionLocationType.class),
     * any(ExecutionLocationDataStoreExecutionContext.class),
     * any(ExecutionLocationTargetColumnExecutionContext.class)); }
     */
    private void validateContextData(final int datasetIndex,
                                     final String colName, final String datasetName,
                                     final TransformationExtension transformationExtension,
                                     final MappingsExtension mappingExtension,
                                     final TargetColumnExtension columnExtension,
                                     final Map<String, PropertyValueHolder> coreProperties,
                                     final String[] mdColumnList, final DataStore dataStore,
                                     InvocationOnMock invocation) {

        Object[] arguments = invocation.getArguments();
        ExecutionLocationDataStoreExecutionContext dsContext = (ExecutionLocationDataStoreExecutionContext) arguments[1];
        ExecutionLocationTargetColumnExecutionContext tcContext = (ExecutionLocationTargetColumnExecutionContext) arguments[2];
        assertEquals(dsContext.getDataSetIndex(), datasetIndex);
        assertEquals(dsContext.getDataSetName(), datasetName);
        assertNotNull(dsContext.getMappingsExtension());
        assertEquals(dsContext.getProperties(), coreProperties);
        assertNotNull(tcContext.getSqlExpressions());
        assertNotNull(tcContext.getTargetColumnName());
        if (tcContext.isExplicitlyMapped()) {
            assertEquals(tcContext.getSqlExpressions().size(), 1);// sqlExpressions.size());
            assertThat(tcContext.getSqlExpressions(),
                    IsIterableContainingInAnyOrder
                            .containsInAnyOrder("alias.colname"));
            assertNotNull(tcContext.getTargetColumnExtension());
            assertEquals(tcContext.getTargetColumnName(), colName);
            assertNotNull(tcContext.getTargetColumnExtension());
            assertNotNull(columnExtension);
            assertThat(
                    tcContext.getTargetColumnExtension(),
                    SamePropertyValuesAs
                            .<TargetColumnExtension>samePropertyValuesAs(columnExtension));
        } else {
            assertEquals(tcContext.getSqlExpressions().size(), 1);
            assertThat(tcContext.getSqlExpressions(),
                    IsIterableContainingInAnyOrder
                            .containsInAnyOrder("mockAlias."
                                    + tcContext.getTargetColumnName()));
            // assertNull(tcContext.getTargetColumnExtension());
            assertThat(tcContext.getTargetColumnName(), IsIn.isIn(mdColumnList));
        }
        assertEquals(dsContext.getTargetDataStore(), dataStore);
        assertNotNull(dsContext.getTransformationExtension());

        assertThat(
                dsContext.getTransformationExtension(),
                SamePropertyValuesAs
                        .<TransformationExtension>samePropertyValuesAs(transformationExtension));
        assertThat(
                dsContext.getMappingsExtension(),
                SamePropertyValuesAs
                        .<MappingsExtension>samePropertyValuesAs(mappingExtension));

    }

    private void validateContextDataForJoins(final String join,
                                             final DataStore targetDataStore,
                                             DataStoreWithAlias[] expectedDataStores,
                                             final TransformationExtension transformationExtension,
                                             final SourceExtension sourceExtension,
                                             final Map<String, PropertyValueHolder> coreProperties,
                                             Object[] arguments) {
        ExecutionLocationJoinExecutionContext executionContext = (ExecutionLocationJoinExecutionContext) arguments[1];
        assertNotNull(executionContext.getJoinedDataStores());
        assertNotNull(executionContext.getJoinCondition());
        assertNotNull(executionContext.getJoinType());
        assertNotNull(executionContext.getProperties());
        assertNotNull(executionContext.getSourceExtension());
        assertNotNull(executionContext.getTargetDataStore());
        assertNotNull(executionContext.getTransformationExtension());

        assertEquals(executionContext.getProperties(), coreProperties);
        assertEquals(expectedDataStores.length, executionContext
                .getJoinedDataStores().size());
        List<DataStoreWithAlias> returnedDataStores = executionContext
                .getJoinedDataStores();
        assertEquals(expectedDataStores.length,
                isMatch(expectedDataStores, returnedDataStores));
        assertEquals(executionContext.getJoinCondition(), join);
        assertEquals(executionContext.getJoinType(), JoinTypeEnum.INNER);
        assertEquals(executionContext.getTargetDataStore(), targetDataStore);

        assertThat(
                executionContext.getTransformationExtension(),
                SamePropertyValuesAs
                        .<TransformationExtension>samePropertyValuesAs(transformationExtension));
        assertThat(
                executionContext.getSourceExtension(),
                SamePropertyValuesAs
                        .<SourceExtension>samePropertyValuesAs(sourceExtension));
    }

    private void validateContextDataForFilter(final String filter,
                                              final DataStore targetDataStore,
                                              DataStoreWithAlias[] expectedDataStores,
                                              final TransformationExtension transformationExtension,
                                              final SourceExtension sourceExtension,
                                              final Map<String, PropertyValueHolder> coreProperties,
                                              Object[] arguments) {
        ExecutionLocationFilterExecutionContext executionContext = (ExecutionLocationFilterExecutionContext) arguments[1];
        assertNotNull(executionContext.getFilteredDataStores());
        assertNotNull(executionContext.getFilterCondition());
        assertNotNull(executionContext.getProperties());
        assertNotNull(executionContext.getSourceExtension());
        assertNotNull(executionContext.getTargetDataStore());
        assertNotNull(executionContext.getTransformationExtension());
        assertNotNull(executionContext.getFilteredDataStores());

        assertEquals(executionContext.getProperties(), coreProperties);
        assertEquals(expectedDataStores.length, executionContext
                .getFilteredDataStores().size());
        List<DataStoreWithAlias> returnedDataStores = executionContext
                .getFilteredDataStores();
        assertEquals(expectedDataStores.length,
                isMatch(expectedDataStores, returnedDataStores));
        assertEquals(executionContext.getFilterCondition(), filter);
        assertEquals(executionContext.getTargetDataStore(), targetDataStore);

        assertThat(
                executionContext.getTransformationExtension(),
                SamePropertyValuesAs
                        .<TransformationExtension>samePropertyValuesAs(transformationExtension));
        assertThat(
                executionContext.getSourceExtension(),
                SamePropertyValuesAs
                        .<SourceExtension>samePropertyValuesAs(sourceExtension));
        // assertTrue(executionContext.isSameModelInTransformation());
    }

    private int isMatch(DataStoreWithAlias[] expectedDataStores,
                        List<DataStoreWithAlias> returnedDataStores) {
        int matches = 0;
        for (DataStoreWithAlias expected : expectedDataStores) {
            for (DataStoreWithAlias filtered : returnedDataStores) {
                System.out.println(expected.getAlias() + " <> "
                        + filtered.getAlias());
                System.out.println(expected.getDataStore() + " <> "
                        + filtered.getDataStore());
                if (expected.getAlias().equals(filtered.getAlias())
                        && expected.getDataStore().equals(
                        filtered.getDataStore())) {
                    matches++;
                    break;
                }
            }
        }
        return matches;
    }

    @SuppressWarnings({"unchecked", "unused"})
    private void testTemplateGetTargetExecutionLocation(
            final ExecutionLocationtypeEnum expectedExecLocType,
            final String alt_alias, final Class<? extends Throwable> exception,
            final String errorMessage,
            final ExecutionLocationType defaultLocation,
            final ExecutionLocationType customLocation) {
        final String modelName = "MODEL_NAME";
        final String targetDataStoreName = "TARGET_DATASTORE";
        Source source = InputModelMockHelper.createMockETLSource(
                "source_alias", "source", "model");
        Source s1 = mock(Source.class);
        Dataset ds = mock(Dataset.class);
        Transformation transformation = mock(Transformation.class);
        final TransformationExtension transformationExtension = InputModelMockHelper
                .createMockTransformationExtension();
        final SourceExtension sourceExtension = InputModelMockHelper
                .createMockSourceExtension();
        final Map<String, PropertyValueHolder> coreProperties = mock(Map.class);
        Mappings mappings = mock(Mappings.class);
        final DataStore targetDataStore = mock(DataStore.class);
        final DataStore sourceDataStore1 = mock(DataStore.class);
        final DataStoreWithAlias dataStore_wAlias1 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias1.getAlias()).thenReturn("source_alias");
        when(dataStore_wAlias1.getDataStore()).thenReturn(sourceDataStore1);
        final DataStore sourceDataStore2 = mock(DataStore.class);
        final DataStoreWithAlias dataStore_wAlias2 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias2.getDataStore()).thenReturn(sourceDataStore2);
        when(dataStore_wAlias2.getAlias()).thenReturn("s1");

        final String filter = "source_alias.col1 = value and source_alias2.col2=s1.col1";

        // additional code for test case of two aliases for same data store
        Source source_alt = InputModelMockHelper.createMockETLSource(alt_alias,
                "source", "model");
        final DataStoreWithAlias dataStore_wAlias1_1 = mock(DataStoreWithAlias.class);
        when(dataStore_wAlias1_1.getDataStore()).thenReturn(sourceDataStore1);
        if (alt_alias != null) {
            when(dataStore_wAlias1_1.getAlias()).thenReturn(alt_alias);
        }

        // //////// Variation of Test Case
        when(source.getFilterExecutionLocation()).thenReturn(
                expectedExecLocType);
        // ////////
        when(s1.getName()).thenReturn("s1");
        when(s1.getAlias()).thenReturn("s1");
        when(source.getFilter()).thenReturn(filter);
        if (alt_alias == null) {
            when(ds.getSources()).thenReturn(Arrays.asList(source, s1));
        } else {
            // alternate code for test case of two aliases for same data store
            when(ds.getSources()).thenReturn(
                    Arrays.asList(source, source_alt, s1));
        }
        when(transformation.getMappings()).thenReturn(mappings);
        when(mappings.getParent()).thenReturn(transformation);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStoreName);
        when(mappings.getModel()).thenReturn(modelName);
        when(source.getModel()).thenReturn("source_model_name");
        when(s1.getModel()).thenReturn("s1_model_name");

        when(databaseMetadataService.getCoreProperties()).thenReturn(
                coreProperties);
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(source.getExtension()).thenReturn(sourceExtension);
        when(ds.getParent()).thenReturn(transformation);
        when(source.getParent()).thenReturn(ds);
        // when(source.getJoinType()).thenReturn(JoinTypeEnum.INNER);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings))
                .thenReturn(targetDataStore);
        when(source.getModel()).thenReturn("source_model_name");
        when(s1.getModel()).thenReturn("s1_model_name");
        when(
                databaseMetadataService.getSourceDataStoreInModel("source",
                        "source_model_name")).thenReturn(sourceDataStore1);
        when(
                databaseMetadataService.getSourceDataStoreInModel("s1",
                        "s1_model_name")).thenReturn(sourceDataStore2);

        // additional code for test case of two aliases for same data store
        if (alt_alias != null) {
            when(source_alt.getModel()).thenReturn("source_model_name");
            when(source_alt.getExtension()).thenReturn(null);
            when(source_alt.getParent()).thenReturn(ds);
        }

        when(
                defaultStrategy.getFilterExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationFilterExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];
                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            assertEquals(converteEnumType(expectedExecLocType),
                                    defaultExecLoc);
                        }
                        // ////////
                        DataStoreWithAlias[] ds;
                        // additional code for test case of two aliases for same
                        // data store
                        if (alt_alias == null) {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias2};
                        } else {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias1_1, dataStore_wAlias2};
                        }
                        validateContextDataForFilter(filter, targetDataStore,
                                ds, transformationExtension, sourceExtension,
                                coreProperties, arguments);
                        return defaultLocation;
                    }

                });

        when(
                customStrategy.getFilterExecutionLocation(
                        any(ExecutionLocationType.class),
                        any(ExecutionLocationFilterExecutionContext.class)))
                .thenAnswer(new Answer<ExecutionLocationType>() {

                    @Override
                    public ExecutionLocationType answer(
                            InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        ExecutionLocationType defaultExecLoc = (ExecutionLocationType) arguments[0];
                        // //////// Variation of Test Case
                        if (expectedExecLocType != null) {
                            // assertEquals(ExecutionLocationType.WORK,
                            // defaultExecLoc);
                            assertEquals(defaultLocation, defaultExecLoc);
                        }
                        // ////////
                        DataStoreWithAlias[] ds;
                        // additional code for test case of two aliases for same
                        // data store
                        if (alt_alias == null) {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias2};
                        } else {
                            ds = new DataStoreWithAlias[]{dataStore_wAlias1,
                                    dataStore_wAlias1_1, dataStore_wAlias2};
                        }
                        validateContextDataForFilter(filter, targetDataStore,
                                ds, transformationExtension, sourceExtension,
                                coreProperties, arguments);
                        return customLocation;
                    }

                });
        if (exception != null) {
            thrown.expect(exception);
            thrown.expectMessage(errorMessage);
        }
        ExecutionLocationType result = fixture
                .getFilterExecutionLocation(source);

        assertNotNull(result);
        assertEquals(result, ExecutionLocationType.SOURCE);
        verify(defaultStrategy).getFilterExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationFilterExecutionContext.class));
        verify(customStrategy).getFilterExecutionLocation(
                any(ExecutionLocationType.class),
                any(ExecutionLocationFilterExecutionContext.class));

        verify(databaseMetadataService, atLeastOnce())
                .getTargetDataStoreInModel(mappings);
    }


}