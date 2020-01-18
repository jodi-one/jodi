package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.service.annotation.NameSpaceComponent.NameSpaceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class KeyParserTest {

    private ErrorWarningMessageJodi errorWarningMessages;
    private KeyParser fixture;

    @Before
    public void setUp() throws Exception {
        errorWarningMessages = ErrorWarningMessageJodiHelper
                .getTestErrorWarningMessages();
        errorWarningMessages.clear();
        fixture = new KeyParserImpl(errorWarningMessages);
    }

    @Test
    public void testMalformedWrongKeyLength() {
        String key = "schemas.ssss.tables";
        try {
            fixture.parseKey(key);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Key '" + key + "' is malformed.",
                    e.getMessage().substring(8));
        }
    }

    @Test
    public void testTableKey() {
        Key key = fixture.parseKey("schemas.ssss.tables.tttt");
        assertEquals(2, key.getNameSpace().size());
        assertEquals(NameSpaceType.SCHEMA, key.getNameSpace().get(0).getType());
        assertEquals("ssss", key.getNameSpace().get(0).getName());
        assertEquals(NameSpaceType.TABLE, key.getNameSpace().get(1).getType());
        assertEquals("tttt", key.getNameSpace().get(1).getName());
    }

    @Test
    public void testMalformedWrongSchemaKeyword() {
        String key = "schem.ssss.tables.tttt";
        try {
            fixture.parseKey(key);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Key '" + key +
                            "' is malformed. Expected keyword 'SCHEMA' in position 0.",
                    e.getMessage().substring(8));
        }
    }


    @Test
    public void testMalformedMissingSchemaName() {
        String key = "schemas..tables.tttt";
        try {
            fixture.parseKey(key);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Key '" + key + "' is malformed. Expected non-empty name " +
                    "in position 1.", e.getMessage().substring(8));
        }
    }

    @Test
    public void testMalformedWrongTableKeyword() {
        String key = "schemas.ssss.table.tttt";
        try {
            fixture.parseKey(key);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Key '" + key +
                            "' is malformed. Expected keyword 'TABLE' in position 2.",
                    e.getMessage().substring(8));
        }
    }

    @Test
    public void testMalformedMissingTableName() {
        String key = "schemas.ssss.tables.";
        try {
            fixture.parseKey(key);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Key 'schemas.ssss.tables.' is malformed.",
                    e.getMessage().substring(8));
        }
    }

}
