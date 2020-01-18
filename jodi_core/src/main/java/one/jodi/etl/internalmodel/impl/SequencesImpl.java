package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.internalmodel.Sequences;

import java.util.Collection;

public class SequencesImpl implements Sequences {

    private final Collection<Sequence> sequences;

    public SequencesImpl(final Collection<Sequence> sequences) {
        this.sequences = sequences;
    }

    @Override
    public Collection<Sequence> getSequences() {
        return sequences;
    }
}
