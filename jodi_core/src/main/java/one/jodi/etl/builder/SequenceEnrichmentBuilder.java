package one.jodi.etl.builder;

import one.jodi.etl.internalmodel.Sequences;

@FunctionalInterface
public interface SequenceEnrichmentBuilder {
    void enrich(Sequences internalSequences);
}
