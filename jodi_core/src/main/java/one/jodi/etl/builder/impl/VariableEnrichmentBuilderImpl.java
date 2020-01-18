package one.jodi.etl.builder.impl;

import one.jodi.etl.builder.VariableEnrichmentBuilder;
import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.internalmodel.Variables;
import one.jodi.etl.internalmodel.impl.VariableImpl;

public class VariableEnrichmentBuilderImpl implements VariableEnrichmentBuilder {

    @Override
    public void enrich(
            Variables internalVariables) {
        internalVariables.getVariables().forEach(internalVar -> {
            if (internalVar.getGlobal() == null) {
                ((VariableImpl) internalVar).setGlobal(false);
            }
            if (internalVar.getKeephistory() == null) {
                ((VariableImpl) internalVar)
                        .setKeephistory(Variable.Keephistory.LATEST_VALUE);
            }
        });
    }
}
