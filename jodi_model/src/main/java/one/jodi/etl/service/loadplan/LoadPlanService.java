package one.jodi.etl.service.loadplan;

import one.jodi.core.lpmodel.Loadplan;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStep;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanTree;

import java.util.List;

/**
 * Tranforms an external model into an internal model,
 * and create an OdiLoadPlan from the internal model.
 */
public interface LoadPlanService {

    /**
     * @param loadplan
     * @return internal model representation of the model
     */
    public List<LoadPlanTree<LoadPlanStep>> transform(Loadplan loadplan);

    /**
     * Creates an LoadPlan in ODI.
     *
     * @param loadPlan internal loadplan
     */
    void build(List<LoadPlanTree<LoadPlanStep>> pInteralLoadPlans, LoadPlanDetails loadPlanDetails);
}