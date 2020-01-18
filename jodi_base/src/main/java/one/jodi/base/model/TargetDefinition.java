package one.jodi.base.model;

import java.util.Optional;

public class TargetDefinition<T> {
    private final FkRelationshipBase targetFk;
    private final Optional<FkRelationshipBase> outriggerFk;
    private final T annotationValue;

    TargetDefinition(final FkRelationshipBase targetFk,
                     final Optional<FkRelationshipBase> outriggerFk,
                     final T annotationValue) {
        super();
        assert (outriggerFk != null);
        this.targetFk = targetFk;
        this.outriggerFk = outriggerFk;
        this.annotationValue = annotationValue;
    }

    public FkRelationshipBase getTargetFk() {
        return this.targetFk;
    }

    public Optional<FkRelationshipBase> getOutriggerFk() {
        return this.outriggerFk;
    }

    public T getAnnotationValue() {
        return annotationValue;
    }

    public String getTargetDimensionName() {
        return this.targetFk.getReferencedPrimaryKey().getParent().getName();
    }

}
