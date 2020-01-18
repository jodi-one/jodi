package one.jodi.etl.builder;

import one.jodi.etl.internalmodel.Variables;

@FunctionalInterface
public interface VariableEnrichmentBuilder {

    void enrich(Variables internalVariables);

}
