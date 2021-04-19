package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.KeyBase.Type;
import one.jodi.base.service.metadata.ForeignReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FkRelationshipBase {

    private static final Logger logger =
            LogManager.getLogger(FkRelationshipBase.class);

    private static final String ERROR_MESSAGE_84300 =
            "Fk '%1$s' cannot be constructed: the referenced primary key " +
                    "is not attached to target table '%2$s'.";

    private static final String ERROR_MESSAGE_84310 =
            "Fk '%1$s' cannot be constructed: the referenced key '%2$s' at target " +
                    "table '%3$s' is an alternative key. Currently, a FK constraints must " +
                    "refer to a primary key.";

    private static final String ERROR_MESSAGE_84320 =
            "Fk '%1$s' cannot be constructed: the referenced key '%2$s' at target " +
                    "table '%3$s' defines %4$s columns while the Fk constraint defines " +
                    "%5$s columns.";
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final TableBase parent;
    private final String name;
    private final List<ForeignReference.RefColumns> referenceColumns;
    private final String primaryTableModel;
    private final String primaryTableName;
    private final Cardinality cardinality;
    private KeyBase referencedPrimaryKey = null;

    private FkRelationshipBase(final TableBase parent, final String name,
                               final List<ForeignReference.RefColumns> referenceColumns,
                               final Cardinality cardinality,
                               final String primaryTableModel,
                               final String primaryTableName,
                               final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        assert (parent != null && name != null && !name.equals("") && cardinality != null);
        this.name = name;
        this.parent = parent;
        this.referenceColumns = referenceColumns;
        this.cardinality = cardinality;
        this.primaryTableModel = primaryTableModel;
        this.primaryTableName = primaryTableName;

        this.errorWarningMessages = errorWarningMessages;
    }

    protected FkRelationshipBase(final TableBase parent, final String name,
                                 final ForeignReference fk,
                                 final Cardinality cardinality,
                                 final ErrorWarningMessageJodi errorWarningMessages) {
        this(parent, name, fk.getReferenceColumns(), cardinality,
                fk.getPrimaryKeyDataStoreModelCode(), fk.getPrimaryKeyDataStoreName(),
                errorWarningMessages);
    }

    // used only for alias References
    protected FkRelationshipBase(final TableBase parent, final String name,
                                 final Cardinality cardinality,
                                 final ErrorWarningMessageJodi errorWarningMessages) {
        this(parent, name, Collections.emptyList(), cardinality, null, null,
                errorWarningMessages);
    }

    public TableBase getParent() {
        return this.parent;
    }

    public String getName() {
        return this.name;
    }

    public TableBase getReferringTable() {
        return this.parent;
    }

    private boolean matchingColumns(final KeyBase key,
                                    final List<ColumnBase> fkColumns) {
        assert (fkColumns != null && key != null);
        if (fkColumns.size() != key.getColumns().size()) {
            return false;
        }

        Set<ColumnBase> columnSet = new HashSet<>(fkColumns);
        columnSet.removeAll(key.getColumns());
        return columnSet.isEmpty();
    }

    private List<ColumnBase> getReferencedPkColumns(final TableBase referencedTable) {
        List<ColumnBase> columns = new ArrayList<>();
        for (ForeignReference.RefColumns columnRef : this.referenceColumns) {
            String columnName = columnRef.getPrimaryKeyColumnName();
            ColumnBase column = referencedTable.getColumns().get(columnName);
            assert (column != null) : "Expected column " + columnName +
                    " was not found for " + this.name;
            columns.add(column);
        }
        return Collections.unmodifiableList(columns);
    }

    public KeyBase getReferencedPrimaryKey() {
        //lazy linking of referenced table to avoid recursion in Table creation
        if (referencedPrimaryKey == null) {
            SchemaBase schema = this.getParent().getParent();
            TableBase referencedTable = schema.findTable(schema.getName(),
                    this.primaryTableName);

            KeyBase primaryKey = referencedTable.getPrimaryKey();
            KeyBase altKey = referencedTable.getAlternateKey();
            List<ColumnBase> refColumns = getReferencedPkColumns(referencedTable);

            KeyBase key = null;
            if (primaryKey != null && matchingColumns(primaryKey, refColumns)) {
                key = primaryKey;
            } else if (altKey != null && matchingColumns(altKey, refColumns)) {
                key = altKey;
            }

            // The following checks are introduced to detect incorrectly formed
            // ODI-based DB constraints, They are possible due to the lack of
            // ODI enforcement of such constraints
            if (key == null) {
                String msg = errorWarningMessages.formatMessage(84300,
                        ERROR_MESSAGE_84300, this.getClass(),
                        this.name, referencedTable.getName());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new MalformedModelException(getParent(), msg);
            } else if (key.getType() != Type.PRIMARY) {
                String msg = errorWarningMessages.formatMessage(84310,
                        ERROR_MESSAGE_84310, this.getClass(),
                        this.name, key.getName(),
                        referencedTable.getName());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new MalformedModelException(getParent(), msg);
            } else if (key.getColumns().size() != this.referenceColumns.size()) {
                String msg = errorWarningMessages.formatMessage(84320,
                        ERROR_MESSAGE_84320, this.getClass(), this.name,
                        key.getName(), referencedTable.getName(),
                        key.getColumns().size(), this.referenceColumns.size());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new MalformedModelException(getParent(), msg);
            }

            //add this as incoming FK to PK of referred table
            key.addIncomingFks(this);
            this.referencedPrimaryKey = key;
            logger.debug("Fk: " + getParent().getName() + "." + this.getName() +
                    " to Pk: " + key.getParent().getName() + "." +
                    key.getName());
        }
        return referencedPrimaryKey;
    }

    public List<? extends ColumnBase> getFKColumns() {
        List<ColumnBase> columns = new ArrayList<>();
        for (ForeignReference.RefColumns columnRef : this.referenceColumns) {
            String columnName = columnRef.getForeignKeyColumnName();
            ColumnBase column = getParent().getColumns().get(columnName);
            assert (column != null) : "Expected column " + columnName +
                    " was not found for " + this.name;
            columns.add(column);
        }
        return Collections.unmodifiableList(columns);
    }

    protected void remove() {
        this.parent.remove(this);
    }

    public List<String> getFkColumnNames() {
        return getFKColumns().stream().map(ColumnBase::getName)
                .collect(Collectors.toList());
    }

    public boolean hasMatchingColumnNames(final FkRelationshipBase other) {

        boolean matching = false;
        if (this.getFKColumns().size() == other.getFKColumns().size()) {
            matching = true;

            //setup map to compare columns names
            Map<String, ColumnBase> otherFkColumnMap = new HashMap<>();
            for (ColumnBase otherC : other.getFKColumns()) {
                otherFkColumnMap.put(otherC.getName().toUpperCase(), otherC);
            }
            //match FK column names
            for (ColumnBase thisColumn : this.getFKColumns()) {
                if (otherFkColumnMap.get(thisColumn.getName().toUpperCase()) == null) {
                    matching = false;
                    break;
                }
            }
        }
        return matching;
    }

    public Cardinality getCardinality() {
        return (this.cardinality != null) ? cardinality : Cardinality.MANY_TO_ONE;
    }

    public enum Cardinality {
        ZERO_OR_ONE_TO_ONE("zero or one to one"),    // 0..1:1
        ONE_TO_ONE("one to one"),            //    1:1
        MANY_TO_ZERO_OR_ONE("many to zero or one"),   //    *:0..1
        MANY_TO_ONE("many to one");           //    *:1

        private static final Map<String, Cardinality> lookup = new HashMap<>();

        static {
            for (Cardinality c : Cardinality.values()) {
                lookup.put(c.getName(), c);
            }
        }

        private final String cardinality;

        Cardinality(final String cardinality) {
            this.cardinality = cardinality;
        }

        public static Cardinality get(final String cardinality) {
            return lookup.get(cardinality);
        }

        public String getName() {
            return cardinality;
        }
    }

}