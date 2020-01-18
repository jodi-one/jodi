package one.jodi.base.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelBase {
    private final String name;
    private final LevelBase levelParent;
    private final List<ColumnBase> drillColumns;
    private final ColumnBase displayColumn;
    private final List<ColumnBase> chronologicalColumns;
    private final List<ColumnBase> associatedColumns;
    private final Boolean skipLevel;
    private boolean initialized = false;
    private LevelBase child = null;

    protected LevelBase(final String name, final LevelBase levelParent,
                        final List<ColumnBase> drillColumns,
                        final ColumnBase displayColumn,
                        final List<ColumnBase> chronologicalColumns,
                        final List<ColumnBase> associatedColumns,
                        final Boolean skipLevel) {
        assert (name != null && !name.trim().isEmpty());
        assert (drillColumns != null && associatedColumns != null);
        this.name = name;
        this.levelParent = levelParent;
        this.drillColumns = drillColumns;
        this.displayColumn = displayColumn;
        this.chronologicalColumns = chronologicalColumns;
        this.associatedColumns = associatedColumns;
        this.skipLevel = skipLevel;
        if (levelParent != null) {
            levelParent.child = this;
        }
    }

    @Override
    public boolean equals(final Object thatO) {
        if (this == thatO) return true;
        if (!(thatO instanceof LevelBase)) return false;
        LevelBase that = (LevelBase) thatO;
        return this.name.equals(that.name);
    }

    private String getKey(final ColumnBase column) {
        assert (column.getParent() != null);
        assert (column.getParent().getParent() != null);
        return column.getParent().getParent().getName() + "." +
                column.getParent().getName() + "." +
                column.getName();
    }

    public boolean isSame(final LevelBase that) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.getClass() != that.getClass()) return false;
        if (this.drillColumns.size() != that.getDrillColumns().size()) return false;

        Map<String, ColumnBase> thisColumns = new HashMap<>();
        for (ColumnBase column : this.drillColumns) {
            thisColumns.put(getKey(column), column);
        }

        boolean isSame = true;
        for (ColumnBase thatColumn : that.getDrillColumns()) {
            if (thisColumns.get(getKey(thatColumn)) == null) {
                isSame = false;
                break;
            }
        }
        return isSame;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (ColumnBase column : this.drillColumns) {
            hashCode = 31 * hashCode + getKey(column).hashCode();
        }
        return hashCode;
    }

    public String getName() {
        return this.name;
    }

    public LevelBase getLevelParent() {
        return this.levelParent;
    }

    public LevelBase getChild() {
        return this.child;
    }

    public boolean isDetail() {
        return (this.child == null);
    }

    public List<? extends ColumnBase> getDrillColumns() {
        return Collections.unmodifiableList(this.drillColumns);
    }

    public ColumnBase getDisplayColumn() {
        return displayColumn;
    }

    public List<? extends ColumnBase> getChronologicalColumns() {
        return Collections.unmodifiableList(this.chronologicalColumns);
    }

    public List<? extends ColumnBase> getAssociatedColumns() {
        return Collections.unmodifiableList(associatedColumns);
    }

    protected boolean allowAddAssociateColumns(
            final List<? extends ColumnBase> associatedColumns) {
        return !initialized;
    }

    // This method must not be called after the physical hierarchies and levels
    // are derived and assigned to to tables
    public void addAssociatedColumns(final List<? extends ColumnBase> associatedColumns) {
        // assert modified to accommodate dimension measure feature
        assert (allowAddAssociateColumns(associatedColumns)) :
                "Called after initialization.";
        this.associatedColumns.addAll(associatedColumns);
    }

    public void initializeAssociatedColumns() {
        assert (!initialized);
        for (ColumnBase column : this.associatedColumns) {
            assert (column != null);
            column.addAssociatedLevel(this);
        }
        initialized = true;
    }

    public Boolean isSkipLevel() {
        return skipLevel;
    }
}
