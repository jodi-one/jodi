package one.jodi.etl.service.loadplan;

import one.jodi.etl.service.loadplan.internalmodel.LoadPlanTree;

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
public interface Visitor<T> {

    Visitor<T> visit(final LoadPlanTree<T> tree);

    Collection<T> getChildren();

    void visit(final T data);

    void visitVariables(LoadPlanTree<T> pInteralLoadPlan);

}