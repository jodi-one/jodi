package one.jodi.base.config;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class AbstactConfigurationTest {
    private ErrorWarningMessageJodi errorWarningMessages;
    private AbstactConfiguration fixture;
    private String propFile = "src/test/java/one/jodi/base/config/base.properties";
    private Configuration config;
    private Pattern lregExPattern;

    @Before
    public void setUp() throws Exception {
        errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
        fixture = new TestAbstactConfiguration(errorWarningMessages, propFile, false);
        config = fixture.getConfig();
        lregExPattern = Pattern.compile(AbstactConfiguration.BI_LOGICAL_OBJECT_NAME_REGEX);
    }

    @After
    public void tearDown() throws Exception {
        errorWarningMessages.clear();
        if (config != null) {
            config.clear();
        }
    }

    @Test
    public final void testFileDoesNotExist() {
        try {
            fixture = new TestAbstactConfiguration(errorWarningMessages, "noFile", false);
        } catch (RuntimeException re) {
            assertTrue(re.getMessage().contains("80100"));
        }
    }


    @Test
    public final void testFileDoesExist() {
        fixture = new TestAbstactConfiguration(errorWarningMessages, propFile, false);
    }

    @Test
    public final void testGetProperty_null() {
        String key = "nullValue";
        config.addProperty(key, null);
        assertEquals((String) config.getProperty(key), fixture.getProperty(key));
    }

    @Test
    public final void testGetProperty() {
        String key = "someValue";
        config.addProperty(key, "value");

        String testCase = (String) config.getProperty(key);
        assertEquals(testCase, "value");

        String tc = fixture.getProperty(key);
        assertEquals(tc, "value");
    }

    @Test
    public final void testGetProperty_embedded() {
        String key = "embeddedValue";
        config.addProperty(key, " value   ");

        String testCase = (String) config.getProperty(key);
        assertEquals(testCase, "value");

        String tc = fixture.getProperty(key);
        assertEquals("value", tc);
    }

    @Test
    public final void testGetProperty_values2() {
        String key = "commaSeparateValuesInString";
        config.addProperty(key, "value1");
        config.addProperty(key, "value2");
        config.addProperty(key, "value3");
        config.addProperty(key, "value4");
        assertEquals(config.getProperty(key).toString(), fixture.getProperty(key));
    }

    @Test
    public final void testGetProperty_values() {
        String key = "commaSeparateValuesInString";
        config.addProperty(key, "value1, value2, value3");
        assertEquals(config.getProperty(key).toString(), fixture.getProperty(key));
    }

    @Test
    public final void testGetProperty_noValue() {
        String key = "noValue";
        String value = "";
        config.addProperty(key, value);
        assertEquals(config.getProperty(key), fixture.getProperty(key));
    }

    @Test
    public final void testGetPropertyList_values() {
        String key = "3commaSeparatedValues";
        String values = "value1, value2, value3";
        config.addProperty(key, values);
        assertEquals(config.getProperty(key), fixture.getPropertyList(key));
    }

    @Test
    public final void testGetPropertyList_valuesList() {
        String key = "listOfThreeValues";
        List<String> values = new ArrayList<String>();
        values.add("value1");
        values.add("value2");
        values.add("value3");
        config.addProperty(key, values);
        assertEquals(values, fixture.getPropertyList(key));
    }

    @Test
    public final void testGetPropertyList_emptyList() {
        String key = "emptyListValues";
        assertEquals(new ArrayList<String>(), fixture.getPropertyList(key));
    }

    @Test
    public final void testGetPropertyList_nullConfig() {
        String key = "emptyListNullConfig";
        fixture.setConfig(null);
        assertEquals(new ArrayList<String>(), fixture.getPropertyList(key));
    }

    @Test
    public final void testGetProperty_nullConfig() {
        String key = "nullConfig";
        fixture.setConfig(null);
        assertEquals(null, fixture.getProperty(key));
    }

    @Test
    public final void testGetProperty_nullProperty() {
        String key = "nullProperty";
        assertEquals(null, fixture.getProperty(key));
    }

    @Test
    public final void testGetPropertyList_nullProperty() {
        String key = null;
        assertEquals(new ArrayList<String>(), fixture.getPropertyList(key));
    }

    @Test
    public final void testContainsKey() {
        String key = "values";
        List<String> values = new ArrayList<String>();
        values.add("value1");
        values.add("value2");
        values.add("value3");
        config.addProperty(key, values);
        assertEquals(values, fixture.getPropertyList(key));
        assertTrue(config.containsKey(key));
    }

    @SuppressWarnings("deprecation")
    @Test
    public final void testGetStringArray() {
        String key = "values";
        List<String> values = new ArrayList<String>();
        values.add("value1");
        values.add("value2");
        values.add("value3");
        config.addProperty(key, values);
        assertEquals(values, fixture.getPropertyList(key));
        Object[] saValues = values.toArray();
        assertEquals(saValues, config.getStringArray(key));
    }

    //Test
    public final void testGetKeys() {
        String key = "values";
        List<String> values = new ArrayList<String>();
        values.add("value1");
        values.add("value2");
        values.add("value3");
        config.addProperty(key, values);
        assertEquals(values, fixture.getPropertyList(key));
        assertTrue(config.containsKey(key));
        List<String> keys = new ArrayList<String>();
        keys.add(key);
    }

    @Test
    public void testRegExLogicalEnglish() {
        Matcher m = lregExPattern.matcher("abc ABC123!@#$%^&()_+{}[]|\\:;<>,");
        assertTrue(m.matches());
    }

    @Test
    public void testRegExLogicalLeadingSpace() {
        Matcher m = lregExPattern.matcher(" abcABC123!@#$%^&()_+{}[]|\\:;<>,");
        assertFalse(m.matches());
    }

    @Test
    public void testRegExLogicalTrailingSpace() {
        Matcher m = lregExPattern.matcher("a ");
        assertFalse(m.matches());
    }

    @Test
    public void testRegExLogicalUmlaute() {
        Matcher m = lregExPattern.matcher("äçèéêëìíîïüàöüÖÅÜÏï");
        assertTrue(m.matches());
    }

    @Test
    public void testRegExLogicalChinese() {
        Matcher m = lregExPattern.matcher("導字會");
        assertTrue(m.matches());
    }

    @Test
    public void testRegExPhysical() {
        Pattern p = Pattern.compile(AbstactConfiguration.ORACLE_OBJECT_NAME_REGEX);
        Matcher m = p.matcher("ABC_DEF_123_#$");
        assertTrue(m.matches());
    }

    @Test
    public void testRegExPhysicalIncorrect() {
        Pattern p = Pattern.compile(AbstactConfiguration.ORACLE_OBJECT_NAME_REGEX);
        Matcher m = p.matcher("$ABC_DEF_#$");
        assertFalse(m.matches());

        p = Pattern.compile(AbstactConfiguration.ORACLE_OBJECT_NAME_REGEX);
        m = p.matcher("1ABC_DEF_#$");
        assertFalse(m.matches());

        p = Pattern.compile(AbstactConfiguration.ORACLE_OBJECT_NAME_REGEX);
        m = p.matcher(" ABC_DEF_#$");
        assertFalse(m.matches());

        p = Pattern.compile(AbstactConfiguration.ORACLE_OBJECT_NAME_REGEX);
        m = p.matcher("ABC_DEF_:");
        assertFalse(m.matches());
    }


}
