package one.jodi.core.service.impl;

import junit.framework.TestCase;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.service.datastore.DatastoreServiceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/**
 * The class <code>DatastoreServiceImplTest</code> contains tests for the class
 * {@link DatastoreServiceImpl}
 *
 */
public class DatastoreServiceImplTest extends TestCase {
    DatastoreServiceProvider mockProvider;
    SchemaMetaDataProvider etlProvider;
    JodiProperties properties;

    DatastoreServiceImpl fixture;


    /**
     * Construct new test instance
     *
     * @param name the test name
     */
    public DatastoreServiceImplTest(final String name) {
        super(name);
    }

    /**
     * Perform pre-test initialization
     *
     * @throws Exception
     * @see TestCase#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockProvider = mock(DatastoreServiceProvider.class);
        etlProvider = mock(SchemaMetaDataProvider.class);
        properties = mock(JodiProperties.class);

        fixture = new DatastoreServiceImpl(etlProvider, mockProvider);
    }

    /**
     * Perform post-test clean up
     *
     * @throws Exception
     * @see TestCase#tearDown()
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Run the void deleteDatastore(String) method test
     */
    @Test
    public void testDeleteDatastore() {
        String datastoreName = "testdatastore";
        String modelName = "model";

        fixture.deleteDatastore(datastoreName, modelName);

        verify(mockProvider).deleteDatastore(datastoreName, modelName);
    }

    /**
     * Run the void deleteTempDatastores() method test
     */
//    @Test public void testDeleteTempDatastores() {
//        String modelName = "model";
//        
//        List<DataModelDescriptor> models = new ArrayList<DataModelDescriptor>();
//        models.add( MockDatastoreHelper.createMockDataModelDescriptor(modelName) );
//        
//        Map<String, DataStoreDescriptor> dataStores = new HashMap<String, DataStoreDescriptor>();
//        for (int i=1 ; i<4; i++) {
//          Map<String, DataStoreDescriptor> m 
//               = MockDatastoreHelper.createMockDSDescriptor("testdatastore"+i, modelName, i%2==0);
//          dataStores.putAll(m);
//        }
//
//        when(etlProvider.getDataModelDescriptors()).thenReturn(models);
//        when(etlProvider.getDataStoreDescriptorsInModel(modelName)).thenReturn(dataStores);
//
//        fixture.deleteTempDatastores();
//
//        verify(mockProvider, times(1)).deleteDatastore(anyString(), anyString());
//    }

}
