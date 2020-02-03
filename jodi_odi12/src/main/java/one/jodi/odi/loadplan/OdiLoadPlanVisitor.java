package one.jodi.odi.loadplan;

import java.util.Collection;

/**
 * Interface definition of the vistor pattern,
 * the visitor pattern is used to traverse the tree of an internal model.
 * <p>
 * On of the implementors uses this to transform an internal model to external model,
 * the reverse engineeer visitor.
 * <p>
 * Another implementor uses this to validate the internal model.
 * <p>
 * Antoher implementor uses this to create an OdiLoadPlan while traversing the internal model.
 */
public interface OdiLoadPlanVisitor<Odiloadplanstep> {

    OdiLoadPlanVisitor<Odiloadplanstep> visit(final OdiLoadPlanTree<Odiloadplanstep> tree);

    Collection<Odiloadplanstep> getChildren();

    void visit(final Odiloadplanstep data);

}