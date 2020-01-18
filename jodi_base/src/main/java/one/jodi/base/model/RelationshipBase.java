package one.jodi.base.model;

public class RelationshipBase implements ModelNode {

    private final RelType type;
    private final TableBase parent;
    private final TableBase target;
    /**
     * Defines additional relationships that may exist between tables that are only
     * inferred indirectly using naming conventions or relationships that are
     * expressed based on sub-sets of columns.
     *
     * @author $Author$
     * @version $Revision$
     * @date $Date$
     */
    protected RelationshipBase(final TableBase parent, final TableBase target,
                               final RelType type) {
        super();
        this.parent = parent;
        this.target = target;
        this.type = type;
    }

    public RelType getType() {
        return type;
    }

    @Override
    public String getName() {
        return "";
    }

    public TableBase getParent() {
        return this.parent;
    }

    public TableBase getTarget() {
        return this.target;
    }

    public enum RelType {
        AGGREGATED_FROM, SHRUNKEN_FROM
    }

}
