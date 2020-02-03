package one.jodi.base.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A hierarchy branch represents a path from the Grand Total level to the
 * Detail level. In effect, a hierarchy is defined as a set of all possible paths
 * from Grand Total to Detail.
 */
public class HierarchyBranchBase {
    private final TableBase parent;
    private final String hierarchyName;
    private final boolean nameExplicitlyDefined;
    private final List<LevelBase> levels = new ArrayList<>();
    private boolean isTimeDimension = false;

    protected HierarchyBranchBase(final String hierarchyName,
                                  final TableBase parent) {
        this(hierarchyName, parent, true);
    }

    protected HierarchyBranchBase(final String hierarchyName, final TableBase parent,
                                  final boolean nameExplicitlyDefined) {
        super();
        this.parent = parent;
        this.hierarchyName = hierarchyName;
        this.nameExplicitlyDefined = nameExplicitlyDefined;
    }

    protected void initializeAssociatedColumns() {
        levels.forEach(LevelBase::initializeAssociatedColumns);
    }

    public boolean isSame(final HierarchyBranchBase that) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.levels.size() != that.getLevels().size()) return false;

        boolean isSame = true;
        for (int i = 0; i < this.levels.size(); i++) {
            if (!levels.get(i).isSame(that.getLevels().get(i))) {
                isSame = false;
                break;
            }
        }
        return isSame;
    }

    public String getName() {
        return hierarchyName;
    }

    public TableBase getParent() {
        return this.parent;
    }

    public boolean isNameExplicitlyDefined() {
        return nameExplicitlyDefined;
    }

    public boolean isTimeDimension() {
        return this.isTimeDimension;
    }

    protected LevelBase createLevel(final ColumnBase nameColumn,
                                    final LevelBase lastLevel,
                                    final List<ColumnBase> drillColumns,
                                    final ColumnBase displayColumn,
                                    final List<ColumnBase> chronologicalColumns,
                                    final List<ColumnBase> associatedColumns,
                                    final Boolean skipLevel) {
        return new LevelBase(nameColumn.getName(), lastLevel, drillColumns,
                displayColumn, chronologicalColumns,
                associatedColumns, skipLevel);
    }

    public void addLevel(final ColumnBase nameColumn,
                         final List<ColumnBase> drillColumns,
                         final ColumnBase displayColumn,
                         final List<ColumnBase> chronologicalColumns,
                         final List<ColumnBase> associatedColumns,
                         final Boolean skipLevel) {
        // chain levels and place into list
        LevelBase lastLevel = levels.isEmpty() ? null : levels.get(levels.size() - 1);
        LevelBase level = createLevel(nameColumn, lastLevel, drillColumns,
                displayColumn, chronologicalColumns, associatedColumns,
                skipLevel);
        levels.add(level);
        if (!chronologicalColumns.isEmpty()) {
            this.isTimeDimension = true;
        }
    }

    public List<? extends LevelBase> getLevels() {
        return Collections.unmodifiableList(this.levels);
    }

    public List<? extends LevelBase> getReverseLevels() {
        final List<LevelBase> reverseLevels = new ArrayList<>();
        for (final LevelBase level : this.levels) {
            reverseLevels.add(0, level);
        }
        return Collections.unmodifiableList(reverseLevels);
    }

    public LevelBase getRoot() {
        LevelBase root = null;
        if (!levels.isEmpty()) {
            root = levels.get(0);
        }
        return root;
    }

    public LevelBase getDetail() {
        LevelBase detail = null;
        if (!levels.isEmpty()) {
            detail = levels.get(levels.size() - 1);
        }
        assert (detail != null && detail.isDetail()) :
                "This level is not a Detail: " + (detail != null ? detail.getName() : "NULL");
        return detail;
    }

    public int size() {
        return levels.size();
    }

    public LevelBase getLevel(final int index) {
        LevelBase detail = null;
        assert (levels.size() >= index && index > 0) : "Incorrect Index " + index;
        detail = levels.get(levels.size() - index);
        assert (detail != null);
        return detail;
    }

    //test and diagnosis purposes only
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String sep = "";
        for (LevelBase level : levels) {
            sb.append(sep)
                    .append(level.getDrillColumns().get(0).getParent().getName())
                    .append(":")
                    .append(level.getName());
            sep = " <- ";
        }
        return sb.toString();
    }

}
