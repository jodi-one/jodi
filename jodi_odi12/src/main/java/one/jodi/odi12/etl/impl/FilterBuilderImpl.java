package one.jodi.odi12.etl.impl;


import com.google.inject.Inject;
import one.jodi.base.annotations.DevMode;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.FilterBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.FilterComponent;
import oracle.odi.domain.mapping.component.ReusableMappingComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.topology.OdiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FilterBuilderImpl implements FilterBuilder {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(FilterBuilderImpl.class);
    private final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;

    @Inject
    protected FilterBuilderImpl(final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy,
                                final @DevMode Boolean devMode) {
        this.odiAccessStrategy = odiAccessStrategy;
    }


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.FilterBuilder#addFilter(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Source, boolean, one.jodi.odi12.etl.EtlOperators)
     */
    @Override
    public void addFilter(final MapRootContainer mapping, final Source source, final boolean journalized,
                          final EtlOperators etlOperators) throws AdapterException, MappingException,
            TransformationAccessStrategyException, ResourceNotFoundException {
        String filterExpr = source.getParent().translateExpression(source.getFilter());
        if (filterExpr == null || filterExpr.trim().length() == 0) {
            return;
        }
        boolean hasFlows = !etlOperators.getFlows(source).isEmpty();

        String key = FilterBuilder.getFilterKeyForSource(source, journalized);
        FilterComponent filterComponent = new FilterComponent(mapping, key);
        IMapComponent dsc = odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(), source);
        filterComponent.setFilterCondition(filterExpr);
        filterComponent.setName(key);
        MapExpression.ExecuteOnLocation location = mapFromExecutionLocationType(source.getFilterExecutionLocation());
        filterComponent.setExecuteOnHint(location);
        dsc.connectTo(filterComponent);
//		if(hasFlows) {
//			dsc.connectTo(filterComponent);
//			//filterComponent.connectTo(etlOperators.getFlows(source).get(etlOperators.getFlows(source).size() -1));
//		} else {
//			dsc.connectTo(filterComponent);
//		}

        etlOperators.addFilterComponent(key, filterComponent);

    }


    private MapExpression.ExecuteOnLocation mapFromExecutionLocationType(
            final ExecutionLocationtypeEnum executionLocation) {
        if (executionLocation == null) {
            return null;
        }
        switch (executionLocation) {
            case SOURCE:
                return MapExpression.ExecuteOnLocation.SOURCE;
            case TARGET:
                return MapExpression.ExecuteOnLocation.TARGET;
            default:
                return MapExpression.ExecuteOnLocation.STAGING;
        }
    }

}
