package one.jodi.odi12.etl.impl;

import com.google.inject.Inject;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi.etl.OdiCommon;
import one.jodi.odi12.etl.AggregateBuilder;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.AggregateComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class AggregateBuilderImpl implements AggregateBuilder {
    private final static Logger logger = LogManager.getLogger(AggregateBuilderImpl.class);
    private final OdiCommon odiCommon;

    @Inject
    protected AggregateBuilderImpl(final OdiCommon odiCommon) {
        this.odiCommon = odiCommon;
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.AggregateBuilder#setAggregate(one.jodi.etl.internalmodel.Transformation, java.util.List, int)
     */
    @Override
    public void setAggregate(final Transformation transformation, final List<AggregateComponent> aggregateComponents,
                             int dataSetNumber) throws AdapterException, MappingException {
        if (transformation.getMappings().isAggregateTransformation(dataSetNumber)) {
            logger.debug("Getting Aggregate component with datasetNumber: " + dataSetNumber);
            assert (dataSetNumber > 0) : "DatasetNumber starts with 1.";
            AggregateComponent agg = aggregateComponents.get(0);
            for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
                String epression = "";
                if (tc.getMappingExpressions().size() > 0) {
                    epression = transformation.getDatasets().get(dataSetNumber - 1).translateExpression(tc.getMappingExpressions().get(dataSetNumber - 1));
                } else {
                    epression = " null ";
                }
                MapAttribute ma = agg.addAttribute(
                        tc.getName(), epression, odiCommon.getOdiModel(transformation.getMappings().getModel())
                                .getLogicalSchema().getTechnology().getDataType(tc.getDataType()),
                        tc.getLength(), tc.getScale());
                boolean isa = tc.isAggregateColumn(dataSetNumber);
                AggregateComponent.IsGroupByColumn isGroupByColumn = tc.isAggregateColumn(dataSetNumber)
                        ? AggregateComponent.IsGroupByColumn.NO : AggregateComponent.IsGroupByColumn.YES;
                // or use// AggregateComponent.IsGroupByColumn.AUTO;
                agg.setIsGroupByColumn(ma, isGroupByColumn);
            }
        }
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.AggregateBuilder#addAggregate(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Transformation, one.jodi.odi12.etl.EtlOperators, int)
     */
    @Override
    public void addAggregate(final MapRootContainer mapping, final Transformation transformation,
                             final EtlOperators etlOperators, int dataSetNumber) throws AdapterException, MappingException {
        assert (dataSetNumber > 0) : "dataSetNumber starts with 1.";
        if (transformation.getMappings().isAggregateTransformation(dataSetNumber)) {
            logger.debug("Adding aggregate " + "D" + dataSetNumber + "_" + ComponentPrefixType.AGGREGATE.getAbbreviation());
            etlOperators.addAggregate(createAggregateComponent(mapping, "D" + dataSetNumber + "_" + ComponentPrefixType.AGGREGATE.getAbbreviation()));
        }
    }

    protected AggregateComponent createAggregateComponent(MapRootContainer mapping, String name) throws MappingException {
        return new AggregateComponent(mapping, name);
    }
}
