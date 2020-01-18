package one.jodi.base.service.odb;

import one.jodi.base.config.BaseConfigurations;
import one.jodi.base.config.BaseConfigurationsHelper;
import one.jodi.base.config.PasswordConfig;
import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.service.metadata.*;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Unit test for OdbETLProvider.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OdbETLProviderTest {

    private static final String FACT_FK1 = "FACT_FK1";
    private static final String TST_CD = "TST_CD";
    private static final String EMP_ID = "EMP_ID";
    private static final String ROW_WID = "ROW_WID";
    private static final String PRIMARY_KEY_TYPE_STR = "P";
    private static final String ENABLED = "ENABLED";
    private static final String VALID = "VALID";
    private static final String MATVIEW_UK1 = "MATVIEW_UK1";
    private static final String MATVIEW_PK1 = "MATVIEW_PK1";
    private static final String FACT_PK1 = "FACT_PK1";
    private static final String DIM_UK1 = "DIM_UK1";
    private static final String DIM_PK1 = "DIM_PK1";
    private final String SCHEMA_PWD ;
    private static final String W_X_TEST_DV = "W_X_TEST_DV";
    private static final String W_X_TEST_F = "W_X_TEST_F";
    private static final String W_X_TEST_D = "W_X_TEST_D";
    private static final String TBL_QUERY = "select * from (" +
            "SELECT uo.object_name as TabName, utc.comments as TabComments FROM user_objects uo LEFT JOIN user_tab_comments utc ON uo.object_name = utc.table_name " +
            "WHERE uo.object_type = 'TABLE' " +
            "union all  " +
            "SELECT uo.object_name as TabName, utc.comments as TabComments FROM user_objects uo " +
            "LEFT JOIN user_synonyms us ON us.synonym_name = uo.object_name " +
            "LEFT JOIN user_tab_comments utc ON us.table_name = utc.table_name " +
            "WHERE uo.object_type like 'SYNONYM') " +
            "ORDER BY TabName";
    private static final String COL_QUERY = "SELECT cols.column_name as ColumnName, cols.data_type as DataType, cols.data_length as DataLength, " +
            " cols.data_scale as DataScale, cols.data_precision as DataPrecision, cols.nullable as IsNullable, cols.column_id as ColPosition, colcmnts.comments as ColCmnts " +
            " FROM user_tab_cols cols " +
            " left join user_synonyms us on (us.table_name = cols.table_name) " +
            " LEFT JOIN user_col_comments colcmnts on cols.table_name = colcmnts.table_name and cols.column_name = colcmnts.column_name ";
    private static final String INDEX_QUERY = "SELECT uic.index_name as IndexName, uic.column_name as IndColName, inds.status as IsEnabled "
            + "FROM user_ind_columns uic JOIN user_indexes inds ON inds.index_name = uic.index_name "
            + " left join user_synonyms us on (us.table_name = inds.table_name) "
            + "WHERE (inds.table_name = ? or us.synonym_name = ?) "
            + "ORDER BY uic.index_name";
    private static final String KEYS_QUERY = "SELECT ucc.owner Owner, ucc.constraint_name ConstraintName, uc.constraint_type ConstraintType, ucc.column_name KeyColName, uc.Status IsEnabled "
            + "FROM user_cons_columns ucc JOIN user_constraints uc ON ucc.owner = uc.owner AND ucc.constraint_name = uc.constraint_name "
            + " left join user_synonyms us on (us.table_name = ucc.table_name) "
            + "WHERE (ucc.table_name = ? or us.synonym_name = ?) "
            + "AND uc.constraint_type in ('P','U') ORDER BY uc.constraint_type, ucc.constraint_name";
    @Mock
    Connection conn;
    @Mock
    Statement stmt;
    @Mock
    PreparedStatement stmtPrepared;
    @Mock
    ResultSet rs;
    @Mock
    DataModelDescriptor dmDescr;
    @Mock
    OracleDataSource ods;
    @Mock
    OdbMetaDataHelper dbHelper;
    OdbETLProvider fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
    private DbConnectionUtil dbConnUtil = new DbConnectionUtil(errorWarningMessages);

    public OdbETLProviderTest(){
        SCHEMA_PWD = new PasswordConfigImpl().getOdiMasterRepoPassword();

    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
//		when(properties.getProperty(OdbConstants.ODB_STAR_URL)).thenReturn("jdbc:oracle:thin:@//jodi:1521/ft06");
//		when(properties.getProperty(OdbConstants.ODB_STAR_USERNAME)).thenReturn(SCHEMA_NAME);
//		when(properties.getProperty(OdbConstants.ODB_STAR_PASSWORD)).thenReturn(SCHEMA_PWD);
    }

    //@Test
    public void testgetDataStoreDescriptorsInModel() throws Exception {

        when(ods.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(TBL_QUERY)).thenReturn(rs);
        when(rs.getString("TabName")).thenReturn(W_X_TEST_D, W_X_TEST_F, W_X_TEST_DV);
        when(rs.getString("TabComments")).thenReturn("Test dimension.", "Test fact.", "Test materialized view.");
        //W_X_TEST_D -- table (true, true, true, false), col (true, true, true, true, false), pk & index (true, true, false, true, false), fk (false)
        //---- true, true, true, false, true, true, true, true, false, true, true, false, true, false, false
        //W_X_TEST_F -- col (true, true, true, false), pk & index (true, false, true, false), fk (true, false)
        //---- true, true, true, false, true, false, true, false, true, false
        //W_X_TEST_DV --- col(true, true, true, false), pk & index (true, true, true, false, true, false), fk (false)
        //----- true, true, true, false, true, true, true, false, true, false, false
        // true, true, true, false, true, true, true, true, false, true, true, false, true, false, false,true, true, true, false, true, false, true, false, true, false, true, true, true, false, true, true, true, false, true, false, false
        when(rs.next()).thenReturn(true, true, true, false, true, true, true, true, false, true, true, false, true, false, false, true, true, true, false, true, false, true, false, true, false, true, true, true, false, true, true, true, false, true, false, false);

        //col query
        when(conn.prepareStatement(COL_QUERY)).thenReturn(stmtPrepared);
        when(conn.prepareStatement(COL_QUERY)).thenReturn(stmtPrepared);
        when(conn.prepareStatement(COL_QUERY)).thenReturn(stmtPrepared);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);

        when(rs.getString("ColumnName")).thenReturn(ROW_WID, EMP_ID, "FIRST_NAME", "LAST_NAME", ROW_WID, "EMP_WID", "TST_FLD1", ROW_WID, TST_CD, "TST_NM");
        when(rs.getString("DataType")).thenReturn("NUMBER", "VARCHAR2", "VARCHAR2", "VARCHAR2");
        when(rs.getInt("DataLength")).thenReturn(22, 8, 255, 255);
        when(rs.getInt("DataScale")).thenReturn(10, 0, 0, 0);
        when(rs.getInt("ColPosition")).thenReturn(1, 2, 3, 4);
        when(rs.getString("ColCmnts")).thenReturn("Surrogate Key---This is the surrogate primary key.", "Employee Number---Employee number.", "Full Name---Full name.", "Last Name---Last Name.");

        //index query
        when(conn.prepareStatement(INDEX_QUERY)).thenReturn(stmtPrepared);
        when(conn.prepareStatement(INDEX_QUERY)).thenReturn(stmtPrepared);
        when(conn.prepareStatement(INDEX_QUERY)).thenReturn(stmtPrepared);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);

        //keys query
        when(conn.prepareStatement(KEYS_QUERY)).thenReturn(stmtPrepared);
        when(conn.prepareStatement(KEYS_QUERY)).thenReturn(stmtPrepared);
        when(conn.prepareStatement(KEYS_QUERY)).thenReturn(stmtPrepared);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);

        when(rs.getString("IndexName")).thenReturn(DIM_PK1, DIM_PK1, DIM_UK1, DIM_UK1, FACT_PK1, FACT_PK1, MATVIEW_PK1, MATVIEW_PK1, MATVIEW_UK1, MATVIEW_UK1);
        when(rs.getString("IndColName")).thenReturn(ROW_WID, ROW_WID, EMP_ID, EMP_ID, ROW_WID, ROW_WID, ROW_WID, ROW_WID, TST_CD, TST_CD);
        when(rs.getString("IsEnabled")).thenReturn(VALID, VALID, ENABLED, ENABLED, VALID, VALID, VALID, VALID, ENABLED, ENABLED);
        when(rs.getString("ConstraintName")).thenReturn(DIM_PK1, DIM_PK1, FACT_PK1, FACT_PK1, MATVIEW_PK1, MATVIEW_PK1);
        when(rs.getString("ConstraintType")).thenReturn(PRIMARY_KEY_TYPE_STR, PRIMARY_KEY_TYPE_STR, PRIMARY_KEY_TYPE_STR);
        when(rs.getString("KeyColName")).thenReturn(ROW_WID, ROW_WID, ROW_WID);
        when(rs.isAfterLast()).thenReturn(true);

        //fk query
        when(conn.prepareStatement(getFKQuery(W_X_TEST_D))).thenReturn(stmtPrepared);
        when(conn.prepareStatement(getFKQuery(W_X_TEST_F))).thenReturn(stmtPrepared);
        when(conn.prepareStatement(getFKQuery(W_X_TEST_DV))).thenReturn(stmtPrepared);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(stmtPrepared.executeQuery()).thenReturn(rs);
        when(rs.getString("FKeyName")).thenReturn(FACT_FK1);
        when(rs.getString("IsEnabled")).thenReturn(ENABLED);
        when(rs.getString("Owner")).thenReturn(SCHEMA_PWD);
        when(rs.getString("RefTableName")).thenReturn(W_X_TEST_D);
        when(rs.getString("FKColumnName")).thenReturn("EMP_ROW_ID");
        when(rs.getString("PKColumnName")).thenReturn(ROW_WID);

        BaseConfigurations biProperties =
                BaseConfigurationsHelper.getTestBaseConfigurations();

        fixture = new OdbETLProvider(dbConnUtil, dbHelper, biProperties,
                errorWarningMessages);
        Map<String, DataStoreDescriptor> dataStoreDescriptorsInModel
                = fixture.getDataStoreDescriptorsInModel("test");
        assertNotNull(dataStoreDescriptorsInModel);
        assertEquals(3, dataStoreDescriptorsInModel.size());

        //W_X_TEST_D
        DataStoreDescriptor dxDescriptor = dataStoreDescriptorsInModel.get(W_X_TEST_D);
        assertNotNull(dxDescriptor);
        assertEquals(W_X_TEST_D, dxDescriptor.getDataStoreName());
        Collection<ColumnMetaData> columnMetaData = dxDescriptor.getColumnMetaData();
        assertEquals(4, columnMetaData.size());
        List<Key> keys = dxDescriptor.getKeys();
        assertEquals(DIM_PK1, keys.get(0).getName());
        assertEquals(DIM_UK1, keys.get(1).getName());
        assertEquals(DIM_PK1, keys.get(2).getName());
        assertEquals("PRIMARY", keys.get(2).getType().name());
        assertEquals(3, keys.size());
        List<ForeignReference> fkRelationships = dxDescriptor.getFKRelationships();
        assertEquals(0, fkRelationships.size());

        //W_X_TEST_F
        dxDescriptor = dataStoreDescriptorsInModel.get(W_X_TEST_F);
        assertNotNull(dxDescriptor);
        assertEquals(W_X_TEST_F, dxDescriptor.getDataStoreName());
        columnMetaData = dxDescriptor.getColumnMetaData();
        assertEquals(3, columnMetaData.size());
        keys = dxDescriptor.getKeys();
        assertEquals(2, keys.size());
        fkRelationships = dxDescriptor.getFKRelationships();
        assertEquals(1, fkRelationships.size());
        assertEquals(FACT_FK1, fkRelationships.get(0).getName());

        //W_X_TEST_DV
        dxDescriptor = dataStoreDescriptorsInModel.get(W_X_TEST_DV);
        assertNotNull(dxDescriptor);
        assertEquals(W_X_TEST_DV, dxDescriptor.getDataStoreName());
        columnMetaData = dxDescriptor.getColumnMetaData();
        assertEquals(3, columnMetaData.size());
        keys = dxDescriptor.getKeys();
        assertEquals(3, keys.size());
        assertEquals(MATVIEW_UK1, keys.get(1).getName());
        fkRelationships = dxDescriptor.getFKRelationships();
        assertEquals(0, fkRelationships.size());

    }

    @After
    public void print() {
        errorWarningMessages.printMessages();
        errorWarningMessages.clear();
    }

    private String getFKQuery(String dataStore) {
        return "SELECT ucc.owner Owner, ucc.column_name FKColumnName, ucc.constraint_name FKeyName, uc.Status IsEnabled, " +
                "refcons.RefTableName, refcons.constraint_name RefConstraintName, " +
                "pks.column_name as PKColumnName " +
                "FROM user_cons_columns ucc " +
                "JOIN user_constraints uc ON (ucc.owner = uc.owner AND ucc.constraint_name = uc.constraint_name) " +
                "LEFT JOIN " +
                "(select nvl(refus.synonym_name, refcons.table_name) RefTableName, " +
                "refcons.* " +
                "from user_constraints refcons " +
                "left join user_synonyms refus on (refcons.table_name = refus.table_name)) refcons " +
                "ON (uc.r_owner = refcons.owner AND uc.r_constraint_name = refcons.constraint_name ) " +
                "LEFT JOIN  (SELECT table_name, column_name from user_cons_columns refucc WHERE  constraint_name like '%_PK%') pks ON pks.table_name=refcons.table_name " +
                "LEFT JOIN user_synonyms us on (us.table_name = ucc.table_name) " +
                "WHERE (ucc.table_name = ? or us.synonym_name = ?) " +
                "AND uc.constraint_type = 'R' ORDER BY ucc.constraint_name";
    }
}
