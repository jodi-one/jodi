package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.*;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * The class <code>ModelValidatorImplTest</code> contains tests for the class
 * {@link ModelValidatoImpl}
 */
public class ModelValidatorImplTest {

    DataStore mockDatastore;
    ModelValidatorImpl fixture;
    @Mock
    JodiProperties mockProperties;

    ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fixture = new ModelValidatorImpl(mockProperties, errorWarningMessages);
    }

    @After
    public void tearDown()
            throws Exception {
    }

    private DataStore createWithKeys(final DataStore in, final DataStoreType type,
                                     final DataStoreKey... keys) {

        final ArrayList<DataStoreKey> keyList = new ArrayList<DataStoreKey>();
        for (DataStoreKey key : keys)
            keyList.add(key);

        return new DataStore() {

            @Override
            public String getDataStoreName() {
                return in.getDataStoreName();
            }

            @Override
            public boolean isTemporary() {
                return in.isTemporary();
            }

            @Override
            public Map<String, DataStoreColumn> getColumns() {
                return in.getColumns();
            }

            @Override
            public DataStoreType getDataStoreType() {
                return type;
            }

            @Override
            public List<DataStoreKey> getDataStoreKeys() {
                return keyList;
            }

            @Override
            public DataStoreKey getPrimaryKey() {
                return in.getPrimaryKey();
            }

            @Override
            public List<DataStoreForeignReference>
            getDataStoreForeignReference() {
                return in.getDataStoreForeignReference();
            }

            @Override
            public Map<String, Object> getDataStoreFlexfields() {
                return in.getDataStoreFlexfields();
            }

            @Override
            public String getDescription() {
                return in.getDescription();
            }

            @Override
            public DataModel getDataModel() {
                return in.getDataModel();
            }

            @Override
            public DataStoreKey getAlternateKey() {
                return in.getAlternateKey();
            }
        };
    }

    @Test
    public void testDoCheckKeyNotFound() {
        final String datamartPrefix = "W_";
        final String dataSourceName = datamartPrefix + "ZIPCODE";
        String testModel = "testModel";
        final String[] keyNames = {"NAME", "ZIPCODE_U1"};
        mockDatastore = MockDatastoreHelper.createMockDataStore(dataSourceName,
                testModel, false, keyNames);
        when(mockProperties.getProperty(JodiConstants.DATA_MART_PREFIX)).thenReturn(
                datamartPrefix);
        List<String> listDm = new ArrayList<String>();
        listDm.add(datamartPrefix);
        when(mockProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)).thenReturn(listDm);
        mockDatastore = createWithKeys(mockDatastore, DataStoreType.FACT,
                new DataStoreKey() {
                    @Override
                    public String getName() {
                        return dataSourceName;
                    }

                    @Override
                    public KeyType getType() {
                        return KeyType.ALTERNATE;
                    }

                    @Override
                    public List<String> getColumns() {
                        return Arrays.
                                asList(keyNames);
                    }

                    @Override
                    public boolean existsInDatabase() {
                        return true;
                    }

                    @Override
                    public boolean isEnabledInDatabase() {
                        return true;
                    }
                });
        assertFalse(fixture.doCheck(mockDatastore));
    }

    @Test
    public void testDoCheckKeyFound() {
        final String datamartPrefix = "W_";
        final String dataSourceName = datamartPrefix + "ZIPCODE";
        String testModel = "testModel";
        final String[] keyNames = {"NAME", "ZIPCODE_U1"};
        mockDatastore = MockDatastoreHelper.createMockDataStore(dataSourceName,
                testModel, false, keyNames);
        when(mockProperties.getProperty(JodiConstants.DATA_MART_PREFIX)).thenReturn(
                datamartPrefix);

        mockDatastore = createWithKeys(mockDatastore, DataStoreType.FACT, new
                DataStoreKey() {
                    @Override
                    public String getName() {
                        return dataSourceName + "_U1";
                    }

                    @Override
                    public KeyType getType() {
                        return KeyType.ALTERNATE;
                    }

                    @Override
                    public List<String> getColumns() {
                        return Arrays.
                                asList(keyNames);
                    }

                    @Override
                    public boolean existsInDatabase() {
                        return true;
                    }

                    @Override
                    public boolean isEnabledInDatabase() {
                        return true;
                    }
                });
        assertTrue(fixture.doCheck(mockDatastore));
    }
}
