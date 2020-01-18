package one.jodi.core.validation.sequences;

import one.jodi.etl.internalmodel.Sequences;

@FunctionalInterface
public interface SequenceValidator {

    boolean validate(Sequences internalSequences);
}
