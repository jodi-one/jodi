package one.jodi.odi.loadplan;

import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;

import java.util.LinkedHashSet;
import java.util.Set;

interface Visitable<T> {

    void accept(OdiLoadPlanVisitor<T> visitor);

}

/**
 * Internal model representation of a LoadPlanTree.
 *
 */
public class OdiLoadPlanTree<T> implements Visitable<T> {

    private final Set<OdiLoadPlanTree<T>> children = new LinkedHashSet<>();

    private final T root;
    private final LoadPlanDetails loadPlanDetails;
    private final String projectCode;
    private final OdiLoadPlan odiLoadPlan;

    public OdiLoadPlanTree(T data,
                           final OdiLoadPlan odiLoadPlan,
                           final LoadPlanDetails loadPlanDetails,
                           final String projectCode) {
        assert odiLoadPlan != null;
        assert loadPlanDetails != null;
        assert projectCode != null;
        this.root = data;
        this.loadPlanDetails = loadPlanDetails;
        this.projectCode = projectCode;
        this.odiLoadPlan = odiLoadPlan;
    }

    public T getRoot() {
        return root;
    }

    public OdiLoadPlanTree<T> child(T data) {

        for (OdiLoadPlanTree<T> child : children) {
            if (child.root == data) return child;
        }

        return child(new OdiLoadPlanTree<>(data,
                odiLoadPlan, this.loadPlanDetails, projectCode));

    }

    public OdiLoadPlanTree<T> child(OdiLoadPlanTree<T> child) {

        children.add(child);

        return child;

    }

    public void accept(OdiLoadPlanVisitor<T> visitor) {

        visitor.visit(root);

        for (OdiLoadPlanTree<T> child : children) {

            OdiLoadPlanVisitor<T> childVisitor = visitor.visit(child);

            child.accept(childVisitor);

        }

    }

    public LoadPlanDetails getLoadPlanDetails() {
        return loadPlanDetails;
    }

    public void accept(OdiLoadPlanTransformationService transFormationvisitor, OdiLoadPlan odiLoadPlan,
                       LoadPlanDetails originalLoadPlanDetails, String projectCode) {
        // TODO Auto-generated method stub

    }

    public Set<OdiLoadPlanTree<T>> getChildren() {
        return children;
    }

}