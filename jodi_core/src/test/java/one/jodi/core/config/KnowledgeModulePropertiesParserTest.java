package one.jodi.core.config;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.km.KnowledgeModuleProperties;
import one.jodi.core.config.km.KnowledgeModulePropertiesImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class KnowledgeModulePropertiesParserTest {

    private final static String BEAN_ID_FIELD = "id";
    private final static String TOPIC = "km";
    private final String[] PARAMETER_DEFS = new String[]{"name[]!", "order", "global", "options{}", "trg_technology!", "src_technology", "default", "trg_temporary", "trg_regex", "trg_layer[]", "trg_tabletype[]", "src_regex", "src_layer[]", "src_tabletype[]"};
    // Infrastructure
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    JodiProperties mockProperties;
    List<String> expectedParameters;
    PropertiesParser<KnowledgeModulePropertiesImpl> fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

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
            }
        }
    }


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }


    Map<String, String> buildPropertiesMap() {
        Map<String, String> propertiesMap = new HashMap<String, String>();

        propertiesMap.put("km.lkm_default.name", "LKM Name");
        propertiesMap.put("km.lkm_default.global", "true");
        propertiesMap.put("km.lkm_default.order", "100");
        propertiesMap.put("km.lkm_default.trg_technology", "trg_technology");
        propertiesMap.put("km.lkm_default.src_technology", "src_technology");
        propertiesMap.put("km.lkm_default.default", "true");
        propertiesMap.put("km.lkm_default.trg_temporary", "1");
        propertiesMap.put("km.lkm_default.trg_regex", "^(?!W_)[\\w\\d]+|^W_[\\w\\d]+(?<!_[HF])$");
        propertiesMap.put("km.lkm_default.trg_layer", "trg_layer");
        propertiesMap.put("km.lkm_default.src_regex", "^(?!W_)[\\w\\d]+|^W_[\\w\\d]+(?<!_[HF])$");
        propertiesMap.put("km.lkm_default.src_layer", "src_layer");
        propertiesMap.put("km.lkm_default.trg_tabletype", "DIMENSION");
        propertiesMap.put("km.lkm_default.src_tabletype", "UNKNOWN");
        propertiesMap.put("km.lkm_default.undefined", "SOMETHING ELSE");
        propertiesMap.put("km.lkm_default.options", "true:true,false:false,integer:1,string:string");
        propertiesMap.put("km.lkm_default.datastoretype", "DIMENSION");

        return propertiesMap;
    }


    @Test
    public void parseFullProperties() {
        Map<String, String> propertiesMap = buildPropertiesMap();

        configure(propertiesMap);


        fixture = new PropertiesParser<KnowledgeModulePropertiesImpl>(mockProperties, errorWarningMessages);
        List<KnowledgeModulePropertiesImpl> modPropList = fixture.parseProperties(TOPIC, BEAN_ID_FIELD, expectedParameters, KnowledgeModulePropertiesImpl.class);

        assertEquals(1, modPropList.size());

        KnowledgeModuleProperties km = modPropList.get(0);

        assertEquals("km.lkm_default", km.getId());
        assertEquals(new Boolean(propertiesMap.get("km.lkm_default.global")), km.isGlobal());
        assertEquals(new Integer(propertiesMap.get("km.lkm_default.order")), km.getOrder());

        assertEquals(1, km.getName().size());
        assertEquals(propertiesMap.get("km.lkm_default.name").replace("[]!", ""), km.getName().get(0));
        assertEquals(propertiesMap.get("km.lkm_default.trg_technology"), km.getTrg_technology());
        assertEquals(propertiesMap.get("km.lkm_default.src_technology"), km.getSrc_technology());
        assertEquals(new Boolean(propertiesMap.get("km.lkm_default.default")), km.isDefault());
        assertEquals(new Integer(propertiesMap.get("km.lkm_default.trg_temporary")), km.getTrg_temporary());
        assertEquals(propertiesMap.get("km.lkm_default.trg_regex"), km.getTrg_regex());
        assertEquals(propertiesMap.get("km.lkm_default.trg_layer"), km.getTrg_layer().get(0));
        assertEquals(propertiesMap.get("km.lkm_default.src_regex"), km.getSrc_regex());
        assertEquals(propertiesMap.get("km.lkm_default.src_layer"), km.getSrc_layer().get(0));
        assertEquals(propertiesMap.get("km.lkm_default.trg_tabletype"), km.getTrg_tabletype().get(0));
        assertEquals(propertiesMap.get("km.lkm_default.src_tabletype"), km.getSrc_tabletype().get(0));
        assertNotNull(km.getOptions().get("true"));
        assertEquals(km.getOptions().get("true"), true);
        assertNotNull(km.getOptions().get("false"));
        assertEquals(km.getOptions().get("false"), false);
        assertNotNull(km.getOptions().get("integer"));
        assertEquals(km.getOptions().get("integer"), 1);
        assertNotNull(km.getOptions().get("string"));
        assertEquals(km.getOptions().get("string"), "string");
        assertNull(km.getOptions().get("shouldnt find me"));
    }


    @Test
    public void parseTestStringPropertyMissing() {
        // test forgetting to include "trg_technology"
        Map<String, String> propertiesMap = buildPropertiesMap();

        propertiesMap.remove("km.lkm_default.trg_technology");
        configure(propertiesMap);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Mandatory properties missing");

        fixture = new PropertiesParser<KnowledgeModulePropertiesImpl>(mockProperties, errorWarningMessages);
        fixture.parseProperties(TOPIC, expectedParameters, KnowledgeModulePropertiesImpl.class);

    }

    @Test
    public void parseTestListPropertyMissing() {
        // test forgetting to include "trg_technology"
        Map<String, String> propertiesMap = buildPropertiesMap();

        propertiesMap.remove("km.lkm_default.name");
        configure(propertiesMap);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Mandatory properties missing");

        fixture = new PropertiesParser<KnowledgeModulePropertiesImpl>(mockProperties, errorWarningMessages);
        fixture.parseProperties(TOPIC, expectedParameters, KnowledgeModulePropertiesImpl.class);
    }

    @Test
    public void parseRegexTest() {
        buildPropertiesMap();
    }


}