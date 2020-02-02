package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.FkRelationshipBase.Cardinality;
import one.jodi.base.model.RelationshipBase.RelType;
import one.jodi.base.service.annotation.ColumnAnnotations;
import one.jodi.base.service.annotation.TableAnnotations;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.ForeignReference;
import one.jodi.base.service.metadata.Key;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class TableBaseImpl implements TableBase {

    private final static Logger logger = LogManager.getLogger(TableBaseImpl.class);

    private final static String ERROR_MESSAGE_84000 =
            "Tables including '%1$s' contains a cycle.";

    private static final String ERROR_MESSAGE_84020 =
            "Hierarchy associated with base table '%1$s' misses levels and cannot " +
                    "properly reflect levels in itself and shrunken dimension table '%2$s'.";

    // Adaptee of Table
    private final boolean defaultConstruction;
    private final DataStoreDescriptor tableData;
    //TODO update type to new SCHEMA
    private final SchemaBase parent;
    private final String name;
    private final String schemaName;
    private final String physicalDataServerName;
    private final String dataBaseServiceName;
    private final int dataBaseServicePort;
    private final List<FkRelationshipBase> cachedFks = new ArrayList<>();
    private final List<RelationshipBase> relationships = new ArrayList<>();
    private final List<RelationshipBase> incomingRelationships = new ArrayList<>();
    private final List<HierarchyBranchBase> hierarchy = new ArrayList<>();
    private final ErrorWarningMessageJodi errorWarningMessages;
    protected List<KeyBase> cachedKeys = new ArrayList<>();
    private Optional<TableAnnotations> tableAnnotations = Optional.empty();
    // Cache Lists and Maps so that the objects can be referenced
    // by other model classes
    private Map<String, ColumnBase> cachedColumns = new HashMap<>();

    protected TableBaseImpl(final SchemaBase parent,
                            final DataStoreDescriptor tableData,
                            final Optional<TableAnnotations> tableAnnotations,
                            final ErrorWarningMessageJodi errorWarningMessages,
                            final boolean defaultConstruction) {
        super();
        assert (tableData != null && parent != null);
        assert (tableData.getDataModelDescriptor()
                .getSchemaName()
                .equals(parent.getPhysicalSchemaName())) :
                tableData.getDataModelDescriptor().getSchemaName() + " differs from +" +
                        parent.getPhysicalSchemaName();
        this.defaultConstruction = defaultConstruction;
        this.parent = parent;
        this.name = tableData.getDataStoreName();
        this.schemaName = parent.getName();
        this.physicalDataServerName = tableData.getDataModelDescriptor()
                .getPhysicalDataServerName();
        this.dataBaseServiceName = tableData.getDataModelDescriptor()
                .getDataBaseServiceName();
        this.dataBaseServicePort = tableData.getDataModelDescriptor()
                .getDataBaseServicePort();

        this.tableAnnotations = tableAnnotations;
        this.errorWarningMessages = errorWarningMessages;
        this.tableData = tableData;
    }

    // used with Alias table creation
    protected TableBaseImpl(final SchemaBase parent,
                            final TableBase otherTable,
                            final Optional<TableAnnotations> tableAnnotations,
                            final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.defaultConstruction = false;
        this.tableData = ((TableBaseImpl) otherTable).tableData;
        this.parent = parent;
        this.name = ((TableBaseImpl) otherTable).name;
        this.schemaName = ((TableBaseImpl) otherTable).schemaName;
        this.physicalDataServerName = ((TableBaseImpl) otherTable).physicalDataServerName;
        this.dataBaseServiceName = ((TableBaseImpl) otherTable).dataBaseServiceName;
        this.dataBaseServicePort = ((TableBaseImpl) otherTable).dataBaseServicePort;

        this.tableAnnotations = tableAnnotations;
        this.errorWarningMessages = errorWarningMessages;
    }

    protected void initializeObject() {
        // In some cases columns may be created based on other input,
        // e.g. in Alias columns. This coded will be skipped in these cases.
        if (defaultConstruction) {
            this.cachedColumns.putAll(createColumns(this.tableData));
            this.cachedKeys = createDataStoreKeys(this.tableData);
            addFks(this.tableData);
        }
    }

    protected Optional<ColumnAnnotations> getColumnAnnotations(
            final String columnName) {
        Optional<ColumnAnnotations> columnAnnotations = Optional.empty();
        if (tableAnnotations.isPresent()) {
            ColumnAnnotations ca = (ColumnAnnotations) tableAnnotations
                    .get()
                    .getColumnAnnotations()
                    .get(columnName);
            columnAnnotations = Optional.ofNullable(ca);
        }
        return columnAnnotations;
    }

    //TODO needs to point to new SCHEMA class, which implements ModelNode
    public SchemaBase getParent() {
        return parent;
    }

    // needed to allow Alias to create additional FkRelationships
    protected void addFk(FkRelationshipBase fkRelationship) {
        this.cachedFks.add(fkRelationship);
    }

    // TODO remove after refactoring task jodi-527
    public DataStoreDescriptor getDataStore() {
        return tableData;
    }

    //
    // Functionality offered by Adaptee of Adapter pattern. It is acceptable to
    // remove that is not required for the intended purpose or modify existing
    // functionality
    //

    @Override
    public String getPhysicalDataServerName() {
        return this.physicalDataServerName;
    }

    @Override
    public String getDataBaseServiceName() {
        return this.dataBaseServiceName;
    }

    @Override
    public int getDataBaseServicePort() {
        return this.dataBaseServicePort;
    }

    @Override
    public String getSchemaName() {
        return this.schemaName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    //
    // Annotation Data
    //

    @Override
    public String getDescription() {
        return this.tableAnnotations.isPresent() &&
                this.tableAnnotations.get().getDescription().isPresent()
                ? this.tableAnnotations.get().getDescription().get()
                : "";
    }

    @Override
    public String getBusinessName() {
        return this.tableAnnotations.isPresent() &&
                this.tableAnnotations.get().getBusinessName().isPresent()
                ? this.tableAnnotations.get().getBusinessName().get()
                : "";
    }

    @Override
    public String getAbbreviatedBuisnessName() {
        return this.tableAnnotations.isPresent() &&
                this.tableAnnotations.get().getAbbreviatedBusinessName().isPresent()
                ? this.tableAnnotations.get().getAbbreviatedBusinessName().get()
                : "";
    }

    //
    // create and access columns
    //

    // factory method that may be overwritten by subclass
    protected ColumnBase createColumn(final String columnName,
                                      final ColumnMetaData dColumn) {
        return new ColumnBase(dColumn, this, getColumnAnnotations(columnName),
                errorWarningMessages);
    }

    // only used if useDateStore() is returning false
    protected void addColumns(final Map<String, ColumnBase> columns) {
        assert (this.cachedColumns.isEmpty());
        this.cachedColumns = columns;
    }

    private Map<String, ColumnBase> createColumns(final DataStoreDescriptor tableData) {
        // populate caches needed to prevent multiple instances of the same object
        // this will simplify analysis and enriching of the model
        return tableData.getColumnMetaData()
                .stream()
                .filter(m -> !m.getName().contains("$") &&
                        !m.getName().contains("#")) // filter out system columns
                .collect(Collectors.toMap(ColumnMetaData::getName,
                        c -> createColumn(c.getName(), c)));
    }

    @Override
    public Map<String, ? extends ColumnBase> getColumns() {
        return Collections.unmodifiableMap(this.cachedColumns);
    }

    @Override
    public List<? extends ColumnBase> getOrderedColumns(final boolean reversed) {
        int sign = (reversed) ? -1 : 1;

        List<ColumnBase> orderedColumns = new ArrayList<>();
        // use TreeMap to order items
        Map<Integer, ColumnBase> orderedMap = new TreeMap<>();
        for (ColumnBase column : this.getColumns().values()) {
            orderedMap.put(column.getPosition() * sign, column);
        }
        orderedColumns.addAll(orderedMap.values());

        return Collections.unmodifiableList(orderedColumns);
    }

    @Override
    public List<? extends ColumnBase> getOrderedColumns() {
        return getOrderedColumns(false);
    }

    //
    // Keys
    //

    // factory method
    protected KeyBase createKey(Key k) {
        return new KeyBase(k, this, this.errorWarningMessages);
    }

    private Set<String> getColumnNameSet(final DataStoreDescriptor tableData) {
        return tableData.getColumnMetaData()
                .stream()
                .map(c -> c.getName())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isValidKey(final Key key, final DataStoreDescriptor targetData) {
        Set<String> tableColumns = getColumnNameSet(targetData);
        return !key.getColumns()
                .stream()
                .filter(c -> !tableColumns.contains(c))
                .findFirst()
                .isPresent();
    }

    /*
     * Defensive programming. This method removes the index with the same name
     * as the primary key as it is redundant.
     */
    private List<Key> removeIndexForPk(final List<Key> dataStoreKeys) {
        // find primary key if it exists
        Optional<Key> pk =
                dataStoreKeys.stream()
                        .filter(e -> e.getType() == Key.KeyType.PRIMARY)
                        .findFirst();

        // include all but the index with same name as the primary key
        List<Key> consolidatedKeyList = dataStoreKeys;
        if (pk.isPresent()) {
            consolidatedKeyList =
                    dataStoreKeys.stream()
                            .filter(k -> !k.getName().equals(pk.get().getName()) ||
                                    k.getType() == Key.KeyType.PRIMARY)
                            .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(consolidatedKeyList);
    }

    private List<KeyBase> createDataStoreKeys(final DataStoreDescriptor tableData) {
        return removeIndexForPk(tableData.getKeys())
                .stream()
                .peek(k -> {
                    if (!isValidKey(k, tableData))
                        logger.debug("Skip creation of inconsistent key: " +
                                tableData.getDataStoreName() + "." +
                                k.getName());
                })
                .filter(k -> isValidKey(k, tableData))
                .map(k -> createKey(k))
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends KeyBase> getKeys() {
        return Collections.unmodifiableList(this.cachedKeys);
    }

    @Override
    public KeyBase getPrimaryKey() {
        KeyBase primaryKey = null;
        for (KeyBase key : getKeys()) {
            if (key.isPrimaryKey()) {
                primaryKey = key;
                break;
            }
        }
        return primaryKey;
    }

    @Override // TODO likely needs to be expanded to a list of alt keys
    public KeyBase getAlternateKey() {
        Optional<? extends KeyBase> firstKey =
                getKeys().stream()
                        .filter(key -> key.isAlternativeKey())
                        .findFirst();
        return firstKey.isPresent() ? firstKey.get() : null;
    }

    //
    // create and access FK Relationships
    //

    // factory method
    protected FkRelationshipBase createFkRelationship(final ForeignReference fk,
                                                      final Cardinality cardinality) {
        assert (fk.getReferenceColumns() != null && fk.getReferenceColumns().size() > 0) :
                "Invalid FK Key defintion for " + fk.getName();
        assert (cardinality != null);
        FkRelationshipBase newFk = new FkRelationshipBase(this, fk.getName(), fk,
                cardinality,
                errorWarningMessages);
        return newFk;
    }

    private boolean keysAreDefined(final ForeignReference fk,
                                   final DataStoreDescriptor tableData) {
        Set<String> tableColumns = getColumnNameSet(tableData);
        return !fk.getReferenceColumns()
                .stream()
                .map(r -> r.getForeignKeyColumnName())
                .filter(c -> !tableColumns.contains(c))
                .findFirst()
                .isPresent();
    }

    private void addFks(final DataStoreDescriptor tableData) {
        for (ForeignReference fk : tableData.getFKRelationships()) {
            if (keysAreDefined(fk, tableData)) {
                addFk(createFk(fk));
            } else {
                logger.warn("Fk not created " + tableData.getDataStoreName() + "." +
                        fk.getName() + ". FK columns not all present.");
            }
        }
    }

    protected Cardinality getCardinality(final ColumnBase column) {
        assert (column != null);
        return (column.isNullable() ? Cardinality.MANY_TO_ZERO_OR_ONE
                : Cardinality.MANY_TO_ONE);
    }

    protected final FkRelationshipBase createFk(final ForeignReference fk) {
        assert (fk != null && fk.getReferenceColumns().size() > 0);

        // determine defined or implicit cardinality
        String namingColumnName = fk.getReferenceColumns().get(0)
                .getForeignKeyColumnName();
        ColumnBase namingColumn = getColumns().get(namingColumnName);
        Cardinality cardinality = getCardinality(namingColumn);

        FkRelationshipBase newFk = createFkRelationship(fk, cardinality);
        return newFk;
    }


    @Override
    public List<? extends FkRelationshipBase> getFks() {
        return Collections.unmodifiableList(this.cachedFks);
    }

    @Override // Only to be used during table initialization
    public void remove(FkRelationshipBase fk) {
        this.cachedFks.remove(fk);
    }

    @Override
    public List<? extends FkRelationshipBase> getFks(List<ColumnBase> columns) {
        List<FkRelationshipBase> found = new ArrayList<>();
        for (ColumnBase column : columns) {
            column.getAssociatedFks().stream().filter(fk -> !found.contains(fk))
                    .forEach(found::add);
        }
        return Collections.unmodifiableList(found);
    }

    @Override
    public List<? extends FkRelationshipBase> getFks(final ColumnBase namingColumn) {
        return Collections.unmodifiableList(
                namingColumn.getAssociatedFksUsingNamingColumn());
    }

    @Override
    public List<? extends FkRelationshipBase> getFks(final TableBase target) {
        List<FkRelationshipBase> found =
                this.cachedFks.stream()
                        .filter(fk -> fk.getReferencedPrimaryKey()
                                .getParent() == target)
                        .collect(Collectors.toList());
        return found;
    }

    @Override
    public Set<? extends FkRelationshipBase> getIncomingFks() {
        final Set<FkRelationshipBase> fks = new HashSet<>();
        for (KeyBase key : getKeys()) {
            fks.addAll(key.getIncomingFks());
        }
        return Collections.unmodifiableSet(fks);
    }

    @Override
    public Set<? extends FkRelationshipBase> getIncomingFks(final TableBase from) {
        final Set<FkRelationshipBase> matchingFks =
                getIncomingFks().stream()
                        .filter(fk -> fk.getReferringTable() == from)
                        .collect(Collectors.toSet());
        return Collections.unmodifiableSet(matchingFks);
    }

    //
    // Insert Additional Functionality Below This Section
    //

    private boolean stringEndsWith(final String name, final List<String> postfixes) {
        boolean match = false;
        for (String postfix : postfixes) {
            if (name.toUpperCase().endsWith(postfix.toUpperCase())) {
                match = true;
                break;
            }
        }
        return match;
    }

    @Override
    public boolean nameEndsWith(final List<String> postfixes) {
        assert (postfixes != null);
        return stringEndsWith(this.getName(), postfixes);
    }

    private boolean stringContains(final String name, final List<String> substrings) {
        boolean match = false;
        for (String postfix : substrings) {
            if (name.toUpperCase().contains(postfix.toUpperCase())) {
                match = true;
                break;
            }
        }
        return match;
    }

    @Override
    public List<? extends ColumnBase> getFkColumns() {
        List<ColumnBase> fkKeyColumns =
                getColumns().values().stream().filter(ColumnBase::isFkColumn)
                        .collect(Collectors.toList());
        return Collections.unmodifiableList(fkKeyColumns);
    }

    @Override
    public List<? extends ColumnBase> columnNameEndingWith(
            final List<String> postfixes) {
        List<ColumnBase> matchedColumns = new ArrayList<>();
        for (ColumnBase column : getColumns().values()) {
            boolean match = stringContains(column.getName(), postfixes);
            if (match) {
                matchedColumns.add(column);
            }
        }
        return Collections.unmodifiableList(matchedColumns);
    }

    @Override
    public List<? extends ColumnBase> columnNameContains(
            final List<String> postfixes) {
        List<ColumnBase> matchedColumns = new ArrayList<>();
        for (ColumnBase column : getColumns().values()) {
            boolean match = stringContains(column.getName(), postfixes);
            if (match) {
                matchedColumns.add(column);
            }
        }
        return Collections.unmodifiableList(matchedColumns);
    }

    //
    //
    //

    // factory method for Relationship creation
    protected RelationshipBase createRelationship(final TableBase target,
                                                  final RelType type) {
        return new RelationshipBase(this, target, type);
    }

    public RelationshipBase addRelationship(final TableBase target,
                                            final RelType type) {
        assert (target != null && type != null) :
                "Target Table and Relationship type must not be null";
        assert (getRelationship(type, target) == null) : "Only 1 relationship allowed";
        RelationshipBase relationship = createRelationship(target, type);
        addRelationship(relationship);
        ((TableBaseImpl) target).addIncomingRelationship(relationship);
        return relationship;
    }

    protected void addRelationship(final RelationshipBase relationship) {
        this.relationships.add(relationship);
    }

    /**
     * Get outgoing relationships that are attached to this table.
     *
     * @return list of relationships of various types (but not FK type)
     */
    public List<? extends RelationshipBase> getRelationships() {
        return Collections.unmodifiableList(this.relationships);
    }

    /**
     * Get outgoing relationships for a given type that are attached to this table.
     *
     * @param type the type of relationship
     * @return list of relationships of specified type
     */
    public List<? extends RelationshipBase> getRelationships(final RelType type) {
        assert (type != null);
        List<RelationshipBase> filteredRelationships =
                this.relationships.stream()
                        .filter(rel -> rel.getType() == type)
                        .collect(Collectors.toList());

        return Collections.unmodifiableList(filteredRelationships);
    }

    protected RelationshipBase getRelationship(final RelType type,
                                               final TableBase target) {
        RelationshipBase found = null;

        for (RelationshipBase rel : this.relationships) {
            if ((rel.getType() == type) && (rel.getTarget() == target)) {
                found = rel;
                break;
            }
        }

        return found;
    }

    public List<? extends TableBase> getReachableTables(final RelType type) {
        boolean errorDetected = false;
        List<TableBase> reachable = new ArrayList<>();

        // I use an implementation that avoids recursion to better handle
        // situation in which a cycle exists.
        Stack<TableBase> toProcess = new Stack<>();
        TableBase thisObject = this;
        toProcess.add(thisObject);
        while (!toProcess.isEmpty()) {
            TableBase currentTable = (TableBase) toProcess.pop();
            for (RelationshipBase rel : ((TableBaseImpl) currentTable).getRelationships()) {
                if ((rel.getType() == type) && !reachable.contains(rel.getTarget())) {
                    reachable.add(rel.getTarget());
                    toProcess.push(rel.getTarget());
                } else if (!errorDetected && (rel.getType() == type) &&
                        reachable.contains(rel.getTarget())) {
                    String msg = errorWarningMessages.formatMessage(84000,
                            ERROR_MESSAGE_84000, this.getClass(),
                            rel.getParent().getName());
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(),
                            msg, MESSAGE_TYPE.ERRORS);
                    logger.error(msg);
                    errorDetected = true;
                }
            }
        }

        return reachable;
    }

    public boolean isReachable(final TableBase target, final RelType type) {
        List<? extends TableBase> reachableTables = getReachableTables(type);
        return reachableTables.contains(target);
    }

    public int countTablesBetween(final TableBase toDimension, final RelType type) {
        int count = 0;
        TableBaseImpl current = (TableBaseImpl) this;
        while ((current != toDimension) && (current.getRelationships(type).size() > 0)) {
            count++;
            // we can assume that only one relationship exists
            assert (current.getRelationships(type).size() == 1);
            assert (count < 100) : "Endless loop found";
            RelationshipBase r = current.getRelationships(type).get(0);
            current = (TableBaseImpl) r.getTarget();
        }
        if (current != toDimension) {
            // indicates that toDimension is not a shrunken dimension of fromDimension
            count = -1;
        }
        return count;
    }

    /**
     * Get incoming relationships that are attached to this table.
     * This method is intentionally set to private as the registration
     * must occur when new relationship is created.
     *
     * @param incomingRelationship the incoming relationship
     */
    protected void addIncomingRelationship(RelationshipBase incomingRelationship) {
        this.incomingRelationships.add(incomingRelationship);
    }

    public List<? extends RelationshipBase> getIncomingRelationships() {
        return Collections.unmodifiableList(this.incomingRelationships);
    }

    /**
     * Get incoming relationships for a given type that are attached to this table.
     *
     * @param type the type of the relationships
     * @return list of relationships of specified type
     */
    public List<? extends RelationshipBase> getIncomingRelationships(final RelType type) {
        assert (type != null);
        List<RelationshipBase> filteredRelationships =
                this.incomingRelationships.stream()
                        .filter(rel -> rel.getType() == type)
                        .collect(Collectors.toList());

        return Collections.unmodifiableList(filteredRelationships);
    }

    private boolean hasMoreRelationships(final TableBase table,
                                         final RelType relType) {
        return !((TableBaseImpl) table).getRelationships(relType).isEmpty();
    }

    public TableBase getBaseTable(final RelType relType) {
        TableBase baseTable = this;
        // traverse from this Table back to its base Table along
        // specified relationship type
        while (hasMoreRelationships(baseTable, relType)) {
            baseTable = ((TableBaseImpl) baseTable).getRelationships(relType)
                    .get(0).getTarget();
        }
        return baseTable;
    }


    //
    // processing hierarchies
    //

    public List<? extends HierarchyBranchBase> getHierarchy() {
        return Collections.unmodifiableList(this.hierarchy);
    }

    @Override
    public HierarchyBranchBase createBranch(final String name,
                                            final boolean explicitlyDefined) {
        return new HierarchyBranchBase(name, this, explicitlyDefined);
    }

    @Override
    public void addBranch(final HierarchyBranchBase branch) {
        assert (branch.getParent() == this) :
                "Incorrect attempt to associate a branch that belongs to table " +
                        branch.getParent().getName() + " and not to this table " +
                        this.getName();
        this.hierarchy.add(branch);

        // linking the level column associations from cached attribute map
        // with level and column
        branch.initializeAssociatedColumns();
    }

    private List<? extends LevelBase> getDistinctLevels() {
        Set<LevelBase> distinctLevels = new LinkedHashSet<>();
        for (HierarchyBranchBase branch : this.getHierarchy()) {
            LevelBase currentLevel = branch.getRoot();
            while (currentLevel != null) {
                distinctLevels.add(currentLevel);
                currentLevel = currentLevel.getChild();
            }
        }
        return new ArrayList<>(distinctLevels);
    }

    public List<? extends LevelBase> getSortedDistinctLevels() {
        return Collections.unmodifiableList(getDistinctLevels());
    }

    public List<? extends LevelBase> getReverseSortedDistinctLevels() {
        List<? extends LevelBase> distinctLevels = getDistinctLevels();
        Collections.reverse(distinctLevels);
        return Collections.unmodifiableList(distinctLevels);
    }

    public LevelBase getDetailLevel() {
        TableBase baseTable = this.getBaseTable(RelType.SHRUNKEN_FROM);
        LevelBase detail = null;
        for (LevelBase level : ((TableBaseImpl) baseTable).getSortedDistinctLevels()) {
            if (level.isDetail()) {
                detail = level;
                break;
            }
        }
        return detail;
    }

    private boolean containsOneColumn(final List<? extends ColumnBase> columns) {
        boolean contains = false;
        for (ColumnBase column : columns) {
            if (this.getColumns().get(column.getName()) != null) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    /**
     * Mostly relevant for shrunken dimensions; this method will return
     * the Detail level for all base tables
     *
     * @return the level of the table
     */
    public LevelBase getAssociatedLevel() {
        TableBase baseTable = this.getBaseTable(RelType.SHRUNKEN_FROM);
        boolean isShrunkenDimension = (baseTable != this);
        int countTablesBetween = countTablesBetween(baseTable, RelType.SHRUNKEN_FROM);
        LevelBase associatedLevel = null;
        if (isShrunkenDimension) {
            List<? extends LevelBase> levels =
                    ((TableBaseImpl) baseTable).getReverseSortedDistinctLevels();
            //assert(levels.size()>countTablesBetween);
            for (LevelBase level : levels.subList(1, levels.size())) {
                if (containsOneColumn(level.getDrillColumns())) {
                    associatedLevel = level;
                    break;
                }
            }
            if (levels.size() <= countTablesBetween) {
                String msg = errorWarningMessages.formatMessage(84020,
                        ERROR_MESSAGE_84020, this.getClass(),
                        baseTable.getName(), this.getName());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                // set default value "Detail"
                if (associatedLevel == null) {
                    associatedLevel = ((TableBaseImpl) baseTable).getDetailLevel();
                }
            }
        } else {
            associatedLevel = ((TableBaseImpl) baseTable).getDetailLevel();
        }
        return associatedLevel;
    }

}
