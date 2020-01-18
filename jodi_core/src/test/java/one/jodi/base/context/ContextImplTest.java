package one.jodi.base.context;

import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.DataStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ContextImplTest {

    private static String dsName = "dataStore";
    private static String dsName2 = "dataStore2";
    private static String dsName3 = "dataStore3";

    private static String dsModel = "model";
    private static String dsModel2 = "model2";
    private static String[] columns = {"column1", "column2", "column3"};

    private DataStore mockDataStore;
    private DataStore mockDataStore2;
    private Context fixture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockDataStore = MockDatastoreHelper.createMockDataStore(dsName, dsModel, columns);
        mockDataStore2 = MockDatastoreHelper.createMockDataStore(dsName2, dsModel2, columns);
        MockDatastoreHelper.createMockDataStore(dsName3, dsModel, columns);
        fixture = new ContextImpl();
    }

    @Test
    public void testAddDataStore_NoDataStore() {
        assertNull(fixture.getDataStore(dsName, dsModel));
    }

    @Test
    public void testAddDataStore_OneDataStore() {
        fixture.addDataStore(mockDataStore);
        assertNotNull(fixture.getDataStore(dsName, dsModel));
        assertEquals(mockDataStore, fixture.getDataStore(dsName, dsModel));
        assertNull(fixture.getDataStore(dsName + "3", dsModel));
    }

    @Test
    public void testAddDataStore_TwoDataStores() {
        fixture.addDataStore(mockDataStore);
        fixture.addDataStore(mockDataStore2);
        assertNotNull(fixture.getDataStore(dsName, dsModel));
        assertEquals(mockDataStore, fixture.getDataStore(dsName, dsModel));
        assertNull(fixture.getDataStore(dsName + "3", dsModel));
        assertNotNull(fixture.getDataStore(dsName2, dsModel2));
        assertEquals(mockDataStore2, fixture.getDataStore(dsName2, dsModel2));
        assertNull(fixture.getDataStore(dsName2 + "3", dsModel2));
    }

    @Test
    public void testGetDataStore_AddDuplicate() {
        fixture.addDataStore(mockDataStore);
        fixture.addDataStore(mockDataStore);
        assertNotNull(fixture.getDataStore(dsName, dsModel));
        assertEquals(mockDataStore, fixture.getDataStore(dsName, dsModel));
        assertNull(fixture.getDataStore(dsName + "3", dsModel));
    }

    @Test
    public void testGetCachedDataModel_Success() {
        fixture.addDataStore(mockDataStore);
        assertNotNull(fixture.getDataModel(dsModel));
        assertEquals(mockDataStore.getDataModel(), fixture.getDataModel(dsModel));
    }

    @Test
    public void testNoCachedDataModel_Success() {
        fixture.addDataStore(mockDataStore);
        assertNull(fixture.getDataModel(dsModel2));
    }

    @Test
    public void testClear() {
        fixture.addDataStore(mockDataStore);
        fixture.addDataStore(mockDataStore2);
        fixture.clear();
        assertNull(fixture.getDataStore(dsName, dsModel));
        assertNull(fixture.getDataStore(dsName2, dsModel2));
    }

}
