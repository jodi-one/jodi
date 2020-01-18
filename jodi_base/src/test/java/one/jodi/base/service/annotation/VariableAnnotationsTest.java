package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class VariableAnnotationsTest {

    private ErrorWarningMessageJodi errorWarningMessages;
    private Map<String, Object> annotations;
    private TestVariableAnnotations fixture;
    private String variableName = "VARIABLES";

    @Before
    public void setUp() throws Exception {
        errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
        errorWarningMessages.clear();
        annotations = new HashMap<>();
        fixture = new TestVariableAnnotations(variableName, errorWarningMessages);
    }

    @After
    public void tearDown() throws Exception {
        assertEquals(0, this.errorWarningMessages.getErrorMessages().size());
        assertEquals(0, this.errorWarningMessages.getWarningMessages().size());
        errorWarningMessages.clear();
    }

    @Test
    public void testVariable() {
        String varName = "Some Variable";
        TestAnnotationFactory taf = new TestAnnotationFactory(errorWarningMessages);
        VariableAnnotations variable = taf.createVariableAnnotations(varName);
        assertEquals(varName, variable.getName());
        variable.initializeAnnotations(annotations);
        assertTrue(variable.isValid());
    }

    @Test
    public void testIsEmpty() {
        fixture.initializeAnnotations(Collections.emptyMap());
        assertTrue(fixture.isEmpty());
        assertEquals(variableName, fixture.getName());
        assertTrue(fixture.isValid());
    }

    @Test
    public void testEmptyVariableName() {
        TestVariableAnnotations fixture =
                new TestVariableAnnotations("", errorWarningMessages);
        assertFalse(fixture.isValid());
        assertEquals(1, errorWarningMessages.getErrorMessages().size());
        errorWarningMessages.clear();
    }

    @Test
    public void testEmptyInitialization() {
        fixture.initializeAnnotations(new HashMap<>());
        assertEquals(new HashMap<>(), annotations);
    }

    @Test
    public void testUndefinedAnnotation() {
        annotations.put("NOT DEFINED", null);
        fixture.initializeAnnotations(annotations);
        assertEquals(0, this.errorWarningMessages.getErrorMessages().size());
        assertEquals(1, this.errorWarningMessages.getWarningMessages().size());
        errorWarningMessages.clear();
    }

}
