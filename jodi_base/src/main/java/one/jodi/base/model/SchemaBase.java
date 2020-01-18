package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.annotation.AnnotationService;
import one.jodi.base.service.annotation.TableAnnotations;
import one.jodi.base.service.metadata.DataStoreDescriptor;

import java.util.*;
import java.util.regex.Pattern;

public class SchemaBase implements ModelNode {
    private final ApplicationBase parent;
    // placeholder to possible extension supporting multiple schemas
    private final String containerName;
    // name in the logical model
    private final String name;
    // additional properties of the schema
    private final String physicalSchemaName;
    private final Map<String, TableBase> tables = new HashMap<>();
    private final DatabaseBase database;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private AnnotationService annotationService;

    public SchemaBase(final ApplicationBase parent,
                      final String name, final String containerName,
                      final String physicalSchemaName,
                      final DatabaseBase database,
                      final AnnotationService annotationService,
                      final ErrorWarningMessageJodi errorWarningMessages) {
        this.parent = parent;
        this.name = name;
        this.containerName = containerName;
        this.physicalSchemaName = physicalSchemaName;
        this.database = database;
        this.annotationService = annotationService;
        this.errorWarningMessages = errorWarningMessages;
    }

    private String getMapKey(final String model, final String tableName) {
        return model + "." + tableName;
    }

    protected TableBase findTable(final String schemaName, final String tableName) {
        assert (schemaName != null && !schemaName.isEmpty() &&
                tableName != null && !tableName.isEmpty());
        String mapKey = getMapKey(schemaName, tableName);
        TableBase table = tables.get(mapKey);
        if (table == null) {
            String msg = String.format("Can't find table %s in model %s.",
                    tableName, schemaName);
            throw new MalformedModelException(null, msg);
        }
        return table;
    }

    //
    //
    //

    @Override
    public ApplicationBase getParent() {
        return this.parent;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void fullName(final StringBuffer sb, final String quote) {
        sb.append(quote)
                .append(getContainerName())
                .append(quote)
                .append("..")
                .append(quote)
                .append(getName())
                .append(quote);
    }

    public List<? extends TableBase> getTables() {
        List<TableBase> tablesInModel = new ArrayList<>(tables.values());
        return Collections.unmodifiableList(tablesInModel);
    }

    public Map<String, ? extends TableBase> getTablesMap() {
        return Collections.unmodifiableMap(tables);
    }

    void addTable(final String name, final TableBase table) {
        this.tables.put(name, table);
    }

    public TableBase getTable(final String name) {
        return this.tables.get(name);
    }

    public String getContainerName() {
        return containerName;
    }

    public String getPhysicalSchemaName() {
        return physicalSchemaName;
    }

    public DatabaseBase getDatabase() {
        return database;
    }

    //
    // table factory method
    //

    protected void setTestAnnotationService(final AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    private String getMapKey(final DataStoreDescriptor tableData) {
        return getName() + "." + tableData.getDataStoreName();
    }

    private void initializeFkRelationships(final Map<String, TableBase> tables) {
        final List<FkRelationshipBase> defectFks = new ArrayList<>();
        for (final TableBase table : tables.values()) {
            for (final FkRelationshipBase fk : table.getFks()) {
                // here the actual initialization takes place
                try {
                    fk.getReferencedPrimaryKey();
                } catch (MalformedModelException e) {
                    defectFks.add(fk);
                }
            }
        }

        // remove defective FKs
        defectFks.forEach(FkRelationshipBase::remove);
    }

    /**
     * Factory method. Used for internal purposes only and will be overwritten by subclass
     *
     * @param tableData
     * @param tableAnnotations
     * @return
     */
    protected TableBase createTableInstance(
            final DataStoreDescriptor tableData,
            final Optional<TableAnnotations> tableAnnotations) {
        TableBaseImpl table = new TableBaseImpl(this, tableData, tableAnnotations,
                this.errorWarningMessages, true);
        table.initializeObject();
        return table;
    }

    /**
     * This method is used as the only entry point to create instances of Table.
     * The approach guarantees that only one instance of a table is created.
     * However, the method does not guarantee a fully instantiated Table. The
     * main issue is that incoming relationship counts are only available after
     * initialization of relationships.
     *
     * @param tableData        object to be decorated
     * @param tableAnnotations annotations associated with dataStore
     * @return adapter table.
     */
    private TableBase createTable(final DataStoreDescriptor tableData,
                                  final Optional<TableAnnotations> tableAnnotations,
                                  final Map<String, TableBase> existingTables) {
        assert (tableData != null && tableAnnotations != null);
        String mapKey = getMapKey(tableData);
        TableBase table = existingTables.get(mapKey);
        if (table == null) {
            table = createTableInstance(tableData, tableAnnotations);
        }
        return table;
    }

    public Map<String, ? extends TableBase> createTables(
            final List<DataStoreDescriptor> dataStores,
            final List<Pattern> hiddenColumnPattern) {
        Map<String, TableBase> newTables = new TreeMap<>();
        for (DataStoreDescriptor tableData : dataStores) {
            final Optional<TableAnnotations> taBase =
                    annotationService.getAnnotations(tableData, hiddenColumnPattern);
            Optional<TableAnnotations> ta = Optional.empty();
            if (taBase.isPresent()) {
                ta = Optional.of((TableAnnotations) taBase.get());
            }
            TableBase newTable = createTable(tableData, ta,
                    Collections.unmodifiableMap(newTables));
            newTables.put(getMapKey(tableData), newTable);
            // add table to correct schema
            addTable(getMapKey(tableData), newTable);
        }
        initializeFkRelationships(newTables);
        return newTables;
    }

}