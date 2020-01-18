package one.jodi.etl.service.loadplan.internalmodel;

import one.jodi.etl.service.loadplan.Visitor;

import java.util.LinkedHashSet;
import java.util.Set;

interface Visitable<T> {

    void accept(Visitor<T> visitor);

}

/**
 * Internal model representation of a LoadPlanTree.
 *
 */
public class LoadPlanTree<T> implements Visitable<T> {

    private final Set<LoadPlanTree<T>> children = new LinkedHashSet<>();

    private final T root;
    private final LoadPlanDetails loadPlanDetails;

    public LoadPlanTree(T data, final LoadPlanDetails loadPlanDetails) {
        this.root = data;
        this.loadPlanDetails = loadPlanDetails;
    }

    public T getRoot() {
        return root;
    }

    public LoadPlanTree<T> child(T data) {

        for (LoadPlanTree<T> child : children) {
            if (child.root == data) return child;
        }

        return child(new LoadPlanTree<>(data,
                null));

    }

    public LoadPlanTree<T> child(LoadPlanTree<T> child) {

        children.add(child);

        return child;

    }

    public void accept(Visitor<T> visitor) {

        visitor.visit(root);

        for (LoadPlanTree<T> child : children) {

            Visitor<T> childVisitor = visitor.visit(child);

            child.accept(childVisitor);

        }

    }

    public LoadPlanDetails getLoadPlanDetails() {
        return loadPlanDetails;
    }

    public Set<LoadPlanTree<T>> getChildren() {
        return children;
    }

}