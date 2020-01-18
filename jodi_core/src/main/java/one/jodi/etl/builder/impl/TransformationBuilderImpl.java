package one.jodi.etl.builder.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.impl.TransformationImpl;
import one.jodi.core.model.visitors.DepthFirstTraverserImpl;
import one.jodi.core.model.visitors.TraversingVisitor;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.internalmodel.Transformation;

/**
 * Implementation of Dataset interface.
 *
 */
public class TransformationBuilderImpl implements TransformationBuilder {

    private final ErrorWarningMessageJodi errorWarningMessages;
    private final JodiProperties properties;

    @Inject
    public TransformationBuilderImpl(final ErrorWarningMessageJodi errorWarningMessages,
                                     final JodiProperties properties) {
        this.errorWarningMessages = errorWarningMessages;
        this.properties = properties;
    }

    @Override
    public Transformation transmute(one.jodi.core.model.Transformation transformation, int packageSequence) {
        TransmutingVisitor transmutingVisitor = new TransmutingVisitor(this.errorWarningMessages, "Init ", this.properties);
        TraversingVisitor tv = new TraversingVisitor(new DepthFirstTraverserImpl(), transmutingVisitor);
        tv.setTraverseFirst(true);

        ((TransformationImpl) transformation).accept(tv);

        return transmutingVisitor.getTransformation(packageSequence);
    }


}
