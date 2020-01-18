package one.jodi.core.automapping.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.ColumnMappingStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.*;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


/**
 * The class <code>ColumnMappingContextImplTest</code> validates that the Column Mapping plugin
 * creates and correctly populates the execution context object for consumption by strategies
 * and that the strategy results are correctly returned.
 *
 */

@RunWith(JUnit4.class)
public class ColumnMappingContextImplTest {
    ColumnMappingContextImpl fixture = null;

    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    JodiProperties jodiProperties;
    @Mock
    ColumnMappingStrategy defaultStrategy;
    @Mock
    ColumnMappingStrategy optionalStrategy;
    @Mock
    ETLValidator validator;

    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private DataStoreWithAlias createMockDataStoreWithAlias(String name, final String alias, final Boolean isSource, final SourceExtension sourceExtension, String... columnNames) {
        final DataStore ds = createMockDataStore(name, columnNames);
        DataStoreWithAlias dsa = new DataStoreWithAlias() {
            @Override
            public String getAlias() {
                return alias;
            }

            @Override
            public DataStore getDataStore() {
                return ds;
            }

            @Override
            public Type getType() {
                return isSource ? DataStoreWithAlias.Type.Source : DataStoreWithAlias.Type.Lookup;
            }

            @Override
            public SourceExtension getSourceExtension() {
                return sourceExtension;
            }
        };

        return dsa;
    }

    private DataStore createMockDataStore(String name, String... columnNames) {
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn(name);
        HashMap<String, DataStoreColumn> dataStoreColumns = new HashMap<String, DataStoreColumn>();
        for (String columnName : columnNames) {
            DataStoreColumn dsc = createMockDataStoreColumn(columnName);
            dataStoreColumns.put(columnName, dsc);
        }
        when(dataStore.getColumns()).thenReturn(dataStoreColumns);

        return dataStore;
    }

    private DataStoreColumn createMockDataStoreColumn(String name) {
        DataStoreColumn dsc = mock(DataStoreColumn.class);
        when(dsc.getName()).thenReturn(name);
        return dsc;
    }

    @SuppressWarnings("unused")
    private Transformation createMockTransformation(Mappings mappings, Dataset... datasets) {
        Transformation t = mock(Transformation.class);
        ArrayList<Dataset> list = new ArrayList<Dataset>();
        for (Dataset ds : datasets) {
            list.add(ds);
        }
        when(t.getDatasets()).thenReturn(list);
        when(t.getMappings()).thenReturn(mappings);

        return t;
    }


    private Mappings createMockMappings(String model, List<Targetcolumn> list) {
        Mappings mappings = mock(Mappings.class);
        when(mappings.getModel()).thenReturn(model);
        when(mappings.getTargetColumns()).thenReturn(list);
        return mappings;
    }

    private List<Targetcolumn> createMockTargetColumns(String... names) {
        ArrayList<Targetcolumn> list = new ArrayList<Targetcolumn>();
        for (String name : names) {
            Targetcolumn tc = mock(Targetcolumn.class);
            when(tc.getName()).thenReturn(name);
            list.add(tc);
        }

        return list;
    }

    // Test two dataset each with single source.  Make sure size of expression list is two and mapped correctly.
    @Test
    public void test_multiple_datasets() {
        String sourceName1 = "sourceName1";
        String sourceName2 = "sourceName2";
        String sourceAlias1 = "sourceAlias1";
        String sourceAlias2 = "sourceAlias2";
        String sourceModel1 = "sourceModel1";
        String sourceModel2 = "sourceModel2";
        final DataStore targetDataStore = createMockDataStore("target", "c1", "c2", "c3", "c4");
        final SourceExtension sourceExtension1 = InputModelMockHelper.createMockSourceExtension();
        final SourceExtension sourceExtension2 = InputModelMockHelper.createMockSourceExtension();
        final DataStoreWithAlias sourceDataStore1 = createMockDataStoreWithAlias(sourceName1, sourceAlias1, true, sourceExtension1, "c1", "c2");
        final DataStoreWithAlias sourceDataStore2 = createMockDataStoreWithAlias(sourceName1, sourceAlias1, true, sourceExtension2, "c1", "c2");
        List<Targetcolumn> targetcolumns = createMockTargetColumns("c1", "c2", "c3", "c4");
        Mappings mappings = createMockMappings("model", targetcolumns);
        final MappingsExtension mappingsExtension = InputModelMockHelper.createMockMappingsExtension();
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        Transformation transformation = mock(Transformation.class);
        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(transformation.getMappings()).thenReturn(mappings);

        //Create two datasets each with a single source
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{sourceAlias1, sourceAlias2}, new String[]{sourceName1, sourceName2}, new String[]{sourceModel1, sourceModel2});
        when(transformation.getDatasets()).thenReturn(datasets);
        when(databaseMetadataService.getSourceDataStoreInModel("sourceName1", "sourceModel1")).thenReturn(sourceDataStore1.getDataStore());
        when(databaseMetadataService.getSourceDataStoreInModel("sourceName2", "sourceModel2")).thenReturn(sourceDataStore2.getDataStore());

        when(datasets.get(0).getSources().get(0).getExtension()).thenReturn(sourceExtension1);
        when(datasets.get(0).getSources().get(0).getExtension()).thenReturn(sourceExtension2);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);

        ColumnMappingStrategy defaultStrategy = mock(ColumnMappingStrategy.class);
        when(defaultStrategy.getMappingExpression(anyString(), any(ColumnMappingExecutionContext.class), any(TargetColumnExecutionContext.class))).thenReturn("mappingExpression");
        ColumnMappingStrategy customStrategy = mock(ColumnMappingStrategy.class);
        when(customStrategy.getMappingExpression(anyString(), any(ColumnMappingExecutionContext.class), any(TargetColumnExecutionContext.class))).thenReturn("mappingExpression");

        fixture = new ColumnMappingContextImpl(databaseMetadataService, defaultStrategy, customStrategy, validator, errorWarningMessages);
        Map<String, List<String>> columnMappings = fixture.getMappings(transformation);
        verify(defaultStrategy, times(8)).getMappingExpression(anyString(), any(ColumnMappingExecutionContext.class), any(TargetColumnExecutionContext.class));
        verify(customStrategy, times(8)).getMappingExpression(anyString(), any(ColumnMappingExecutionContext.class), any(TargetColumnExecutionContext.class));

        assertEquals(4, columnMappings.size());
        for (String key : columnMappings.keySet()) {
            assertEquals(2, columnMappings.get(key).size());
        }
    }

    @Test //(expected=RuntimeException.class)
    public void testContext() {
        final DataStore targetDataStore = createMockDataStore("target", "c1", "c2", "c3", "c4");
        final SourceExtension sourceExtension = InputModelMockHelper.createMockSourceExtension();
        final DataStoreWithAlias sourceDataStore = createMockDataStoreWithAlias("sourceName", "sourceAlias", true, sourceExtension, "c1", "c2");
        final DataStoreWithAlias lookupDataStore = createMockDataStoreWithAlias("lookupName", "lookupAlias", true, sourceExtension, "c3", "c4");
        List<Targetcolumn> targetcolumns = createMockTargetColumns("c1", "c2", "c3", "c4");
        Mappings mappings = createMockMappings("model", targetcolumns);
        final MappingsExtension mappingsExtension = InputModelMockHelper.createMockMappingsExtension();
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        Transformation transformation = mock(Transformation.class);
        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(transformation.getMappings()).thenReturn(mappings);

        //Create two datasets each with a single source
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"sourceAlias"}, new String[]{"sourceName"}, new String[]{"sourceModel"});
        List<Lookup> lookups = InputModelMockHelper.createMockETLLookups("join");
        when(lookups.get(0).getAlias()).thenReturn("lookupAlias");
        when(lookups.get(0).getLookupDataStore()).thenReturn("lookupName");
        when(lookups.get(0).getModel()).thenReturn("lookupModel");
        System.out.println(lookups.get(0).getAlias() + "/" + lookups.get(0).getLookupDataStore() + " " + lookups.get(0).getModel());
        when(datasets.get(0).getSources().get(0).getLookups()).thenReturn(lookups);
        when(transformation.getDatasets()).thenReturn(datasets);
        when(databaseMetadataService.getSourceDataStoreInModel("sourceName", "sourceModel")).thenReturn(sourceDataStore.getDataStore());
        when(databaseMetadataService.getSourceDataStoreInModel("lookupName", "lookupModel")).thenReturn(lookupDataStore.getDataStore());
        when(datasets.get(0).getSources().get(0).getExtension()).thenReturn(sourceExtension);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);

        fixture = new ColumnMappingContextImpl(
                databaseMetadataService,
                new ColumnMappingStrategy() {

                    @Override
                    public String getMappingExpression(
                            String currentMappingExpression,
                            ColumnMappingExecutionContext context,
                            TargetColumnExecutionContext columnContext) {
                        assert (context.getTransformationExtension() != null);
                        assertThat(context.getTransformationExtension(), SamePropertyValuesAs.<TransformationExtension>samePropertyValuesAs(transformationExtension));
                        assertThat(context.getMappingsExtension(), SamePropertyValuesAs.<MappingsExtension>samePropertyValuesAs(mappingsExtension));

                        assertEquals(targetDataStore, context.getTargetDataStore());

                        String rval = null;

                        for (DataStoreWithAlias dsa : context.getDataStores()) {
                            if (dsa.getAlias().equals("lookupAlias")) {
                                assertEquals(lookupDataStore.getAlias(), dsa.getAlias());
                            } else if (dsa.getAlias().equals("sourceAlias")) {
                                assertEquals(sourceDataStore.getAlias(), dsa.getAlias());
                            } else {
                                throw new RuntimeException("unknown datastore passed to context");
                            }
                            assertThat(dsa.getSourceExtension(), SamePropertyValuesAs.<SourceExtension>samePropertyValuesAs(sourceExtension));

                            if (rval == null && dsa.getDataStore().getColumns().keySet().contains(columnContext.getTargetColumnName())) {
                                rval = dsa.getAlias() + "." + columnContext.getTargetColumnName();
                            }

                        }

                        return rval;
                    }
                },
                new ColumnMappingStrategy() {


                    @Override
                    public String getMappingExpression(
                            String currentMappingExpression,
                            ColumnMappingExecutionContext context,
                            TargetColumnExecutionContext columnContext) {
                        return currentMappingExpression;
                    }

                },
                validator, errorWarningMessages
        );

        final Map<String, List<String>> columnMappings = fixture.getMappings(transformation);
        for (String key : columnMappings.keySet()) {
            for (String col : columnMappings.get(key)) {
                System.out.println(key + " -> " + col);
            }
        }

        verify(databaseMetadataService, atLeastOnce()).getSourceDataStoreInModel("lookupName", "lookupModel");
        verify(databaseMetadataService, atLeastOnce()).getSourceDataStoreInModel("sourceName", "sourceModel");

        assert (columnMappings.size() == 4);
        assertEquals("sourceAlias.c1", columnMappings.get("c1").get(0));
        assertEquals("sourceAlias.c2", columnMappings.get("c2").get(0));
        System.out.println(columnMappings.get("c4").get(0));
        System.out.println(columnMappings.get("c3").get(0));

        assertEquals("lookupAlias.c3", columnMappings.get("c3").get(0));
        assertEquals("lookupAlias.c4", columnMappings.get("c4").get(0));

    }

    //@Test ( expected=RuntimeException.class)
    public void testContext_no_mapping_found() {
        final DataStore targetDataStore = createMockDataStore("target", "c1", "c2", "c3", "c5");
        final SourceExtension sourceExtension = InputModelMockHelper.createMockSourceExtension();
        final DataStoreWithAlias sourceDataStore = createMockDataStoreWithAlias("sourceName", "sourceAlias", true, sourceExtension, "c1", "c2");
        final DataStoreWithAlias lookupDataStore = createMockDataStoreWithAlias("lookupName", "lookupAlias", true, sourceExtension, "c3", "c4");
        List<Targetcolumn> targetcolumns = createMockTargetColumns("c1", "c2", "c3", "c4");
        Mappings mappings = createMockMappings("model", targetcolumns);
        final MappingsExtension mappingsExtension = InputModelMockHelper.createMockMappingsExtension();
        when(mappings.getExtension()).thenReturn(mappingsExtension);
        Transformation transformation = mock(Transformation.class);
        final TransformationExtension transformationExtension = InputModelMockHelper.createMockTransformationExtension();
        when(transformation.getExtension()).thenReturn(transformationExtension);
        when(transformation.getMappings()).thenReturn(mappings);

        //Create two datasets each with a single source
        List<Dataset> datasets = InputModelMockHelper.createMockETLDatasets(transformation, new String[]{"sourceAlias"}, new String[]{"sourceName"}, new String[]{"sourceModel"});
        List<Lookup> lookups = InputModelMockHelper.createMockETLLookups("join");
        when(lookups.get(0).getAlias()).thenReturn("lookupAlias");
        when(lookups.get(0).getLookupDataStore()).thenReturn("lookupName");
        when(lookups.get(0).getModel()).thenReturn("lookupModel");
        System.out.println(lookups.get(0).getAlias() + "/" + lookups.get(0).getLookupDataStore() + " " + lookups.get(0).getModel());
        when(datasets.get(0).getSources().get(0).getLookups()).thenReturn(lookups);
        when(transformation.getDatasets()).thenReturn(datasets);
        when(databaseMetadataService.getSourceDataStoreInModel("sourceName", "sourceModel")).thenReturn(sourceDataStore.getDataStore());
        when(databaseMetadataService.getSourceDataStoreInModel("lookupName", "lookupModel")).thenReturn(lookupDataStore.getDataStore());
        when(datasets.get(0).getSources().get(0).getExtension()).thenReturn(sourceExtension);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);

        fixture = new ColumnMappingContextImpl(
                databaseMetadataService,
                new ColumnMappingStrategy() {

                    @Override
                    public String getMappingExpression(
                            String currentMappingExpression,
                            ColumnMappingExecutionContext context,
                            TargetColumnExecutionContext columnContext) {
                        return currentMappingExpression;
                    }

                },
                new ColumnMappingStrategy() {
                    @Override
                    public String getMappingExpression(
                            String currentMappingExpression,
                            ColumnMappingExecutionContext context,
                            TargetColumnExecutionContext columnContext) {
                        assert (currentMappingExpression.equals("column"));
                        return currentMappingExpression;
                    }

                },
                validator, errorWarningMessages
        );

        @SuppressWarnings("unused")
        Map<String, List<String>> columnMappings = fixture.getMappings(transformation);


    }

}
