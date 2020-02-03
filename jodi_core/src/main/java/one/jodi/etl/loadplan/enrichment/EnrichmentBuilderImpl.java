package one.jodi.etl.loadplan.enrichment;

import com.google.inject.Inject;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanStep;
import one.jodi.etl.service.loadplan.internalmodel.LoadPlanTree;

import java.util.List;

/**
 * Actionrunner to create loadplans from textual specifications.
 */
public class EnrichmentBuilderImpl implements EnrichmentBuilder {
    private final EnrichmentVisitor enrichmentVistor;

    @Inject
    public EnrichmentBuilderImpl(final EnrichmentVisitor enrichmentVistor) {
        this.enrichmentVistor = enrichmentVistor;
    }

    @Override
    public void enrich(List<LoadPlanTree<LoadPlanStep>> internalloadPlan) {
        this.enrichmentVistor.reset();
        for (LoadPlanTree<LoadPlanStep> exceptionOrRoot : internalloadPlan) {
            exceptionOrRoot.accept(enrichmentVistor);
            enrichmentVistor.visit(exceptionOrRoot);
        }
    }
}
