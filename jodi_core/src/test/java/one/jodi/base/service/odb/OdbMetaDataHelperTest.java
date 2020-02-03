package one.jodi.base.service.odb;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataTypeService;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.ForeignReference;
import one.jodi.base.service.metadata.Key;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Unit test for OdbMetaDataHelper.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4.class)
public class OdbMetaDataHelperTest {

    private static final String W_X_TEST_DV = "W_X_TEST_DV";
    private static final String W_X_TEST_F = "W_X_TEST_F";
    private static final String W_X_TEST_D = "W_X_TEST_D";
    private static final String SQL_QUERY = OdbMetaDataHelper.sqlTableQuery;
    private static final String COL_QUERY = OdbMetaDataHelper.colQuery;
    private static final String INDEX_QUERY = OdbMetaDataHelper.indexQuery;
    private static final String KEYS_QUERY = OdbMetaDataHelper.keysQuery;
    private static final String FK_QUERY = OdbMetaDataHelper.fkeysQuery;
    @Mock
    Connection conn;
    @Mock
    PreparedStatement stmt;
    @Mock
    Statement stmtRegular;
    @Mock
    ResultSet rs;
    @Mock
    DataTypeService dataTypeService;
    OdbMetaDataHelper fixture;
    ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Test
    public void testDbConnection() {

    }

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        fixture = new OdbMetaDataHelper(dataTypeService, errorWarningMessages);
    }

    @After
    public void print() {
        errorWarningMessages.printMessages();
        errorWarningMessages.clear();
    }

    //@Test
    public void testgetDataStoreList() throws Exception {

        when(conn.createStatement()).thenReturn(stmtRegular);
        when(stmtRegular.executeQuery(SQL_QUERY)).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getString("TabName")).thenReturn(W_X_TEST_D, W_X_TEST_F, W_X_TEST_DV);
        when(rs.getString("TabComments")).thenReturn("Test dimension.", "Test fact.", "Test materialized view.");

        List<OdbDataStore> dataStoreList = fixture.getDataStoreList(conn);
        assertNotNull(dataStoreList);
        assertEquals(3, dataStoreList.size());

    }

    /**
     * this test is now redundant, since now it is retrieved from cache.
     *
     * @throws Exception
     */
    //@Test
    public void testgetColumnMetaData() throws Exception {

        when(conn.prepareStatement(COL_QUERY)).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, true, false);

        when(rs.getString("ColumnName")).thenReturn("ROW_WID", "EMP_ID", "FIRST_NAME", "LAST_NAME");
        when(rs.getString("DataType")).thenReturn("NUMBER", "VARCHAR2", "VARCHAR2", "VARCHAR2");
        when(rs.getInt("DataLength")).thenReturn(22, 8, 255, 255);
        when(rs.getInt("DataScale")).thenReturn(10, 0, 0, 0);
        when(rs.getInt("ColPosition")).thenReturn(1, 2, 3, 4);
        when(rs.getString("ColCmnts")).thenReturn("Surrogate Key---This is the surrogate primary key.", "Employee Number---Employee number.", "Full Name---Full name.", "Last Name---Last Name.");
        Collection<ColumnMetaData> columnMetaData = fixture.getColumnMetaData(W_X_TEST_D, conn);
        assertNotNull(columnMetaData);
        assertEquals(4, columnMetaData.size());

    }

    //@Test
    // thorough tested with functional tests.
    public void testgetKeyandIndexMetaData() throws Exception {
        when(conn.prepareStatement(COL_QUERY)).thenReturn(stmt);
        when(conn.prepareStatement(INDEX_QUERY)).thenReturn(stmt);
        when(conn.prepareStatement(KEYS_QUERY)).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(stmt.executeQuery()).thenReturn(rs);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false, true, false);
        when(rs.getString("DataType")).thenReturn("NUMBER", "NUMBER", "NUMBER", "NUMBER");
        when(rs.getString("table_name")).thenReturn(W_X_TEST_D, W_X_TEST_D, W_X_TEST_D, W_X_TEST_D);
        when(rs.getString("IndexName")).thenReturn("TEST_PK1", "TEST_PK1", "TEST_UK1", "TEST_UK1");
        when(rs.getString("IsEnabled")).thenReturn("VALID", "VALID", "ENABLED", "ENABLED");
        when(rs.getString("IndColName")).thenReturn("ROW_WID", "ROW_WID", "EMP_ID", "EMP_ID");
        when(rs.getString("ConstraintName")).thenReturn("TEST_PK1");
        when(rs.getString("ConstraintType")).thenReturn("P");
        when(rs.getString("KeyColName")).thenReturn("ROW_WID");
        when(rs.isAfterLast()).thenReturn(true);
        fixture.createCache(conn);
        Collection<Key> keyandIndexMetaData = fixture.getKeyandIndexMetaData(W_X_TEST_D, conn);
        assertNotNull(keyandIndexMetaData);
        assertEquals(3, keyandIndexMetaData.size());
    }

    //@Test
    // thoroughly tested with FTs.
    public void testgetFKRefs() throws Exception {
        when(conn.prepareStatement(FK_QUERY)).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("FKeyName")).thenReturn("TEST_FK1");
        when(rs.getString("IsEnabled")).thenReturn("ENABLED");
        when(rs.getString("Owner")).thenReturn("X3");
        when(rs.getString("RefTableName")).thenReturn(W_X_TEST_D);
        when(rs.getString("FKColumnName")).thenReturn("EMP_ROW_ID");
        when(rs.getString("PKColumnName")).thenReturn("ROW_WID");

        List<ForeignReference> fkRefs = fixture.getFKRefs(W_X_TEST_F, conn);
        assertNotNull(fkRefs);
        assertEquals(1, fkRefs.size());
    }

}