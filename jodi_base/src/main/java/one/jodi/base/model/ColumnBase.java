package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.annotation.ColumnAnnotations;
import one.jodi.base.service.metadata.ColumnMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter of the DatStoreColumn metadata. This allows the extension of the
 * class to reflect specific aspects.
 *
 */
public class ColumnBase implements ModelNode {

    private final TableBase parent;
    private final String name;
    private final String dataType;
    private final int position;
    private final int precision;
    private final int scale;
    private final boolean isNullable;

    private final Optional<ColumnAnnotations> columnAnnotations;

    private List<LevelBase> associatedLevel = new ArrayList<>();

    protected ColumnBase(final ColumnMetaData columnData, final TableBase parent,
                         final Optional<ColumnAnnotations> columnAnnotations,
                         final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.parent = parent;
        this.name = columnData.getName();
        this.dataType = columnData.getColumnDataType();
        this.position = columnData.getPosition();
        this.precision = columnData.getLength();
        this.scale = columnData.getScale();
        this.isNullable = !columnData.hasNotNullConstraint();
        this.columnAnnotations = columnAnnotations;
    }

    // used for creating Alias columns
    protected ColumnBase(final ColumnBase columnBase,
                         final TableBase parent,
                         final Optional<ColumnAnnotations> columnAnnotations,
                         final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.parent = parent;
        this.name = columnBase.name;
        this.dataType = columnBase.dataType;
        this.position = columnBase.position;
        this.precision = columnBase.precision;
        this.scale = columnBase.scale;
        this.isNullable = columnBase.isNullable;
        this.columnAnnotations = columnAnnotations;
    }

    //
    // Functionality offered by Adaptee of Adapter pattern. It is acceptable to
    // remove that is not required for the intended purpose or modify existing
    // functionality
    //

    public String getFullName() {
        return getParent().getParent().getName() + "." +
                getParent().getName() + "." + getName();
    }

    public String getName() {
        return this.name;
    }

    public String getDataType() {
        return this.dataType;
    }

    public int getPrecision() {
        return this.precision;
    }

    public int getScale() {
        return this.scale;
    }

    public boolean isNullable() {
        return this.isNullable;
    }

    public String getDescription() {
        return this.columnAnnotations.isPresent() &&
                this.columnAnnotations.get().getDescription().isPresent()
                ? this.columnAnnotations.get().getDescription().get()
                : "";
    }

    /**
     * contains the additional meta data in the column comment that exist before
     * the meta data separator, e.g. '---'.
     *
     * @return businessName name used by the end user
     */
    public String getBusinessName() {
        return this.columnAnnotations.isPresent() &&
                this.columnAnnotations.get().getBusinessName().isPresent()
                ? this.columnAnnotations.get().getBusinessName().get()
                : "";
    }

    /**
     * contains the additional abbreviation in the table comment that exist before
     * the meta data separator and matches the abbreviation pattern, e.g. '((*))$'.
     *
     * @return abbreviatedBusinessname abbreviation of the name used by the end user.
     */
    public String getAbbreviatedBuisnessName() {
        return this.columnAnnotations.isPresent() &&
                this.columnAnnotations.get().getAbbreviatedBusinessName().isPresent()
                ? this.columnAnnotations.get().getAbbreviatedBusinessName().get()
                : "";
    }

    public int getPosition() {
        return this.position;
    }


    //
    // Insert Additional Functionality Below This Section
    //

    public TableBase getParent() {
        return parent;
    }

    public boolean isFkColumn() {
        return parent.getFks()
                .stream()
                .filter(fk -> fk.getFKColumns().contains(this))
                .findFirst()
                .isPresent();
    }

    public List<? extends FkRelationshipBase> getAssociatedFks() {
        List<FkRelationshipBase> found =
                parent.getFks().stream()
                        .filter(fk -> fk.getFKColumns().contains(this))
                        .collect(Collectors.toList());
        return found;
    }

    public List<? extends FkRelationshipBase> getAssociatedFksUsingNamingColumn() {
        List<FkRelationshipBase> found =
                parent.getFks().stream()
                        .filter(fk -> fk.getFKColumns().get(0) == this)
                        .collect(Collectors.toList());
        return found;
    }

    public boolean belongsToPK() {
        boolean belongs = false;
        if (parent.getPrimaryKey() != null &&
                parent.getPrimaryKey().getColumns() != null &&
                parent.getPrimaryKey().getColumns().contains(this)) {
            belongs = true;
        }
        return belongs;
    }

    //
    // Hierarchies
    //

    public List<? extends LevelBase> getAssociatedLevels() {
        return Collections.unmodifiableList(this.associatedLevel);
    }

    private boolean assertions(final LevelBase level) {
        boolean valid = true;
        if (!this.associatedLevel.isEmpty()) {
            valid = this.associatedLevel.get(0).getName()
                    .equalsIgnoreCase(level.getName());
        }
        return valid;
    }

    //only to be called when initializing level
    protected void addAssociatedLevel(final LevelBase level) {
        assert (assertions(level)) :
                "Expected that this method adds levels with for the same name. Error " +
                        "occurs at column " + this.getName() + " while attemping to associate " +
                        level.getName();
        this.associatedLevel.add(level);
    }

}
