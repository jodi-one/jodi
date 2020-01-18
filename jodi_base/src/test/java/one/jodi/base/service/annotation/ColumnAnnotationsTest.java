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
public class ColumnAnnotationsTest {
    private TestTableAnnotations tableAnnotation;
    private ErrorWarningMessageJodi errorWarningMessages;
    private Map<String, Object> annotations;
    private ColumnAnnotations fixture;

    @Before
    public void setUp() throws Exception {
        errorWarningMessages = ErrorWarningMessageJodiHelper
                .getTestErrorWarningMessages();
        errorWarningMessages.clear();

        annotations = new HashMap<>();
        TestAnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);

        tableAnnotation = new TestTableAnnotations("SCHEMA", "THIS_TABLE",
                annotationFactory,
                errorWarningMessages);
        fixture = new TestColumnAnnotations(tableAnnotation, "THIS_COLUMN",
                errorWarningMessages);
    }

    @Test
    public void testNames() {
        fixture.initializeAnnotations(Collections.emptyMap());
        assertEquals("THIS_TABLE", fixture.getParent().getName());
        assertEquals("THIS_COLUMN", fixture.getName());
    }

    @Test
    public void testWrongTypeBooleanAnnotation() {
        annotations.put("Hide", "yes");
        fixture.initializeAnnotations(annotations);
        assertEquals(1, this.errorWarningMessages.getErrorMessages().size());
        assertEquals(0, this.errorWarningMessages.getWarningMessages().size());
    }

    @Test
    public void testWrongTypeStringAnnotation() {
        annotations.put("Description", true);
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
    public void testGetDescription() {
        annotations.put("Description", "my description text");
        fixture.initializeAnnotations(annotations);

        assertTrue(fixture.getDescription().isPresent());
        assertEquals("my description text", fixture.getDescription().get());

        assertFalse(fixture.getBusinessName().isPresent());
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
}
