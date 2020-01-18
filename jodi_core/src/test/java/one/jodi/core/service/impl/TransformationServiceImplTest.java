package one.jodi.core.service.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.context.Context;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.model.MockDatastoreHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.service.metadata.DataModelDescriptor;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.executionlocation.ExecutionLocationContext;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.targetcolumn.FlagsContext;
import one.jodi.core.transformation.TransformationNameContext;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.builder.DeleteTransformationContext;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.service.datastore.DatastoreServiceProvider;
import one.jodi.etl.service.interfaces.TransformationServiceProvider;
import one.jodi.etl.service.scenarios.ScenarioServiceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The class <code>TransformationServiceImplTest</code> contains tests for the
 * class {@link TransformationServiceImpl}.
 *
 */

@RunWith(JUnit4.class)
public class TransformationServiceImplTest {
    @Mock
    TransformationServiceProvider mockTransformationServiceProvider;
    @Mock
    TransformationBuilder transformationBuilder;
    @Mock
    DatastoreServiceProvider mockDataStoreService;
    @Mock
    SchemaMetaDataProvider mockETLProvider;
    @Mock
    Context context;
    @Mock
    JodiProperties mockProperties;
    @Mock
    ModelPropertiesProvider modelPropertiesProvider;
    @Mock
    ModelCodeContext modelCodeContext;
    MockMetadataServiceProvider mockMetadataProvider;
    @Mock
    FlagsContext flagsContext;
    @Mock
    KnowledgeModuleContext knowledgeModuleContext;
    @Mock
    ExecutionLocationContext executionLocationContext;
    @Mock
    EnrichingBuilder enrichingBuilder;
    @Mock
    ETLValidator validator;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    ScenarioServiceProvider mockScenarioServiceProvider;
    @Mock
    EtlSubSystemVersion etlSubSystemVersion;
    String prefix = "test";
    TransformationNameContext transformationNameContext;
    TransformationServiceImpl fixture;
    private ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Creates a new TransformationServiceImplTest instance.
     */
    public TransformationServiceImplTest() {
    }

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMetadataProvider = new MockMetadataServiceProvider();

        transformationNameContext = mock(TransformationNameContext.class);

        fixture = new TransformationServiceImpl(
                mockTransformationServiceProvider, transformationBuilder,
                mockDataStoreService, mockMetadataProvider,
                mockETLProvider, mockProperties,
                enrichingBuilder, databaseMetadataService, errorWarningMessages,
                mockScenarioServiceProvider,
                etlSubSystemVersion) {
            protected void applySetOperator(Dataset dataset, SetOperatorTypeEnum e) {

            }
        };
        when(mockETLProvider.existsProject("PCODE")).thenReturn(true);
    }

    /**
     * Perform post-test clean up.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Run the void createOrReplaceTransformations(String, boolean) method test.
     *
     * @TODO enable
     */
    @Test
    public void testCreateOrReplaceTransformations() {
        String projectCode = "PCODE";
        boolean journalized = false;

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper.createMockETLTransformation(null);
        when(mockTransformation.getName()).thenReturn("targetdatastore");
        when(mockTransformation.getMappings().getTargetDataStore()).thenReturn("targetdatastore");
        when(mockTransformation.getMappings().getModel()).thenReturn("model");
        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);

        //creates list of Data Store descriptor to be returned by  ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor("targetdatastore", "model", false);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);
        final String transformationName = "T";
        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        fixture.createOrReplaceTransformations(journalized);
    }

    /**
     * DOCUMENT ME!
     */
    @Test
    public void testCreateTempTransformation() {
        final String transformationName = InputModelMockHelper.TARGET_STORE;
        String projectCode = "PCODE";
        boolean journalized = false;

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper.createMockETLTransformation(null);
        when(mockTransformation.getName()).thenReturn("targetdatastore");
        when(mockTransformation.getMappings().getTargetDataStore()).thenReturn(transformationName);
        when(mockTransformation.getMappings().getModel()).thenReturn("model");
        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);

        //creates list of Data Store descriptor to be returned by ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor(transformationName, "model", true);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        when(databaseMetadataService.isTemporaryTransformation(transformationName))
                .thenReturn(true);

        DataStore targetDataStore = mock(DataStore.class);
        when(targetDataStore.isTemporary()).thenReturn(true);
        when(databaseMetadataService.getTargetDataStoreInModel(mockTransformation.getMappings())).thenReturn(targetDataStore);
        when(databaseMetadataService.getTargetDataStoreInModel(any(Mappings.class))).thenReturn(targetDataStore);


        when(transformationNameContext.getTransformationName(mockTransformation)).thenReturn("TEST" + transformationName);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return true;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        fixture.createOrReplaceTransformations(journalized);

    }

    /**
     * Run the void createTransformation(Transformation, String, int) method
     * test.
     */
    @Test
    public void testCreateTransformation() {
        final String transformationName = InputModelMockHelper.TARGET_STORE;
        String model = "model";
        String projectCode = "PCODE";
        boolean journalized = false;

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper.createMockETLTransformation("TYPE");
        when(mockTransformation.getName()).thenReturn(transformationName);
        when(mockTransformation.getMappings().getModel()).thenReturn(model);
        when(mockTransformation.getMappings().getTargetDataStore()).thenReturn(transformationName);
        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);


        //creates list of Data Store descriptor to be returned by ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor(transformationName, model, false);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);
        List<DataModelDescriptor> dataModelDescr = Collections.singletonList(dsMockMap.get(transformationName).getDataModelDescriptor());
        when(mockETLProvider.getDataModelDescriptors()).thenReturn(dataModelDescr);
        when(modelCodeContext.getModelCode(any(Mappings.class))).thenReturn(model);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties(model);
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        when(databaseMetadataService.isTemporaryTransformation(transformationName))
                .thenReturn(true);

        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        fixture.createOrReplaceTransformations(journalized);
    }


    @Test
    public void testCreateTransformations() {
        String transformationName = "testetransformationname";
        String transformationName2 = "testetransformationname2";
        String projectCode = "PCODE";
        boolean journalized = false;

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation1 = InputModelMockHelper.createMockETLTransformation(null);
        when(mockTransformation1.getName()).thenReturn(transformationName);
        when(mockTransformation1.getMappings().getTargetDataStore()).thenReturn("targetdatastore");
        Transformation mockTransformation2 = InputModelMockHelper.createMockETLTransformation(null);
        when(mockTransformation2.getMappings().getTargetDataStore()).thenReturn("targetdatastore2");
        when(mockTransformation2.getName()).thenReturn(transformationName2);
        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation1, mockTransformation2);

        //creates list of Data Store descriptor to be returned by ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor("targetdatastore", "model", false);
        dsMockMap.putAll(MockDatastoreHelper.createMockDSDescriptor("targetdatastore2", "model", false));
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        fixture.createTransformations(journalized);
    }

    /**
     * DOCUMENT ME!
     */
    @Test//(expected = ValidationException.class)
    public void testCreateTransformationWithDuplicateReferencesToSameTableName() {
        String projectCode = "PCODE";
        boolean journalized = false;

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper
                .createMockETLTransformation(null);
        Dataset mockDS = InputModelMockHelper.createMockMultiSourceETLDataset(
                new String[]{
                        null, null
                }, new String[]{
                        "name1", "name1"
                });
        when(mockTransformation.getDatasets()).thenReturn(Collections.singletonList(mockDS));

        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);

        //creates list of Data Store descriptor to be returned by  ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor("targetdatastore", "model", false);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);
        final String transformationName = "TransformationName";
        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        fixture.createOrReplaceTransformations(journalized);
    }

    /**
     * DOCUMENT ME!
     */
    //@Test (expected = ValidationException.class)
    public void testCreateTransformationWithInvalidSourceTableName() {
        String projectCode = "PCODE";
        boolean journalized = false;

        Mappings mockMappings = InputModelMockHelper.createMockETLMappings(
                "0123456789012345678901234567890123456789", null, "model", 1);

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper
                .createMockETLTransformation("packagelist", new String[]{
                        "alias"
                }, new String[]{
                        "name"
                }, new String[]{
                        "model"
                }, mockMappings);


        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        //creates list of Data Store descriptor to be returned by ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor("0123456789012345678901234567890123456789", "model", false);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        final String transformationName = "TNAME";
        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        fixture.createOrReplaceTransformations(journalized);
    }

    /**
     * DOCUMENT ME!
     */
    @Test
    public void testCreateTransformationWithMultiSourceDataset() {
        final String transformationName = InputModelMockHelper.TARGET_STORE;
        String projectCode = "PCODE";
        boolean journalized = false;

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper.createMockETLTransformation(null);
        when(mockTransformation.getName()).thenReturn(transformationName);
        when(mockTransformation.getMappings().getTargetDataStore()).thenReturn(transformationName);
        when(mockTransformation.getMappings().getModel()).thenReturn("model");

        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        Dataset mockDS = InputModelMockHelper.createMockMultiSourceETLDataset(
                new String[]{
                        "alias1", "alias2"
                }, new String[]{
                        "name1", "name2"
                });
        when(mockTransformation.getDatasets()).thenReturn(
                Collections.singletonList(mockDS));

        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        //creates list of Data Store descriptor to be returned by  ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor(transformationName, "model", false);
        dsMockMap.putAll(MockDatastoreHelper.createMockDSDescriptor(transformationName + "2", "model", false));
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);
        fixture.createOrReplaceTransformations(journalized);
    }

    /**
     * DOCUMENT ME!
     */
    //@Test(expected = ValidationException.class)
    public void testCreateTransformationWithMultiSourceDatasetWithDuplicateAlias() {
        String projectCode = "PCODE";
        boolean journalized = false;

        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper
                .createMockETLTransformation(null);
        Dataset mockDS = InputModelMockHelper.createMockMultiSourceETLDataset(
                new String[]{
                        "alias", "alias"
                }, new String[]{
                        "name1", "name2"
                });
        when(mockTransformation.getDatasets()).thenReturn(
                Collections.singletonList(mockDS));

        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        //creates list of Data Store descriptor to be returned by  ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor("targetdatastore", "model", false);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        final String transformationName = "TNAME";
        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        try {
            fixture.createOrReplaceTransformations(journalized);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ErrorWarningMessageJodiImpl.getInstance().printMessages();
    }

    /**
     * DOCUMENT ME!
     */
    @Test
    public void testCreateTransformationWithNullPackageList() {
        final String transformationName = "testetransformationname";
        String projectCode = "PCODE";
        boolean journalized = false;
        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);
        Transformation mockTransformation = InputModelMockHelper
                .createMockETLTransformation(null, new String[]{
                        "alias"
                }, new String[]{
                        "name"
                }, new String[]{
                        null
                });
        when(mockTransformation.getPackageList()).thenReturn(null);
        when(mockTransformation.getName()).thenReturn(transformationName);

        //creates list of Data Store descriptor to be returned by  ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor("targetdatastore", "model", false);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        mockMetadataProvider.setETLMetadata(plist);
        mockMetadataProvider.addTransformationMetadata(mockTransformation);

        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        when(mockProperties.getProjectCode()).thenReturn(projectCode);
        //thrown.expect(ValidationException.class);
        fixture.createOrReplaceTransformations(journalized);
    }

    /**
     * Run the void deleteTransformation(Transformation, String) method test.
     */
    @Test
    public void testDeleteTransformation() {
        final String transformationName = InputModelMockHelper.TARGET_STORE;
        String projectCode = "PCODE";

        Transformation mockTransformation = InputModelMockHelper.createMockETLTransformation(null);
        when(mockTransformation.getName()).thenReturn(prefix + transformationName);
        //when(transformationNameContext.getTransformationName(mockTransformation)).thenReturn(prefix + transformationName);
        when(mockTransformation.getMappings().getTargetDataStore()).thenReturn(prefix + transformationName);
        when(mockTransformation.getMappings().getModel()).thenReturn("model");

        //creates list of Data Store descriptor to be returned by  ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor(transformationName, "model", false);
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        List<ModelProperties> modelPropertiesList = createModelProperties("model");
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        when(enrichingBuilder.createDeleteContext(mockTransformation, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        fixture.deleteTransformation(mockTransformation, false);
    }

    /**
     * Run the void deleteTransformation(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteTransformationByName() throws Exception {
        String transformationName = "testetransformationname";
        String projectCode = "PCODE";
        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        fixture.deleteTransformation(transformationName, null);
        verify(mockTransformationServiceProvider).deleteTransformation(
                transformationName, null);
    }

    /**
     * DOCUMENT ME!
     */
    @Test
    public void testDeleteTransformations() {
        final String transformationName = "testetransformationname";
        final String transformationName2 = "testetransformationname2";
        String model = "model";
        String projectCode = "PCODE";

        Transformation mockTransformation1 = InputModelMockHelper.createMockETLTransformation(transformationName);
        when(mockTransformation1.getName()).thenReturn(transformationName);
        Transformation mockTransformation2 = InputModelMockHelper.createMockETLTransformation(transformationName2);
        when(mockTransformation2.getName()).thenReturn(transformationName2);
        mockMetadataProvider.addTransformationMetadata(mockTransformation1,
                mockTransformation2);

        //creates list of Data Store descriptor to be returned by  ETLProvider subsystem
        Map<String, DataStoreDescriptor> dsMockMap;
        dsMockMap = MockDatastoreHelper.createMockDSDescriptor(transformationName, model, false);
        dsMockMap.putAll(MockDatastoreHelper.createMockDSDescriptor(transformationName2, model, false));
        when(mockETLProvider.getDataStoreDescriptorsInModel(anyString())).thenReturn(dsMockMap);

        //matching model property information for the data store descriptor used above
        List<ModelProperties> modelPropertiesList = createModelProperties(model);
        when(modelPropertiesProvider.getConfiguredModels()).thenReturn(modelPropertiesList);

        when(mockProperties.getProjectCode()).thenReturn(projectCode);

        when(modelCodeContext.getModelCode(any(Mappings.class))).thenReturn(model);


        when(enrichingBuilder.createDeleteContext(mockTransformation1, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName;
            }

            @Override
            public String getDataStoreName() {
                return transformationName;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });


        when(enrichingBuilder.createDeleteContext(mockTransformation2, false)).thenReturn(new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformationName2;
            }

            @Override
            public String getDataStoreName() {
                return transformationName2;
            }

            @Override
            public String getModel() {
                return "model";
            }

            @Override
            public int getPackageSequence() {
                return 123;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getFolderName() {
                return null;
            }
        });

        fixture.deleteTransformations(false);
    }

    //
    // helper methods
    //

    List<ModelProperties> createModelProperties(final String code) {

        List<ModelProperties> modelPropertiesList = new ArrayList<ModelProperties>();

        ModelProperties mp = new ModelProperties() {

            @Override
            public String getModelID() {
                return "model_def";
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
            public List<String> getJkmoptions() {
                return null;
            }

            @Override
            public List<String> getSubscribers() {
                List<String> subs = new ArrayList<String>();
                subs.add("SUNOPSIS");
                return subs;
            }

            @Override
            public String getJkm() {
                return null; // Empty Journalizing Knowledge Module.
            }

        };

        modelPropertiesList.add(mp);

        return modelPropertiesList;
    }

}
