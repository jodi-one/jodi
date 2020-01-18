package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class JsAnnotationImplTest {

    private ErrorWarningMessageJodi errorWarningMessages;
    private KeyParser keyParser;
    private JsAnnotation fixture;

    @Before
    public void setUp() throws Exception {
        errorWarningMessages = ErrorWarningMessageJodiHelper
                .getTestErrorWarningMessages();
        keyParser = new KeyParserImpl(errorWarningMessages);
    }

    @After
    public void print() {
        errorWarningMessages.printMessages();
        errorWarningMessages.clear();
    }

    //
    //	Test Variable Annotations
    //
    @Test
    public void testGetVariablesEmpty() throws MalformedAnnotationException {
        final String js = "{\"Schemas\" : []," +
                "\"Variables\" : []}";
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);
        List<? extends VariableAnnotations> variableAnnotations =
                fixture.getVariableAnnotations();
        assertEquals(0, variableAnnotations.size());
    }

    @Test
    public void testGetVariablesOnly() {
        final String js = "{\"Variables\" : [{ \"name\" : \"Name\"}]}";
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);
        List<? extends VariableAnnotations> variableAnnotations =
                fixture.getVariableAnnotations();
        assertEquals(1, variableAnnotations.size());
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetVariablesJSONError() {
        final String js = "{\"Variables\" : [{ \"name\" : ]}"; // missing value and '}'
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);
        fixture.getVariableAnnotations();
    }

    @Test
    public void testGetVariablesWithDuplicates()
            throws MalformedAnnotationException {
        final String js = "{\"Schemas\" : []," +
                "\"Variables\" : [" +
                "{\"Name\" : \"Static Repository Variable\"," +
                "\"Description\" : \"This is a variable description\"," +
                "\"Default Initialization\" : \"'Initial Value 1'\"," +
                "\"Is Session\" : false}," +
                "{\"Name\" : \"Static Repository Variable\"," +
                "\"Description\" : \"This is a duplicate variable description\"," +
                "\"Default Initialization\" : \"'Initial Value 1'\"," +
                "\"Is Session\" : false}" +
                "]}";
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);
        List<? extends VariableAnnotations> variableAnnotations =
                fixture.getVariableAnnotations();
        assertEquals(1, variableAnnotations.size());
        assertEquals("Static Repository Variable", variableAnnotations.get(0).getName());
        assertTrue(errorWarningMessages.getErrorMessages().values().toString()
                .contains("[80588] Duplicate variable annotation exist for " +
                        "'Static Repository Variable'."));
    }

    //
    // Test New Bulk Methods
    //
    @Test
    public void testGetTableAnnotationEmptyJs() {
        final String js = "{ }";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.noname.Tables.noname";
        Key key = keyParser.parseKey(errorKeyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetTableAnnotationMalformdedJs() {
        final String js = "{ aaa }";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.noname.Tables.noname";
        Key key = keyParser.parseKey(errorKeyString);
        fixture.getTableAnnotations(key);
    }

    @Test
    public void testGetTableAnnotationNullSchema() {
        final String js = "{\"Schemas\" : null }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.noname.Tables.noname";
        Key key = keyParser.parseKey(errorKeyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetTableAnnotationNullSchemaNameAnnotation() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : null," +
                        "\"Tables\" : null" +
                        "}" +
                        "]" +
                        "}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.CT15.Tables.somename";
        Key key = keyParser.parseKey(errorKeyString);
        fixture.getTableAnnotations(key);
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetTableAnnotationIncorrectTypeSchemaNameAnnotation() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : 1," +
                        "\"Tables\" : null" +
                        "}" +
                        "]" +
                        "}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.CT15.Tables.somename";
        Key key = keyParser.parseKey(errorKeyString);
        fixture.getTableAnnotations(key);

    }

    @Test
    public void testGetTableAnnotationNullTableAnnotation() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"Tables\" : null" +
                        "}" +
                        "]" +
                        "}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.CT15.Tables.somename";
        Key key = keyParser.parseKey(errorKeyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetTableAnnotationTablesNotAnArray() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"Tables\" : 1" +
                        "}" +
                        "]" +
                        "}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.CT15.Tables.noname";
        Key key = keyParser.parseKey(errorKeyString);
        try {
            fixture.getTableAnnotations(key);
            fail("expected an exception instead");
        } catch (MalformedAnnotationException e) {
            assertEquals("Key 'Schemas.CT15.Tables.noname' encountered to an " +
                            "unexpected type in the JSON object.",
                    e.getMessage().substring(8));
        }
    }

    @Test
    public void testGetTableAnnotationTablesNullArray() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"Tables\" : []" +
                        "}" +
                        "]" +
                        "}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.CT15.Tables.noname";
        Key key = keyParser.parseKey(errorKeyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetTableAnnotationFileNotAvailable() {
        AnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);
        fixture = new JsAnnotationImpl("folderDoesNotExist", annotationFactory,
                errorWarningMessages);

        String errorKeyString = "Schemas.noname.Tables.noname";
        Key key = keyParser.parseKey(errorKeyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGatherAnnotationFilesWithNoFiles() {
        AnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);

        fixture = new JsAnnotationImpl("src" +
                File.separator + "test" +
                File.separator + "resources" +
                File.separator + "jsTestFiles" +
                File.separator + "jsSubTestFiles" +
                File.separator + "jsSub_SubTestFiles",
                annotationFactory, errorWarningMessages);
        assertEquals(0, errorWarningMessages.getErrorMessages().size());
        assertEquals(0, errorWarningMessages.getWarningMessages().size());
    }

    @Test
    public void testGatherAnnotationFilesWithFiles() {
        AnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);
        fixture = new JsAnnotationImpl("src" +
                File.separator + "test" +
                File.separator + "resources" +
                File.separator + "jsTestFiles",
                annotationFactory, errorWarningMessages);
        assertEquals(0, errorWarningMessages.getErrorMessages().size());
        assertEquals(0, errorWarningMessages.getWarningMessages().size());

    }

    @Test
    public void testAnnotationValidation() {
        AnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);
        fixture = new JsAnnotationImpl("src" +
                File.separator + "test" +
                File.separator + "resources" +
                File.separator + "jsTestFiles",
                annotationFactory, errorWarningMessages);
        assertEquals(0, errorWarningMessages.getErrorMessages().size());
        assertEquals(0, errorWarningMessages.getWarningMessages().size());
        errorWarningMessages.printMessages();

        // duplicate tables
        String keyString = "Schemas.TEST.Tables.TableName1";
        Key key = keyParser.parseKey(keyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertTrue(target.isPresent());
        TestTableAnnotations ta = (TestTableAnnotations) target.get();
        assertEquals("TableName1", ta.getName());
        assertEquals("TEST", ta.getSchemaName());
        errorWarningMessages.printMessages();
        assertEquals(2, errorWarningMessages.getWarningMessages().size());
        assertTrue(errorWarningMessages.getWarningMessages().values().toString()
                .contains("[80580] Duplicate table 'TableName1' annotation found"));

        keyString = "Schemas.TEST.Tables.TABLEZ";
        key = keyParser.parseKey(keyString);
        target = fixture.getTableAnnotations(key);
        assertTrue(target.isPresent());
        ta = (TestTableAnnotations) target.get();
        assertEquals("TABLEZ", ta.getName());
        assertEquals("TEST", ta.getSchemaName());
        assertEquals(2, errorWarningMessages.getWarningMessages().size());
        assertTrue(errorWarningMessages.getWarningMessages().values().toString()
                .contains("[80580] Duplicate table 'TABLEZ' annotation found"));

        // duplicate columns within the same table
        keyString = "Schemas.TEST.Tables.TableName0";
        key = keyParser.parseKey(keyString);
        try {
            target = fixture.getTableAnnotations(key);
        } catch (MalformedAnnotationException mae) {
            assertTrue(errorWarningMessages.getErrorMessages().values().toString()
                    .contains("[80570] Duplicate column name 'COL10' accessed"));
        }
    }

    @Test
    public void testGetFileNameGivenKey() {
        AnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);
        fixture = new JsAnnotationImpl("src" +
                File.separator + "test" +
                File.separator + "resources" +
                File.separator + "jsTestFiles",
                annotationFactory, errorWarningMessages);
        assertEquals(0, errorWarningMessages.getErrorMessages().size());
        assertEquals(0, errorWarningMessages.getWarningMessages().size());
        errorWarningMessages.printMessages();

        String keyString = "Schemas.TEST.Tables.TableName";
        Key key = keyParser.parseKey(keyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertTrue(target.isPresent());
        TestTableAnnotations ta = (TestTableAnnotations) target.get();
        assertEquals("TableName", ta.getName());
        assertEquals("TEST", ta.getSchemaName());

        keyString = "Schemas.TEST.Tables.name";
        key = keyParser.parseKey(keyString);
        target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());

        fixture = new JsAnnotationImpl("invalidDirectory", annotationFactory,
                errorWarningMessages);
        assertEquals(2, errorWarningMessages.getErrorMessages().size());
        assertEquals(2, errorWarningMessages.getWarningMessages().size());

        keyString = "Schemas.TEST.Tables.table";
        key = keyParser.parseKey(keyString);
        target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetTableAnnotationColumnEmptyName() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"tables\" : [" +
                        "{\"Name\" : \"\"," +
                        "\"Columns\" : [" +
                        "{\"Name\" : \"A_CODE\"," +
                        "\"Business Name\" : \"Drill Key 1 Override\"," +
                        "\"Hide\" : true" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String keyString = "Schemas.CT15.Tables.W_DIMENSION01_D";
        Key key = keyParser.parseKey(keyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        TableAnnotations ta = target.get();

        assertEquals(0, ta.getColumnAnnotations().size());
    }

    @Test
    public void testGetTableAnnotation() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"tables\" : [" +
                        "{\"Name\" : \"W_DIMENSION01_D\"," +
                        "\"Type\" : \"FACT\"," +
                        "\"business Name\" : \"Dimension 01 Override Ignored\"," +
                        "\"Is Ragged Dimension\" : true," +
                        "\"Columns\" : [" +
                        "{\"Name\" : \"DIM01_CODE\"," +
                        "\"Business Name\" : \"Drill Key 1 Override\"," +
                        "\"Hide\" : true" +
                        "}," +
                        "{\"Name\" : \"DIM01_DESC\"," +
                        "\"Is Display Column\" : true," +
                        "\"Target Level\" : 2," +
                        "}," +
                        "{\"Name\" : \"DIM01_Attr\"" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String errorKeyString = "Schemas.noname.Tables.noname";
        Key key = keyParser.parseKey(errorKeyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());

        errorKeyString = "Schemas.CT15.Tables.noname";
        key = keyParser.parseKey(errorKeyString);
        target = fixture.getTableAnnotations(key);
        assertFalse(target.isPresent());

        String keyString = "Schemas.CT15.Tables.W_DIMENSION01_D";
        key = keyParser.parseKey(keyString);
        target = fixture.getTableAnnotations(key);
        assertTrue(target.isPresent());
        TestTableAnnotations ta = (TestTableAnnotations) target.get();

        assertEquals("W_DIMENSION01_D", ta.getName());
        assertEquals("CT15", ta.getSchemaName());

        assertTrue(ta.getBusinessName().isPresent());
        assertEquals("Dimension 01 Override Ignored", ta.getBusinessName().get());

        assertTrue(ta.getType().isPresent());
        assertEquals("FACT", ta.getType().get());

        assertTrue(ta.isRaggedDimension().isPresent());
        assertTrue(ta.isRaggedDimension().get());

        // nothing hidden
        assertFalse(ta.isHidden().isPresent());

        // 2 columns have annotations; one columns has no annotations -> ignore
        assertEquals(2, ta.getColumnAnnotations().size());
        TestColumnAnnotations ca = (TestColumnAnnotations) ta.getColumnAnnotations()
                .get("DIM01_CODE");
        assertNotNull(ca);
        assertEquals("Drill Key 1 Override", ca.getBusinessName().get());
        assertTrue(ca.isHidden().get());

        ca = (TestColumnAnnotations) ta.getColumnAnnotations().get("DIM01_DESC");
        assertNotNull(ca);
        assertTrue(ca.isDisplayColumn().get());
        assertEquals(2, ca.targetLevel().get().intValue());

        ca = (TestColumnAnnotations) ta.getColumnAnnotations().get("DIM01_Attr");
        assertNull(ca);

        // validate that upper case annotation will work
        keyString = "schemas.CT15.tables.W_DIMENSION01_D";
        key = keyParser.parseKey(keyString);
        target = fixture.getTableAnnotations(key);
        assertTrue(target.isPresent());
    }

    @Test
    public void testGetColumnAnnotationNullArray() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"tables\" : [" +
                        "{\"Name\" : \"W_DIMENSION01_D\"," +
                        "\"Columns\" : null" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String keyString = "Schemas.CT15.Tables.W_DIMENSION01_D";
        Key key = keyParser.parseKey(keyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        TableAnnotations ta = target.get();

        assertEquals(0, ta.getColumnAnnotations().size());
    }

    @Test
    public void testGetColumnAnnotationArrayWithNull() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"tables\" : [" +
                        "{\"Name\" : \"W_DIMENSION01_D\"," +
                        "\"Columns\" : [null]" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String keyString = "Schemas.CT15.Tables.W_DIMENSION01_D";
        Key key = keyParser.parseKey(keyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        TableAnnotations ta = target.get();

        assertEquals(0, ta.getColumnAnnotations().size());
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetColumnAnnotationColumnNameTypeError() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"tables\" : [" +
                        "{\"Name\" : \"W_DIMENSION01_D\"," +
                        "\"Columns\" : [" +
                        "{\"Name\" : true," +
                        "\"Business Name\" : \"Drill Key 1 Override\"," +
                        "\"Hide\" : true" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String keyString = "Schemas.CT15.Tables.W_DIMENSION01_D";
        Key key = keyParser.parseKey(keyString);
        Optional<TableAnnotations> target = fixture.getTableAnnotations(key);
        TableAnnotations ta = target.get();

        assertEquals(0, ta.getColumnAnnotations().size());
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetColumnAnnotationDuplicateColumnNames() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"tables\" : [" +
                        "{\"Name\" : \"W_DIMENSION01_D\"," +
                        "\"Columns\" : [" +
                        "{\"Name\" : \"DIM01_CODE\"," +
                        "\"Business Name\" : \"Drill Key 1 Override\"," +
                        "\"Hide\" : true" +
                        "}," +
                        "{\"Name\" : \"DIM01_CODE\"," +
                        "\"Is Display Column\" : true" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String keyString = "Schemas.CT15.Tables.W_DIMENSION01_D";
        Key key = keyParser.parseKey(keyString);
        fixture.getTableAnnotations(key);
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetColumnAnnotationEmptyColumnName() {
        final String js =
                "{\"Schemas\" : [" +
                        "{\"Name\" : \"CT15\"," +
                        "\"tables\" : [" +
                        "{\"Name\" : \"W_DIMENSION01_D\"," +
                        "\"Columns\" : [" +
                        "{\"Name\" : \"\"," +
                        "\"Business Name\" : \"XXXX\"," +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}";

        // TODO trim keys after refactoring (no longer needed)
        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.setJsModel(js);

        String keyString = "Schemas.CT15.Tables.W_DIMENSION01_D";
        Key key = keyParser.parseKey(keyString);
        fixture.getTableAnnotations(key);
    }

    //
    // Tests for processing plain JSON expressions
    //

    @Test
    public void testGetTableAnnotations() {
        final String js = "{\"Type\" : \"FACT\"," +
                "\"Business Name\" : \"Dimension 01 Override Ignored\"," +
                "\"Is Ragged Dimension\" : true," +
                "\"Columns\" : [" +
                "{\"Name\" : \"DIM01_CODE\"," +
                "\"Business Name\" : \"Drill Key 1 Override\"" +
                "}] }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        Optional<TableAnnotations> target =
                fixture.getTableAnnotations("schema", "a table", js);
        assertTrue(target.isPresent());
        TestTableAnnotations ta = (TestTableAnnotations) target.get();

        assertEquals("a table", ta.getName());
        assertEquals("schema", ta.getSchemaName());

        assertTrue(ta.getBusinessName().isPresent());
        assertEquals("Dimension 01 Override Ignored", ta.getBusinessName().get());

        assertTrue(ta.getType().isPresent());
        assertEquals("FACT", ta.getType().get());

        assertTrue(ta.isRaggedDimension().isPresent());
        assertTrue(ta.isRaggedDimension().get());
    }

    @Test(expected = MalformedAnnotationException.class)
    public void testGetTableAnnotationsSyntaxError() {
        final String js = "{ wrong }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);
        fixture.getTableAnnotations("schema", "a table", js);
    }

    @Test
    public void testGetTableAnnotationsEmpty() {
        final String js = "{ }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        Optional<TableAnnotations> target =
                fixture.getTableAnnotations("schema", "a table", js);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetTableAnnotationsNullElement() {
        final String js = "{\"Type\" : null}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        Optional<TableAnnotations> target =
                fixture.getTableAnnotations("schema", "a table", js);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetTableAnnotationsWrongType() {
        final String js = "{\"Type\" : 1}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        Optional<TableAnnotations> target =
                fixture.getTableAnnotations("schema", "a table", js);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetColumnAnnotations() {
        final String jsT = "{\"Type\" : \"FACT\"}";

        final String js = "{\"Business Name\" : \"Dimension 01 Override Ignored\"," +
                "\"Hide\" : true }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations tableAnnotation = fixture.getTableAnnotations(
                "Schema", "Table", jsT).get();

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(tableAnnotation,
                        "a column", js);
        assertTrue(target.isPresent());
        ColumnAnnotations ta = target.get();

        assertEquals("a column", ta.getName());
        assertEquals(tableAnnotation, ta.getParent());

        assertTrue(ta.getBusinessName().isPresent());
        assertEquals("Dimension 01 Override Ignored", ta.getBusinessName().get());

    }

    @Test
    public void testGetColumnAnnotationsEmpty() {
        final String js = "{ }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations ta = mock(TableAnnotations.class);
        when(ta.getSchemaName()).thenReturn("schema");
        when(ta.getName()).thenReturn("tableName");

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(ta, "a column", js);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetColumnAnnotationsNullElement() {
        final String js = "{\"Type\" : null}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations ta = mock(TableAnnotations.class);
        when(ta.getSchemaName()).thenReturn("schema");
        when(ta.getName()).thenReturn("tableName");

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(ta, "a column", js);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetColumnAnnotationsWrongType() {
        final String jsT = "{\"Type\" : \"FACT\"}";
        final String js = "{\"Business Name\" : 1}";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations tableAnnotation = fixture.getTableAnnotations(
                "Schema", "Table", jsT).get();

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(tableAnnotation,
                        "a column", js);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetColumnAnnotationWithArray() {
        final String jsT = "{\"Type\" : \"FACT\"}";

        final String js = "{\"Target Levels\" : [\"colum1:3\", \"column2:2\"] }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations tableAnnotation = fixture.getTableAnnotations(
                "Schema", "Table", jsT).get();

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(tableAnnotation,
                        "a column", js);
        assertTrue(target.isPresent());
        TestColumnAnnotations ta = (TestColumnAnnotations) target.get();
        assertTrue(ta.targetLevels().isPresent());
        assertEquals(2, ta.targetLevels().get().size());

        assertEquals("a column", ta.getName());
    }

    @Test
    public void testGetColumnAnnotationWithNull() {
        final String jsT = "{\"Type\" : \"FACT\"}";

        final String js = "{\"Target Levels\" : [\"colum1:3\", null] }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations tableAnnotation = fixture.getTableAnnotations(
                "Schema", "Table", jsT).get();

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(tableAnnotation,
                        "a column", js);
        assertTrue(target.isPresent());
        TestColumnAnnotations ta = (TestColumnAnnotations) target.get();
        assertTrue(ta.targetLevels().isPresent());
        assertEquals(2, ta.targetLevels().get().size());
    }

    @Test
    public void testGetColumnAnnotationWithNestedObject() {
        final String jsT = "{\"Type\" : \"FACT\"}";

        final String js = "{\"Target Levels\" : [\"colum1:3\", {\"a\" : \"b\" }] }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations tableAnnotation = fixture.getTableAnnotations(
                "Schema", "Table", jsT).get();

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(tableAnnotation,
                        "a column", js);
        assertTrue(target.isPresent());
        TestColumnAnnotations ta = (TestColumnAnnotations) target.get();
        assertTrue(ta.targetLevels().isPresent());
        assertEquals(2, ta.targetLevels().get().size());
    }

    @Test
    public void testGetColumnAnnotationTargetLevelsNullArray() {
        final String jsT = "{\"Type\" : \"FACT\"}";

        final String js = "{\"Target Levels\" : null }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations tableAnnotation = fixture.getTableAnnotations(
                "Schema", "Table", jsT).get();

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(tableAnnotation,
                        "a column", js);
        assertFalse(target.isPresent());
    }

    @Test
    public void testGetColumnAnnotationTargetLevelsEmptyArray() {
        final String jsT = "{\"Type\" : \"FACT\"}";

        final String js = "{\"Target Levels\" : [] }";

        JsTestAnnotation fixture = BaseAnnotationServiceHelper
                .createJsTestAnnotation(errorWarningMessages);

        TableAnnotations tableAnnotation = fixture.getTableAnnotations(
                "Schema", "Table", jsT).get();

        Optional<ColumnAnnotations> target =
                fixture.getColumnAnnotations(tableAnnotation,
                        "a column", js);
        assertTrue(target.isPresent());
        assertTrue(((TestColumnAnnotations) target.get()).targetLevels().isPresent());
        assertTrue(((TestColumnAnnotations) target.get()).targetLevels().get().isEmpty());
    }

}
