package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.StandardSequence;

public class StandardSequenceImpl extends SequenceImpl
        implements StandardSequence {

    public StandardSequenceImpl(final String name, final Integer increment,
                                final Boolean global) {
        super(name, increment, global);
    }

}
