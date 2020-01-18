package one.jodi.core.config;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.config.km.KnowledgeModulePropertiesProviderImpl;
import one.jodi.core.metadata.ETLSubsystemService;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.etl.km.KnowledgeModuleType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * <p>
 * With this unit test class the group does has semantic value, eg, the group name becomes the KM name.
 * THis is done to speed up testing.
 */
@RunWith(JUnit4.class)
public class KnowledgeModulePropertiesProviderTest {

    private final String[] PARAMETER_DEFS = new String[]{"name[]!", "order", "global", "options{}", "trg_technology!", "src_technology", "default", "trg_temporary", "trg_regex", "trg_layer", "trg_tabletype[]", "src_regex", "src_layer", "src_tabletype[]"};
    // Infrastructure
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    JodiProperties mockProperties;
    @Mock
    ETLSubsystemService etlSubsystemService;
    List<String> expectedParameters;
    KnowledgeModulePropertiesProviderImpl fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    private void add(ArrayList<KnowledgeModule> list, final KnowledgeModuleType type, final boolean multiTech, final String... names) {
        for (final String name : names) {
            list.add(new KnowledgeModule() {
                @Override
                public KnowledgeModuleType getType() {
                    return type;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public boolean isMultiTechnology() {
                    return multiTech;
                }

                @Override
                public Map<String, KMOptionType> getOptions() {
                    return new HashMap<String, KMOptionType>();
                }
            });
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        ArrayList<KnowledgeModule> kms = new ArrayList<KnowledgeModule>();
        this.add(kms, KnowledgeModuleType.Check, false, "ckm", "ckm1", "ckm2", "ckm_default");
        this.add(kms, KnowledgeModuleType.Integration, false, "ikm", "ikm1", "ikm2", "ikm3");
        this.add(kms, KnowledgeModuleType.Loading, false, "lkm", "lkm_default", "lkm1", "lkm2", "lkm3", "no white space");
        when(etlSubsystemService.getKMs()).thenReturn(kms);
    }


    boolean isCollection(final String name, String collectionSymbol, final List<String> parameterDefs) {
        boolean isList = false;

        for (String param : parameterDefs) {
            String p = param.endsWith("!") ? param.substring(0, param.length() - 1)
                    : param;
            if (p.endsWith(collectionSymbol) && name.endsWith(p.substring(0, p.length() - collectionSymbol.length()))) {
                isList = true;
                break;
            }
        }
        return isList;
    }

    private void configure(Map<String, String> propertiesMap) {

        when(mockProperties.getPropertyKeys()).thenReturn(new ArrayList<String>(propertiesMap.keySet()));

        expectedParameters = new ArrayList<String>();
        for (int i = 0; i < PARAMETER_DEFS.length; i++) {
            expectedParameters.add(PARAMETER_DEFS[i]);
        }

        // define properties file - identical to the mockProp Map
        for (String key : propertiesMap.keySet()) {
            if (isCollection(key, "[]", expectedParameters)) {
                List<String> l = Arrays.asList(propertiesMap.get(key).split(","));
                when(mockProperties.getPropertyList(key)).thenReturn(l);
            } else if (isCollection(key, "{}", expectedParameters)) {
                HashMap<String, String> map = new HashMap<String, String>();
                for (String nvpString : propertiesMap.get(key).split(",")) {
                    String[] nvpArray = nvpString.split(":", 2);
                    assert (nvpArray.length == 2);
                    map.put(nvpArray[0], nvpArray[1]);
                }
                when(mockProperties.getPropertyMap(key)).thenReturn(map);
            } else {
                when(mockProperties.getProperty(key)).thenReturn(propertiesMap.get(key));
                when(mockProperties.getPropertyList(key)).thenReturn(Arrays.asList(propertiesMap.get(key).split(",")));
            }
        }
    }


    Map<String, String> buildPropertiesMap(String[] groups, KnowledgeModuleType type) {
        Map<String, String> propertiesMap = new HashMap<String, String>();
        int order = 1;
        for (String group : groups) {
            propertiesMap.put("km." + group + ".name", group);
            propertiesMap.put("km." + group + ".global", "false");
            propertiesMap.put("km." + group + ".order", order++ + "");
            propertiesMap.put("km." + group + ".trg_technology", "Oracle");
            propertiesMap.put("km." + group + ".default", group.contains("default") ? "true" : "false");
            propertiesMap.put("km." + group + ".trg_regex", "^(?!W_)[\\w\\d]+|^W_[\\w\\d]+(?<!_[HF])$");
            propertiesMap.put("km." + group + ".trg_layer", "trg_layer");
            propertiesMap.put("km." + group + ".options", "RECYCLE_ERRORS : true, TRUNCATE : true");
            propertiesMap.put("km." + group + ".trg_tabletype", "DIMENSION,UNKNOWN");
        }


        return propertiesMap;
    }


    Map<String, String> addLoadingProperties(String[] groups, Map<String, String> propertiesMap) {
        for (String group : groups) {
            propertiesMap.put("km." + group + ".src_technology", "Oracle");
            propertiesMap.put("km." + group + ".src_regex", "^(?!W_)[\\w\\d]+|^W_[\\w\\d]+(?<!_[HF])$");
            propertiesMap.put("km." + group + ".src_layer", "src_layer");
            propertiesMap.put("km." + group + ".src_tabletype", "FACT,UNKNOWN");
        }

        propertiesMap.put("km.ckm.name", "ckm");
        propertiesMap.put("km.ckm.trg_technology", "Oracle");
        propertiesMap.put("km.ckm.order", "10");

        propertiesMap.put("km.ikm.name", "ikm");
        propertiesMap.put("km.ikm.trg_technology", "Oracle");
        propertiesMap.put("km.ikm.order", "10");

        return propertiesMap;
    }

    Map<String, String> addIntegrationProperties(String[] groups, Map<String, String> propertiesMap) {


        for (String group : groups)
            propertiesMap.put("km." + group + ".trg_temporary", "-1");

        propertiesMap.put("km.ckm.name", "ckm");
        propertiesMap.put("km.ckm.trg_technology", "Oracle");
        propertiesMap.put("km.ckm.order", "10");

        propertiesMap.put("km.lkm.name", "ikm");
        propertiesMap.put("km.lkm.trg_technology", "Oracle");

        propertiesMap.put("km.lkm.order", "10");

        return propertiesMap;
    }

    Map<String, String> addCheckProperties(String[] groups, Map<String, String> propertiesMap) {


        for (String group : groups)
            propertiesMap.put("km." + group + ".trg_temporary", "-1");

        propertiesMap.put("km.ikm.name", "ikm");
        propertiesMap.put("km.ikm.default", "true");
        propertiesMap.put("km.ikm.trg_technology", "Oracle");
        propertiesMap.put("km.ikm.order", "10");

        propertiesMap.put("km.lkm.name", "lkm");
        propertiesMap.put("km.lkm.default", "true");
        propertiesMap.put("km.lkm.trg_technology", "Oracle");
        propertiesMap.put("km.lkm.src_technology", "Oracle");
        propertiesMap.put("km.lkm.order", "10");

        return propertiesMap;
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicatedOrder() {
        String[] groups = new String[]{"lkm1", "lkm_default", "lkm2"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        for (String group : groups) {
            propertiesMap.put("km." + group + ".order", "10");
        }
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        fixture.getProperties(KnowledgeModuleType.Loading);
        assert (fixture.getErrorMessages().size() > 0);
    }


    @Test
    public void testLoading() {
        String[] groups = new String[]{"lkm1", "lkm_default", "lkm2"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        configure(propertiesMap);

        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);

        List<KnowledgeModuleProperties> configurations = fixture.getProperties(KnowledgeModuleType.Loading);
        assertEquals(groups.length, configurations.size());
        configurations.get(0).getOrder();
        for (int i = 1; i < configurations.size(); i++) {
            assertTrue(configurations.get(i).getOrder() > configurations.get(i - 1).getOrder());
        }
    }

    @Test
    public void testLoadingWithWhitespace() {
        String[] groups = new String[]{"lkm"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        propertiesMap.put("km.lkm.name", " no   white \t space ");
        configure(propertiesMap);

        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);

        fixture.getProperties(KnowledgeModuleType.Loading).forEach(kmp -> {
            kmp.getName().forEach(name -> {
                System.out.println(">" + name + "<");
            });
        });
    }

    // In this case the only extra parameter is trg_temporary
    @Test
    public void testLoading_extra_parameters() {
        String[] groups = new String[]{"lkm1", "lkm_default", "lkm2"};
        Map<String, String> propertiesMap = addIntegrationProperties(groups, addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading)));
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);

        thrown.expect(RuntimeException.class);
        fixture.getProperties(KnowledgeModuleType.Loading);

    }

    @Test
    public void testCheckAndIntegration() {
        String[] groups = new String[]{"ckm1", "ckm_default", "ckm2"};
        Map<String, String> propertiesMap = addCheckProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Check));
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        List<KnowledgeModuleProperties> configurations = fixture.getProperties(KnowledgeModuleType.Check);
        assertEquals(groups.length, configurations.size());
        configurations.get(0).getOrder();
        for (int i = 1; i < configurations.size(); i++) {
            assertTrue(configurations.get(i).getOrder() > configurations.get(i - 1).getOrder());
        }
    }

    @Test
    public void testCheckNoRules() {
        String[] groups = new String[]{};
        Map<String, String> propertiesMap = addCheckProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Check));
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        List<KnowledgeModuleProperties> configurations = fixture.getProperties(KnowledgeModuleType.Check);
        assertEquals(0, configurations.size());
    }

    @Test
    public void testMissingKMConstraintsInPropertiesFile() {
        String[] groups = new String[]{"ckm1", "ckm_default", "ckm2"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, addCheckProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Check)));
        //propertiesMap.remove(JodiConstants.CKM_CONSTRAINTS);
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);

        try {
            fixture.getProperties(KnowledgeModuleType.Check);
        } catch (Exception e) {
            assertTrue(fixture.getErrorMessages().size() > 3);

        }

    }
	
	/* TEST DOESNT APPLY WITHOUT VALIDATION OF KM NAME, TODO ADD LATER
	@Test (expected = RuntimeException.class)
	public void testMixedKMTypesInSingleRule() {
		String[] groups = new String[] { "ikm"};
		Map<String, String> propertiesMap = addLoadingProperties(groups, addCheckAndIntegrationProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Check)));
		propertiesMap.put(JodiConstants.CKM_CONSTRAINTS, "ckm");
		propertiesMap.put(JodiConstants.IKM_CONSTRAINTS, "ikm");
		propertiesMap.put("km.ikm.name", "ikm,ckm");
		configure(propertiesMap);
		fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties);
		
		assertTrue(fixture.getErrorMessages().size() >0);
		
		fixture.getProperties(KnowledgeModuleType.Check);
		fixture.getProperties(KnowledgeModuleType.Loading);
	}
	*/


    @Test(expected = RuntimeException.class)
    public void testCheckAndIntegration_extra_parameters() {
        String[] groups = new String[]{"ckm1", "ckm_default", "ckm2"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, addCheckProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Check)));
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        List<KnowledgeModuleProperties> configurations = fixture.getProperties(KnowledgeModuleType.Check);
        System.out.println(configurations.size());
        for (KnowledgeModuleProperties p : configurations) {
            System.out.println(p.getSrc_layer());
        }

        assertTrue(fixture.getErrorMessages().size() > 0);
    }

    @Test(expected = RuntimeException.class)
    public void testUnknownKM() {
        String[] groups = new String[]{"xkm1", "xkm_default", "xkm2"};
        Map<String, String> propertiesMap = buildPropertiesMap(groups, KnowledgeModuleType.Check);
        //propertiesMap.put(JodiConstants.CKM_CONSTRAINTS, "ckm,ckm1");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        fixture.getProperties(KnowledgeModuleType.Check);
        assertTrue(fixture.getErrorMessages().size() > 0);
    }

    @Test(expected = RuntimeException.class)
    public void test_non_global_with_many_names() {
        String[] groups = new String[]{"ikm"};
        Map<String, String> propertiesMap = buildPropertiesMap(groups, KnowledgeModuleType.Integration);
        propertiesMap.put("km.ikm.name", "IKM 1,IKM 1");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        fixture.getProperties(KnowledgeModuleType.Integration);
        assertTrue(fixture.getErrorMessages().size() > 0);
    }

    @Test(expected = RuntimeException.class)
    public void test_bad_datastoretype() {
        String[] groups = new String[]{"ikm"};
        Map<String, String> propertiesMap = buildPropertiesMap(groups, KnowledgeModuleType.Integration);
        propertiesMap.put("km.ikm.trg_tabletype", "BAD,UNKNOWN");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        fixture.getProperties(KnowledgeModuleType.Integration);
    }

    @Test(expected = RuntimeException.class)
    public void test_multiple_defaults_same_technologies() {
        String[] groups = new String[]{"ckm1", "ckm2", "ckm3"};
        Map<String, String> propertiesMap = addCheckProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Check));
        ///propertiesMap.put(JodiConstants.CKM_CONSTRAINTS, "ckm1,ckm2,ckm3");

        propertiesMap.put("km.ckm1.default", "true");
        propertiesMap.put("km.ckm3.default", "true");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        fixture.getProperties(KnowledgeModuleType.Check);
        assertTrue(fixture.getErrorMessages().size() > 0);
    }

    @Test
    public void test_multiple_defaults_multiple_technologies() {
        String[] groups = new String[]{"ckm1", "ckm2", "ckm3"};
        Map<String, String> propertiesMap = addCheckProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Check));
        //propertiesMap.put(JodiConstants.CKM_CONSTRAINTS, "ckm1,ckm2,ckm3");
        propertiesMap.put("km.ckm1.default", "true");
        propertiesMap.put("km.ckm1.trg_technology", "foo");
        propertiesMap.put("km.ckm3.default", "true");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        assertTrue(fixture.getErrorMessages().size() == 0);
    }

    @Test(expected = RuntimeException.class)
    public void test_lkm_multiple_defaults_same_technologies() {
        String[] groups = new String[]{"lkm1", "lkm2", "lkm3"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        //propertiesMap.put(JodiConstants.LKM_CONSTRAINTS, "lkm1,lkm2,lkm3");
        propertiesMap.put("km.lkm1.default", "true");
        propertiesMap.put("km.lkm3.default", "true");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        fixture.getProperties(KnowledgeModuleType.Loading);
        assertTrue(fixture.getErrorMessages().size() > 0);

    }

    @Test
    public void test_lkm_multiple_defaults_multiple_technologies() {
        String[] groups = new String[]{"lkm1", "lkm2", "lkm3"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        //propertiesMap.put(JodiConstants.LKM_CONSTRAINTS, "lkm1,lkm2,lkm3");
        propertiesMap.put("km.lkm1.default", "true");
        propertiesMap.put("km.lkm3.default", "true");
        propertiesMap.put("km.lkm1.src_technology", "foo");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        fixture.getProperties(KnowledgeModuleType.Loading);
        assertTrue(fixture.getErrorMessages().size() == 0);
    }

    @Test
    public void test_missing_lkm_rules() {
        String[] groups = new String[]{"ikm1"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        propertiesMap.remove("km.lkm.name");
        propertiesMap.remove("km.lkm.order");
        propertiesMap.remove("km.lkm.trg_technology");
        propertiesMap.remove("km.lkm.src_technology");

        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        try {
            fixture.getProperties(KnowledgeModuleType.Integration);
        } catch (Exception e) {
        }
        assertTrue(fixture.getErrorMessages().size() > 0);
    }

    //@Test
    public void test_missing_ckm_rules() {
        String[] groups = new String[]{"lkm1", "lkm2", "lkm3"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        propertiesMap.remove("km.ckm.name");
        propertiesMap.remove("km.ckm.order");
        propertiesMap.remove("km.ckm.trg_technology");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        try {
            fixture.getProperties(KnowledgeModuleType.Loading);
        } catch (Exception e) {
        }
        assertTrue(fixture.getErrorMessages().size() > 0);
    }

    @Test
    public void test_missing_ikm_rules() {
        String[] groups = new String[]{"lkm1", "lkm2", "lkm3"};
        Map<String, String> propertiesMap = addLoadingProperties(groups, buildPropertiesMap(groups, KnowledgeModuleType.Loading));
        propertiesMap.remove("km.ikm.name");
        propertiesMap.remove("km.ikm.order");
        propertiesMap.remove("km.ikm.trg_technology");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);
        try {
            fixture.getProperties(KnowledgeModuleType.Loading);
        } catch (Exception e) {
        }
        assertTrue(fixture.getErrorMessages().size() > 0);
    }


    @Test
    public void test_multi_tech_missing_src_technology() {
        this.add((ArrayList<KnowledgeModule>) etlSubsystemService.getKMs(), KnowledgeModuleType.Integration, true, "ikm4");
        String[] groups = new String[]{"ikm1", "ikm4"};
        Map<String, String> propertiesMap = addIntegrationProperties(new String[]{}, buildPropertiesMap(groups, KnowledgeModuleType.Integration));
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);

        try {
            fixture.getProperties(KnowledgeModuleType.Integration);
        } catch (Exception e) {
        }
        assertTrue(fixture.getErrorMessages().size() > 0);
        assertTrue(fixture.getErrorMessages().get(0).contains("src_technology"));
    }

    @Test
    public void test_integration_superfluous_src_technology() {
        String[] groups = new String[]{"ikm1", "ikm3"};
        Map<String, String> propertiesMap = addIntegrationProperties(new String[]{}, buildPropertiesMap(groups, KnowledgeModuleType.Integration));
        propertiesMap.put("km.lkm.src_technology", "Oracle");
        configure(propertiesMap);
        fixture = new KnowledgeModulePropertiesProviderImpl(mockProperties, etlSubsystemService, errorWarningMessages);

        try {
            fixture.getProperties(KnowledgeModuleType.Integration);
        } catch (Exception e) {
        }
        assertTrue(fixture.getErrorMessages().size() > 0);
        assertTrue(fixture.getErrorMessages().get(0).contains("src_technology"));
    }

}