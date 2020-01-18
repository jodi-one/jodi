package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.metadata.DatabaseMetadataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * The class <code>OneToOneMappingGenerationImplTest</code> contains tests for
 * the class {@link OneToOneMappingGenerationImpl}
 */
public class OneToOneMappingGenerationImplTest {
    private final static Logger logger = LogManager.getLogger(
            OneToOneMappingGenerationImplTest.class);
    @Mock
    DataStore dataStore;
    @Mock
    ModelProperties modelProperties;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    ModelPropertiesProvider modelPropertiesProvider;
    String outputXMLDir;
    int packageSequence;
    String targetModelName;
    String sourceModelName;
    String testModel;
    Map<String, DataStore> mockDataStores = new HashMap<String, DataStore>();
    OneToOneMappingGenerationImpl fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp() throws Exception {
        outputXMLDir = "TestDir";
        new File(outputXMLDir).mkdir();

        packageSequence = 0;
        targetModelName = "model";
        sourceModelName = "model";
        testModel = "model";

        final String[] keyNames = {"NAME", "ZIPCODE_U1"};
        MockitoAnnotations.initMocks(this);
        dataStore = MockDatastoreHelper.createMockDataStore(sourceModelName,
                testModel, false, keyNames);

        for (int i = 0; i < 4; i++) {
            mockDataStores.put(
                    testModel + i + "." + dataStore.getDataStoreName(),
                    dataStore);
        }
        when(databaseMetadataService.getAllDataStoresInModel(testModel))
                .thenReturn(mockDataStores);
    }

    @After
    public void tearDown() throws Exception {
        File cleanup = new File(outputXMLDir);
        File[] files = cleanup.listFiles();

        if (cleanup.list().length > 0) {
            for (File f : files) {
                f.delete();
            }
        }
        cleanup.delete();
    }

    List<ModelProperties> createModelProperties(final String code) {

        List<ModelProperties> modelPropertiesList = new ArrayList<ModelProperties>();

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
                return null; // empty Journalizing Knowledge Module
            }
        };
        modelPropertiesList.add(mp);
        return modelPropertiesList;
    }

    @Test
    public void testOneToOneMappingGenerationImpl() {
        fixture = new OneToOneMappingGenerationImpl(databaseMetadataService,
                modelPropertiesProvider, errorWarningMessages);
    }

    /**
     * Test method for genExtractTables when all values are enabled.
     */
    @Test
    public void testGenExtractTablesAllEnabled() {
        String expectedDir = outputXMLDir;

        fixture = new OneToOneMappingGenerationImpl(databaseMetadataService,
                modelPropertiesProvider, errorWarningMessages);
        List<ModelProperties> mockModelPropertiesProvider =
                createModelProperties(testModel);

        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(
                mockModelPropertiesProvider);

        fixture.genExtractTables(sourceModelName, targetModelName,
                this.packageSequence, outputXMLDir);
        assertEquals(expectedDir, outputXMLDir);
        assert (mockDataStores != null);
        assert (new File(expectedDir) != null);

        assertEquals(mockDataStores.size(), new File(expectedDir).list().length);
    }

    /**
     * Test method for genExtractTables when sourceModelName is null.
     */
    @Test
    public void testGenExtractTablesNullSourceModel() {

        fixture = new OneToOneMappingGenerationImpl(databaseMetadataService,
                modelPropertiesProvider, errorWarningMessages);
        List<ModelProperties> mockModelPropertiesProvider =
                createModelProperties(testModel);

        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(
                mockModelPropertiesProvider);

        try {
            fixture.genExtractTables(null, targetModelName,
                    this.packageSequence, outputXMLDir);
        } catch (RuntimeException r) {
            logger.error(r.getMessage());
        }
    }

    /**
     * Test method for genExtractTables when targetModelName is null.
     */
    @Test
    public void testGenExtractTablesNullTargetModel() {

        fixture = new OneToOneMappingGenerationImpl(databaseMetadataService,
                modelPropertiesProvider, errorWarningMessages);
        List<ModelProperties> mockModelPropertiesProvider =
                createModelProperties(testModel);

        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(
                mockModelPropertiesProvider);

        try {
            fixture.genExtractTables(sourceModelName, null,
                    this.packageSequence, outputXMLDir);
        } catch (RuntimeException r) {
            logger.error(r.getMessage());
        }
    }

    /**
     * Test method for genExtractTables when targetModelName is not null and the
     * testModelCode is null.
     */
    @Test
    public void testGenExtractTablesNullGetModelCodeNullTargetModel() {

        testModel = null;

        fixture = new OneToOneMappingGenerationImpl(databaseMetadataService,
                modelPropertiesProvider, errorWarningMessages);
        List<ModelProperties> mockModelPropertiesProvider =
                createModelProperties(testModel);

        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(
                mockModelPropertiesProvider);

        try {
            fixture.genExtractTables(sourceModelName, null,
                    this.packageSequence, outputXMLDir);
        } catch (RuntimeException r) {
            logger.error(r.getMessage());
        }
    }

    /**
     * Test method for genExtractTables when sourceModelName and targetModelName
     * are both null.
     */
    @Test
    public void testGenExtractTablesNullSourceModelNullTargetModel() {

        fixture = new OneToOneMappingGenerationImpl(databaseMetadataService,
                modelPropertiesProvider, errorWarningMessages);
        List<ModelProperties> mockModelPropertiesProvider =
                createModelProperties(testModel);

        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(
                mockModelPropertiesProvider);

        try {
            fixture.genExtractTables(null, null, this.packageSequence,
                    outputXMLDir);
        } catch (RuntimeException r) {
            logger.error(r.getMessage());
        }
    }

    /**
     * Test method for genExtractTables when the testModel value is null. This
     * variable is used in the creation of the list of ModelProperties.
     */
    @Test
    public void testGenExtractTablesNullGetModelCode() {

        testModel = null;

        fixture = new OneToOneMappingGenerationImpl(databaseMetadataService,
                modelPropertiesProvider, errorWarningMessages);
        List<ModelProperties> mockModelPropertiesProvider =
                createModelProperties(testModel);

        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(
                mockModelPropertiesProvider);

        try {
            fixture.genExtractTables(sourceModelName, targetModelName,
                    this.packageSequence, outputXMLDir);
        } catch (RuntimeException r) {
            logger.error(r.getMessage());
        }
    }
}
