package one.jodi.etl.loadplan.enrichment;

import one.jodi.etl.service.loadplan.Visitor;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStep;

/**
 * Actionrunner to create loadplans from textual specifications.
 */
public interface EnrichmentVisitor extends Visitor<LoadPlanStep> {

    /**
     * Method to set default values if the values are not specified in XML.
     *
     * @param loadPlanStep the load plan step to be enriched
     */

    void enrich(LoadPlanStep loadPlanStep);

    void reset();

}
