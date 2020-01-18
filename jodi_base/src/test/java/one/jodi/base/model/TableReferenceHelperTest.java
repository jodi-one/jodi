package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableReferenceHelperTest {
    private ErrorWarningMessageJodi errorWarningMessages;
    private TableBase table1;
    private TableBase table2;
    private TableReferenceHelper fixture;


    private ApplicationBase createMockup() {
        ApplicationBase application = mock(ApplicationBase.class);

        SchemaBase schema1 = mock(SchemaBase.class);
        TableBase table1 = mock(TableBase.class);

        when(schema1.getName()).thenReturn("schema1");
        when(schema1.getParent()).thenReturn(application);
        when(schema1.getTable("schema1.table1")).thenReturn(table1);
        Map<String, TableBase> schema1TableMap = new HashMap<>();
        schema1TableMap.put("schema1.table1", table1);
        when(schema1.getTablesMap()).thenAnswer(
                new Answer<Map<String, TableBase>>() {
                    @Override
                    public Map<String, TableBase> answer(InvocationOnMock invocation) throws Throwable {
                        return schema1TableMap;
                    }
                });
        when(table1.getName()).thenReturn("table1");
        when(table1.getParent()).thenReturn(schema1);

        SchemaBase schema2 = mock(SchemaBase.class);
        TableBase table2 = mock(TableBase.class);

        when(schema2.getName()).thenReturn("schema2");
        when(schema2.getParent()).thenReturn(application);
        when(schema2.getTable("schema2.table2")).thenReturn(table2);
        Map<String, TableBase> schema2TableMap = new HashMap<>();
        schema2TableMap.put("schema2.table2", table2);
        when(schema2.getTablesMap()).thenAnswer(
                new Answer<Map<String, TableBase>>() {
                    @Override
                    public Map<String, TableBase> answer(InvocationOnMock invocation) throws Throwable {
                        return schema2TableMap;
                    }
                });

        Map<String, SchemaBase> schemaMap = new HashMap<>();
        schemaMap.put("schema1", schema1);
        schemaMap.put("schema2", schema2);

        when(application.getSchemaMap()).thenAnswer(
                new Answer<Map<String, SchemaBase>>() {
                    @Override
                    public Map<String, SchemaBase> answer(InvocationOnMock invocation) throws Throwable {
                        return schemaMap;
                    }
                });

        return application;
    }

    @Before
    public void setUp() throws Exception {
        errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
        errorWarningMessages.clear();
        fixture = new TableReferenceHelper(errorWarningMessages);

        ApplicationBase application = createMockup();
        SchemaBase schema1 = application.getSchemaMap().get("schema1");
        assertNotNull(schema1);
        this.table1 = schema1.getTablesMap().get("schema1.table1");
        assertNotNull(table1);
        SchemaBase schema2 = application.getSchemaMap().get("schema2");
        assertNotNull(schema2);
        this.table2 = schema2.getTablesMap().get("schema2.table2");
        assertNotNull(table2);
    }

    @Test
    public void testParseAnnotationExistingWithoutSchema() {
        @SuppressWarnings("unchecked")
        Optional<TableBase> table = (Optional<TableBase>)
                fixture.parseAnnotation(table1, "table1", "A Key");
        assertNotNull(table);
        assertEquals(table1, table.get());
    }

    @Test
    public void testParseAnnotationNotExistingWithoutSchema() {
        @SuppressWarnings("unchecked")
        Optional<TableBase> table = (Optional<TableBase>)
                fixture.parseAnnotation(table1, "table3", "A Key");
        assertNotNull(table);
        assertEquals(Optional.empty(), table);
        assertEquals(1, this.errorWarningMessages.getErrorMessages().size());
    }

    @Test
    public void testParseAnnotationExistingSameSchema() {
        @SuppressWarnings("unchecked")
        Optional<TableBase> table = (Optional<TableBase>)
                fixture.parseAnnotation(table1, "schema1.table1", "A Key");
        assertNotNull(table);
        assertEquals(table1, table.get());
    }

    @Test
    public void testParseAnnotationNotExistingSameSchema() {
        @SuppressWarnings("unchecked")
        Optional<TableBase> table = (Optional<TableBase>)
                fixture.parseAnnotation(table1, "schema1.table3", "A Key");
        assertNotNull(table);
        assertEquals(Optional.empty(), table);
        assertEquals(1, this.errorWarningMessages.getErrorMessages().size());
    }

    @Test
    public void testParseAnnotationExistingDifferentSchema() {
        @SuppressWarnings("unchecked")
        Optional<TableBase> table = (Optional<TableBase>)
                fixture.parseAnnotation(table1, "schema2.table2", "A Key");
        assertNotNull(table);
        assertEquals(table2, table.get());
    }

    @Test
    public void testParseAnnotationNotExistingDifferentSchema() {
        @SuppressWarnings("unchecked")
        Optional<TableBase> table = (Optional<TableBase>)
                fixture.parseAnnotation(table1, "schema2.table4", "A Key");
        assertNotNull(table);
        assertEquals(Optional.empty(), table);
        assertEquals(1, this.errorWarningMessages.getErrorMessages().size());
    }


}
