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
     * @param externalTransformation
     * @param transformation
     */
    public void processPreEnrichment(one.jodi.core.model.Transformation transformation, Mapping mapping, MappingHolder mappingHolder);

    /**
     * The external transformation is the class to be enriched, the Transformation class
     * contains non-enriched.
     * <p>
     * Partipants can chose to modify externalTransformation, report on issues.
     *
     * @param externalTransformation
     * @param transformation
     * @param mode
     */
    public void processPostEnrichment(one.jodi.core.model.Transformation externalTransformation, Transformation transformation, Mapping mapping, MappingHolder mappingHolder);

}
