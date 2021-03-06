package one.jodi.odi.loadplan;

import one.jodi.etl.service.loadplan.internalmodel.LoadPlanDetails;

public interface OdiLoadPlanValidationService {
    /**
     * Validate the loadplan details, like name and folder.
     *
     * @param pLoadPlanDetails
     */
    public boolean validate(final LoadPlanDetails pLoadPlanDetails);

    /**
     * Validate the LoadPlanTree.
     *
     * @param pLoadPlanTree
     * @return indicating valid or not
     */
    public boolean validate(final OdiLoadPlanTree<Odiloadplanstep> pLoadPlanTree);

    void reset(String loadPlanName);
}