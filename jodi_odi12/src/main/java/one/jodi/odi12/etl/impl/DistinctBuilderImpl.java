package one.jodi.odi12.etl.impl;

import com.google.inject.Inject;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi12.etl.DistinctBuilder;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.DistinctComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DistinctBuilderImpl implements DistinctBuilder {
    private static final Logger logger = LogManager.getLogger(DistinctBuilderImpl.class);
    private final OdiCommon odiCommon;


    @Inject
    protected DistinctBuilderImpl(final OdiCommon odiCommon) {
        this.odiCommon = odiCommon;
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.DistinctBuilder#addDistinct(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Transformation, one.jodi.odi12.etl.EtlOperators)
     */
    @Override
    public void addDistinct(final MapRootContainer mapping, final Transformation transformation,
                            final EtlOperators etlOperators) throws AdapterException, MappingException {
        if (transformation.getMappings().isDistinct()) {
            logger.debug("Setting interfaceoptions");
            logger.debug("Distinct operator:" + transformation.getMappings().isDistinct());
            etlOperators.addDistinctComponents(createDistinctComponent(mapping));
        }
    }


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.DistinctBuilder#setDistinct(one.jodi.etl.internalmodel.Transformation, java.util.List, boolean)
     */
    @Override
    public void setDistinct(final Transformation transformation, final List<DistinctComponent> distinctComponents,
                            final boolean useExpressions) throws AdapterException, MappingException {
        if (transformation.getMappings().isDistinct()) {
            for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
                String expression = "";
                int ij = tc.getParent().getParent().getMaxDatasetNumber();
                if (tc.getParent().getParent().getMaxDatasetNumber() > 1) {
                    expression = ComponentPrefixType.SETCOMPONENT.getAbbreviation() + "." + tc.getName();
                } else if (transformation.useExpressions()) {
                    expression = ComponentPrefixType.TARGET_EXPRESSIONS.getAbbreviation() + "." + tc.getName();
                } else {
                    try {
                        expression = getTranslation(tc, transformation)[0];
                    } catch (ArrayIndexOutOfBoundsException ai) {
                        // this is the case when expression is omitted
                        expression = null;
                    }
                }
                if (expression != null && expression.toLowerCase().contains("nextval")) {
                    expression = "";
                }

                distinctComponents.get(0).addAttribute(
                        tc.getName(), expression, odiCommon.getOdiModel(transformation.getMappings().getModel())
                                .getLogicalSchema().getTechnology().getDataType(tc.getDataType()),
                        tc.getLength(), tc.getScale());
            }
        }
    }

    /**
     * Get the internal representation of the expression of the column, for
     * instance if the alias of the source was SRC and the expression was
     * (SRC.KEY + 1) and there was only 1 Dataset this translates to (D1SRC.KEY
     * + 1)
     *
     * @param targetColumn
     * @param transformation
     * @return
     */
    private String[] getTranslation(final Targetcolumn targetColumn, final Transformation transformation) {
        String[] array = new String[targetColumn.getMappingExpressions().size()];
        targetColumn.getMappingExpressions().toArray(array);
        String[] translated = new String[targetColumn.getMappingExpressions().size()];
        for (int counter = 0; counter < array.length; counter++) {
            String expression = array[counter];
            translated[counter] = transformation.getDatasets().get(counter).translateExpression(expression);
        }
        return translated;
    }

    protected DistinctComponent createDistinctComponent(MapRootContainer mapping) throws MappingException {
        return new DistinctComponent(mapping, ComponentPrefixType.DISTINCT.getAbbreviation());
    }
}
