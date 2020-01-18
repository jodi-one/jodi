package one.jodi.core.km.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.config.km.KnowledgeModulePropertiesImpl;
import one.jodi.core.config.km.KnowledgeModulePropertiesProvider;
import one.jodi.core.extensions.contexts.CheckKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.KnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.LoadKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.StagingKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.strategies.KnowledgeModuleStrategy;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.metadata.ETLSubsystemService;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.SourceImpl;
import one.jodi.etl.km.KnowledgeModuleType;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KnowledgeModuleContextImplTest {
    KnowledgeModuleContextImpl fixture = null;

    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    ETLSubsystemService etlSubsystemService;
    @Mock
    JodiProperties jodiProperties;
    @Mock
    KnowledgeModulePropertiesProvider provider;
    @Mock
    ETLValidator validator;

    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(KnowledgeModuleContextImplTest.class);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private void configure(Map<String, String> propertiesMap) {

        when(jodiProperties.getPropertyKeys()).thenReturn(new ArrayList<String>(propertiesMap.keySet()));

        for (String key : propertiesMap.keySet()) {
            when(jodiProperties.getProperty(key)).thenReturn(propertiesMap.get(key));
            when(jodiProperties.getPropertyList(key)).thenReturn(Arrays.asList(propertiesMap.get(key).split(",")));
        }
    }

    private KnowledgeModuleProperties buildRule(String id, boolean global) {
        KnowledgeModulePropertiesImpl rule = new KnowledgeModulePropertiesImpl();
        rule.setId(id);
        rule.setGlobal(global);

        return rule;
    }

    @Test(expected = RuntimeException.class)
    public void test_prevalidate_rule_not_found() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm2.name", "ikm1");
        configure(map);
        Transformation transformation = buildIKMTransformation("km.ikm1", new String[]{"option1", "option2"}, new String[]{"value1", "value2"});
        ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();

        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);

        fixture = new KnowledgeModuleContextImpl(
                databaseMetadataService,
                etlSubsystemService,
                new StrategyTester(),
                provider,
                new StrategyTester(),
                jodiProperties,
                validator, errorWarningMessages);

        fixture.getIKMConfig(transformation);
    }

    @Test(expected = RuntimeException.class)
    public void test_prevalidate_global_rule() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        Transformation transformation = buildIKMTransformation("km.ikm1", new String[]{"option1", "option2"}, new String[]{"value1", "value2"});
        ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", true));  // global
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);

        fixture = new KnowledgeModuleContextImpl(
                databaseMetadataService,
                etlSubsystemService,
                new StrategyTester(),
                provider,
                new StrategyTester(),
                jodiProperties,
                validator, errorWarningMessages);

        fixture.getIKMConfig(transformation);
    }

    @Test
    public void test_integration() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        Transformation transformation = buildIKMTransformation("km.ikm1", new String[]{"option1", "option2"}, new String[]{"value1", "value2"});
        ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);

        fixture = new KnowledgeModuleContextImpl(
                databaseMetadataService,
                etlSubsystemService,
                new StrategyTester(),
                provider,
                new StrategyTester(), jodiProperties, validator, errorWarningMessages);
        when(validator.validateIKM(any(Mappings.class))).thenReturn(true);
        fixture.getIKMConfig(transformation);
    }

    @Test(expected = RuntimeException.class)
    public void test_postvalidate_default_strategy_rule_not_found() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        Transformation transformation = buildIKMTransformation("km.ikm1", new String[]{"option1", "option2"}, new String[]{"value1", "value2"});
        ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);
        fixture = new KnowledgeModuleContextImpl(
                databaseMetadataService,
                etlSubsystemService,
                new StrategyTester(null, null, "km.ikm3"),
                provider,
                new StrategyTester(), jodiProperties, validator, errorWarningMessages);

        fixture.getIKMConfig(transformation);
    }

    @Test(expected = RuntimeException.class)
    public void test_postvalidate_custom_strategy_rule_not_found() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        Transformation transformation = buildIKMTransformation("km.ikm1", new String[]{"option1", "option2"}, new String[]{"value1", "value2"});
        ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);

        fixture = new KnowledgeModuleContextImpl(
                databaseMetadataService,
                etlSubsystemService,
                new StrategyTester(),
                provider,
                new StrategyTester(null, null, "km.ikm3"), jodiProperties, validator, errorWarningMessages);

        fixture.getIKMConfig(transformation);
    }

    @Test
    public void test_integration_with_ikm() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        final String kmCode = "km.ikm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};
        final Transformation transformation = buildIKMTransformation(kmCode, kmOptions, kmValues);

        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);
        final MappingsExtension mExtension = mock(MappingsExtension.class);
        when(transformation.getMappings().getExtension()).thenReturn(mExtension);

        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));

        final DataStore dataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);
        Mappings mappings = transformation.getMappings();
        when(mappings.getModel()).thenReturn("modelCode");
        when(databaseMetadataService.getTargetDataStoreInModel(transformation.getMappings())).thenReturn(dataStore);

        StrategyTester st = new StrategyTester() {
            @Override
            public KnowledgeModuleConfiguration getIKMConfig(
                    KnowledgeModuleConfiguration configuration,
                    KnowledgeModuleExecutionContext executionContext) {
                assertEquals(configuration.getName(), transformation.getMappings().getIkm().getName());

                assertEquals(transformation.getMappings().getIkm().getOptions().size(), configuration.getOptionKeys().size());
                for (String k : transformation.getMappings().getIkm().getOptions().keySet()) {
                    assertEquals(transformation.getMappings().getIkm().getOptions().get(k), configuration.getOptionValue(k));
                }

                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());
                return configuration;
            }
        };

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, st, provider, new StrategyTester(),
                jodiProperties, validator, errorWarningMessages);

        when(validator.validateIKM(mappings)).thenReturn(true);
        fixture.getIKMConfig(transformation);
    }

    @Test
    public void test_integration_without_ikm() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        final Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);
        final Mappings mappings = mock(Mappings.class);
        when(transformation.getMappings()).thenReturn(mappings);
        final MappingsExtension mExtension = mock(MappingsExtension.class);
        when(mappings.getExtension()).thenReturn(mExtension);
        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));

        final DataStore dataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);
        when(mappings.getModel()).thenReturn("modelCode");
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(dataStore);

        StrategyTester st = new StrategyTester() {
            @Override
            public KnowledgeModuleConfiguration getIKMConfig(
                    KnowledgeModuleConfiguration configuration,
                    KnowledgeModuleExecutionContext executionContext) {
                assertNull(configuration);

                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());

                KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
                kmc.setName("km.ikm1");
                return kmc;
            }
        };

        fixture = new KnowledgeModuleContextImpl(
                databaseMetadataService,
                etlSubsystemService,
                st,
                provider,
                new StrategyTester(), jodiProperties, validator, errorWarningMessages);

        fixture.getIKMConfig(transformation);
    }

    @Test
    public void test_check_with_ckm() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ckm1.name", "ckm1");
        configure(map);
        final String kmCode = "km.ckm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};
        final Transformation transformation = buildCKMTransformation(kmCode, kmOptions, kmValues);
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);
        final MappingsImpl mappings = (MappingsImpl) transformation.getMappings();
        final MappingsExtension mExtension = mock(MappingsExtension.class);
        mappings.setExtension(mExtension);
        mappings.setModel("modelCode");

        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ckm1", false));

        final DataStore dataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Check)).thenReturn(rules);
        when(databaseMetadataService.getTargetDataStoreInModel(transformation.getMappings())).thenReturn(dataStore);

        StrategyTester st = new StrategyTester() {
            @Override
            public KnowledgeModuleConfiguration getCKMConfig(
                    KnowledgeModuleConfiguration configuration,
                    CheckKnowledgeModuleExecutionContext executionContext) {
                assertEquals(configuration.getName(), transformation.getMappings().getCkm().getName());

                assertEquals(transformation.getMappings().getCkm().getOptions().size(), configuration.getOptionKeys().size());
                for (String k : transformation.getMappings().getCkm().getOptions().keySet()) {
                    assertEquals(transformation.getMappings().getCkm().getOptions().get(k), configuration.getOptionValue(k));
                }

                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());
                return configuration;
            }
        };

        when(validator.validateCKM(mappings)).thenReturn(true);

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, st, provider, new StrategyTester(),
                jodiProperties, validator, errorWarningMessages);

        fixture.getCKMConfig(transformation);
    }

    @Test
    public void test_check_without_ckm() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ckm1.name", "ckm1");
        configure(map);

        final Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);

        MappingsImpl mappings = new MappingsImpl();
        when(transformation.getMappings()).thenReturn(mappings);

        final MappingsExtension mExtension = mock(MappingsExtension.class);
        mappings.setExtension(mExtension);

        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ckm1", false));

        final DataStore dataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Check)).thenReturn(rules);
        mappings.setModel("modelCode");
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(dataStore);

        StrategyTester st = new StrategyTester() {
            @Override
            public KnowledgeModuleConfiguration getCKMConfig(
                    KnowledgeModuleConfiguration configuration,
                    CheckKnowledgeModuleExecutionContext executionContext) {
                assertNull(configuration);

                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());

                KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
                kmc.setName("km.ckm1");
                return kmc;
            }
        };

        when(validator.validateCKM(mappings)).thenReturn(true);


        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, st, provider, new StrategyTester(),
                jodiProperties, validator, errorWarningMessages);

        fixture.getCKMConfig(transformation);
    }

    @Test
    public void test_loading_execution_without_lkm() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.lkm1.name", "lkm1");
        configure(map);
        final Transformation transformation = mock(Transformation.class);
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getParent()).thenReturn(transformation);
        when(dataset.getName()).thenReturn("dataset-0");
        SourceImpl source = new SourceImpl();
        source.setModel("sourceModel");
        source.setName("sourceDataStore");
        source.setParent(dataset);
        when(dataset.getSources()).thenReturn(Collections.<Source>singletonList(source));
        ArrayList<Dataset> datasetList = new ArrayList<Dataset>();
        datasetList.add(dataset);
        when(transformation.getDatasets()).thenReturn(datasetList);
        MappingsImpl mappings = new MappingsImpl();
        when(transformation.getMappings()).thenReturn(mappings);
        mappings.setParent(transformation);
        final MappingsExtension mExtension = mock(MappingsExtension.class);
        mappings.setExtension(mExtension);

        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.lkm1", false));

        final DataStore targetDataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Loading)).thenReturn(rules);
        mappings.setModel("targetModelCode");
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);

        final DataStore sourceDataStore = mock(DataStore.class);
        when(databaseMetadataService.getSourceDataStoreInModel("sourceDataStore", "sourceModel")).thenReturn(sourceDataStore);

        mappings.setStagingModel("stagingModel");
        final DataModel stagingDataModel = mock(DataModel.class);
        when(databaseMetadataService.getDataModel("stagingModel")).thenReturn(stagingDataModel);


        StrategyTester st = new StrategyTester() {
            @Override
            public KnowledgeModuleConfiguration getLKMConfig(
                    KnowledgeModuleConfiguration configuration,
                    LoadKnowledgeModuleExecutionContext executionContext) {
                assertNull(configuration);

                assertEquals(targetDataStore, executionContext.getTargetDataStore());
                assertEquals(sourceDataStore, executionContext.getSourceDataStore());
                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());
                assertEquals(stagingDataModel, executionContext.getStagingDataModel());

                KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
                kmc.setName("km.lkm1");
                return kmc;
            }
        };

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, st, provider, new StrategyTester(),
                jodiProperties, validator, errorWarningMessages);

        fixture.getLKMConfig(source, "dataset-0");
    }

    @Test
    public void test_loading_execution_with_source_lkm() {
        String sourceName = "sourceName";
        String sourceModel = "sourceModel";
        String targetModel = "targetModel";
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.lkm1.name", "lkm1");
        configure(map);
        final String kmCode = "km.lkm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};
        final Transformation transformation = mock(Transformation.class);
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getParent()).thenReturn(transformation);
        when(dataset.getName()).thenReturn("dataset-0");
        ArrayList<Dataset> datasets = new ArrayList<Dataset>();
        datasets.add(dataset);
        when(transformation.getDatasets()).thenReturn(datasets);
        Mappings mappings = mock(Mappings.class);
        when(transformation.getMappings()).thenReturn(mappings);
        when(mappings.getParent()).thenReturn(transformation);
		/*Source source = mock(Source.class);
		when(source.getParent()).thenReturn(dataset);
		when(source.getName()).thenReturn(sourceName);
		when(source.getModel()).thenReturn(sourceModel);
		*/
        SourceImpl source = new SourceImpl();
        source.setParent(dataset);
        source.setName(sourceName);
        source.setModel(sourceModel);
        when(dataset.getSources()).thenReturn(Collections.<Source>singletonList(source));
        KmType kmType = buildMockType(kmCode, kmOptions, kmValues);
        //when(source.getLkm()).thenReturn(kmType);
        source.setLkm(kmType);
        final MappingsExtension mExtension = mock(MappingsExtension.class);
        when(mappings.getExtension()).thenReturn(mExtension);


        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.lkm1", false));

        final DataStore targetDataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Loading)).thenReturn(rules);
        when(transformation.getMappings().getModel()).thenReturn(targetModel);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);

        final DataStore sourceDataStore = mock(DataStore.class);
        when(databaseMetadataService.getSourceDataStoreInModel(sourceName, sourceModel)).thenReturn(sourceDataStore);

        StrategyTester st = new StrategyTester() {
            @Override
            public KnowledgeModuleConfiguration getLKMConfig(
                    KnowledgeModuleConfiguration configuration,
                    LoadKnowledgeModuleExecutionContext executionContext) {

                assertEquals(configuration.getName(), transformation.getDatasets().get(0).getSources().get(0).getLkm().getName());

                assertEquals(transformation.getDatasets().get(0).getSources().get(0).getLkm().getOptions().size(), configuration.getOptionKeys().size());
                for (String k : transformation.getDatasets().get(0).getSources().get(0).getLkm().getOptions().keySet()) {
                    assertEquals(transformation.getDatasets().get(0).getSources().get(0).getLkm().getOptions().get(k), configuration.getOptionValue(k));
                }

                assertEquals(targetDataStore, executionContext.getTargetDataStore());
                assertEquals(sourceDataStore, executionContext.getSourceDataStore());
                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());

                KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
                kmc.setName("km.lkm1");
                return kmc;
            }
        };

        when(validator.validateLKM(source)).thenReturn(true);
        when(validator.validateLKM(source, st)).thenReturn(true);

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, st, provider, new StrategyTester(),
                jodiProperties, validator, errorWarningMessages);

        fixture.getLKMConfig(source, "dataset-0");
    }

    private Transformation buildCKMTransformation(String code, String[] options, String[] values) {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        MappingsImpl mappings = new MappingsImpl();
        when(transformation.getMappings()).thenReturn(mappings);
        KmType kmType = buildMockType(code, options, values);
        mappings.setCkm(kmType);
        mappings.setTargetDataStore("targetDataStore");
        return transformation;
    }

    private Transformation buildIKMTransformation(String code, String[] options, String[] values) {
        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        Mappings mappings = mock(Mappings.class);
        when(transformation.getMappings()).thenReturn(mappings);
        KmType kmType = buildMockType(code, options, values);
        when(mappings.getIkm()).thenReturn(kmType);
        when(mappings.getTargetDataStore()).thenReturn("targetDataStore");
        return transformation;
    }

    private KmType buildMockType(String code, String[] options, String[] values) {
        KmType type = mock(KmType.class);
        when(type.getName()).thenReturn(code);
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < options.length; i++) {
            map.put(options[i], values[i]);
        }
        when(type.getOptions()).thenReturn(map);

        return type;
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testGetLKMConfigDefaultStrategyRuntimeException() throws Exception {
        final String kmCode = "km.lkm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};
        final Transformation transformation = mock(Transformation.class);

        final Dataset dataset = mock(Dataset.class);
        when(dataset.getParent()).thenReturn(transformation);
        when(dataset.getName()).thenReturn("dataset-0");
        ArrayList<Dataset> datasetList = new ArrayList<Dataset>();
        datasetList.add(dataset);
        when(transformation.getDatasets()).thenReturn(datasetList);
        Mappings mappings = mock(Mappings.class);
        when(transformation.getMappings()).thenReturn(mappings);
        KmType kmType = buildMockType(kmCode, kmOptions, kmValues);
        //when(dataset.getLkm()).thenReturn(kmType);
        KnowledgeModuleStrategy defaultStrategy = mock(KnowledgeModuleStrategy.class);
        Source source = mock(Source.class);
        when(source.getLkm()).thenReturn(kmType);

        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.lkm1", false));
        when(provider.getProperties(KnowledgeModuleType.Loading)).thenReturn(rules);
        when(defaultStrategy.getLKMConfig(any(KnowledgeModuleConfigurationImpl.class), any(LoadKnowledgeModuleExecutionContext.class))).thenThrow(RuntimeException.class);
        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, defaultStrategy, provider,
                new StrategyTester(), jodiProperties, validator,
                errorWarningMessages);
        fixture.getLKMConfig(source, "");
        fail();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testGetLKMConfigCustomStrategyIncorrectCustomStrategyException() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.lkm1.name", "lkm1");
        configure(map);
        final String kmCode = "km.lkm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};

        final Transformation transformation = mock(Transformation.class);

        final Dataset dataset = mock(Dataset.class);
        when(dataset.getParent()).thenReturn(transformation);
        when(dataset.getName()).thenReturn("dataset-0");
        ArrayList<Dataset> datasetList = new ArrayList<Dataset>();
        datasetList.add(dataset);
        when(transformation.getDatasets()).thenReturn(datasetList);
        Mappings mappings = mock(Mappings.class);
        when(transformation.getMappings()).thenReturn(mappings);
        KmType kmType = buildMockType(kmCode, kmOptions, kmValues);
        //when(dataset.getLkm()).thenReturn(kmType);
        KnowledgeModuleStrategy defaultStrategy = mock(KnowledgeModuleStrategy.class);
        Source source = mock(Source.class);
        when(source.getLkm()).thenReturn(kmType);
        when(source.getParent()).thenReturn(dataset);


        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.lkm1", false));

        final DataStore targetDataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Loading)).thenReturn(rules);
        when(transformation.getMappings().getModel()).thenReturn("targetModelCode");
        when(databaseMetadataService.getTargetDataStoreInModel(null)).thenReturn(targetDataStore);

        final DataStore sourceDataStore = mock(DataStore.class);
        when(databaseMetadataService.getTargetDataStoreInModel(null)).thenReturn(sourceDataStore);

        KnowledgeModuleStrategy customStrategy = mock(KnowledgeModuleStrategy.class);

        KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
        kmc.setName("km.lkm1");

        when(defaultStrategy.getLKMConfig(any(KnowledgeModuleConfiguration.class), any(LoadKnowledgeModuleExecutionContext.class))).thenReturn(kmc);
        when(customStrategy.getLKMConfig(any(KnowledgeModuleConfigurationImpl.class), any(LoadKnowledgeModuleExecutionContext.class))).thenThrow(RuntimeException.class);
        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, new StrategyTester(), provider,
                customStrategy, jodiProperties, validator, errorWarningMessages);
        fixture.getLKMConfig(source, "");
        fail();
    }


    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testGetIKMConfigDefaultStrategyRuntimeException() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        Transformation transformation = buildIKMTransformation("km.ikm1", new String[]{"option1", "option2"}, new String[]{"value1", "value2"});
        ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));
        KnowledgeModuleStrategy defaultStrategy = mock(KnowledgeModuleStrategy.class);
        when(defaultStrategy.getIKMConfig(any(KnowledgeModuleConfigurationImpl.class), any(LoadKnowledgeModuleExecutionContext.class))).thenThrow(RuntimeException.class);
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, defaultStrategy, provider,
                new StrategyTester(), jodiProperties, validator,
                errorWarningMessages);

        fixture.getIKMConfig(transformation);
        fail();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testGetIKMConfigCustomStrategyRuntimeException() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ikm1.name", "ikm1");
        configure(map);
        Transformation transformation = buildIKMTransformation("km.ikm1", new String[]{"option1", "option2"}, new String[]{"value1", "value2"});
        ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));
        KnowledgeModuleStrategy customStrategy = mock(KnowledgeModuleStrategy.class);
        when(customStrategy.getIKMConfig(any(KnowledgeModuleConfigurationImpl.class), any(LoadKnowledgeModuleExecutionContext.class))).thenThrow(RuntimeException.class);
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, new StrategyTester(), provider,
                customStrategy, jodiProperties, validator, errorWarningMessages);

        fixture.getIKMConfig(transformation);
        fail();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testGetCKMConfigDefaultRuntimeException() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ckm1.name", "ckm1");
        configure(map);
        final String kmCode = "km.ckm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};
        final Transformation transformation = buildCKMTransformation(kmCode, kmOptions, kmValues);
        final MappingsImpl mappings = (MappingsImpl) transformation.getMappings();
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);
        final MappingsExtension mExtension = mock(MappingsExtension.class);
        mappings.setExtension(mExtension);

        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ckm1", false));

        final DataStore dataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Check)).thenReturn(rules);
        mappings.setModel("modelCode");
        when(databaseMetadataService.getTargetDataStoreInModel(transformation.getMappings())).thenReturn(dataStore);
        KnowledgeModuleStrategy defaultStrategy = mock(KnowledgeModuleStrategy.class);
        when(defaultStrategy.getCKMConfig(any(KnowledgeModuleConfiguration.class), any(CheckKnowledgeModuleExecutionContext.class))).thenThrow(RuntimeException.class);

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, defaultStrategy, provider,
                new StrategyTester(), jodiProperties, validator,
                errorWarningMessages);

        fixture.getCKMConfig(transformation);

    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testGetCKMConfigCustomRuntimeException() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.ckm1.name", "ckm1");
        configure(map);
        final String kmCode = "km.ckm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};
        final Transformation transformation = buildCKMTransformation(kmCode, kmOptions, kmValues);
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);

        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ckm1", false));

        final DataStore dataStore = mock(DataStore.class);
        when(provider.getProperties(KnowledgeModuleType.Check)).thenReturn(rules);
        //when(transformation.getMappings().getModel()).thenReturn("modelCode");
        ((MappingsImpl) transformation.getMappings()).setModel("modelCode");
        when(databaseMetadataService.getTargetDataStoreInModel(transformation.getMappings())).thenReturn(dataStore);
        KnowledgeModuleStrategy customStrategy = mock(KnowledgeModuleStrategy.class);
        when(customStrategy.getCKMConfig(any(KnowledgeModuleConfiguration.class), any(CheckKnowledgeModuleExecutionContext.class))).thenThrow(RuntimeException.class);
        StrategyTester st = new StrategyTester() {
            @Override
            public KnowledgeModuleConfiguration getCKMConfig(
                    KnowledgeModuleConfiguration configuration,
                    CheckKnowledgeModuleExecutionContext executionContext) {
                assertEquals(configuration.getName(), transformation.getMappings().getCkm().getName());

                assertEquals(transformation.getMappings().getCkm().getOptions().size(), configuration.getOptionKeys().size());
                for (String k : transformation.getMappings().getCkm().getOptions().keySet()) {
                    assertEquals(transformation.getMappings().getCkm().getOptions().get(k), configuration.getOptionValue(k));
                }
                assertEquals(dataStore, executionContext.getTargetDataStore());
                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());
                return configuration;
            }
        };

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, st, provider, customStrategy,
                jodiProperties, validator, errorWarningMessages);

        fixture.getCKMConfig(transformation);

    }

    @Test
    public void test_staging_model() {
        String sourceName = "sourceName";
        String sourceModel = "sourceModel";
        String targetModel = "targetModel";
        String lookupName = "lookupName";
        final String lookupAlias = "lookupAlias";
        final String lookupModel = "lookupModel";
        final String sourceAlias = "sourceAlias";
        final String stagingModel = "stagingModel";
        final String ikmCode = "km.ikm";
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("km.lkm1.name", "lkm1");
        configure(map);
        final String kmCode = "km.lkm1";
        final String[] kmOptions = new String[]{"option1", "option2"};
        final String[] kmValues = new String[]{"value1", "value2"};
        final Transformation transformation = mock(Transformation.class);
        final TransformationExtension tExtension = mock(TransformationExtension.class);
        when(transformation.getExtension()).thenReturn(tExtension);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getParent()).thenReturn(transformation);
        when(dataset.getName()).thenReturn("dataset-0");
        ArrayList<Dataset> datasets = new ArrayList<Dataset>();
        datasets.add(dataset);
        when(transformation.getDatasets()).thenReturn(datasets);
        MappingsImpl mappings = new MappingsImpl();
        mappings.setStagingModel(stagingModel);
        when(transformation.getMappings()).thenReturn(mappings);
        mappings.setParent(transformation);
        Source source = mock(Source.class);
        when(source.getParent()).thenReturn(dataset);
        when(source.getName()).thenReturn(sourceName);
        when(source.getAlias()).thenReturn(sourceAlias);
        when(source.getModel()).thenReturn(sourceModel);
        when(dataset.getSources()).thenReturn(Collections.<Source>singletonList(source));
        final SourceExtension sourceExtension = new SourceExtension();
        when(source.getExtension()).thenReturn(sourceExtension);
        KmType kmType = buildMockType(kmCode, kmOptions, kmValues);
        when(source.getLkm()).thenReturn(kmType);
        final MappingsExtension mExtension = mock(MappingsExtension.class);
        mappings.setExtension(mExtension);
        KmType ikm = mock(KmType.class);
        when(ikm.getName()).thenReturn(ikmCode);
        mappings.setIkm(ikm);

        Lookup lookup = mock(Lookup.class);
        when(lookup.getLookupDataStore()).thenReturn(lookupName);
        when(lookup.getAlias()).thenReturn(lookupAlias);
        when(lookup.getModel()).thenReturn(lookupModel);
        when(lookup.getParent()).thenReturn(source);

        when(source.getLookups()).thenReturn(Collections.singletonList(lookup));


        final ArrayList<KnowledgeModuleProperties> rules = new ArrayList<KnowledgeModuleProperties>();
        rules.add(buildRule("km.ikm1", false));
        when(provider.getProperties(KnowledgeModuleType.Integration)).thenReturn(rules);

        final DataStore targetDataStore = mock(DataStore.class);
        mappings.setModel(targetModel);
        when(databaseMetadataService.getTargetDataStoreInModel(mappings)).thenReturn(targetDataStore);

        final DataStore sourceDataStore = mock(DataStore.class);
        when(databaseMetadataService.getSourceDataStoreInModel(sourceName, sourceModel)).thenReturn(sourceDataStore);

        final DataStore lookupDataStore = mock(DataStore.class);
        when(databaseMetadataService.getSourceDataStoreInModel(lookupName, lookupModel)).thenReturn(lookupDataStore);

        StrategyTester st = new StrategyTester() {
            @Override
            public String getStagingModel(
                    String defaultStagingModel,
                    StagingKnowledgeModuleExecutionContext executionContext) {

                assertEquals(stagingModel, defaultStagingModel);


                assertEquals(targetDataStore, executionContext.getTargetDataStore());
                assertEquals(sourceDataStore, executionContext.getSourceDataStores().get(0).getDataStore());
                assertEquals(sourceAlias, executionContext.getSourceDataStores().get(0).getAlias());
                assertEquals(DataStoreWithAlias.Type.Source, executionContext.getSourceDataStores().get(0).getType());
                assertEquals(lookupDataStore, executionContext.getSourceDataStores().get(1).getDataStore());
                assertEquals(lookupAlias, executionContext.getSourceDataStores().get(1).getAlias());
                assertEquals(DataStoreWithAlias.Type.Lookup, executionContext.getSourceDataStores().get(1).getType());
                assertEquals(ikmCode, executionContext.getIKMCode());

                assertEquals(rules, executionContext.getConfigurations());
                assertNotNull(executionContext.getTransformationExtension());
                assertNotNull(executionContext.getMappingsExtension());
                assertThat(executionContext.getSourceDataStores().get(0).getSourceExtension(), SamePropertyValuesAs.<SourceExtension>samePropertyValuesAs(sourceExtension));
                assertThat(executionContext.getSourceDataStores().get(1).getSourceExtension(), SamePropertyValuesAs.<SourceExtension>samePropertyValuesAs(sourceExtension));

                return defaultStagingModel;
            }
        };

        fixture = new KnowledgeModuleContextImpl(databaseMetadataService,
                etlSubsystemService, st, provider, new StrategyTester(),
                jodiProperties, validator, errorWarningMessages);

        fixture.getStagingModel(transformation);
    }

    // This inner class is intended to test the execution context is correctly populated.
    class StrategyTester implements KnowledgeModuleStrategy {

        String loading, check, integration;

        StrategyTester(String loading, String check, String integration) {
            this.loading = loading;
            this.check = check;
            this.integration = integration;
        }

        StrategyTester() {
        }

        @Override
        public KnowledgeModuleConfiguration getLKMConfig(
                KnowledgeModuleConfiguration configuration,
                LoadKnowledgeModuleExecutionContext executionContext) {
            if (loading != null)
                ((KnowledgeModuleConfigurationImpl) configuration).setName(loading);
            return configuration;
        }

        @Override
        public KnowledgeModuleConfiguration getCKMConfig(
                KnowledgeModuleConfiguration configuration,
                CheckKnowledgeModuleExecutionContext executionContext) {
            if (check != null)
                ((KnowledgeModuleConfigurationImpl) configuration).setName(check);
            return configuration;
        }

        @Override
        public KnowledgeModuleConfiguration getIKMConfig(
                KnowledgeModuleConfiguration configuration,
                KnowledgeModuleExecutionContext executionContext) {
            if (integration != null)
                ((KnowledgeModuleConfigurationImpl) configuration).setName(integration);
            return configuration;
        }

        @Override
        public String getStagingModel(String defaultStagingModel,
                                      StagingKnowledgeModuleExecutionContext executionContext) {
            return defaultStagingModel;
        }

    }
}
