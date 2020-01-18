package one.jodi.etl.builder;

public interface SequenceTransformationBuilder {
    one.jodi.core.sequences.Sequences transmute(one.jodi.etl.internalmodel.Sequences internalSequences);

    one.jodi.etl.internalmodel.Sequences transmute(one.jodi.core.sequences.Sequences sequences);
}
