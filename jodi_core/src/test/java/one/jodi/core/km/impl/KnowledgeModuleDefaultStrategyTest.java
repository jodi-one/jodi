package one.jodi.core.km.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.model.types.ModelSolutionLayer;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.config.km.KnowledgeModulePropertiesImpl;
import one.jodi.core.extensions.contexts.CheckKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.KnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.LoadKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.contexts.StagingKnowledgeModuleExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.etl.km.KnowledgeModuleType;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.TransformationExtension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class KnowledgeModuleDefaultStrategyTest {

    static String DefaultSourceTechnology = "Oracle";
    static String DefaultSourceDataServerName = "sourceDataServerName";
    static String DefaultSourceLayer = "sourceLayer";
    static String DefaultSourceDataStoreName = "sourceDataStoreName";
    static String DefaultTargetTechnology = "Oracle";
    static String DefaultTargetDataServerName = "targetDataServerName";
    //	@Mock ModelSolutionLayer modelSolutionLayer;
    static String DefaultTargetLayer = "targetLayer";
    static String DefaultTargetDataStoreName = "targetDataStoreName";
    static DataStoreType DefaultTargetDataStoreType = DataStoreType.DIMENSION;
    static DataStoreType DefaultSourceDataStoreType = DataStoreType.FACT;
    KnowledgeModuleDefaultStrategy fixture;
    @Mock
    JodiProperties properties;
    @Mock
    DataStore dataStore;
    @Mock
    LoadKnowledgeModuleExecutionContext lkmExecutionContext;
    @Mock
    KnowledgeModuleExecutionContext ckmExecutionContext;
    @Mock
    KnowledgeModuleExecutionContext ikmExecutionContext;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(KnowledgeModuleDefaultStrategyTest.class);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new KnowledgeModuleDefaultStrategy(properties,
                errorWarningMessages);
    }

    private DataStore mockDataStore(
            String dataServerName,
            String layer,
            String dataStoreName,
            String technology,
            DataStoreType type) {
        DataStore sourceDataStore = mock(DataStore.class);
        DataModel dataModel = mock(DataModel.class);

        when(sourceDataStore.getDataModel()).thenReturn(dataModel);
        when(dataModel.getDataServerTechnology()).thenReturn(technology);
        when(dataModel.getDataServerName()).thenReturn(dataServerName);
        ModelSolutionLayer modelLayer = mock(ModelSolutionLayer.class);
        when(modelLayer.getSolutionLayerName()).thenReturn(layer);
        when(dataModel.getSolutionLayer()).thenReturn(modelLayer);
        when(sourceDataStore.isTemporary()).thenReturn(false);
        when(sourceDataStore.getDataStoreName()).thenReturn(dataStoreName);
        when(sourceDataStore.getDataStoreType()).thenReturn(type);
        return sourceDataStore;

    }

    private Map<String, Object> parseMap(String options) {

        HashMap<String, Object> map = new HashMap<String, Object>();
        String[] list = options.split(",");
        for (String s : list) {
            String[] keyValuePair = s.split(":", 2);
            if (keyValuePair.length != 2) {
                throw new RuntimeException("Unsuccessful attempt to parse name value pair");
            } else {
                try {
                    map.put(keyValuePair[0].trim(), new Integer(keyValuePair[1].trim()));
                } catch (NumberFormatException nfe) {
                    if ("true".equalsIgnoreCase(keyValuePair[1].trim()) || "false".equalsIgnoreCase(keyValuePair[1].trim())) {
                        map.put(keyValuePair[0].trim(), new Boolean(keyValuePair[1].trim()));
                    } else {
                        map.put(keyValuePair[0].trim(), keyValuePair[1].trim());
                    }
                }
            }
        }

        return map;
    }

    /**
     * creates a KnowldgeModuleProperties to be used for a Integration or Check or other KM
     */
    private KnowledgeModuleProperties configuration(
            String id,
            Integer order,
            boolean isGlobal,
            String name,
            String options,
            String trg_technology,
            boolean isDefault,
            Integer trg_temporary,
            String trg_regex,
            String trg_layer,
            String[] trg_tabletype) {
        KnowledgeModulePropertiesImpl p = new KnowledgeModulePropertiesImpl();
        p.setId(id);
        p.setOrder(order);
        p.setName(Arrays.asList(name.split(",")));
        p.setOptions(parseMap(options));
        p.setTrg_technology(trg_technology);
        p.setSrc_technology(null);
        p.setDefault(isDefault);
        p.setGlobal(isGlobal);
        p.setTrg_temporary(trg_temporary);
        p.setTrg_regex(trg_regex);
        p.setTrg_layer(Arrays.asList(trg_layer.split(",")));
        p.setSrc_regex(null);
        p.setSrc_layer(null);
        p.setTrg_tabletype(Arrays.asList(trg_tabletype));

        return p;
    }

    /**
     * Returns a KnowledgeModuleProperties to be used for a Loading KM
     */
    private KnowledgeModuleProperties configurationLoading(
            String id,
            Integer order,
            boolean isGlobal,
            String name,
            String options,
            String trg_technology,
            String src_technology,
            boolean isDefault,
            Integer trg_temporary,
            String trg_regex,
            String trg_layer,
            String[] trg_tabletype,
            String src_regex,
            String src_layer,
            String[] src_tabletype) {
        KnowledgeModulePropertiesImpl p = new KnowledgeModulePropertiesImpl();
        p.setId(id);
        p.setOrder(order);
        p.setName(Arrays.asList(name.split(",")));
        p.setOptions(parseMap(options));
        p.setTrg_technology(trg_technology);
        p.setSrc_technology(src_technology);
        p.setGlobal(isGlobal);
        p.setDefault(isDefault);
        p.setTrg_temporary(trg_temporary);
        p.setTrg_tabletype(Arrays.asList(trg_tabletype));
        p.setTrg_regex(trg_regex);
        p.setTrg_layer(Arrays.asList(trg_layer.split(",")));
        p.setSrc_regex(src_regex);
        p.setSrc_layer(Arrays.asList(src_layer.split(",")));
        p.setSrc_tabletype(Arrays.asList(src_tabletype));

        return p;
    }

    private LoadKnowledgeModuleExecutionContext createLoadExecutionContext(List<KnowledgeModuleProperties> configurations) {
        DataStore sourceDataStore = mockDataStore(
                DefaultSourceDataServerName,
                DefaultSourceLayer,
                DefaultSourceDataStoreName,
                DefaultSourceTechnology,
                DefaultSourceDataStoreType);
        DataStore targetDataStore = mockDataStore(
                DefaultTargetDataServerName,
                DefaultTargetLayer,
                DefaultTargetDataStoreName,
                DefaultTargetTechnology,
                DefaultTargetDataStoreType);

        LoadKnowledgeModuleExecutionContext executionContext = mock(LoadKnowledgeModuleExecutionContext.class);
        when(executionContext.getTargetDataStore()).thenReturn(targetDataStore);
        when(executionContext.getSourceDataStore()).thenReturn(sourceDataStore);
        when(executionContext.getConfigurations()).thenReturn(configurations);
        return executionContext;
    }

    private CheckKnowledgeModuleExecutionContext createCheckExecutionContext(List<KnowledgeModuleProperties> configurations) {
        DataStore targetDataStore = mockDataStore(
                DefaultTargetDataServerName,
                DefaultTargetLayer,
                DefaultTargetDataStoreName,
                DefaultTargetTechnology,
                DefaultTargetDataStoreType);

        CheckKnowledgeModuleExecutionContext executionContext = mock(CheckKnowledgeModuleExecutionContext.class);
        when(executionContext.getTargetDataStore()).thenReturn(targetDataStore);
        when(executionContext.getConfigurations()).thenReturn(configurations);
        when(executionContext.getStagingDataModel()).thenReturn(null);
        return executionContext;
    }

    @SuppressWarnings("unused")
    private KnowledgeModuleExecutionContext createExecutionContext(List<KnowledgeModuleProperties> configurations) {
        DataStore targetDataStore = mockDataStore(
                DefaultTargetDataServerName,
                DefaultTargetLayer,
                DefaultTargetDataStoreName,
                DefaultTargetTechnology,
                DefaultTargetDataStoreType);

        KnowledgeModuleExecutionContext executionContext = mock(KnowledgeModuleExecutionContext.class);
        when(executionContext.getTargetDataStore()).thenReturn(targetDataStore);
        when(executionContext.getConfigurations()).thenReturn(configurations);
        return executionContext;
    }

    // make sure that when no matches occur in rule chain, its executed a second time to pick up default.
    @Test
    public void testConfigureLoading_default_secondpass() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>(3);
        configurations.add(0, configurationLoading(
                "lkm0",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM 0",                                    //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                false,                                        //default
                null,                                        //trg_temporary
                "foo",                                        //trg_rgeex
                "foo",                                    //trg_layer
                new String[]{},                            //trg_tabletype
                "foo",                                    //src_regex
                "foo",                                        //src_layer
                new String[]{}));                            //src_tabletype

        configurations.add(1, configurationLoading(
                "lkm1",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM 1",                                    //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                true,                                        //default
                null,                                        //trg_temporary
                "foo",                                        //trg_rgeex
                "foo",                                    //trg_layer
                new String[]{},                            //trg_tabletype
                "foo",                                    //src_regex
                "foo",                                        //src_layer
                new String[]{}));                            //src_tabletype
        configurations.add(2, configurationLoading(
                "lkm2",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM 2",                                    //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                false,                                        //default
                null,                                        //trg_temporary
                "foo",                                        //trg_rgeex
                "foo",                                    //trg_layer
                new String[]{},                            //trg_tabletype
                "foo",                                    //src_regex
                "foo",                                        //src_layer
                new String[]{}));                            //src_tabletype


        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
        assertEquals("lkm1", configuration.getName());
        assertEquals(true, configuration.getOptionValue("AUTO_CREATE_DB_LINK"));
    }

    @Test(expected = RuntimeException.class)
    public void testConfigureLoading_nonmatching_tabletypes() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configurationLoading(
                "lkm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM Oracle to Oracle (DBLINK)",            //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                false,                                        //default
                null,                                        //trg_temporary
                null,                                        //trg_rgeex
                null,                                        //trg_layer
                new String[]{DataStoreType.HELPER + ""},                            //trg_tabletype
                null,                                        //src_regex
                null,                                        //src_layer
                new String[]{DataStoreType.HELPER + ""}));                            //src_tabletype

        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
    }

    @Test(expected = RuntimeException.class)
    public void testConfigureLoading_nonmatching_technologies() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configurationLoading(
                "lkm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM Oracle to Oracle (DBLINK)",            //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "HSQL",                                    //trg_technology
                "Oracle",                                    //src_technology
                true,                                        //default
                null,                                        //trg_temporary
                null,                                        //trg_rgeex
                null,                                        //trg_layer
                new String[]{},                            //trg_tabletype
                null,                                        //src_regex
                null,                                        //src_layer
                new String[]{}));                            //src_tabletype

        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
    }

    // Make sure that the default option hits (based solely on technologies) when other rules fall through
    // Note that even though trg_temporary is invalid option, its still ignored.
    @Test
    public void testConfigureLoading_default_override() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>(2);
        configurations.add(0, configurationLoading(
                "lkm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM Other",                                //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                false,                                        //default
                new Integer(1),                                //trg_temporary
                "foo",                                        //trg_rgeex
                "foo",                                    //trg_layer
                new String[]{},                            //trg_tabletype
                "foo",                                    //src_regex
                "foo",                                        //src_layer
                new String[]{}));                            //src_tabletype

        configurations.add(1, configurationLoading(
                "lkm",                                        // id
                new Integer(90),                            // order
                false,                                        //global
                "LKM Oracle to Oracle (DBLINK)",            //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                true,                                        //default
                new Integer(1),                                //trg_temporary
                "foo",                                        //trg_rgeex
                "foo",                                    //trg_layer
                new String[]{},                            //trg_tabletype
                "foo",                                    //src_regex
                "foo",                                        //src_layer
                new String[]{}));                            //src_tabletype

        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
        assertEquals("lkm", configuration.getName());
        assertEquals(true, configuration.getOptionValue("AUTO_CREATE_DB_LINK"));
    }

    @Test
    public void testConfigureLoading_global() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>(4);
        configurations.add(0, configurationLoading(
                "lkm",                                        // id
                new Integer(10),                            // order
                true,                                        //global
                "LKM Pickme, LKM Pickmenot",                //name
                "option1:10,option2:10,option3:10,option4:10,option5:10",                                //options
                DefaultTargetTechnology,                    //trg_technology
                DefaultSourceTechnology,                    //trg_technology
                false,                                        //default
                null,                                        //trg_temporary
                DefaultTargetDataStoreName,                //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{},                            //trg_tabletype
                DefaultSourceDataStoreName,                //src_regex
                DefaultSourceLayer,                            //src_layer
                new String[]{}));                            //src_tabletype

        configurations.add(1, configurationLoading(
                "lkm",                                        // id
                new Integer(20),                            // order
                false,                                        //global
                "LKM Pickme",                                //name
                "option1:20,option2:20,option3:20",                                //options
                DefaultTargetTechnology,                    //trg_technology
                DefaultSourceTechnology,                    //trg_technology
                false,                                        //default
                null,                                        //trg_temporary
                DefaultTargetDataStoreName,                //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{},                            //trg_tabletype
                DefaultSourceDataStoreName,                //src_regex
                DefaultSourceLayer,                        //src_layer
                new String[]{}));                            //src_tabletype

        configurations.add(2, configurationLoading(
                "lkm",                                        // id
                new Integer(30),                            // order
                true,                                        //global
                "LKM Pickme",                                //name
                "option1:30,option2:30",                                //options
                DefaultTargetTechnology,                    //trg_technology
                DefaultSourceTechnology,                    //trg_technology
                false,                                        //default
                null,                                        //trg_temporary
                DefaultTargetDataStoreName,                //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{},                            //trg_tabletype
                DefaultSourceDataStoreName,                //src_regex
                DefaultSourceLayer,                            //src_layer
                new String[]{}));                            //src_tabletype

        configurations.add(3, configurationLoading(
                "lkm",                                        // id
                new Integer(40),                            // order
                true,                                        //global
                "LKM Pickme,LKM Foo",                                //name
                "option1:40",                                //options
                DefaultTargetTechnology,                    //trg_technology
                DefaultSourceTechnology,                    //trg_technology
                false,                                        //default
                null,                                        //trg_temporary
                DefaultTargetDataStoreName,                //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{},                            //trg_tabletype
                DefaultSourceDataStoreName,                //src_regex
                DefaultSourceLayer,                            //src_layer
                new String[]{}));                            //src_tabletype
        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
        assertEquals("lkm", configuration.getName());


        // option one should be set by configuration 40
        assertEquals(new Integer(40), configuration.getOptionValue("option1"));
        assertEquals(new Integer(30), configuration.getOptionValue("option2"));
        // don't know why this fails
        // assertEquals(new Integer(20), configuration.getOptionValue("option3"));
        assertEquals(new Integer(10), configuration.getOptionValue("option4"));

    }

    // This is the most stringent case which looks for every field except for trg_temporary.
    @Test
    public void testConfigureLoading_match_on_all_fields() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>(3);
        configurations.add(0, configurationLoading(
                "lkm",                                        // id
                new Integer(70),                            // order
                false,                                        //global
                "LKM Oracle to Oracle (DBLINK)",            //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                false,                                        //default
                new Integer(-1),                            //trg_temporary
                caseify(DefaultTargetDataStoreName),        //trg_rgeex
                DefaultTargetLayer,                            //trg_layer
                new String[]{},                            //trg_tabletype
                caseify(DefaultSourceDataStoreName),        //src_regex
                DefaultSourceLayer,                        //src_layer
                new String[]{}));                            //src_tabletype

        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
        assertEquals("lkm", configuration.getName());
        assertEquals(true, configuration.getOptionValue("AUTO_CREATE_DB_LINK"));
    }

    @Test
    public void testConfigureLoading_matching_by_technologies() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configurationLoading(
                "lkm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM Oracle to Oracle (DBLINK)",            //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                true,                                        //default
                null,                                        //trg_temporary
                null,                                        //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{},                            //trg_tabletype
                null,                                        //src_regex
                DefaultSourceLayer,                            //src_layer
                new String[]{}));                            //src_tabletype

        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
        assertEquals("lkm", configuration.getName());
        assertEquals(true, configuration.getOptionValue("AUTO_CREATE_DB_LINK"));
    }

    @Test
    public void testConfigureLoading_explicitlyDefined() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configurationLoading(
                "lkm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM Oracle to Oracle (DBLINK)",            //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                true,                                        //default
                null,                                        //trg_temporary
                null,                                        //trg_rgeex
                DefaultTargetLayer,                                        //trg_layer
                new String[]{},                            //trg_tabletype
                null,                                        //src_regex
                DefaultSourceLayer,                                        //src_layer
                new String[]{}));                            //src_tabletype

        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfigurationImpl explicitConfiguration = new KnowledgeModuleConfigurationImpl();
        explicitConfiguration.setName("lkm");
        explicitConfiguration.setType(KnowledgeModuleType.Loading);
        explicitConfiguration.putOption("AUTO_CREATE_DB_LINK", false);
        explicitConfiguration.putOption("integer", new Integer(1));
        KnowledgeModuleConfiguration configuration = fixture.getLKMConfig(explicitConfiguration, executionContext);
        assertEquals(explicitConfiguration.getName(), configuration.getName());
        assertEquals(false, configuration.getOptionValue("AUTO_CREATE_DB_LINK"));
        assertEquals(new Integer(1), configuration.getOptionValue("integer"));
    }

    @Test(expected = RuntimeException.class)
    public void testConfigureLoading_noProperties() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();


        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);

        KnowledgeModuleConfigurationImpl explicitConfiguration = new KnowledgeModuleConfigurationImpl();
        explicitConfiguration.setName("LKM 1");
        explicitConfiguration.setType(KnowledgeModuleType.Loading);
        explicitConfiguration.putOption("AUTO_CREATE_DB_LINK", false);
        explicitConfiguration.putOption("integer", new Integer(1));
        fixture.getLKMConfig(null, executionContext);
    }

    @Test
    public void testConfigureCheck_matching_all_fields() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configuration(
                "ckm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "CKM 1",                                    //name
                "boolean:true,string:string,integer:1",        //options
                "Oracle",                                    //trg_technology
                false,                                        //default
                null,                                        //trg_temporary
                DefaultTargetDataStoreName,                //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{}));                            //trg_tabletype


        CheckKnowledgeModuleExecutionContext executionContext = createCheckExecutionContext(configurations);
        KnowledgeModuleConfiguration configuration = fixture.getCKMConfig(null, executionContext);
        assertEquals("ckm", configuration.getName());
        assertEquals(new Integer(1), configuration.getOptionValue("integer"));
        assertEquals(new Boolean(true), configuration.getOptionValue("boolean"));
        assertEquals("string", configuration.getOptionValue("string"));
    }

    /**
     * This helper function is used for testing the trg_temporary specification
     *
     * @param ckmName
     * @param isTemporary
     * @param trg_temporary
     * @return
     */
    private KnowledgeModuleConfiguration testConfigureCheck(String ckmName, boolean isTemporary, Integer trg_temporary) {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configuration(
                "ckm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                ckmName,                                    //name
                "boolean:true,string:string,integer:1",        //options
                "Oracle",                                    //trg_technology
                false,                                        //default
                trg_temporary,                                //trg_temporary
                null,                                        //trg_rgeex
                DefaultTargetLayer,                    //trg_layer
                new String[]{}));                            //trg_tabletype


        CheckKnowledgeModuleExecutionContext executionContext = createCheckExecutionContext(configurations);
        when(executionContext.getTargetDataStore().isTemporary()).thenReturn(isTemporary);
        return fixture.getCKMConfig(null, executionContext);
    }

    @Test
    public void testConfigureCheck_temporary_specifies_any() {
        KnowledgeModuleConfiguration configuration = testConfigureCheck("CKM 1", true, 0);
        assertEquals("ckm", configuration.getName());
    }

    @Test
    public void testConfigureCheck_temporary_specifies_temporary() {
        KnowledgeModuleConfiguration configuration = testConfigureCheck("CKM 1", true, 1);
        assertEquals("ckm", configuration.getName());
    }

    public void testConfigureCheck_temporary_specifies_nottemporary() {
        KnowledgeModuleConfiguration configuration = testConfigureCheck("CKM 1", true, -1);
        assertEquals(KnowledgeModuleConfiguration.Null, configuration);
    }

    @Test
    public void testConfigureCheck_nottemporary_specifies_any() {
        KnowledgeModuleConfiguration configuration = testConfigureCheck("CKM 1", false, 0);
        assertEquals("ckm", configuration.getName());
    }

    @Test
    public void testConfigureCheck_nottemporary_specifies_nottemporary() {
        KnowledgeModuleConfiguration configuration = testConfigureCheck("CKM 1", false, -1);
        assertEquals("ckm", configuration.getName());
    }

    public void testConfigureCheck_nottemporary_specifies_temporary() {
        KnowledgeModuleConfiguration configuration = testConfigureCheck("CKM 1", false, 1);
        assertEquals(KnowledgeModuleConfiguration.Null, configuration);
    }

    @Test
    public void testConfigureCheck_explicitly_defined() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configuration(
                "ckm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "CKM 1",                                    //name
                "boolean:true,string:string,integer:1",        //options
                "Oracle",                                    //trg_technology
                false,                                        //default
                null,                                        //trg_temporary
                DefaultTargetDataStoreName,                //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{}));                            //trg_tabletype


        KnowledgeModuleConfigurationImpl explicitConfiguration = new KnowledgeModuleConfigurationImpl();
        explicitConfiguration.setName("ckm");
        explicitConfiguration.setType(KnowledgeModuleType.Check);
        explicitConfiguration.putOption("boolean", false);
        CheckKnowledgeModuleExecutionContext executionContext = createCheckExecutionContext(configurations);
        KnowledgeModuleConfiguration configuration = fixture.getCKMConfig(explicitConfiguration, executionContext);
        assertEquals("ckm", configuration.getName());
        assertEquals(new Integer(1), configuration.getOptionValue("integer"));
        assertEquals(new Boolean(false), configuration.getOptionValue("boolean"));
        assertEquals("string", configuration.getOptionValue("string"));

    }

    // Make sure that even with an empty collection for layer we dont use as selector
    @Test
    public void testConfigureIntegration_matching_all_fields_with_empty_layer() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configuration(
                "ikm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "IKM 1",                                    //name
                "boolean:true,string:string,integer:1",        //options
                "Oracle",                                    //trg_technology
                false,                                        //default
                null,                                        //trg_temporary
                DefaultSourceDataStoreName,                //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{}));                            //trg_tabletype

        ((KnowledgeModulePropertiesImpl) configurations.get(0)).setTrg_layer(new ArrayList<String>());
        DataStore targetDataStore = mockDataStore(
                DefaultTargetDataServerName,
                DefaultTargetLayer,
                DefaultSourceDataStoreName,
                DefaultTargetTechnology,
                DefaultTargetDataStoreType);

        KnowledgeModuleExecutionContext executionContext = mock(KnowledgeModuleExecutionContext.class);
        when(executionContext.getTargetDataStore()).thenReturn(targetDataStore);
        when(executionContext.getConfigurations()).thenReturn(configurations);

        KnowledgeModuleConfiguration configuration = fixture.getIKMConfig(null, executionContext);
        assertEquals("ikm", configuration.getName());
        assertEquals(new Integer(1), configuration.getOptionValue("integer"));
        assertEquals(new Boolean(true), configuration.getOptionValue("boolean"));
        assertEquals("string", configuration.getOptionValue("string"));
    }

    @Test
    public void testStagingModel() {
        DataStore ds1 = mock(DataStore.class);
        DataModel dm1 = mock(DataModel.class);
        when(ds1.getDataModel()).thenReturn(dm1);
        when(dm1.getModelCode()).thenReturn("model1");
        DataStoreWithAlias dsa1 = mock(DataStoreWithAlias.class);
        when(dsa1.getDataStore()).thenReturn(ds1);

        DataStore ds2 = mock(DataStore.class);
        DataModel dm2 = mock(DataModel.class);
        when(ds2.getDataModel()).thenReturn(dm2);
        when(dm2.getModelCode()).thenReturn("model2");
        DataStoreWithAlias dsa2 = mock(DataStoreWithAlias.class);
        when(dsa2.getDataStore()).thenReturn(ds2);
        final ArrayList<DataStoreWithAlias> dataStores = new ArrayList<DataStoreWithAlias>();
        dataStores.add(dsa1);
        dataStores.add(dsa2);
        KnowledgeModule km = createKM("KM", true);
        final List<KnowledgeModule> kms = Collections.singletonList(km);

        when(properties.getProperty("km.ikm.name")).thenReturn("KM");

        StagingKnowledgeModuleExecutionContext executionContext = new StagingKnowledgeModuleExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return null;
            }

            @Override
            public List<KnowledgeModuleProperties> getConfigurations() {
                return null;
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                return null;
            }

            @Override
            public List<KnowledgeModule> getKMs() {
                return kms;
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                return null;
            }

            @Override
            public List<DataStoreWithAlias> getSourceDataStores() {
                return dataStores;
            }

            @Override
            public String getIKMCode() {
                return "km.ikm";
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return null;
            }
        };

        String stagingModel = fixture.getStagingModel(null, executionContext);
        assertEquals("model1", stagingModel);
        verify(dm1, times(1)).getModelCode();
    }

    @Test
    public void testStagingModel_explicit_not_multi_technology() {
        // strategy should return null as IKM not multi-technology
        when(properties.getProperty("km.ikm")).thenReturn("KM");
        KnowledgeModule km = createKM("KM", false);
        final List<KnowledgeModule> kms = Collections.singletonList(km);
        StagingKnowledgeModuleExecutionContext executionContext = new StagingKnowledgeModuleExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return null;
            }

            @Override
            public List<KnowledgeModuleProperties> getConfigurations() {
                return null;
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                return null;
            }

            @Override
            public List<KnowledgeModule> getKMs() {
                return kms;
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                return null;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return null;
            }

            @Override
            public List<DataStoreWithAlias> getSourceDataStores() {
                return null;
            }

            @Override
            public String getIKMCode() {
                return "km.ikm";
            }
        };

        when(properties.getProperty("km.ikm.name")).thenReturn("KM");
        String defaultStagingModel = "defaultStagingModel";
        String stagingModel = fixture.getStagingModel(defaultStagingModel, executionContext);
        assertEquals(null, stagingModel);
    }

    @Test
    public void testStagingModel_explicit_multi_technology() {
        // strategy should return null as IKM not multi-technology
        when(properties.getProperty("km.ikm")).thenReturn("KM");
        KnowledgeModule km = createKM("KM", true);
        final List<KnowledgeModule> kms = Collections.singletonList(km);
        StagingKnowledgeModuleExecutionContext executionContext = new StagingKnowledgeModuleExecutionContext() {

            @Override
            public DataStore getTargetDataStore() {
                return null;
            }

            @Override
            public List<KnowledgeModuleProperties> getConfigurations() {
                return null;
            }

            @Override
            public MappingsExtension getMappingsExtension() {
                return null;
            }

            @Override
            public List<KnowledgeModule> getKMs() {
                return kms;
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                return null;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return null;
            }

            @Override
            public List<DataStoreWithAlias> getSourceDataStores() {
                return null;
            }

            @Override
            public String getIKMCode() {
                return "km.ikm";
            }
        };

        when(properties.getProperty("km.ikm.name")).thenReturn("KM");
        when(properties.getProperty("model.staging")).thenReturn("defaultStagingModel");
        String stagingModel = fixture.getStagingModel("model.staging", executionContext);
        assertEquals("defaultStagingModel", stagingModel);
    }

    @Test
    public void testLKM_uses_staging_model() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>(3);

        configurations.add(0, configurationLoading(
                "lkm1",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM 1",                                    //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "Oracle",                                    //trg_technology
                "Oracle",                                    //src_technology
                true,                                        //default
                null,                                        //trg_temporary
                "foo",                                        //trg_rgeex
                "foo",                                    //trg_layer
                new String[]{},                            //trg_tabletype
                "foo",                                    //src_regex
                "foo",                                        //src_layer
                new String[]{}));                            //src_tabletype
        configurations.add(1, configurationLoading(
                "lkm2",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "LKM 2",                                    //name
                "AUTO_CREATE_DB_LINK: true",                //options
                "FILE",                                    //trg_technology
                "Oracle",                                    //src_technology
                false,                                        //default
                null,                                        //trg_temporary
                null,                                        //trg_rgeex
                DefaultTargetLayer,                        //trg_layer
                new String[]{},                            //trg_tabletype
                null,                                        //src_regex
                DefaultSourceLayer,                            //src_layer
                new String[]{}));                            //src_tabletype


        LoadKnowledgeModuleExecutionContext executionContext = createLoadExecutionContext(configurations);
        DataModel dataModel = mock(DataModel.class);
        when(dataModel.getDataServerTechnology()).thenReturn("FILE");
        when(dataModel.getModelCode()).thenReturn("stagingModel");
        when(executionContext.getStagingDataModel()).thenReturn(dataModel);

        ArrayList<KnowledgeModule> kms = new ArrayList<KnowledgeModule>();
        KnowledgeModule km1 = createKM("LKM 1", false);
        kms.add(km1);
        KnowledgeModule km2 = createKM("LKM 2", true);
        kms.add(km2);

        when(executionContext.getKMs()).thenReturn(kms);


        KnowledgeModuleConfiguration configuration = null;
        configuration = fixture.getLKMConfig(configuration, executionContext);
        assertEquals("lkm2", configuration.getName());
        assertEquals(true, configuration.getOptionValue("AUTO_CREATE_DB_LINK"));
    }

    private KnowledgeModule createKM(String name, boolean isMultiTechnology) {
        KnowledgeModule km = mock(KnowledgeModule.class);
        when(km.getName()).thenReturn(name);
        when(km.isMultiTechnology()).thenReturn(isMultiTechnology);
        return km;
    }

    @Test
    public void testCKM_no_rule_found_returns_NULL() {
        ArrayList<KnowledgeModuleProperties> configurations = new ArrayList<KnowledgeModuleProperties>();
        configurations.add(configuration(
                "ckm",                                        // id
                new Integer(80),                            // order
                false,                                        //global
                "CKM 1",                                    //name
                "boolean:true,string:string,integer:1",        //options
                "Other",                                    //trg_technology
                false,                                        //default
                0,                                //trg_temporary
                null,                                        //trg_rgeex
                DefaultTargetLayer,                    //trg_layer
                new String[]{}));                            //trg_tabletype


        CheckKnowledgeModuleExecutionContext executionContext = createCheckExecutionContext(configurations);
        when(executionContext.getTargetDataStore().isTemporary()).thenReturn(false);
        assertEquals(KnowledgeModuleConfiguration.Null, fixture.getCKMConfig(null, executionContext));
    }

    /**
     * Utility function to alternate case in a string
     *
     * @param string to modify
     * @return string in alternating case
     */
    private String caseify(String s) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : s.toCharArray()) {
            upper = !upper;
            if (upper) sb.append((c + "").toUpperCase());
            else sb.append((c + "").toLowerCase());
        }

        return sb.toString();
    }
}

