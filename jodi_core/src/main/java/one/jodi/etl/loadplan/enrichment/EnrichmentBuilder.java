package one.jodi.etl.loadplan.enrichment;

import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStep;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanTree;

import java.util.List;

/**
 * Actionrunner to create loadplans from textual specifications.
 */
public interface EnrichmentBuilder {
    /**
     * Enrich the internal model with default values.
     *
     * @param internalloadPlan the internal loadplan to enrich
     */
    void enrich(List<LoadPlanTree<LoadPlanStep>> internalloadPlan);

}
