package one.jodi.etl.builder.impl;

import one.jodi.etl.builder.SequenceEnrichmentBuilder;
import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.internalmodel.Sequences;
import one.jodi.etl.internalmodel.impl.NativeSequenceImpl;
import one.jodi.etl.internalmodel.impl.SpecificSequenceImpl;
import one.jodi.etl.internalmodel.impl.StandardSequenceImpl;

public class SequenceEnrichmentBuilderImpl implements SequenceEnrichmentBuilder {
    @Override
    public void enrich(Sequences internalSequences) {
        internalSequences.getSequences().forEach(s -> enrich(s));
    }

    public void enrich(Sequence internalSequence) {
        // Stub for future use.
        if (internalSequence.getGlobal() == null && internalSequence instanceof StandardSequenceImpl) {
            ((StandardSequenceImpl) internalSequence).setGlobal(false);
        }
        if (internalSequence.getGlobal() == null && internalSequence instanceof SpecificSequenceImpl) {
            ((SpecificSequenceImpl) internalSequence).setGlobal(false);
        }
        if (internalSequence.getGlobal() == null && internalSequence instanceof NativeSequenceImpl) {
            ((NativeSequenceImpl) internalSequence).setGlobal(false);
        }
        if (internalSequence.getIncrement() == null && internalSequence instanceof StandardSequenceImpl) {
            ((StandardSequenceImpl) internalSequence).setIncrement(1);
        }
        if (internalSequence.getIncrement() == null && internalSequence instanceof SpecificSequenceImpl) {
            ((SpecificSequenceImpl) internalSequence).setIncrement(1);
        }
        if (internalSequence.getIncrement() == null && internalSequence instanceof NativeSequenceImpl) {
            ((NativeSequenceImpl) internalSequence).setIncrement(1);
        }
    }
}