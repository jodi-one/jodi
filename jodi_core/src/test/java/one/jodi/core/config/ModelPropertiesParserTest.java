package one.jodi.core.config;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesImpl;
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
public class ModelPropertiesParserTest {

    private final static String BEAN_ID_FIELD = "modelID";
    private final static String TOPIC = "model";
    private final static String[] PARAMETER_DEFS = new String[]{"name!", "default", "order", "layer", "prefix[]", "postfix[]"};
    private final static String[] INCORRECT_PAR_DEFS = new String[]{"layer", "prefix[", "postfix[]"};
    // Infrastructure
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    JodiProperties mockProperties;
    Map<String, String> mockProps = new HashMap<String, String>();
    List<String> expectedParameters;
    PropertiesParser<ModelPropertiesImpl> fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    boolean isList(final String name, final List<String> parameterDefs) {
        boolean isList = false;

        for (String param : parameterDefs) {
            String p = param.endsWith("!") ? param.substring(0, param.length() - 1)
                    : param;
            if (p.endsWith("[]") && name.endsWith(p.substring(0, p.length() - "[]".length()))) {
                isList = true;
                break;
            }
        }
        return isList;
    }

    private void configure(String additionalKey, String additionalValue) {
        mockProps.put("yyyyy.xxx.name", "SOMETHING DIFFERENT");
        mockProps.put("model.star.layer", "STAR");
        mockProps.put("model.star.order", "100");
        mockProps.put("model.star.default", "true");
        mockProps.put("model.star.undefined", "SOMETHING ELSE");
        mockProps.put("model.star.prefix", "W_");
        if (additionalKey != null) {
            mockProps.put(additionalKey, additionalValue);
        }

        expectedParameters = new ArrayList<String>();
        for (int i = 0; i < PARAMETER_DEFS.length; i++) {
            expectedParameters.add(PARAMETER_DEFS[i]);
        }

        // define properties file - identical to the mockProp Map
        for (String key : mockProps.keySet()) {
            if (isList(key, expectedParameters)) {
                List<String> l = Arrays.asList(mockProps.get(key).replaceAll("\\s", "").split(","));
                when(mockProperties.getPropertyList(key)).thenReturn(l);
            } else {
                when(mockProperties.getProperty(key)).thenReturn(mockProps.get(key));
            }
        }
    }


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsList() {
        configure("model.star.postfix", "_A, _D, _F, _H");
        fixture = new PropertiesParser<ModelPropertiesImpl>(mockProperties, errorWarningMessages);
        assertTrue(fixture.isList("prefix", expectedParameters));
        assertFalse(fixture.isList("order", expectedParameters));
    }

    @Test
    public void parseTest() {
        configure("model.star.postfix", "_A, _D, _F, _H");
        List<ModelPropertiesImpl> modPropList;
        when(mockProperties.getProperty("model.star.name")).thenReturn("GBU_DATA_MART_MODEL");
        mockProps.put("model.star.name", "GBU_DATA_MART_MODEL");
        when(mockProperties.getPropertyKeys()).thenReturn(new ArrayList<String>(mockProps.keySet()));

        fixture = new PropertiesParser<ModelPropertiesImpl>(mockProperties, errorWarningMessages);
        modPropList = fixture.parseProperties(TOPIC, BEAN_ID_FIELD, expectedParameters, ModelPropertiesImpl.class);

        assertEquals(1, modPropList.size());

        ModelProperties modProp = modPropList.get(0);

        assertEquals("model.star", modProp.getModelID());
        assertTrue(modProp.isDefault());
        assertEquals(100, modProp.getOrder());
        assertEquals("STAR", modProp.getLayer());
        assertEquals(1, modProp.getPrefix().size());
        assertEquals(4, modProp.getPostfix().size());
        assertTrue(modProp.getPostfix().contains("_F"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseTestFails() {
        configure("model.star.postfix", "_A, _D, _F, _H");
        expectedParameters = new ArrayList<String>();
        for (int i = 0; i < INCORRECT_PAR_DEFS.length; i++) {
            expectedParameters.add(INCORRECT_PAR_DEFS[i]);
        }
        fixture = new PropertiesParser<ModelPropertiesImpl>(mockProperties, errorWarningMessages);
        fixture.parseProperties(TOPIC, expectedParameters, ModelPropertiesImpl.class);
    }

    @Test
    public void parseTestMandatoryPropertyMissing() {
        configure("model.star.postfix", "_A, _D, _F, _H");
        when(mockProperties.getPropertyKeys()).thenReturn(new ArrayList<String>(mockProps.keySet()));

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Mandatory properties missing");

        fixture = new PropertiesParser<ModelPropertiesImpl>(mockProperties, errorWarningMessages);
        fixture.parseProperties(TOPIC, expectedParameters, ModelPropertiesImpl.class);
    }

    @Test
    public void parseTestMandatoryPropertyNoProperty() {
        configure("model.star.postfix", "");
        when(mockProperties.getProperty("model.star.name")).thenReturn("GBU_DATA_MART_MODEL");
        mockProps.put("model.star.name", "GBU_DATA_MART_MODEL");
        when(mockProperties.getPropertyKeys()).thenReturn(new ArrayList<String>(mockProps.keySet()));

        List<ModelPropertiesImpl> modPropList;
        fixture = new PropertiesParser<ModelPropertiesImpl>(mockProperties, errorWarningMessages);
        modPropList = fixture.parseProperties(TOPIC, BEAN_ID_FIELD, expectedParameters, ModelPropertiesImpl.class);

        ModelProperties modProp = modPropList.get(0);

        assertEquals(0, modProp.getPostfix().size());
    }

    /*
     * ensure that parser sets Lists that are not explicitly defined to emptyList
     */
    @Test
    public void parseTestMandatoryPropertyUndefinedProperty() {
        configure(null, null);
        when(mockProperties.getProperty("model.star.name")).thenReturn("GBU_DATA_MART_MODEL");
        mockProps.put("model.star.name", "GBU_DATA_MART_MODEL");
        when(mockProperties.getPropertyKeys()).thenReturn(new ArrayList<String>(mockProps.keySet()));

        List<ModelPropertiesImpl> modPropList;
        fixture = new PropertiesParser<ModelPropertiesImpl>(mockProperties, errorWarningMessages);
        modPropList = fixture.parseProperties(TOPIC, BEAN_ID_FIELD, expectedParameters, ModelPropertiesImpl.class);

        ModelProperties modProp = modPropList.get(0);

        assertEquals(0, modProp.getPostfix().size());
    }

}