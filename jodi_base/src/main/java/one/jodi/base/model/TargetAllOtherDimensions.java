package one.jodi.base.model;

import java.util.Optional;

public class TargetAllOtherDimensions<T> extends TargetDefinition<T> {

    public TargetAllOtherDimensions(final T annotationValue) {
        super(null, Optional.empty(), annotationValue);
    }

    @Override
    public FkRelationshipBase getTargetFk() {
        throw new UnsupportedOperationException("Not supported for this subclass");
    }

    @Override
    public Optional<FkRelationshipBase> getOutriggerFk() {
        throw new UnsupportedOperationException("Not supported for this subclass");
    }

    @Override
    public String getTargetDimensionName() {
        return "_All Other Dimensions_";
    }

}
