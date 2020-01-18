package one.jodi.odi12.etl.impl;


import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.SubQuery.ExpressionSource;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.DatastoreBuilder;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.FlowsBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.IDataType;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.ReusableMapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.*;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.topology.OdiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class FlowsBuilderImpl implements FlowsBuilder {
    private final static Logger logger = LogManager.getLogger(FlowsBuilderImpl.class);
    private final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;
    private final JodiProperties properties;
    private final DatastoreBuilder datastoreBuilder;

    @Inject
    protected FlowsBuilderImpl(final DatastoreBuilder datastoreBuilder,
                               final OdiTransformationAccessStrategy<MapRootContainer,
                                       Dataset, DatastoreComponent, ReusableMappingComponent,
                                       IMapComponent, OdiContext,
                                       ILogicalSchema> odiAccessStrategy,
                               JodiProperties properties) {
        this.odiAccessStrategy = odiAccessStrategy;
        this.datastoreBuilder = datastoreBuilder;
        this.properties = properties;
    }


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.FlowsBuilder#addFlow(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Source, one.jodi.odi12.etl.EtlOperators, boolean)
     */
    @Override
    public void addFlow(MapRootContainer mapping, Source source, EtlOperators etlOperators, boolean journalized) throws AdapterException, ResourceNotFoundException, MappingException, TransformationAccessStrategyException, ResourceFoundAmbiguouslyException {
        if (source.getFlows() != null) {

            IMapComponent previous = odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(), source);
            if (source.getFilter().length() > 2) {
                // This will need to get connected when filter is later added.
                previous = null;
            }
            for (Flow flow : source.getFlows()) {
                IMapComponent next = null;
                if (flow instanceof Pivot) {
                    next = addPivot(mapping, etlOperators, (Pivot) flow, journalized);
                } else if (flow instanceof UnPivot) {
                    next = addUnpivot(mapping, etlOperators, (UnPivot) flow, journalized);
                } else if (flow instanceof SubQuery) {
                    next = addSubQuery(mapping, etlOperators, (SubQuery) flow, journalized);
                }

                if (previous != null) previous.connectTo(next);
                previous = next;
            }
        }
    }

    private IMapComponent addSubQuery(final MapRootContainer mapping, EtlOperators etlOperators, final SubQuery subQuery, boolean journalized) throws AdapterException, MappingException, ResourceNotFoundException, TransformationAccessStrategyException, ResourceFoundAmbiguouslyException {

        String key = getSubQueryKeyForSource(subQuery, journalized);
        SubqueryFilterComponent component = createSubqueryFilterComponent(mapping, key);
        Source source = subQuery.getParent();
        one.jodi.etl.internalmodel.Dataset dataset = source.getParent();

        if (subQuery.isTemporary()) {
            String folder = source.getParent().getParent().getFolderName();
            ReusableMapping reusable = (ReusableMapping) odiAccessStrategy
                    .findMappingsByName(subQuery.getFilterSource(), folder, properties.getProjectCode());
            assert (reusable != null);
            IMapComponent dataStoreComponent = datastoreBuilder.createComponent(mapping, reusable, false);
            dataStoreComponent.connectTo(component.getSubqueryFilterConnectorPoint());
        } else {
            OdiDataStore subqueryDataStore = odiAccessStrategy.findDataStore(subQuery.getFilterSource(),
                    subQuery.getFilterSourceModel());

            assert (subqueryDataStore != null);

            IMapComponent dataStoreComponent = datastoreBuilder.createComponent(mapping, subqueryDataStore, false);
            //dataStoreComponent.connectTo(component);
            dataStoreComponent.connectTo(component.getSubqueryFilterConnectorPoint());
        }


        component.setName(subQuery.getName());
        if (subQuery.getCondition() != null)
            component.setSubqueryFilterCondition(dataset.translateExpression(subQuery.getCondition()));

        if (subQuery.getGroupComparison() != GroupComparisonEnum.NONE)
            component.setGroupComparisonCondition(subQuery.getGroupComparison().getValue());
        logger.debug("ROLE " + subQuery.getRole().getValue() + " " + subQuery.getRole().name());
        //component.setSubqueryFilterInputRole(SubqueryFilterComponent.SUBQUERY_FILTER_INPUT_ROLE_GREATER);
        component.setSubqueryFilterInputRole(subQuery.getRole().getValue());


        for (OutputAttribute oa : subQuery.getOutputAttributes()) {
            String attributeExpression = dataset.translateExpression(oa.getExpressions().get(ExpressionSource.DRIVER.name()));
            String comparisonExpression = oa.getExpressions().get(ExpressionSource.FILTER.name()) != null ?
                    dataset.translateExpression(oa.getExpressions().get(ExpressionSource.FILTER.name())) :
                    null;

            MapAttribute attribute = component.addAttribute(oa.getName(), attributeExpression, comparisonExpression, null, null, null);
            logger.debug("Added attribute " + attribute.getName() + " with expression " + attribute.getExpression());
        }

        component.setExecuteOnHint(mapFromExecutionLocationType(subQuery.getExecutionLocation()));

        etlOperators.addFlowItem(subQuery.getParent(), component);
        return component;

    }


    private IMapComponent addPivot(final MapRootContainer mapping, EtlOperators etlOperators, final Pivot pivot, boolean journalized) throws AdapterException, MappingException, ResourceNotFoundException, TransformationAccessStrategyException {

        String key = getPivotKeyForSource(pivot, journalized);
        PivotComponent pivotComponent = createPivotComponent(mapping, key);
        Source source = pivot.getParent();
        IMapComponent dsc = odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(), source);
        pivotComponent.setName(pivot.getName());

        String rowLocator = source.getParent().translateExpression(pivot.getRowLocator());
        logger.debug("Using row locator expression '" + rowLocator + "'");

        pivotComponent.setAggregateFunction(pivot.getAggregateFunction().getValue());
        //dsc.connectTo(pivotComponent);
        for (OutputAttribute oa : pivot.getOutputAttributes()) {
            IDataType type = getAttributeType(dsc, rowLocator.substring(rowLocator.indexOf('.') + 1, rowLocator.length()));
            Integer size = null;
            Integer scale = null;

            // Validated so that a single value->expression pair is contained.
            for (String value : oa.getExpressions().keySet()) {
                String attributeExpression = source.getParent().translateExpression(oa.getExpressions().get(value));
                logger.debug(oa.getName() + " -> " + attributeExpression);

                MapAttribute attribute = pivotComponent.addAttribute(oa.getName(), attributeExpression, type, size, scale);
                attribute.setPivotMatchingRow(value);
                pivotComponent.addRowLocatorValue(value);
            }

        }

        //pivotComponent.setRowLocator(rowLocator);

        logger.debug("adding FLOW item " + pivotComponent.getName());

        etlOperators.addFlowItem(pivot.getParent(), pivotComponent);
        return pivotComponent;

    }


    private IMapComponent addUnpivot(final MapRootContainer mapping, EtlOperators etlOperators, final UnPivot unpivot, boolean journalized)
            throws AdapterException, MappingException, ResourceNotFoundException, TransformationAccessStrategyException {

        String key = getUnPivotKeyForSource(unpivot, journalized);
        UnpivotComponent unpivotComponent = createUnpivotComponent(mapping, key);
        Source source = unpivot.getParent();
        IMapComponent dsc = odiAccessStrategy.findSourceComponent(mapping, source.getParent().getParent(), source);
        unpivotComponent.setName(unpivot.getName());
        String rowLocator = source.getParent().translateExpression(unpivot.getRowLocator());

        for (OutputAttribute oa : unpivot.getOutputAttributes()) {
            IDataType type = getAttributeType(dsc, rowLocator);
            Integer size = null;
            Integer scale = null;

            if (oa.hasQualifiedExpressions()) {
                // pivoted column
                unpivotComponent.addAttribute(oa.getName(), null, type, size, scale);
            } else {
                // non-pivoted column
                String attributeExpression = oa.getExpressions().get(null);
                if (attributeExpression != null) {
                    attributeExpression = source.getParent().translateExpression(attributeExpression);
                }
                logger.debug("adding attribute with name " + oa.getName());
                MapAttribute attribute = unpivotComponent.addAttribute(oa.getName(), attributeExpression, type, size, scale);
                if (oa.getName().equalsIgnoreCase(rowLocator)) {
                    logger.debug("setting row locator to attribute " + oa.getName());
                    unpivotComponent.setRowLocator(attribute);
                }
            }
            unpivotComponent.setIncludeNulls(unpivot.getIsIncludeNulls());
//			for(String value : oa.getExpressions().keySet()) {
//				String attributeExpression = source.getParent().translateExpression(oa.getExpressions().get(value));
//				logger.info(oa.getName() + " -> " + attributeExpression);
//				
//				MapAttribute attribute = unpivotComponent.addAttribute(oa.getName(), attributeExpression, type, size, scale);
//				if(rowLocator.equalsIgnoreCase(oa.getName())) {
//					logger.info("setting row locator to attribute " + oa.getName());
//					unpivotComponent.setRowLocator(attribute);
//				}
//			}
        }


        for (OutputAttribute oa : unpivot.getOutputAttributes()) {
            if (oa.hasQualifiedExpressions()) {
                for (String value : oa.getExpressions().keySet()) {
                    logger.debug("adding unpivot transform " + value + " " + oa.getExpressions().get(value));

                    unpivotComponent.addTransform(value, source.getParent().translateExpression(oa.getExpressions().get(value)));
                }
            }
        }

        logger.debug("adding FLOW item " + unpivotComponent.getName());
        etlOperators.addFlowItem(unpivot.getParent(), unpivotComponent);
        return unpivotComponent;
    }


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.FlowsBuilder#setFlows(one.jodi.etl.internalmodel.Transformation, java.util.Map, boolean)
     */
    @Override
    public void setFlows(final Transformation transformation, final Map<Source, List<IMapComponent>> flowComponents, final boolean useExpressions) throws AdapterException, MappingException {

        for (Entry<Source, List<IMapComponent>> entry : flowComponents.entrySet()) {
            for (IMapComponent flowComponent : entry.getValue()) {
                String name = flowComponent.getName();
                Flow flow = getFlow(name, transformation);

                if (flowComponent instanceof PivotComponent) {
                    Pivot pivot = (Pivot) flow;

                    String rowLocator = flow.getParent().getParent().translateExpression(pivot.getRowLocator());
                    logger.debug("Setting row locator for pivot " + pivot.getName() + " to " + rowLocator);
                    try {
                        ((PivotComponent) flowComponent).setRowLocator(rowLocator);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (flowComponent instanceof SubQuery) {

                }
				/* 
				 * else if(flowComponent instanceof UnPivot) {
				UnPivot unpivot = (UnPivot) flow;
				String rowLocator = flow.getParent().getParent().translateExpression(unpivot.getRowLocator());
				((UnpivotComponent) flowComponent).setRowLocator(rowLocator);

				}*/
            }
        }

    }

    // TODO refactor to specify source and dataset.
    private Flow getFlow(String name, Transformation transformation) {
        for (one.jodi.etl.internalmodel.Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                for (Flow flow : source.getFlows()) {
                    if (flow.getName().equalsIgnoreCase(name)) {
                        return flow;
                    } else if (flow instanceof Pivot && getPivotKeyForSource((Pivot) flow, false).equals(name)) {
                        return flow;
                    } else if (flow instanceof UnPivot && getUnPivotKeyForSource((UnPivot) flow, false).equals(name)) {
                        return flow;
                    }
                }
            }
        }

        throw new RuntimeException("No flow item with name " + name + " in transformation specification.");
    }


    private String getSubQueryKeyForSource(final SubQuery subQuery, final boolean journalized) {
        String key = subQuery.getParent().getComponentName();
        return ComponentPrefixType.SUBQUERY.getAbbreviation() + "_" + key;
    }

    private String getPivotKeyForSource(final Pivot pivot, final boolean journalized) {
        String key = pivot.getParent().getComponentName();
        return ComponentPrefixType.PIVOT.getAbbreviation() + "_" + key;
    }

    private String getUnPivotKeyForSource(final UnPivot unpivot, final boolean journalized) {
        String key = unpivot.getParent().getComponentName();
        return ComponentPrefixType.UNPIVOT.getAbbreviation() + "_" + key;
    }


    private IDataType getAttributeType(IMapComponent component, String attributeName) throws AdapterException, MappingException {

        for (MapExpression me : component.getAttributeExpressions()) {
            MapAttribute ma = me.getOwningAttribute();
            if (ma != null && ma.getName().equalsIgnoreCase(attributeName)) {
                return ma.getDataType();
            }
        }

        return null;
    }


    protected PivotComponent createPivotComponent(final MapRootContainer mapping, String key) throws MappingException {
        return new PivotComponent(mapping, key);
    }

    protected UnpivotComponent createUnpivotComponent(final MapRootContainer mapping, String key) throws MappingException {
        return new UnpivotComponent(mapping, key);
    }

    protected SubqueryFilterComponent createSubqueryFilterComponent(final MapRootContainer mapping, String key) throws MappingException {
        return new SubqueryFilterComponent(mapping, key);
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