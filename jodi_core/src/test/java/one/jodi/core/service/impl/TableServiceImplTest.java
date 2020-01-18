package one.jodi.core.service.impl;

import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.SCDType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.service.ModelValidator;
import one.jodi.core.service.TableService;
import one.jodi.etl.service.table.ColumnDefaultBehaviors;
import one.jodi.etl.service.table.TableDefaultBehaviors;
import one.jodi.etl.service.table.TableDefaultBehaviors.OlapType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * The class <code>TableServiceImplTest</code> contains tests for the class
 * {@link TableServiceImpl}
 */
public class TableServiceImplTest {

    TableService fixture;
    private @Mock
    DatabaseMetadataService mockDatabaseMetadataService;
    private @Mock
    JodiProperties mockJodiProperties;
    private @Mock
    ModelValidator mockModelValidator;

    List<ModelProperties> createModelProperties(final String code) {
        ModelProperties mp = new ModelProperties() {
            @Override
            public List<String> getJkmoptions() {
                return null;
            }

            @Override
            public String getModelID() {
                if (code != null) {
                    return code + "_def";
                } else {
                    return null;
                }
            }

            @Override
            public String getCode() {
                return code;
            }

            @Override
            public boolean isDefault() {
                return false;
            }

            @Override
            public boolean isIgnoredByHeuristics() {
                return false;
            }

            @Override
            public int getOrder() {
                return 0;
            }

            @Override
            public String getLayer() {
                return "layer";
            }

            @Override
            public List<String> getPrefix() {
                return Arrays.asList(new String[]{"pre_"});
            }

            @Override
            public List<String> getPostfix() {
                return Arrays.asList(new String[]{"_post"});
            }

            @Override
            public int compareOrderTo(ModelProperties otherModelProperties) {
                return 0;
            }

            @Override
            public boolean isJournalized() {
                return false;
            }

            @Override
            public void setJournalized(boolean journalized) {
            }

            @Override
            public List<String> getSubscribers() {
                List<String> subs = new ArrayList<String>();
                subs.add("SUNOPSIS");
                return subs;
            }

            @Override
            public String getJkm() {
                return null;
            }
        };
        List<ModelProperties> modelPropertiesList = new ArrayList<ModelProperties>();
        modelPropertiesList.add(mp);
        return modelPropertiesList;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockModelValidator.doCheck(any(DataStore.class))).thenReturn(true);
        when(mockDatabaseMetadataService.isConnectorModel("model")).thenReturn(false);

        List<String> prefix = Collections.singletonList("W_");
        when(mockJodiProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX))
                .thenReturn(prefix);
        when(mockJodiProperties.getProperty(JodiConstants.FACT_SUFFIX))
                .thenReturn("_F");
        when(mockJodiProperties.getProperty(JodiConstants.DIMENSION_SUFFIX))
                .thenReturn("_D");

        when(mockJodiProperties.getRowidColumnName()).thenReturn("ROW_WID");
        when(mockJodiProperties.getProperty(JodiConstants.EFFECTIVE_DATE))
                .thenReturn("EFFECTIVE_DATE");
        when(mockJodiProperties.getProperty(JodiConstants.EXPIRATION_DATE))
                .thenReturn("EXPIRATION_DATE");
        when(mockJodiProperties.getProperty(JodiConstants.CURRENT_FLG))
                .thenReturn("CURRENT_FLG");

        when(mockJodiProperties.getProperty(JodiConstants.ETL_PROC_WID))
                .thenReturn("ETL_PROC_WID");
        when(mockJodiProperties.getProperty(JodiConstants.W_INSERT_DT))
                .thenReturn("W_INSERT_DT");
        when(mockJodiProperties.getProperty(JodiConstants.W_UPDATE_DT))
                .thenReturn("W_UPDATE_DT");

        List<ModelProperties> models = createModelProperties("model");
        when(mockDatabaseMetadataService.getConfiguredModels()).thenReturn(models);

        fixture = new TableServiceImpl(mockDatabaseMetadataService,
                mockJodiProperties, mockModelValidator);
    }

    @Test
    public void testFactOLAPType() {
        String model = "model";
        String tableName = "W_MYFACT_F";
        String[] columns = {"ROW_WID", "MEASURE1", "MEASURE2"};

        Map<String, DataStore> empty = Collections.emptyMap();
        DataStore ds = MockDatastoreHelper.createMockDataStore(tableName, model,
                columns, 1, empty);

        Map<String, DataStore> dataStores = new HashMap<String, DataStore>();
        dataStores.put(tableName, ds);

        // set metadata service to return dataset
        when(mockDatabaseMetadataService.getAllDataStoresInModel("model"))
                .thenReturn(dataStores);

        List<TableDefaultBehaviors> tdb = fixture.assembleDefaultBehaviors();

        assertNotNull(tdb);
        assertEquals(1, tdb.size());
        TableDefaultBehaviors tableBehavior = tdb.get(0);
        assertEquals(OlapType.FACT, tableBehavior.getOlapType());
        assertEquals(tableName, tableBehavior.getDefaultAlias());
        assertEquals(model, tableBehavior.getModel());
        assertEquals(tableName, tableBehavior.getTableName());
        assertFalse(tableBehavior.isConnectorModel());
        assertEquals(1, tableBehavior.getColumnDefaultBehaviors().size());

        assertEquals(1, tableBehavior.getColumnDefaultBehaviors().size());
        ColumnDefaultBehaviors cdb = tableBehavior.getColumnDefaultBehaviors().get(0);
        assertEquals("ROW_WID", cdb.getColumnName());
        assertEquals(SCDType.SURROGATE_KEY.toString(), cdb.getScdType());
        assertFalse(cdb.isMandatory());
        assertTrue(cdb.isInDatabase());
        assertFalse(cdb.isFlowCheckEnabled());
        assertFalse(cdb.isDataServiceAllowSelect());
        assertFalse(cdb.isDataServiceAllowUpdate());
        assertTrue(cdb.isStaticCheckEnabled());
    }

    @Test
    public void testDimensionOLAPType() {
        String model = "model";
        String tableName = "W_MYDIMENSIONT_D";
        String[] columns = {"ROW_WID", "ATTR1", "ATTR2",
                "ETL_PROC_WID", "W_INSERT_DT", "W_UPDATE_DT",
                "EFFECTIVE_DATE", "EXPIRATION_DATE", "CURRENT_FLG"};

        DataStore ds = MockDatastoreHelper.createMockDataStore(tableName, model, columns);
        Map<String, DataStore> dataStores = new HashMap<String, DataStore>();
        dataStores.put(tableName, ds);

        // set metadata service to return dataset
        when(mockDatabaseMetadataService.getAllDataStoresInModel("model"))
                .thenReturn(dataStores);

        List<TableDefaultBehaviors> tdb = fixture.assembleDefaultBehaviors();

        assertNotNull(tdb);
        assertEquals(1, tdb.size());
        TableDefaultBehaviors tableBehavior = tdb.get(0);
        assertEquals(OlapType.DIMENSION, tableBehavior.getOlapType());
        assertEquals(tableName, tableBehavior.getDefaultAlias());
        assertEquals(model, tableBehavior.getModel());
        assertEquals(tableName, tableBehavior.getTableName());
        assertEquals(false, tableBehavior.isConnectorModel());

        assertEquals(0, tableBehavior.getColumnDefaultBehaviors().size());
    }

    @Test
    public void testScd2OLAPType() {
        String model = "model";
        String tableName = "W_MYSCD2DIM_SCD2_D";
        String[] columns = {"ROW_WID", "COLUMN_U1", "ATTR1", "ATTR2",
                "ETL_PROC_WID", "W_INSERT_DT", "W_UPDATE_DT",
                "EFFECTIVE_DATE", "EXPIRATION_DATE", "CURRENT_FLG"};

        Map<String, DataStore> empty = Collections.emptyMap();
        DataStore ds = MockDatastoreHelper.createMockDataStore(tableName, model,
                columns, 0, empty);

        Map<String, DataStore> dataStores = new HashMap<String, DataStore>();
        dataStores.put(tableName, ds);

        // set metadata service to return dataset
        when(mockDatabaseMetadataService.getAllDataStoresInModel("model"))
                .thenReturn(dataStores);

        List<TableDefaultBehaviors> tdb = fixture.assembleDefaultBehaviors();

        assertNotNull(tdb);
        assertEquals(1, tdb.size());
        TableDefaultBehaviors tableBehavior = tdb.get(0);
        assertEquals(OlapType.SLOWLY_CHANGING_DIMENSION, tableBehavior.getOlapType());
        assertEquals(tableName, tableBehavior.getDefaultAlias());
        assertEquals(model, tableBehavior.getModel());
        assertEquals(tableName, tableBehavior.getTableName());
        assertEquals(false, tableBehavior.isConnectorModel());

        assertEquals(columns.length, tableBehavior.getColumnDefaultBehaviors().size());
        for (ColumnDefaultBehaviors cdb : tableBehavior.getColumnDefaultBehaviors()) {
            String msg = "incorrect in column " + cdb.getColumnName();
            assertFalse(msg, cdb.isMandatory());
            assertFalse(msg, cdb.isInDatabase());
            assertFalse(msg, cdb.isFlowCheckEnabled());
            assertFalse(msg, cdb.isDataServiceAllowSelect());
            assertFalse(msg, cdb.isDataServiceAllowUpdate());

            if (cdb.getColumnName().equals("ROW_WID")) {
                assertEquals(msg, SCDType.SURROGATE_KEY.toString(), cdb.getScdType());
                assertFalse(msg, cdb.isStaticCheckEnabled());
            } else if (cdb.getColumnName().equals("EFFECTIVE_DATE")) {
                assertEquals(msg, SCDType.START_TIMESTAMP.toString(), cdb.getScdType());
                assertTrue(msg, cdb.isStaticCheckEnabled());
            } else if (cdb.getColumnName().equals("EXPIRATION_DATE")) {
                assertEquals(msg, SCDType.END_TIMESTAMP.toString(), cdb.getScdType());
                assertTrue(msg, cdb.isStaticCheckEnabled());
            } else if (cdb.getColumnName().equals("CURRENT_FLG")) {
                assertEquals(msg, SCDType.CURRENT_RECORD_FLAG.toString(), cdb.getScdType());
                assertFalse(msg, cdb.isStaticCheckEnabled());
            } else if (cdb.getColumnName().equals("ETL_PROC_WID") ||
                    cdb.getColumnName().equals("W_UPDATE_DT") ||
                    cdb.getColumnName().equals("W_INSERT_DT")) {
                assertEquals(msg, SCDType.OVERWRITE_ON_CHANGE.toString(), cdb.getScdType());
                assertFalse(msg, cdb.isStaticCheckEnabled());
            } else if (cdb.getColumnName().startsWith("ATTR")) {
                assertEquals(msg, SCDType.ADD_ROW_ON_CHANGE.toString(), cdb.getScdType());
                assertFalse(msg, cdb.isStaticCheckEnabled());
            } else if (cdb.getColumnName().endsWith("_U1")) {
                assertEquals(msg, SCDType.NATURAL_KEY.toString(), cdb.getScdType());
                assertFalse(msg, cdb.isStaticCheckEnabled());
            } else {
                fail(msg);
            }
        }
    }

}
