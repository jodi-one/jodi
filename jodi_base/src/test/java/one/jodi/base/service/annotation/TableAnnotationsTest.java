package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TableAnnotationsTest {
    private ErrorWarningMessageJodi errorWarningMessages;
    private Map<String, Object> annotations;
    private TestTableAnnotations fixture;

    @Before
    public void setUp() throws Exception {
        errorWarningMessages = ErrorWarningMessageJodiHelper
                .getTestErrorWarningMessages();
        errorWarningMessages.clear();
        annotations = new HashMap<>();
        TestAnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);
        fixture = new TestTableAnnotations("SCHEMA", "THIS_TABLE", annotationFactory,
                errorWarningMessages);
    }

    @Test
    public void testNames() {
        fixture.initializeAnnotations(Collections.emptyMap());
        assertEquals("SCHEMA", fixture.getSchemaName());
        assertEquals("THIS_TABLE", fixture.getName());
    }

    @Test
    public void testWrongTypeStringAnnotation() {
        annotations.put("Type", true);
        fixture.initializeAnnotations(annotations);
        assertEquals(1, this.errorWarningMessages.getErrorMessages().size());
        assertEquals(0, this.errorWarningMessages.getWarningMessages().size());
    }

    @Test
    public void testWrongTypeBooleanAnnotation() {
        annotations.put("hide", "yes");
        fixture.initializeAnnotations(annotations);
        assertEquals(1, this.errorWarningMessages.getErrorMessages().size());
        assertEquals(0, this.errorWarningMessages.getWarningMessages().size());
    }

    @Test
    public void testUnknownAnnotation() {
        annotations.put("Unknown", "yes");
        fixture.initializeAnnotations(annotations);
        assertEquals(0, this.errorWarningMessages.getErrorMessages().size());
        assertEquals(1, this.errorWarningMessages.getWarningMessages().size());
    }

    @Test
    public void testGetBusinessName() {
        annotations.put("Business Name", "My Business Name");
        annotations.put("description", "my description text");
        fixture.initializeAnnotations(annotations);

        assertTrue(fixture.getBusinessName().isPresent());
        assertEquals("My Business Name", fixture.getBusinessName().get());
        assertTrue(fixture.getDescription().isPresent());
        assertEquals("my description text", fixture.getDescription().get());

        assertFalse(fixture.getAbbreviatedBusinessName().isPresent());
    }

    @Test
    public void testGetAbbreviatedBusinessName() {
        annotations.put("Abbreviated Business Name", "My abbrev Business Name");
        annotations.put("business name", "My Business Name");
        fixture.initializeAnnotations(annotations);

        assertTrue(fixture.getAbbreviatedBusinessName().isPresent());
        assertEquals("My abbrev Business Name",
                fixture.getAbbreviatedBusinessName().get());
        assertTrue(fixture.getBusinessName().isPresent());
        assertEquals("My Business Name", fixture.getBusinessName().get());
    }

    @Test
    public void testGetColumnAnnotations() {
        annotations.put("Business Name", "My Business Name");
        annotations.put("description", "my description text");
        fixture.addColumnAnnotations("COLUMN1", annotations);
        fixture.addColumnAnnotations("COLUMN2", annotations);

        Map<String, ColumnAnnotations> ca = fixture.getColumnAnnotations();
        assertEquals(2, ca.size());
        assertNotNull(ca.get("COLUMN1"));
        assertNotNull(ca.get("COLUMN2"));

        assertEquals("My Business Name", ca.get("COLUMN1").getBusinessName().get());
        assertEquals("My Business Name", ca.get("COLUMN2").getBusinessName().get());
    }

}
