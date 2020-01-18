package one.jodi.odi12.etl.impl;

import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.SetBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.SetComponent;
import oracle.odi.domain.mapping.exception.MappingException;


public class SetBuilderImpl implements SetBuilder {


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.SetBuilder#addSetComponent(one.jodi.etl.internalmodel.Transformation, oracle.odi.domain.mapping.MapRootContainer, boolean, one.jodi.odi12.etl.EtlOperators)
     */
    @Override
    public void addSetComponent(final Transformation transformation, final MapRootContainer mapping,
                                final boolean useExpressions, final EtlOperators etlOperators) throws AdapterException, MappingException {
        if (transformation.getDatasets().size() > 1) {
            SetComponent sc = createSetComponent(mapping);
            //SetComponent sc = new SetComponent(mapping, ComponentPrefixType.SETCOMPONENT.getAbbreviation());
            for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
                // the translation is necessary since we prefix our source and
                // lookups
                // with the dataset D1 for dataset 1 and D2 for dataset 2 etc.
                //String[] translated = getTranslation(tc, transformation);
                String[] translated = {"", ""};
                sc.addSetAttribute(null, tc.getName(), translated);
            }
            etlOperators.addSetComponent(sc);
        }
    }


    protected SetComponent createSetComponent(final MapRootContainer mapping) throws MappingException {
        return new SetComponent(mapping, ComponentPrefixType.SETCOMPONENT.getAbbreviation());
    }

}
