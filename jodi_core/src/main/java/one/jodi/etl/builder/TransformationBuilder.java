package one.jodi.etl.builder;

import one.jodi.etl.internalmodel.Transformation;

public interface TransformationBuilder {

    public Transformation transmute(one.jodi.core.model.Transformation transformation, int packageSequence);

}
