package one.jodi.etl.service.loadplan;

/**
 * Import a OdiLoadPlan into an internal model representation and print or serialize it.
 */
public interface LoadPlanExportService {
    void exportLoadPlans(boolean useDefaultNames);

    void printLoadPlans(boolean useDefaultNames);
}
