package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.service.metadata.Key;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KeyBase implements ModelNode {

    private static final Logger logger = LogManager.getLogger(KeyBase.class);

    private static final String ERROR_MESSAGE_84200 =
            "Operation only applies to key of type %s or %s.";

    protected final ErrorWarningMessageJodi errorWarningMessages;
    private final TableBase parent;
    private final String name;
    private final Type type;
    private final boolean isPrimaryKey;
    private final boolean isAlternativeKey;
    private final Set<FkRelationshipBase> incomingFks = new HashSet<>();
    private List<String> dataStoreKeyColumnNames; // is assigned null after use
    private List<ColumnBase> columns = null; // initialized in lazy fashion

    protected KeyBase(final TableBase parent, final String name,
                      final ErrorWarningMessageJodi errorWarningMessages) {
        this.parent = parent;
        this.name = name;
        this.dataStoreKeyColumnNames = null;
        this.type = Type.ALTERNATE;
        this.isPrimaryKey = false;
        this.isAlternativeKey = true;

        this.errorWarningMessages = errorWarningMessages;
    }

    protected KeyBase(final TableBase parent, final String name,
                      final Key keyData,
                      final ErrorWarningMessageJodi errorWarningMessages) {
        this.parent = parent;
        this.name = name;

        assert (keyData != null && parent != null);
        assert (keyData.getColumns() != null && !keyData.getColumns().isEmpty()) :
                "Invalid Key definition for " + keyData.getName();

        this.dataStoreKeyColumnNames = keyData.getColumns();
        this.type = mapKeyType(keyData.getType());
        this.isPrimaryKey = (this.type == Type.PRIMARY);
        this.isAlternativeKey = (this.type == Type.ALTERNATE);

        this.errorWarningMessages = errorWarningMessages;
    }

    protected KeyBase(final Key keyData, final TableBase parent,
                      final ErrorWarningMessageJodi errorWarningMessages) {
        this(parent, keyData.getName(), keyData, errorWarningMessages);
    }

    @Override
    public TableBase getParent() {
        return this.parent;
    }

    @Override
    public String getName() {
        return this.name;
    }

    private Type mapKeyType(final Key.KeyType keyType) {
        Type type = null;
        switch (keyType) {
            case PRIMARY:
                type = Type.PRIMARY;
                break;
            case ALTERNATE:
                type = Type.ALTERNATE;
                break;
            case INDEX:
                type = Type.INDEX;
                break;
        }
        assert (type != null);
        return type;
    }

    public Type getType() {
        return this.type;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean isAlternativeKey() {
        return isAlternativeKey;
    }

    private List<ColumnBase> mapColumns() {
        List<ColumnBase> columns = new ArrayList<>();
        for (String columnName : this.dataStoreKeyColumnNames) {
            ColumnBase column = getParent().getColumns().get(columnName);
            assert (column != null) : "Expected column " + columnName + " in table " +
                    getParent().getName() + " was not found.";
            columns.add(column);
        }
        return columns;
    }

    public synchronized List<? extends ColumnBase> getColumns() {
        if (this.columns == null) {
            this.columns = mapColumns();
            this.dataStoreKeyColumnNames = null; // release original name list
        }
        return Collections.unmodifiableList(this.columns);
    }

    public synchronized List<String> getColumnNames() {
        if (this.columns == null) {
            return Collections.emptyList();
        }
        return this.columns.stream()
                .map(ColumnBase::getName)
                .collect(Collectors.toList());
    }

    public Set<? extends FkRelationshipBase> getIncomingFks() {
        return Collections.unmodifiableSet(incomingFks);
    }

    public void addIncomingFks(final FkRelationshipBase incomingFk) {
        if (getType() == Type.PRIMARY || getType() == Type.ALTERNATE) {
            this.incomingFks.add(incomingFk);
        } else {
            String msg = errorWarningMessages.formatMessage(84200,
                    ERROR_MESSAGE_84200, this.getClass(), Type.PRIMARY,
                    Type.ALTERNATE);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new UnsupportedOperationException(msg);
        }
    }

    public enum Type {
        PRIMARY, ALTERNATE, INDEX
    }

}
