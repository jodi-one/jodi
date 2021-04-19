package one.jodi.base.service.annotation;

import one.jodi.base.config.BaseConfigurations;
import one.jodi.base.config.BaseConfigurationsHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataModelDescriptor;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class AnnotationServiceDefaultImplBulkTest {

    private static final String MODEL_NAME = "MODEL";
    private static final String TABLE_NAME = "TABLE";
    private static final String COLUMN_NAME = "THE_COLUMN";

    private BaseConfigurations b;
    private TableBusinessRules businessRules;
    private ErrorWarningMessageJodi errorWarningMessages;
    private JsAnnotation jsAnnotation;
    private KeyParser keyParser;
    private AnnotationFactory annotationFactory;
    private AnnotationService fixture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.b = BaseConfigurationsHelper.getTestBaseConfigurations();
        errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
        errorWarningMessages.clear();
        businessRules = new TableBusinessRulesImpl(b);
        annotationFactory = new TestAnnotationFactory(errorWarningMessages);
        jsAnnotation = new JsAnnotationImpl("pathToTheFolder", annotationFactory, errorWarningMessages);
        keyParser = new KeyParserImpl(errorWarningMessages);
        fixture = new AnnotationServiceDefaultImpl(businessRules, jsAnnotation, keyParser, annotationFactory, b,
                                                   errorWarningMessages);
    }

    private DataStoreDescriptor createMockDataStoreDescriptor(final String descr) {
        DataModelDescriptor dm = mock(DataModelDescriptor.class);
        when(dm.getSchemaName()).thenReturn(MODEL_NAME);
        DataStoreDescriptor ds = mock(DataStoreDescriptor.class);
        when(ds.getDataStoreName()).thenReturn(TABLE_NAME);
        when(ds.getDataModelDescriptor()).thenReturn(dm);
        when(ds.getDescription()).thenReturn(descr);
        return ds;
    }

    private ColumnMetaData createMockColumnMetaData(final String name, final String descr) {
        DataStoreDescriptor dataStore = createMockDataStoreDescriptor(null);

        ColumnMetaData column = mock(ColumnMetaData.class);
        when(column.getName()).thenReturn(name);
        when(column.getDescription()).thenReturn(descr);

        Collection<ColumnMetaData> columns = Collections.singletonList(column);
        when(dataStore.getColumnMetaData()).thenReturn(columns);
        return column;
    }

    @Test
    public void testNoAnnotations() {
        String descr = "";
        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);

        Optional<TableAnnotations> result = fixture.getAnnotations(ds, Collections.emptyList());
        assertFalse(result.isPresent());
    }

    @Test
    public void testClassicDescription() {
        String descr = "---description {\"a\": 3, \"b\": \"string\", " + "\"c\": [1, 2, null], \"d\": null} ";
        String cDescr = "Default Business Name---column description";

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        ColumnMetaData col = createMockColumnMetaData(COLUMN_NAME, cDescr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        columns.add(col);
        when(ds.getColumnMetaData()).thenReturn(columns);

        Optional<TableAnnotations> result = fixture.getAnnotations(ds, Collections.emptyList());

        // table validations
        assertTrue(result.isPresent());
        TableAnnotations ta = result.get();
        assertFalse(ta.getBusinessName()
                      .isPresent());
        assertFalse(ta.getAbbreviatedBusinessName()
                      .isPresent());
        assertTrue(ta.getDescription()
                     .isPresent());
        assertEquals("description", ta.getDescription()
                                      .get());

        // column validations
        assertFalse(ta.getColumnAnnotations()
                      .isEmpty());
        assertEquals(1, ta.getColumnAnnotations()
                          .size());
        assertEquals("Default Business Name", ta.getColumnAnnotations()
                                                .get(COLUMN_NAME)
                                                .getBusinessName()
                                                .get());
        assertEquals("column description", ta.getColumnAnnotations()
                                             .get(COLUMN_NAME)
                                             .getDescription()
                                             .get());
    }

    @Test(expected = UnRecoverableException.class)
    public void testWrongTableJs() {
        String descr = "---{\"Business Name\": } ";
        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        fixture.getAnnotations(ds, Collections.emptyList());
    }

    @Test
    public void testOverrideWithTableJs() {
        String descr =
                "---description {\"Business Name\": \"This Name\", " + "\"Description\": \"This description\", " +
                        "\"c\": [\"a\", true, null], \"d\": null} ";

        String cDescr =
                "---description {\"Business Name\": \"This Name\", " + "\"Description\": \"This description\"} ";

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        ColumnMetaData col = createMockColumnMetaData(COLUMN_NAME, cDescr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        columns.add(col);
        when(ds.getColumnMetaData()).thenReturn(columns);

        Optional<TableAnnotations> result = fixture.getAnnotations(ds, Collections.emptyList());

        // table validation
        assertTrue(result.isPresent());
        TableAnnotations ta = result.get();
        assertTrue(ta.getBusinessName()
                     .isPresent());
        assertEquals("This Name", ta.getBusinessName()
                                    .get());
        assertFalse(ta.getAbbreviatedBusinessName()
                      .isPresent());
        assertTrue(ta.getDescription()
                     .isPresent());
        assertEquals("This description", ta.getDescription()
                                           .get());

        // column validations
        assertFalse(ta.getColumnAnnotations()
                      .isEmpty());
        assertEquals(1, ta.getColumnAnnotations()
                          .size());
        assertEquals("This Name", ta.getColumnAnnotations()
                                    .get(COLUMN_NAME)
                                    .getBusinessName()
                                    .get());
        assertEquals("This description", ta.getColumnAnnotations()
                                           .get(COLUMN_NAME)
                                           .getDescription()
                                           .get());
    }

    @Test
    public void testOnlyTableJs() {
        String descr = "---{\"Business Name\": \"This Name\", " + "\"Description\": \"This description\"} ";
        String cDescr = "---{\"Business Name\": \"This Name\", " + "\"Description\": \"This description\"} ";

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        ColumnMetaData col = createMockColumnMetaData(COLUMN_NAME, cDescr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        columns.add(col);
        when(ds.getColumnMetaData()).thenReturn(columns);
        Optional<TableAnnotations> result = fixture.getAnnotations(ds, Collections.emptyList());

        // table validation
        assertTrue(result.isPresent());
        TableAnnotations ta = result.get();
        assertTrue(ta.getBusinessName()
                     .isPresent());
        assertEquals("This Name", ta.getBusinessName()
                                    .get());
        assertFalse(ta.getAbbreviatedBusinessName()
                      .isPresent());
        assertTrue(ta.getDescription()
                     .isPresent());
        assertEquals("This description", ta.getDescription()
                                           .get());

        // column validations
        assertFalse(ta.getColumnAnnotations()
                      .isEmpty());
        assertEquals(1, ta.getColumnAnnotations()
                          .size());
        assertEquals("This Name", ta.getColumnAnnotations()
                                    .get(COLUMN_NAME)
                                    .getBusinessName()
                                    .get());
        assertEquals("This description", ta.getColumnAnnotations()
                                           .get(COLUMN_NAME)
                                           .getDescription()
                                           .get());
    }

    @Test(expected = UnRecoverableException.class)
    public void testOverrideWithWrongJsFile() {
        String descr = "";
        final String js = "{\"Schemas\" : |" + // incorrect '|' vs. correct '['
                "{\"Name\" : \"MODEL\"," + "\"Tables\" : [" + "{\"Name\" : \"TABLE\"," + "\"Business Name\" : \"BN\"," +
                "}" + "]" + "}" + "]" + "}";

        JsTestAnnotation jsAnnotation = BaseAnnotationServiceHelper.createJsTestAnnotation(errorWarningMessages);
        jsAnnotation.setJsModel(js);
        fixture = new AnnotationServiceDefaultImpl(businessRules, jsAnnotation, keyParser, annotationFactory, b,
                                                   errorWarningMessages);

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        fixture.getAnnotations(ds, Collections.emptyList());
    }

    @Test
    public void testOnlyJsFile() {
        final String descr = "";
        final String cDescr = "";
        final String js = "{\"Schemas\" : [" + "{\"Name\" : \"MODEL\"," + "\"Tables\" : [" + "{\"Name\" : \"TABLE\"," +
                "\"Business Name\" : \"Dimension 01 Override\"," + "\"Columns\" : [" + "{\"Name\" : \"THE_COLUMN\"," +
                "\"Business Name\" : \"Drill Key 1 Override\"" + "}" + "]" + "}" + "]" + "}" + "]" + "}";

        JsTestAnnotation jsAnnotation = BaseAnnotationServiceHelper.createJsTestAnnotation(errorWarningMessages);
        jsAnnotation.setJsModel(js);
        fixture = new AnnotationServiceDefaultImpl(businessRules, jsAnnotation, keyParser, annotationFactory, b,
                                                   errorWarningMessages);

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        ColumnMetaData col = createMockColumnMetaData(COLUMN_NAME, cDescr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        columns.add(col);
        when(ds.getColumnMetaData()).thenReturn(columns);

        // table validation
        Optional<TableAnnotations> o = fixture.getAnnotations(ds, Collections.emptyList());
        assertTrue(o.isPresent());
        TableAnnotations ta = o.get();
        assertEquals("Dimension 01 Override", ta.getBusinessName()
                                                .get());
        assertFalse(ta.getDescription()
                      .isPresent());
        assertFalse(ta.getAbbreviatedBusinessName()
                      .isPresent());

        // column validations
        assertFalse(ta.getColumnAnnotations()
                      .isEmpty());
        assertEquals(1, ta.getColumnAnnotations()
                          .size());
        assertEquals("Drill Key 1 Override", ta.getColumnAnnotations()
                                               .get(COLUMN_NAME)
                                               .getBusinessName()
                                               .get());
        assertFalse(ta.getColumnAnnotations()
                      .get(COLUMN_NAME)
                      .getDescription()
                      .isPresent());
    }

    @Test(expected = UnRecoverableException.class)
    public void testWrongEmbeddedColumnJS() {
        String cDescr = "---{\"Business Name\": }";

        DataStoreDescriptor ds = createMockDataStoreDescriptor("");
        ColumnMetaData col = createMockColumnMetaData(COLUMN_NAME, cDescr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        columns.add(col);
        when(ds.getColumnMetaData()).thenReturn(columns);

        fixture.getAnnotations(ds, Collections.emptyList());
    }

    @Test
    public void testOverrideWithJsFile() {
        String descr =
                "B Name ((Abbrev)) ---" + "description {\"Business Name\": \"This Name\", " + "\"b\": \"string\" } ";
        final String js = "{\"Schemas\" : [" + "{\"Name\" : \"MODEL\"," + "\"Tables\" : [" + "{\"Name\" : \"TABLE\"," +
                "\"Business Name\" : \"Dimension 01 Override\"," + "\"Columns\" : [" + "{\"Name\" : \"THE_COLUMN\"," +
                "\"Business Name\" : \"Drill Key 1 Override\"" + "}" + "]" + "}" + "]" + "}" + "]" + "}";

        JsTestAnnotation jsAnnotation = BaseAnnotationServiceHelper.createJsTestAnnotation(errorWarningMessages);
        jsAnnotation.setJsModel(js);
        fixture = new AnnotationServiceDefaultImpl(businessRules, jsAnnotation, keyParser, annotationFactory, b,
                                                   errorWarningMessages);

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        ColumnMetaData col = createMockColumnMetaData(COLUMN_NAME, "");
        Collection<ColumnMetaData> columns = new ArrayList<>();
        columns.add(col);
        when(ds.getColumnMetaData()).thenReturn(columns);

        Optional<TableAnnotations> o = fixture.getAnnotations(ds, Collections.emptyList());
        assertTrue(o.isPresent());
        TableAnnotations ta = o.get();
        assertEquals("Dimension 01 Override", ta.getBusinessName()
                                                .get());
        assertEquals("description", ta.getDescription()
                                      .get());
        assertEquals("Abbrev", ta.getAbbreviatedBusinessName()
                                 .get());

        assertNotNull(ta.getColumnAnnotations());
        assertEquals(1, ta.getColumnAnnotations()
                          .size());
    }

    @Test
    public void testMergeColumnAnnotationsEmbeddedAndJsFile() {
        final String descr = "";
        String cDescr =
                "---{\"Business Name\": \"This embedded Name\", " + "\"Description\": \"This embedded description\"} ";
        final String js = "{\"Schemas\" : [" + "{\"Name\" : \"MODEL\"," + "\"Tables\" : [" + "{\"Name\" : \"TABLE\"," +
                "\"Business Name\" : \"Dimension 01 Override\"," + "\"Columns\" : [" + "{\"Name\" : \"THE_COLUMN\"," +
                "\"Business Name\" : \"Drill Key 1 Override\"" + "}" + "]" + "}" + "]" + "}" + "]" + "}";

        JsTestAnnotation jsAnnotation = BaseAnnotationServiceHelper.createJsTestAnnotation(errorWarningMessages);
        jsAnnotation.setJsModel(js);
        fixture = new AnnotationServiceDefaultImpl(businessRules, jsAnnotation, keyParser, annotationFactory, b,
                                                   errorWarningMessages);

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        ColumnMetaData col = createMockColumnMetaData(COLUMN_NAME, cDescr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        columns.add(col);
        when(ds.getColumnMetaData()).thenReturn(columns);

        // table validation
        Optional<TableAnnotations> o = fixture.getAnnotations(ds, Collections.emptyList());
        assertTrue(o.isPresent());
        TableAnnotations ta = o.get();
        assertEquals("Dimension 01 Override", ta.getBusinessName()
                                                .get());
        assertFalse(ta.getDescription()
                      .isPresent());
        assertFalse(ta.getAbbreviatedBusinessName()
                      .isPresent());

        // column validations
        assertFalse(ta.getColumnAnnotations()
                      .isEmpty());
        assertEquals(1, ta.getColumnAnnotations()
                          .size());
        assertEquals("Drill Key 1 Override", ta.getColumnAnnotations()
                                               .get(COLUMN_NAME)
                                               .getBusinessName()
                                               .get());
        assertTrue(ta.getColumnAnnotations()
                     .get(COLUMN_NAME)
                     .getDescription()
                     .isPresent());
        assertEquals("This embedded description", ta.getColumnAnnotations()
                                                    .get(COLUMN_NAME)
                                                    .getDescription()
                                                    .get());
    }

    private List<Pattern> getPatterns(final List<String> regEx) {
        return regEx.stream()
                    .map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE))
                    .collect(Collectors.toList());
    }

    @Test
    public void testDefaultHide() {
        final String descr = "";
        String cDescr = "";
        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        when(ds.getColumnMetaData()).thenReturn(columns);

        ColumnMetaData col1 = createMockColumnMetaData(COLUMN_NAME, cDescr);
        columns.add(col1);

        ColumnMetaData col2 = createMockColumnMetaData("HIER_LVL2_CODE", cDescr);
        columns.add(col2);

        ColumnMetaData col3 = createMockColumnMetaData("HIER_LVL2_NAME", cDescr);
        columns.add(col3);

        List<String> regEx = Arrays.asList(new String[]{"HIER_LVL\\d_\\w+"});

        // table validation
        Optional<TableAnnotations> o = fixture.getAnnotations(ds, getPatterns(regEx));
        assertTrue(o.isPresent());
        assertEquals(2, o.get()
                         .getColumnAnnotations()
                         .size());
        assertNotNull(o.get()
                       .getColumnAnnotations()
                       .get("HIER_LVL2_CODE"));
        assertTrue(o.get()
                    .getColumnAnnotations()
                    .get("HIER_LVL2_CODE")
                    .isHidden()
                    .isPresent());
        assertTrue(o.get()
                    .getColumnAnnotations()
                    .get("HIER_LVL2_CODE")
                    .isHidden()
                    .get());

        assertNotNull(o.get()
                       .getColumnAnnotations()
                       .get("HIER_LVL2_NAME"));
        assertTrue(o.get()
                    .getColumnAnnotations()
                    .get("HIER_LVL2_NAME")
                    .isHidden()
                    .isPresent());
        assertTrue(o.get()
                    .getColumnAnnotations()
                    .get("HIER_LVL2_NAME")
                    .isHidden()
                    .get());
    }

    @Test
    public void testDefaultHideOverride() {
        final String descr = "";
        String cDescr = "";
        final String js = "{\"Schemas\" : [" + "{\"Name\" : \"MODEL\"," + "\"Tables\" : [" + "{\"Name\" : \"TABLE\"," +
                "\"Business Name\" : \"Dimension 01 Override\"," + "\"Columns\" : [" +
                "{\"Name\" : \"HIER_LVL2_NAME\"," + "\"Business Name\" : \"Description Override\"," +
                "\"Hide\" : false" + "}" + "]" + "}" + "]" + "}" + "]" + "}";

        JsTestAnnotation jsAnnotation = BaseAnnotationServiceHelper.createJsTestAnnotation(errorWarningMessages);
        jsAnnotation.setJsModel(js);
        fixture = new AnnotationServiceDefaultImpl(businessRules, jsAnnotation, keyParser, annotationFactory, b,
                                                   errorWarningMessages);

        DataStoreDescriptor ds = createMockDataStoreDescriptor(descr);
        Collection<ColumnMetaData> columns = new ArrayList<>();
        when(ds.getColumnMetaData()).thenReturn(columns);

        ColumnMetaData col1 = createMockColumnMetaData(COLUMN_NAME, cDescr);
        columns.add(col1);

        ColumnMetaData col2 = createMockColumnMetaData("HIER_LVL2_CODE", cDescr);
        columns.add(col2);

        ColumnMetaData col3 = createMockColumnMetaData("HIER_LVL2_NAME", cDescr);
        columns.add(col3);

        List<String> regEx = Arrays.asList(new String[]{"HIER_LVL\\d_\\w+"});

        // table validation
        Optional<TableAnnotations> o = fixture.getAnnotations(ds, getPatterns(regEx));
        assertTrue(o.isPresent());
        assertEquals(2, o.get()
                         .getColumnAnnotations()
                         .size());
        assertNotNull(o.get()
                       .getColumnAnnotations()
                       .get("HIER_LVL2_CODE"));
        assertTrue(o.get()
                    .getColumnAnnotations()
                    .get("HIER_LVL2_CODE")
                    .isHidden()
                    .isPresent());
        assertTrue(o.get()
                    .getColumnAnnotations()
                    .get("HIER_LVL2_CODE")
                    .isHidden()
                    .get());

        assertNotNull(o.get()
                       .getColumnAnnotations()
                       .get("HIER_LVL2_NAME"));
        assertTrue(o.get()
                    .getColumnAnnotations()
                    .get("HIER_LVL2_NAME")
                    .isHidden()
                    .isPresent());
        assertFalse(o.get()
                     .getColumnAnnotations()
                     .get("HIER_LVL2_NAME")
                     .isHidden()
                     .get());
    }


}
