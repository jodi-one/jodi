package one.jodi.base.service.annotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ObjectAnnotationTest {

    private final Map<String, Class<?>> unexpTypes = new HashMap<>();
    private final Set<String> undefinedKeys = new HashSet<>();
    private ObjectAnnotation fixture;

    @Before
    public void setUp() throws Exception {
        this.unexpTypes.clear();
        fixture = new ObjectAnnotationTestImpl("THIS_TABLE");
    }

    @Test
    public void testCreateKeyTypeMap() {
        String[] bKeys = new String[]{"aa", "bB", "Cc", "DD"};
        String[] sKeys = new String[]{"hh", "jJ", "Kk", "LL"};
        String[] iKeys = new String[]{"oo", "pP", "Qq", "RR"};
        String[] aKeys = new String[]{"ss", "tT", "Uu", "VV"};
        Map<String, Class<?>> ntMap =
                ObjectAnnotation.createKeyTypeMap(bKeys, sKeys, iKeys, aKeys);

        assertEquals(16, ntMap.size());
        for (String key : bKeys) {
            assertEquals(Boolean.class, ntMap.get(key.toLowerCase()));
        }
        for (String key : sKeys) {
            assertEquals(String.class, ntMap.get(key.toLowerCase()));
        }
        for (String key : iKeys) {
            assertEquals(Integer.class, ntMap.get(key.toLowerCase()));
        }
    }

    @Test
    public void testGetValidAnnotations() {
        String[] bKeys = new String[]{"Is A", "Is B", "Is C", "Is D"};
        String[] sKeys = new String[]{"This G", "This H", "This I", "This J"};
        String[] iKeys = new String[]{"Number O", "Number P", "Number Q", "Number R"};
        String[] aKeys = new String[]{"Number S", "Number T", "Number U", "Number V"};
        Map<String, Class<?>> ntMap =
                ObjectAnnotation.createKeyTypeMap(bKeys, sKeys, iKeys, aKeys);

        Map<String, Object> annotations = new HashMap<>();
        annotations.put("Is A", true);
        annotations.put("Is b", false);
        // Is C missing
        annotations.put("Is D", "wrong");

        annotations.put("This G", "value G");
        annotations.put("This H", null);
        // I is missing
        // This J has the wrong type associated
        annotations.put("This J", true);
        annotations.put("XYZ", "UNDEFINED");

        annotations.put("Number O", 15);
        annotations.put("Number P", 16.0); // Floating point
        // Q is missing
        annotations.put("Number R", "UNDEFINED");

        Map<String, Object> result = fixture.getValidAnnotations(annotations, ntMap);
        assertEquals(4, result.size());
        assertTrue((boolean) result.get("is a"));
        assertFalse((boolean) result.get("is b"));
        assertEquals("value G", (String) result.get("this g"));
        assertTrue(15 == (Integer) result.get("number o"));

        assertEquals(4, this.unexpTypes.size());
        assertEquals(Boolean.class, this.unexpTypes.get("Is D"));
        assertEquals(String.class, this.unexpTypes.get("This J"));
        assertEquals(Integer.class, this.unexpTypes.get("Number P"));
        assertEquals(Integer.class, this.unexpTypes.get("Number R"));

        assertEquals(1, this.undefinedKeys.size());
        assertTrue(this.undefinedKeys.contains("XYZ"));
    }

    class ObjectAnnotationTestImpl extends ObjectAnnotation {
        protected ObjectAnnotationTestImpl(final String name) {
            super(name);
        }

        protected Map<String, Class<?>> getKeyTypeMap() {
            // setup annotation key and expected type association
            return Collections.emptyMap();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        protected void reportUnexpectedType(final String key,
                                            final Class<?> keyTypes) {
            unexpTypes.put(key, keyTypes);
        }

        protected void reportUndefinedAnnotation(final String key) {
            undefinedKeys.add(key);
        }
    }

}
