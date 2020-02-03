package one.jodi.core.automapping.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.contexts.TargetColumnExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TargetColumnExtension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The class <code>ColumnMappingDefaultStrategyTest</code> validates that based on a valid ColumnMappingExecutionContext
 * object, a correct derivation of source expression is returned, if possible.
 */
public class ColumnMappingDefaultStrategyTest {

    @Mock
    JodiProperties properties;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private void setUndefinedProperties() {
        List<String> list = Collections.<String>emptyList();
        when(properties.getPropertyList(JodiConstants.COLUMN_MATCH_REGEX)).thenReturn(list);
        when(properties.getPropertyList(JodiConstants.COLUMN_MATCH_SOURCE_IGNORE)).thenReturn(list);
    }

    private void setIgnores(String... ignores) {
        when(properties.getPropertyList(JodiConstants.COLUMN_MATCH_SOURCE_IGNORE)).thenReturn(Arrays.asList(ignores));
    }

    private void setRegexes(String... regexes) {
        when(properties.getPropertyList(JodiConstants.COLUMN_MATCH_REGEX)).thenReturn(Arrays.asList(regexes));

    }

    private DataStore createMockDataStore(String name, String... columnNames) {
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getDataStoreName()).thenReturn(name);
        HashMap<String, DataStoreColumn> dataStoreColumns = new HashMap<String, DataStoreColumn>();
        for (String columnName : columnNames) {
            DataStoreColumn dsc = createMockDataStoreColumn(columnName);
            dataStoreColumns.put(columnName, dsc);
        }
        when(dataStore.getColumns()).thenReturn(dataStoreColumns);

        return dataStore;
    }

    private DataStoreWithAlias createMockDataStoreWithAlias(String name, String alias, String... columnNames) {
        DataStoreWithAlias dataStoreWithAlias = mock(DataStoreWithAlias.class);

        DataStore dataStore = createMockDataStore(name, columnNames);
        when(dataStoreWithAlias.getDataStore()).thenReturn(dataStore);
        when(dataStoreWithAlias.getAlias()).thenReturn(alias);
        SourceExtension sourceExtension = mock(SourceExtension.class);
        when(dataStoreWithAlias.getSourceExtension()).thenReturn(sourceExtension);
        return dataStoreWithAlias;
    }

    private DataStoreColumn createMockDataStoreColumn(String name) {
        DataStoreColumn dsc = mock(DataStoreColumn.class);
        when(dsc.getName()).thenReturn(name);
        return dsc;
    }


    private TargetColumnExecutionContext createTCExecutionContext(String column, boolean explicitlyMapped) {
        TargetColumnExecutionContext context = mock(TargetColumnExecutionContext.class);
        when(context.getTargetColumnName()).thenReturn(column);
        TargetColumnExtension extension = mock(TargetColumnExtension.class);
        when(context.getTargetColumnExtension()).thenReturn(extension);
        when(context.isExplicitlyMapped()).thenReturn(explicitlyMapped);
        return context;
    }


    private ColumnMappingExecutionContext createCMExecutionContext(DataStore targetDataStore, DataStoreWithAlias... sourceDataStores) {
        ColumnMappingExecutionContext context = mock(ColumnMappingExecutionContext.class);

        when(context.getTargetDataStore()).thenReturn(targetDataStore);
        ArrayList<DataStoreWithAlias> list = new ArrayList<DataStoreWithAlias>();

        for (DataStoreWithAlias sourceDataStore : sourceDataStores) {
            list.add(sourceDataStore);
        }
        when(context.getDataStores()).thenReturn(list);

        MappingsExtension mappingsExtension = mock(MappingsExtension.class);
        when(context.getMappingsExtension()).thenReturn(mappingsExtension);

        return context;
    }

    @Test
    public void testEquals_firstSourcePicked_noRegexInProperties() {
        this.setUndefinedProperties();
        DataStoreWithAlias source1 = createMockDataStoreWithAlias("name1", "alias1", "c1", "c2");
        DataStoreWithAlias source2 = createMockDataStoreWithAlias("name2", "alias2", "c1", "c2");
        DataStore target = createMockDataStore("target", "c1", "c2", "c3", "c4");
        String targetColumn = "c1";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source1, source2);
        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);

        String expression = fixture.getMappingExpression(null, cmContext, tcContext);


        // Make sure that the first source is picked up and used for the mapping
        assertEquals("alias1.c1", expression);
    }

    @Test
    public void testEquals_firstSourcePicked() {
        DataStoreWithAlias source1 = createMockDataStoreWithAlias("name1", "alias1", "c1", "c2");
        DataStoreWithAlias source2 = createMockDataStoreWithAlias("name2", "alias2", "c1", "c2");
        DataStore target = createMockDataStore("target", "c1", "c2", "c3", "c4");
        String targetColumn = "c1";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source1, source2);
        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);
        setRegexes(".*");
        String expression = fixture.getMappingExpression(null, cmContext, tcContext);


        // Make sure that the first source is picked up and used for the mapping
        assertEquals("alias1.c1", expression);
    }

    // Make sure that the first regex is not identity
    @Test
    public void testExclusionInSecondRegex() {
        DataStoreWithAlias source = createMockDataStoreWithAlias("name1", "alias1", "c1", "c5");
        DataStore target = createMockDataStore("target", "c5");
        String targetColumn = "c5";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source);
        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);
        setRegexes("1", ".*");
        setIgnores("c5");
        String expression = fixture.getMappingExpression(null, cmContext, tcContext);


        // Make sure that the first source is picked up and used for the mapping
        assertEquals(null, expression);
    }

    @Test
    public void testExclusionInFirstRegex() {
        DataStoreWithAlias source = createMockDataStoreWithAlias("name1", "alias1", "c1", "c5");
        DataStore target = createMockDataStore("target", "c5");
        String targetColumn = "c5";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source);
        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);
        setRegexes(".*");
        String expression = fixture.getMappingExpression(null, cmContext, tcContext);


        // Make sure that the first source is picked up and used for the mapping
        assertEquals("alias1.c5", expression);
    }


    @Test
    public void testEndsWith() {
        DataStoreWithAlias source1 = createMockDataStoreWithAlias("source1", "alias", "XXX_c1", "XXX_c2");
        DataStore target = createMockDataStore("target", "YYY_c1", "YYY_c2", "YYY_c3", "YYY_c4");
        String targetColumn = "YYY_c1";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source1);
        setRegexes("(?<=[A-Z_]{3}_).*");
        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);

        String expression = fixture.getMappingExpression(null, cmContext, tcContext);
        assertEquals("alias.XXX_c1", expression);
    }

    @Test
    public void testSecondRegexMatchs() {
        DataStoreWithAlias source1 = createMockDataStoreWithAlias("source1", "alias", "XXX_c1", "XXX_c2");
        DataStore target = createMockDataStore("target", "YYY_c1", "YYY_c2", "YYY_c3", "YYY_c4");
        String targetColumn = "YYY_c1";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source1);
        setRegexes("(?<=[A-Z_]{3}_).*", "another");

        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);

        String expression = fixture.getMappingExpression(null, cmContext, tcContext);
        assertEquals("alias.XXX_c1", expression);
    }


    @Test
    public void testNoRegexesMatch() {
        DataStoreWithAlias source1 = createMockDataStoreWithAlias("source1", "alias", "XXXc1", "XXXc2");
        DataStore target = createMockDataStore("target", "YYYc1", "YYYc2", "YYYc3", "YYYc4");
        String targetColumn = "YYYc1";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source1);
        setRegexes("rare");
        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);

        String expression = fixture.getMappingExpression(null, cmContext, tcContext);
        assertEquals(null, expression);
    }

    @Test(expected = RuntimeException.class)
    public void testBadRegex() {
        DataStoreWithAlias source1 = createMockDataStoreWithAlias("source1", "alias", "XXXc1", "XXXc2");
        DataStore target = createMockDataStore("target", "YYYc1", "YYYc2", "YYYc3", "YYYc4");
        String targetColumn = "YYYc1";
        TargetColumnExecutionContext tcContext = createTCExecutionContext(targetColumn, false);
        ColumnMappingExecutionContext cmContext = createCMExecutionContext(target, source1);
        setRegexes("((");
        ColumnMappingDefaultStrategy fixture = new ColumnMappingDefaultStrategy(properties, errorWarningMessages);

        String expression = fixture.getMappingExpression(null, cmContext, tcContext);
        assertEquals(null, expression);
    }


}
