package one.jodi.base.model;

import one.jodi.base.config.BaseConfigurations;
import one.jodi.base.config.BaseConfigurationsHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.service.annotation.AnnotationService;
import one.jodi.base.service.annotation.ColumnAnnotations;
import one.jodi.base.service.annotation.TableAnnotations;
import one.jodi.base.service.metadata.*;
import one.jodi.base.service.metadata.ForeignReference.RefColumns;
import one.jodi.base.service.metadata.Key.KeyType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mock Helper that creates mock TableBase objects with columns, PK, FKs. This class
 * contains also a self-test.
 */
@RunWith(JUnit4.class)
public class MockTableBaseHelper {

    public static final String SCHEMA_NAME = "SCHEMA";
    public static final String PRIMARY_POSTFIX = "_PK";
    public static final String FOREIGN_POSTFIX = "_FK";
    public static final String ALTERNATE_POSTFIX = "_AK";
    public static final String VARCHAR_PREFIX = "V_";
    public static final String NUMBER_PREFIX = "N_";
    public static final String BUSINESS_NAME_SEPERATOR = "~";
    private static final Set<SchemaBase> VISITED = new HashSet<SchemaBase>();
    private static final String COLUMN_STRUCTURE =
            "([NV]_){0,1}([a-z-]+[0-9]{0,1}(_[a-z]{1,2}){0,1})(_[0-9]){0,1}(_[a-z]{2}){0,1}";
    private static final Pattern COLUMN_PATTERN = Pattern.compile(COLUMN_STRUCTURE,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private final static String[] SYSTEM_COLUMNS =
            new String[]{"n_SYS_A", "v_SYS_B"};
    private final static ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
    private SchemaBase schema;

    //
    // MOCK FRAMEWORK BELOW
    //

    private static String getDataType(final String columnName) {
        String type = "VARCHAR";
        if (columnName.toUpperCase().startsWith(VARCHAR_PREFIX)) {
            type = "VARCHAR";
        } else if (columnName.toUpperCase().endsWith(FOREIGN_POSTFIX) ||
                columnName.toUpperCase().endsWith(PRIMARY_POSTFIX) ||
                columnName.toUpperCase().endsWith(ALTERNATE_POSTFIX) ||
                columnName.toUpperCase().startsWith(NUMBER_PREFIX)) {
            type = "DOUBLE";
        }
        return type;
    }

    private static Tuple filter(final String compositeName, final String separator) {
        String before = compositeName;
        String after = "";

        if (compositeName.contains(separator)) {
            int index = compositeName.indexOf(separator);
            assert (index < compositeName.length());
            before = compositeName.substring(0, index).trim();
            after = compositeName.substring(index + 1, compositeName.length()).trim();
        }

        return new Tuple(before, after);
    }

    private static Tuple getBusinessNames(final String compositeName) {
        String businessName = compositeName;
        String abbreviatedName = "";

        int index = compositeName.indexOf("((");
        int index2 = compositeName.indexOf("))");
        if (index >= 0 && index2 >= 0) {
            businessName = compositeName.substring(0, index).trim();
            abbreviatedName = compositeName.substring(index + 2, index2).trim();
            assert (!abbreviatedName.isEmpty());
        }

        return new Tuple(businessName, abbreviatedName);
    }

    public static ColumnAnnotations addColumnAnnotation(final TableAnnotations ta,
                                                        final String columnName,
                                                        final String description,
                                                        final Boolean isHidden) {
        assert (columnName != null && !columnName.isEmpty() && description != null);
        ColumnAnnotations cAnnotations = mock(ColumnAnnotations.class);
        when(cAnnotations.getDescription()).thenReturn(Optional.of(description));
        when(cAnnotations.getBusinessName()).thenReturn(Optional.empty());
        when(cAnnotations.getAbbreviatedBusinessName()).thenReturn(Optional.empty());

        Map<String, ColumnAnnotations> cMap = ta.getColumnAnnotations();
        cMap.put(columnName, cAnnotations);

        return cAnnotations;
    }

    //
    // Create TableBase and Column Annotations
    //

    public static TableAnnotations createTableAnnotation(final String description) {
        TableAnnotations tAnnotations = mock(TableAnnotations.class);
        when(tAnnotations.getDescription())
                .thenReturn(Optional.ofNullable(description));
        when(tAnnotations.getBusinessName()).thenReturn(Optional.empty());
        when(tAnnotations.getAbbreviatedBusinessName()).thenReturn(Optional.empty());

        when(tAnnotations.getColumnAnnotations()).thenReturn(new HashMap<>());

        return tAnnotations;
    }

    @SuppressWarnings("unchecked")
    private static Optional<TableAnnotations> upCastTo(
            final Optional<TableAnnotations> rpdTableAnnotations) {
        return (Optional<TableAnnotations>) (Optional<?>) rpdTableAnnotations;
    }

    private static Map<String, ColumnMetaData> createMockColumns(
            final DataStoreDescriptor parent,
            final TableAnnotations tableAnnotations,
            final String[] columnNames,
            final BaseConfigurations biProperties) {
        final Map<String, ColumnMetaData> columns = new HashMap<>();
        int pos = 1;
        for (String name : columnNames) {

            Tuple naming = filter(name, BUSINESS_NAME_SEPERATOR);

            ColumnMetaData mockColumn = mock(ColumnMetaData.class);
            when(mockColumn.getName()).thenReturn(naming.before);
            // for FK column set NULL Constraint on FK column -> M:1 relationship
            if (mockColumn.getName().endsWith("_FK")) {
                when(mockColumn.hasNotNullConstraint()).thenReturn(true);
            }
            when(mockColumn.getPosition()).thenReturn(pos++);
            when(mockColumn.getColumnDataType()).thenReturn(getDataType(naming.before));
            String description = "column description";

            ColumnAnnotations ca = tableAnnotations.getColumnAnnotations()
                    .get(naming.before);
            if (ca == null) {
                ca = addColumnAnnotation(tableAnnotations, naming.before,
                        description, null);
            }

            if (!naming.after.equals("")) {
                description = naming.after + biProperties.getMetadataSeparator() +
                        description;
                Tuple bNames = getBusinessNames(naming.after);
                when(ca.getBusinessName()).thenReturn(Optional.of(bNames.before));
                if (!"".equals(bNames.after)) {
                    when(ca.getAbbreviatedBusinessName())
                            .thenReturn(Optional.of(bNames.after));
                }
            }
            when(mockColumn.getDescription()).thenReturn(description);
            columns.put(naming.before, mockColumn);
        }
        return columns;
    }

    //
    // create mock objects that are wrapped by corebi model objects
    //

    private static List<ColumnMetaData> getColumnsWithPostfix(
            final Map<String, ColumnMetaData> mockColumns,
            final String postfix) {
        List<ColumnMetaData> pkColumns = new ArrayList<>();
        for (Map.Entry<String, ColumnMetaData> entry : mockColumns.entrySet()) {
            if (entry.getKey().toUpperCase().endsWith(postfix)) {
                pkColumns.add(entry.getValue());
            }
        }
        return pkColumns;
    }

    private static Key getMockPK(final String tableName,
                                 final Map<String, ColumnMetaData> mockColumns) {
        Key pk = null;
        List<ColumnMetaData> pkColumns = getColumnsWithPostfix(mockColumns,
                PRIMARY_POSTFIX);
        List<String> columnNames = new ArrayList<>();
        for (ColumnMetaData column : pkColumns) {
            columnNames.add(column.getName());
        }
        if (!pkColumns.isEmpty()) {
            pk = mock(Key.class);
            when(pk.getName()).thenReturn(tableName.toUpperCase() + PRIMARY_POSTFIX);
            when(pk.getType()).thenReturn(KeyType.PRIMARY);
            when(pk.getColumns()).thenReturn(columnNames);
        }
        return pk;
    }

    private static Optional<Key> getPk(final DataStoreDescriptor dataStore) {
        return dataStore.getKeys()
                .stream()
                .filter(k -> k.getType() == KeyType.PRIMARY)
                .findFirst();
    }

    private static ForeignReference createDataStoreForeignKey(
            final String name, String fkColumnName,
            final String count,
            final DataStoreDescriptor dataStore,
            final DataStoreDescriptor targetDataStore) {
        // create foreign key
        ForeignReference fk = mock(ForeignReference.class);
        String fk_name = dataStore.getDataStoreName() + "_" + name + count +
                FOREIGN_POSTFIX;
        when(fk.getName()).thenReturn(fk_name);

        RefColumns refColumns = mock(RefColumns.class);
        when(refColumns.getForeignKeyColumnName()).thenReturn(fkColumnName);
        Optional<Key> pk = getPk(targetDataStore);
        assert (pk.isPresent());

        String pkName = pk.get().getColumns().get(0);
        when(refColumns.getPrimaryKeyColumnName()).thenReturn(pkName);
        when(fk.getReferenceColumns())
                .thenReturn(Collections.singletonList(refColumns));

        String modelCode = targetDataStore.getDataModelDescriptor().getModelCode();
        when(fk.getPrimaryKeyDataStoreModelCode()).thenReturn(modelCode);
        String targetTableName = targetDataStore.getDataStoreName();
        when(fk.getPrimaryKeyDataStoreName()).thenReturn(targetTableName);
        return fk;
    }

    private static void addFk(final String name, final String count,
                              final TableBase table, final ColumnBase fkColumn,
                              final TableBase targetTable) {
        ForeignReference fk =
                createDataStoreForeignKey(name, fkColumn.getName(), count,
                        ((TableBaseImpl) table).getDataStore(),
                        ((TableBaseImpl) targetTable).getDataStore());

        FkRelationshipBase join = ((TableBaseImpl) table).createFk(fk);
        ((TableBaseImpl) table).addFk(join);
    }


    //
    // create corebi objects based on mock objects that are wrapped
    //

    /**
     * Creates a mock table object that covers key aspects of the class.
     * The column type is determined based on postfixes and prefix.
     * <ul>
     * <li>default type is <code>VARCHAR2</code>
     * <li>Prefix N_ results in selection of <code>NUMBER</code> type
     * <li>Prefix V_ results in selection of <code>VARCHAR2</code> type
     * <li>Postfixes _PK, _AK, and _FK results in selection <code>NUMBER</code> type
     * </ul>
     * <p>
     * In addition, it adds a PK and its columns based on naming conventions:
     * a PK is created if at least one column ends with the _PK postfix. Columns
     * that end on this postfix are added as columns of the key.
     *
     * @param tableName        name of the mock table
     * @param description      description of the mock table
     * @param columnNames
     * @param tableAnnotations TODO
     * @return Mock TableBase object with name, description, columns and PK
     * (if pk columns are defined)
     */
    public static TableBase createMockTable(final SchemaBase schema,
                                            final String tableName,
                                            final String description,
                                            final String[] columnNames,
                                            final boolean noHierarchies,
                                            final TableAnnotations tableAnnotations) {
        BaseConfigurations biProperties =
                BaseConfigurationsHelper.getTestBaseConfigurations();
        TableAnnotations tAnnotations = tableAnnotations;
        if (tAnnotations == null) {
            tAnnotations = createTableAnnotation(description);
        }

        DataStoreDescriptor ds = mock(DataStoreDescriptor.class);
        Map<String, ColumnMetaData> mockColumns = createMockColumns(ds, tAnnotations,
                columnNames, biProperties);

        Key pk = getMockPK(tableName, mockColumns);

        Tuple naming = filter(tableName, BUSINESS_NAME_SEPERATOR);

        if (naming.after.equals("")) {
            when(ds.getDataStoreName()).thenReturn(tableName.toUpperCase());
            when(ds.getDescription()).thenReturn(description);
        } else {
            String desc = naming.after + biProperties.getMetadataSeparator() +
                    description;
            when(ds.getDataStoreName()).thenReturn(naming.before);
            when(ds.getDescription()).thenReturn(desc);
            when(tAnnotations.getBusinessName()).thenReturn(Optional.of(naming.after));
        }

        DataModelDescriptor model = mock(DataModelDescriptor.class);
        when(model.getDataServerTechnology()).thenReturn("ORACLE");
        when(model.getSchemaName()).thenReturn(SCHEMA_NAME);
        when(model.getModelCode()).thenReturn(SCHEMA_NAME);
        when(ds.getDataModelDescriptor()).thenReturn(model);

        if (pk != null) {
            when(ds.getKeys()).thenReturn(Collections.singletonList(pk));
        } else {
            when(ds.getKeys()).thenReturn(Collections.emptyList());
        }
        when(ds.getColumnMetaData()).thenReturn(mockColumns.values());

        Optional<TableAnnotations> defaultTableAnnotations =
                Optional.of(tAnnotations);
        AnnotationService annotationService = mock(AnnotationService.class);
        when(annotationService.getAnnotations(ds, Collections.emptyList()))
                .thenReturn(upCastTo(defaultTableAnnotations));

        schema.setTestAnnotationService(annotationService);
        Map<String, ? extends TableBase> tables =
                schema.createTables(Collections.singletonList(ds),
                        Collections.emptyList());
        TableBase table = tables.values().iterator().next();
//		TableBase table = schema.createTable(ds, upCastTo(defaultTableAnnotations));
        if (!tableName.contains("SHRUNKEN") && !noHierarchies) {
            HierarchyBranchBase branch = table.createBranch("default branch", true);
            table.addBranch(branch);
            ColumnBase column = table.getColumns().get(pk.getColumns().get(0));
            assert (column != null) : "did not find " + pk.getColumns().get(0);

            List<ColumnBase> emptyList = Collections.emptyList();
            branch.addLevel(column, Collections.singletonList(column),
                    null, emptyList, emptyList, false);
        }

        return table;
    }

    public static TableBase createMockTable(final SchemaBase schema,
                                            final String tableName,
                                            final String description,
                                            final String[] columnNames) {
        return createMockTable(schema, tableName, description, columnNames, false,
                null);
    }

    public static TableBase createMockTable(final SchemaBase schema,
                                            final String tableName,
                                            final String description,
                                            final String[] columnNames,
                                            final TableAnnotations annotations) {
        return createMockTable(schema, tableName, description, columnNames, false,
                annotations);
    }

    public static TableBase createMockTable(final SchemaBase schema,
                                            final String tableName,
                                            final String description,
                                            final String[] columnNames,
                                            final String tableType,
                                            final boolean noHierarchies,
                                            final TableAnnotations tableAnnotations) {
        TableAnnotations ta = tableAnnotations;
        if (tableAnnotations == null && tableType != null) {
            ta = createTableAnnotation(description);
        }

        TableBase table = createMockTable(schema, tableName, description, columnNames,
                noHierarchies, ta);
        return table;
    }

    public static TableBase createMockTable(final SchemaBase schema,
                                            final String tableName,
                                            final String description,
                                            final String[] columnNames,
                                            final String tableType,
                                            final TableAnnotations tableAnnotations) {
        return createMockTable(schema, tableName, description, columnNames,
                tableType, false, tableAnnotations);
    }

    private static List<ColumnBase> getTableColumnsWithPostfix(
            final Map<String, ? extends ColumnBase> columns,
            final String postfix) {
        List<ColumnBase> pkColumns = new ArrayList<>();
        for (Map.Entry<String, ? extends ColumnBase> entry : columns.entrySet()) {
            if (entry.getKey().toUpperCase().endsWith(postfix)) {
                pkColumns.add(entry.getValue());
            }
        }
        return pkColumns;
    }

    public static HierarchyBranchBase createHierarchy(final String hierarchyName,
                                                      final TableBase baseTable,
                                                      final String[] keyColumns,
                                                      final String[][] associatedColumns) {
        assert (baseTable != null && keyColumns.length == associatedColumns.length);
        HierarchyBranchBase branch = new HierarchyBranchBase(hierarchyName, baseTable);

        for (int i = 0; i < keyColumns.length; i++) {
            ColumnBase drillColumn = baseTable.getColumns().get(keyColumns[i]);
            assert (drillColumn != null);
            List<ColumnBase> assocColumns = new ArrayList<>();
            for (int j = 0; j < associatedColumns[i].length; j++) {
                ColumnBase assoc = baseTable.getColumns()
                        .get(associatedColumns[i][j]);
                assert (assoc != null);
                assocColumns.add(assoc);
            }
            List<ColumnBase> emptyList = Collections.emptyList();
            branch.addLevel(drillColumn, Collections.singletonList(drillColumn),
                    null, emptyList, assocColumns, false);
        }
        return branch;
    }

    private static void createMockFksPerFkColumn(final TableBase table,
                                                 final Map<String, TableBase> mockTables,
                                                 final Map<TableBase, Set<FkRelationshipBase>> incoming,
                                                 final Map<String, String> shrunkenDimNames) {
        // consider only FK columns
        List<ColumnBase> fkColums = getTableColumnsWithPostfix(table.getColumns(),
                FOREIGN_POSTFIX);

        for (ColumnBase fkColumn : fkColums) {
            String columnName = fkColumn.getName();
            Matcher regexMatcher = COLUMN_PATTERN.matcher(columnName);
            // final String prefix;
            // final String type;
            final String count;
            String name;
            if (regexMatcher.matches() && regexMatcher.group(2) != null) {
                // prefix = regexMatcher.group(1)!=null ? regexMatcher.group(1) :
                // "";
                name = regexMatcher.group(2).toUpperCase();
                // type = regexMatcher.group(3)!=null ? regexMatcher.group(3) : "";
                count = regexMatcher.group(4) != null ? regexMatcher.group(4) : "";
                if (name.length() == columnName.length()) {
                    // patterns is too greedy to avoid matching text_FK in group 2
                    name = name.substring(0, name.length() - 3);
                }
            } else {
                // prefix = "";
                // type = "";
                count = "";
                name = columnName.substring(0, columnName.length() - 3)
                        .toUpperCase();
            }

            // special case to handle shrunken dimensions
            // name of targeted shrunken dimension
            TableBase targetTable = null;
            if (shrunkenDimNames.get(fkColumn.getName()) == null) {
                targetTable = mockTables.get(name);
            } else {
                // needs proper parsing
                String n = shrunkenDimNames.get(fkColumn.getName());
                targetTable = mockTables.get(n.substring(0, n.length() - 3)
                        .toUpperCase());
            }

            assert (targetTable != null) : "Test Case not properly configured. "
                    + "Fk based on " + table.getName() + "." + columnName
                    + " does not find table " + name + ".";

            addFk(name, count, table, fkColumn, targetTable);
        }
    }

    private static void initializeFkRelationships(final Map<String, TableBase> tables) {
        for (final TableBase table : tables.values()) {
            for (final FkRelationshipBase fk : table.getFks()) {
                // here the actual initialization takes place
                try {
                    fk.getReferencedPrimaryKey();
                } catch (MalformedModelException e) {
                }
            }
        }
    }

    /**
     * Adds FK relationships based on naming convention of FK column identified
     * by the _FK column postfix. The target table is identified by the remainder
     * of the column name.
     * <p>
     * <p>
     * Example: V_PATIENT_D_1_PK column on table ENCOUNTER_F suggest that a FK
     * to table PATIENT_D is created. The name of the FK is
     * ENCOUNTYER_F_PATIENT_D_1
     *
     * @param mockTables mock tables to be enhanced
     * @return incoming mock data Map with enhancements
     */
    public static Map<String, TableBase> addFksToTables(
            final SchemaBase schema,
            final Map<String, TableBase> mockTables,
            final Map<TableBase, Map<String, String>> shrunkenDimensionsNameMap) {
        if (!VISITED.contains(schema)) {
            VISITED.add(schema);
        } else {
            System.err.println("Possible ERROR: This method may have been called " +
                    "accidentally more than once on the same model.");
        }

        for (TableBase table : mockTables.values()) {
            Map<String, String> shrunkenDimNames = shrunkenDimensionsNameMap.get(table);
            if (shrunkenDimNames == null) {
                shrunkenDimNames = Collections.emptyMap();
            }
            createMockFksPerFkColumn(table, mockTables, null, shrunkenDimNames);
        }

        initializeFkRelationships(mockTables);
        return mockTables;
    }

    private static String[] createColumnNames(final int index, final int stringTypeCnt,
                                              final int numberTypeCnt,
                                              final String[] referencedTables) {
        String[] columnNames;
        columnNames = new String[1 + stringTypeCnt + numberTypeCnt +
                referencedTables.length + SYSTEM_COLUMNS.length];
        columnNames[0] = "sk_pk";

        int startIndex = 1;
        for (int count = 0; count < referencedTables.length; count++) {
            columnNames[startIndex + count] = referencedTables[count] + "_FK";
        }

        startIndex += referencedTables.length;
        for (int count = 0; count < stringTypeCnt; count++) {
            columnNames[startIndex + count] = "v_COLUMN" + index + "_" + count;
        }

        startIndex += stringTypeCnt;
        for (int count = 0; count < numberTypeCnt; count++) {
            columnNames[startIndex + count] = "n_COLUMN" + index + "_" +
                    (stringTypeCnt + count);
        }

        startIndex += numberTypeCnt;
        for (int count = 0; count < SYSTEM_COLUMNS.length; count++) {
            columnNames[startIndex + count] = SYSTEM_COLUMNS[count];
        }

        return columnNames;
    }

    //
    // More convenient way of defining mock tables
    //

    public static TableBase constructTable(final String name, final SchemaBase schema,
                                           final int index, final String type,
                                           final TableAnnotations tableAnnotation,
                                           final int stringTypeCnt,
                                           final int numberTypeCnt,
                                           final String[] referencedTables) {
        String[] columnNames;
        if (type.equalsIgnoreCase("HIERARCHY") || name.contains("HIER")) {
            //assert (referencedTables.length == 1);
            //assert (stringTypeCnt > 0); // ignore numberTypeCount
            columnNames = new String[2 + referencedTables.length + 2 * stringTypeCnt +
                    numberTypeCnt + SYSTEM_COLUMNS.length];
            columnNames[0] = "sk_pk";

            int startIndex = 1;
            for (int count = 0; count < referencedTables.length; count++) {
                columnNames[startIndex + count] = referencedTables[count] + "_FK";
            }

            startIndex += referencedTables.length;
            columnNames[startIndex] = "v_HIER_NAME";

            startIndex++;
            for (int count = 0; count < stringTypeCnt; count++) {
                columnNames[startIndex + 2 * count] =
                        "v_HIER" + index + "_LVL" + count + "_CODE";
                columnNames[startIndex + 2 * count + 1] =
                        "v_HIER" + index + "_LVL" + count + "_NAME";
            }

            startIndex += 2 * stringTypeCnt;
            for (int count = 0; count < numberTypeCnt; count++) {
                columnNames[startIndex + count] = "n_COLUMN" + index + "_" +
                        (stringTypeCnt + count);
            }

            startIndex += numberTypeCnt;
            for (int count = 0; count < SYSTEM_COLUMNS.length; count++) {
                columnNames[startIndex + count] = SYSTEM_COLUMNS[count];
            }
        } else if (type.equalsIgnoreCase("BRIDGE")) {
            assert (referencedTables.length == 2);
            columnNames = new String[4];
            columnNames[0] = "sk_pk";
            columnNames[1] = referencedTables[0] + "_FK";
            columnNames[2] = referencedTables[1] + "_FK";
            columnNames[3] = "THIS_WEIGHT";
        } else {
            columnNames = createColumnNames(index, stringTypeCnt, numberTypeCnt,
                    referencedTables);
        }

        TableBase t = createMockTable(schema, name, "desc", columnNames, type,
                tableAnnotation);
        return t;
    }

    public static SchemaBase createSchema() {
        ErrorWarningMessageJodi errorWarningMessages =
                ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
        return ApplicationBaseHelper.createMockSchema(errorWarningMessages);
    }

    @Before
    public void setUp() {
        schema = ApplicationBaseHelper.createMockSchema(errorWarningMessages);
    }

    //
    // SELF TEST BELOW
    //

    @After
    public void print() {
        errorWarningMessages.printMessages();
        errorWarningMessages.clear();
    }

    @Test
    public void testMockTable() {
        String[] types = new String[]{"DOUBLE", "VARCHAR", "DOUBLE"};
        String[] columns = new String[]{"a_pk", "v_b", "n_count"};

        TableBase table = MockTableBaseHelper.createMockTable(schema, "table_d",
                "desc", columns);

        assertNotNull(table);
        assertEquals("TABLE_D", table.getName());

        assertNotNull(table.getPrimaryKey());
        assertEquals(1, table.getPrimaryKey().getColumns().size());
        assertEquals("TABLE_D_PK", table.getPrimaryKey().getName());
        assertEquals(table, table.getPrimaryKey().getParent());

        assertEquals(3, table.getColumns().size());
        for (Map.Entry<String, ? extends ColumnBase> entry : table.getColumns().entrySet()) {
            int pos = entry.getValue().getPosition();
            assertTrue(pos >= 1 && pos < 4);
            assertEquals(columns[pos - 1], entry.getValue().getName());
            assertEquals(types[pos - 1], entry.getValue().getDataType());
            assertEquals(table, entry.getValue().getParent());
        }
    }

    @Test
    public void testMockTableColumnTypes() {
        String[] types = new String[]{"VARCHAR", "VARCHAR", "VARCHAR", "VARCHAR",
                "DOUBLE"};
        String[] columns = new String[]{"v_a_pk", "v_b_fk", "v_c", "count", "altkey_ak"};
        TableBase table = MockTableBaseHelper.createMockTable(schema, "table_d", "desc", columns);
        for (Map.Entry<String, ? extends ColumnBase> entry : table.getColumns().entrySet()) {
            int pos = entry.getValue().getPosition();
            assertEquals(types[pos - 1], entry.getValue().getDataType());
        }
    }

    @Test
    public void testMockOneFk() {
        Map<String, TableBase> star = new HashMap<>();
        TableBase table_d = MockTableBaseHelper.createMockTable(schema, "table_d", "desc",
                new String[]{"a_pk", "N_b"});
        star.put(table_d.getName(), table_d);
        TableBase table_f = MockTableBaseHelper.createMockTable(schema, "table_f", "desc",
                new String[]{"a_pk", "table_d_fk", "N_measure1"});
        star.put(table_f.getName(), table_f);

        // method under test
        MockTableBaseHelper.addFksToTables(schema, star, Collections.emptyMap());

        // no FK attached to table_d but one incoming FK
        assertEquals(0, table_d.getFks().size());
        assertEquals(1, table_d.getPrimaryKey().getIncomingFks().size());
        assertEquals(1, table_d.getIncomingFks().size());

        // no incoming FKs but one outgoing FK
        assertEquals(0, table_f.getIncomingFks().size());
        assertEquals(1, table_f.getFks().size());
        FkRelationshipBase fk = table_f.getFks().get(0);
        assertEquals("TABLE_F_TABLE_D_FK", fk.getName());
        assertEquals(1, fk.getFKColumns().size());
        assertEquals("table_d_fk", fk.getFKColumns().get(0).getName());
        assertEquals(table_d.getPrimaryKey(), fk.getReferencedPrimaryKey());
        assertEquals(table_f, fk.getReferringTable());
    }

    @Test
    public void testMockMaformedFkTableName() {
        Map<String, TableBase> star = new HashMap<>();
        TableBase table_d = MockTableBaseHelper.createMockTable(schema, "table_x_d", "desc",
                new String[]{"a_pk", "N_b"});
        star.put(table_d.getName(), table_d);
        TableBase table_f = MockTableBaseHelper.createMockTable(schema, "table_f", "desc",
                new String[]{"a_pk", "table_x_d_fk", "N_measure1"});
        star.put(table_f.getName(), table_f);

        // method under test
        MockTableBaseHelper.addFksToTables(schema, star, Collections.emptyMap());

        // no FK attached to rable_d but one incoming FK
        assertEquals(0, table_d.getFks().size());
        assertEquals(1, table_d.getIncomingFks().size());

        // no incoming FKs but one outgoing FK
        assertEquals(0, table_f.getIncomingFks().size());
        assertEquals(1, table_f.getFks().size());
    }

    @Test
    public void testMockMultipleFks() {
        Map<String, TableBase> star = new HashMap<>();
        TableBase table1_d = MockTableBaseHelper.createMockTable(schema, "table1_d", "desc",
                new String[]{"a_pk", "N_b"});
        star.put(table1_d.getName(), table1_d);
        TableBase table2_d = MockTableBaseHelper.createMockTable(schema, "table2_d", "desc",
                new String[]{"a_pk", "N_b"});
        star.put(table2_d.getName(), table2_d);
        TableBase table_f = MockTableBaseHelper.createMockTable(schema, "table_f", "desc",
                new String[]{"a_pk", "table1_d_fk", "table2_d_fk", "N_measure1"});
        star.put(table_f.getName(), table_f);

        // method under test
        MockTableBaseHelper.addFksToTables(schema, star, Collections.emptyMap());

        assertEquals(0, table1_d.getFks().size());
        assertEquals(1, table1_d.getIncomingFks().size());
        assertEquals(0, table2_d.getFks().size());
        assertEquals(1, table2_d.getIncomingFks().size());

        assertEquals(2, table_f.getFks().size());
    }

    @Test(expected = AssertionError.class)
    public void testIncorrectConfiguration() {
        Map<String, TableBase> star = new HashMap<>();
        TableBase table_f = MockTableBaseHelper.createMockTable(schema, "table_f", "desc",
                new String[]{"a_pk", "table_d_fk"});
        star.put(table_f.getName(), table_f);

        // method under test
        MockTableBaseHelper.addFksToTables(schema, star, Collections.emptyMap());
        throw new AssertionError("should have created an assertion exception. " +
                "Use the Java -ea flag to enable assertion feature.");
    }

    private static class Tuple {

        private final String before;
        private final String after;

        Tuple(final String before, final String after) {
            super();
            this.before = before;
            this.after = after;
        }

    }

}
