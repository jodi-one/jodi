package one.jodi.tools;


import one.jodi.etl.internalmodel.Transformation;
import one.jodi.tools.dependency.MappingHolder;
import oracle.odi.domain.mapping.Mapping;

public interface ModelBuildingStep {

    /**
     * The external transformation is the class to be enriched, the Transformation class
     * contains non-enriched.
     * <p>
     * Partipants can chose to modify externalTransformation, report on issues.
     *
     * @param transformation transformation to pre enrich
     * @param mapping mapping to pre enrich
     * @param mappingHolder mapping holder object
     */
    public void processPreEnrichment(one.jodi.core.model.Transformation transformation, Mapping mapping, MappingHolder mappingHolder);

    /**
     * The external transformation is the class to be enriched, the Transformation class
     * contains non-enriched.
     * <p>
     * Partipants can chose to modify externalTransformation, report on issues.
     *
     * @param externalTransformation external transformation
     * @param transformation internal transformation
     * @param mapping odi maopping
     * @param mappingHolder mapping holder object
     */
    public void processPostEnrichment(one.jodi.core.model.Transformation externalTransformation, Transformation transformation, Mapping mapping, MappingHolder mappingHolder);

}
