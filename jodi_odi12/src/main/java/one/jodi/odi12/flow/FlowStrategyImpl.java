package one.jodi.odi12.flow;

import com.google.inject.Inject;
import one.jodi.etl.internalmodel.*;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapConnectorPoint;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.*;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.topology.OdiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public class FlowStrategyImpl implements FlowStrategy {

    private final static Logger logger = LogManager.getLogger(FlowStrategyImpl.class);
    private final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;

    @Inject
    public FlowStrategyImpl(
            final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy) {
        this.odiAccessStrategy = odiAccessStrategy;
    }

    @SuppressWarnings("unchecked")
    protected List<? extends IMapComponent> getNextComponent(final EtlOperators etlOperators, final IMapComponent currentComponent,
                                                             final Class<? extends IMapComponent> currentClass, final Source source, final Lookup lookup) {
        validate(source, lookup);
        final boolean hasLookups = sourceHasLookups(source, lookup);
        final boolean hasMultipleDataSets = hasMultipleDataSets(source, lookup);
        int datasetNumber = source != null ? source.getParent().getDataSetNumber() : lookup.getParent().getParent().getDataSetNumber();
        boolean useExpression = source != null ? source.getParent().getParent().useExpressions()
                : lookup.getParent().getParent().getParent().useExpressions();
        boolean isAggregate = source != null ? source.getParent().getParent().getMappings().isAggregateTransformation(datasetNumber)
                : lookup.getParent().getParent().getParent().getMappings().isAggregateTransformation(datasetNumber);
        boolean isDistinct = source != null ? source.getParent().getParent().getMappings().isDistinct() : lookup.getParent().getParent().getParent().getMappings().isDistinct();
        final String alias;
        if (source != null) {
            alias = source.getComponentName();
        } else {
            alias = lookup.getComponentName();
        }
        if (currentClass.equals(DatastoreComponent.class) || currentClass.equals(FileComponent.class)
                || currentClass.equals(ReusableMappingComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForDatastore(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(FilterComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForFilter(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(JoinComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForJoin(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(LookupComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForLookup(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(AggregateComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForAggregateComponent(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(SetComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForSetComponent(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(PivotComponent.class) || currentClass.equals(UnpivotComponent.class) || currentClass.equals(SubqueryFilterComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForFlowComponent(source, lookup, currentComponent, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(ExpressionComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForExpression(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else if (currentClass.equals(DistinctComponent.class)) {
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            return getNextComponentForDistinct(source, lookup, hasLookups, hasMultipleDataSets, isDistinct, useExpression, isAggregate, etlOperators, datasetNumber);
        } else {
            // not tested
            logger.debug("Handling for currentClass: " + currentClass.getName() + " for: alias: " + alias);
            logger.debug("Returning EMPTY_MAP");
            return Collections.EMPTY_LIST;
        }
    }

    private boolean isFlowComponent(IMapComponent component) {
        return component instanceof PivotComponent || component instanceof UnpivotComponent || component instanceof SubqueryFilterComponent;
    }

    private List<? extends IMapComponent> getNextComponentForFlowComponent(Source source, Lookup lookup, IMapComponent currentComponent, boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        assert (datasetNumber > 0) : "DatasetNumber starts with 1.";
        int index = etlOperators.getFlows(source).indexOf(currentComponent);
        if (isFlowComponent(currentComponent) && index < etlOperators.getFlows(source).size() - 1) {
            logger.debug("FLOW current = " + currentComponent.getName() + " returning next item " + etlOperators.getFlows(source).get(index + 1).getName());
            return Collections.singletonList(etlOperators.getFlows(source).get(index + 1));
        } else {

            if (source != null && source.getParent().getSources().size() > 1) {
                logger.debug("Returning innerJoiners");
                return etlOperators.getJoiners(datasetNumber);
            } else if (isAggregate) {
                logger.debug("Returning AggregateComponents");
                return etlOperators.getAggregateComponents(datasetNumber);
            } else if (hasMultipleDataSets) {
                logger.debug("Returning SetComponents");
                return etlOperators.getSetComponents();
            } else if (useExpression) {
                logger.debug("Returning targetExpressions");
                return etlOperators.getTargetExpressions();
            } else if (isDistinct) {
                logger.debug("Returning distinct");
                return etlOperators.getDistinctComponents();
            } else {
                return etlOperators.getTargetComponents();
            }

        }
    }

    private List<? extends IMapComponent> getNextComponentForDistinct(Source source, Lookup lookup, boolean hasLookups,
                                                                      boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        logger.debug("Returning targetComponents");
        return etlOperators.getTargetComponents();
    }

    private List<? extends IMapComponent> getNextComponentForSetComponent(Source source, Lookup lookup,
                                                                          boolean hasLookups, boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        if (useExpression) {
            return etlOperators.getTargetExpressions();
        } else if (isDistinct) {
            return etlOperators.getDistinctComponents();
        } else {
            logger.debug("Returning targetComponents");
            return etlOperators.getTargetComponents();
        }
    }

    private List<? extends IMapComponent> getNextComponentForAggregateComponent(Source source, Lookup lookup,
                                                                                boolean hasLookups, boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        if (hasMultipleDataSets) {
            return etlOperators.getSetComponents();
        } else if (useExpression) {
            return etlOperators.getTargetExpressions();
        } else if (isDistinct) {
            return etlOperators.getDistinctComponents();
        } else {
            return etlOperators.getTargetComponents();
        }
    }

    private List<? extends IMapComponent> getNextComponentForExpression(Source source, Lookup lookup,
                                                                        boolean hasLookups, boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        if (isDistinct) {
            logger.debug("Returning Distinct objects");
            return etlOperators.getDistinctComponents();
        } else if (useExpression) {
            logger.debug("Returning getTargetComponent");
            return etlOperators.getTargetComponents();
        } else {
            logger.debug("Returning EMPTY_MAP");
            return Collections.emptyList();
        }
    }

    private void validate(Source source, Lookup lookup) {
        if (source == null && lookup == null) {
            throw new FlowStrategyException("source and lookup can't be null.");
        } else if (source != null && lookup != null) {
            throw new FlowStrategyException("source and lookup can't be not null.");
        }
    }

    private List<? extends IMapComponent> getNextComponentForLookup(Source source, Lookup lookup, boolean hasLookups,
                                                                    boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        boolean hasFlows = source != null ? !source.getFlows().isEmpty() : false;
        if (hasFlows) {
            return Collections.singletonList(etlOperators.getFlows(source).get(0)); //Collections.singletonList(etlOperators.getPivotComponent());
        } else if (hasMultipleDataSets) {
            if (isAggregate) {
                logger.debug("Returning AggregateComponents");
                return etlOperators.getAggregateComponents(datasetNumber);
            } else {
                logger.debug("Returning SetComponents");
                return etlOperators.getSetComponents();
            }
        } else if (isAggregate) {
            logger.debug("Returning AggregateComponents");
            return etlOperators.getAggregateComponents(datasetNumber);
        } else if (useExpression) {
            logger.debug("Returning targetExpressions");
            return etlOperators.getTargetExpressions();
        } else if (isDistinct) {
            logger.debug("Returning distinct");
            return etlOperators.getDistinctComponents();
        } else {
            return etlOperators.getTargetComponents();
        }
    }

    private List<? extends IMapComponent> getNextComponentForJoin(Source source, Lookup lookup, boolean hasLookups,
                                                                  boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        logger.debug("getNextComponentForJoin:");
        if (hasLookups) {
            logger.debug("Returning lookups");
            return etlOperators.getLookups(datasetNumber);
        } else if (hasMultipleDataSets) {
            if (isAggregate) {
                logger.debug("Returning aggregateComponents");
                return etlOperators.getAggregateComponents(datasetNumber);
            } else {
                logger.debug("Returning SetComponent");
                return etlOperators.getSetComponents();
            }
        } else if (isAggregate) {
            logger.debug("Returning aggregateComponents");
            return etlOperators.getAggregateComponents(datasetNumber);
        } else if (!useExpression && !isDistinct) {
            logger.debug("Returning targetComponent");
            return etlOperators.getTargetComponents();
        } else if (!useExpression && isDistinct) {
            logger.debug("Returning targetComponent");
            return etlOperators.getDistinctComponents();
        } else {
            logger.debug("Returning targetExpressions");
            return etlOperators.getTargetExpressions();
        }
    }

    private List<? extends IMapComponent> getNextComponentForFilter(Source source, Lookup lookup, boolean hasLookups,
                                                                    boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        if (source != null && source.getParent().getSources().size() > 1) {
            logger.debug("Returning innerJoiners");
            return etlOperators.getJoiners(datasetNumber);
        } else if (source != null && source.getFlows().size() > 0) {
            return Collections.singletonList(etlOperators.getFlows(source).get(0)); //etlOperators.getFlows(); //Collections.singletonList(etlOperators.getPivotComponent());
        } else if (hasMultipleDataSets) {
            if (isAggregate) {
                logger.debug("Returning aggregateComponents.");
                return etlOperators.getAggregateComponents(datasetNumber);
            } else {
                logger.debug("Returning setoperations");
                return etlOperators.getSetComponents();
            }
        } else if (!useExpression && isDistinct) {
            logger.debug("Returning distctinct");
            return etlOperators.getDistinctComponents();
        } else if (!useExpression && !isDistinct) {
            logger.debug("Returning targetExpressions");
            return etlOperators.getTargetComponents();
        } else {
            logger.debug("Returning targetExpressions");
            return etlOperators.getTargetExpressions();
        }
    }

    private List<? extends IMapComponent> getNextComponentForDatastore(Source source, Lookup lookup, boolean hasLookups,
                                                                       boolean hasMultipleDataSets, boolean isDistinct, boolean useExpression, boolean isAggregate, EtlOperators etlOperators, int datasetNumber) {
        if (source != null && source.getFilter() != null && source.getFilter().length() > 2) {
            logger.debug("Returning filters");
            return etlOperators.getFilterComponents(datasetNumber);
        } else if (source != null && source.getFlows().size() > 0) {
            return Collections.singletonList(etlOperators.getFlows(source).get(0));
        } else if (source != null && source.getParent().getSources().size() > 1) {
            // Do we need counter for already processed sources?
            logger.debug("Returning innerJoiners");
            return etlOperators.getJoiners(datasetNumber);
        } else if (source != null && hasLookups) {
            // we process through the lookup object which is added elsewhere
            logger.debug("Returning emptymap");
            return Collections.emptyList();
        } else if (hasLookups && !hasMultipleDataSets) {
            logger.debug("Returning lookups");
            return etlOperators.getLookups(datasetNumber);
        } else if (isAggregate) {
            logger.debug("Returning aggregateComponents");
            return etlOperators.getAggregateComponents(datasetNumber);
        } else if (source != null && hasMultipleDataSets) {
            logger.debug("Returning setComponents");
            return etlOperators.getSetComponents();
        } else if (hasLookups && hasMultipleDataSets) {
            logger.debug("Returning lookups");
            // not tested
            return etlOperators.getLookups(datasetNumber);
        } else if (!useExpression && !isDistinct) {
            logger.debug("Returning targetComponent");
            return etlOperators.getTargetComponents();
        } else if (!useExpression && isDistinct) {
            logger.debug("Returning distinctComponents");
            return etlOperators.getDistinctComponents();
        } else {
            logger.debug("Returning targetExpressions");
            return etlOperators.getTargetExpressions();
        }
    }

    @Override
    public void handleNextComponent(final EtlOperators etlOperators, final Source source, final Lookup lookup,
                                    final IMapComponent sourceComponent) throws MappingException {
        validate(source, lookup);
        String aliasSourceOrLookup = "";
        if (source != null) {
            aliasSourceOrLookup = source.getComponentName();
        } else {
            aliasSourceOrLookup = lookup.getComponentName();
        }
        int dataSetNumber;
        if (source != null) {
            dataSetNumber = source.getParent().getDataSetNumber();
        } else {
            dataSetNumber = lookup.getParent().getParent().getDataSetNumber();
        }
        boolean hasMultipleDataSets = hasMultipleDataSets(source, lookup);
        boolean hasLookups = sourceHasLookups(source, lookup);
        if (sourceComponent instanceof DatastoreComponent || sourceComponent instanceof ReusableMappingComponent
                || sourceComponent instanceof FileComponent) {
            logger.debug("sourceComponent is DatastoreComponent, ReusableMappingComponent or FileComponent");
            handleDataStoreComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (sourceComponent instanceof FilterComponent) {
            logger.debug("sourceComponent is FilterComponent");
            handleFilterComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (sourceComponent instanceof LookupComponent) {
            // / lookup is subtype of JoinComponent
            // so instance of lookupcomponent should come
            // before instance of joincomponent.
            logger.debug("sourceComponent is LookupComponent");
            handleLookupComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (sourceComponent instanceof JoinComponent) {
            logger.debug("sourceComponent is JoinComponent");
            handleJoinComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (isFlowComponent(sourceComponent)) {
            handleFlowComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (sourceComponent instanceof SetComponent) {
            logger.debug("sourceComponent is SetComponent");
            handleSetComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (sourceComponent instanceof ExpressionComponent) {
            logger.debug("sourceComponent is Exprssioncomponent");
            handleExpressionComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (sourceComponent instanceof DistinctComponent) {
            logger.debug("sourceComponent is DistinctComponent");
            handleDistinctComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        } else if (sourceComponent instanceof AggregateComponent) {
            logger.debug("sourceComponent is AggregateComponent");
            handleAggregateComponent(etlOperators, source, lookup, sourceComponent, hasLookups, hasMultipleDataSets,
                    dataSetNumber, aliasSourceOrLookup);
        }
    }

    private void handleAggregateComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                          IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                          String aliasSourceOrLookup) throws MappingException {
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, AggregateComponent.class, source, lookup)) {
            // not tested
            logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
            logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
            sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
            if (source != null && nextComponent instanceof SetComponent) {
                logger.debug("DSConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("DSfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                addSetOperations(source.getParent().getParent(), source, null, sourceComponent,
                        source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
            }
            if (isNextComponentNotEndOfFlow(nextComponent)) {
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            }
        }
    }


    private void handleFlowComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                     IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                     String aliasSourceOrLookup) throws MappingException {
        assert (isFlowComponent(sourceComponent));

        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, PivotComponent.class, source, lookup)) {
            // not tested
            logger.debug("Pivot Connection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
            logger.debug("Pivot from: " + sourceComponent.getName() + " to: " + nextComponent.getName());


            if (nextComponent instanceof SetComponent) {
                addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
                        source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
            } else if (nextComponent instanceof DistinctComponent) {
                addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
                        source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
            }

            sourceComponent.connectTo(nextComponent);


            if (isNextComponentNotEndOfFlow(nextComponent)) {
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            }
        }
    }

//	private void handlePivotComponent(EtlOperators etlOperators, Source source, Lookup lookup,
//			IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
//			String aliasSourceOrLookup) throws MappingException {
//		logger.debug("handling PIVOT " + sourceComponent.getAlias());
//		for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, PivotComponent.class, source, lookup)) {
//			// not tested
//			logger.debug("Pivot Connection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
//			logger.debug("Pivot from: " + sourceComponent.getName() + " to: " + nextComponent.getName());
//			
//			
//			if(nextComponent instanceof SetComponent) {
//				addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
//							source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
//			}
//			else if(nextComponent instanceof DistinctComponent) {
//				addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
//						source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
//			}
//			
//			sourceComponent.connectTo(nextComponent);
//			
//
//			if (isNextComponentNotEndOfFlow(nextComponent)) {
//				handleNextComponent(etlOperators, source, lookup, nextComponent);
//			}
//		}
//	}
//
//	
//	private void handleUnpivotComponent(EtlOperators etlOperators, Source source, Lookup lookup,
//			IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
//			String aliasSourceOrLookup) throws MappingException {
//		for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, PivotComponent.class, source, lookup)) {
//			// not tested
//			logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
//			logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
//			sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
//			if (isNextComponentNotEndOfFlow(nextComponent)) {
//				handleNextComponent(etlOperators, source, lookup, nextComponent);
//			}
//		}
//	}

    private void handleDistinctComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                         IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                         String aliasSourceOrLookup) throws MappingException {
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, DistinctComponent.class, source, lookup)) {
            logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
            logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
            sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
            if (isNextComponentNotEndOfFlow(nextComponent)) {
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            }
        }
    }

    private void handleExpressionComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                           IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                           String aliasSourceOrLookup) throws MappingException {
        String message = getNextComponent(etlOperators, sourceComponent, ExpressionComponent.class, source, lookup).iterator().next()
                .getClass().getName();
        logger.debug("Handle-ing expressioncomponent and next class is: " + message);
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, ExpressionComponent.class, source, lookup)) {
            logger.debug("ExprConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
            logger.debug("Exprfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
            sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
            // check to see if the end is reached.
            // end being targetdatastore of reusablemapping component
            // or file component
            if (isNextComponentNotEndOfFlow(nextComponent)) {
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            }
        }
    }

    private void handleSetComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                    IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                    String aliasSourceOrLookup) throws MappingException {
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, SetComponent.class, source, lookup)) {
            logger.debug("SetConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
            logger.debug("Setfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
            sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
            if (isNextComponentNotEndOfFlow(nextComponent)) {
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            }
        }
    }

    private void handleJoinComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                     IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                     String aliasSourceOrLookup) throws AdapterException, MappingException {
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, JoinComponent.class, source, lookup)) {
            // not tested
            if (nextComponent instanceof AggregateComponent) {
                if (dataSetNumber == odiAccessStrategy.getDataSetNumberFromComponentName(nextComponent)
                        && odiAccessStrategy.getDataSetNumberFromComponentName(sourceComponent)
                        == odiAccessStrategy.getDataSetNumberFromComponentName(nextComponent)) {
                    logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                    logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
//					addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
//							source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
                    sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                }
            } else if (nextComponent instanceof DistinctComponent) {
                logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
//				addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
//						source.getParent().getSetOperator(), etlOperators);
                sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            } else if (nextComponent instanceof SetComponent) {
                logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
                        source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
                sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            } else if (hasLookups) {
                logger.debug("We continue from lookups.");
            } else if (nextComponent instanceof ExpressionComponent) {
                logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("Joinfrom: " + sourceComponent.getClass() + " to: " + nextComponent.getClass()
                        + " nextcomponentsize "
                        + getNextComponent(etlOperators, sourceComponent, JoinComponent.class, source, lookup).size());
                logger.debug(String.format("Source cmpnt name %1$s target name %2$s ", sourceComponent.getName(),
                        nextComponent.getName()));
                if (((source != null && odiAccessStrategy.getDataSetNumberFromComponentName(sourceComponent) == source
                        .getParent().getDataSetNumber())
                        || (lookup != null
                        && odiAccessStrategy.getDataSetNumberFromComponentName(sourceComponent) == lookup
                        .getParent().getParent().getDataSetNumber()))) {
                    // we are in the same dataset & we are join component.
                    int dataSetNumberJoin = source != null ? source.getParent().getDataSetNumber()
                            : lookup.getParent().getParent().getDataSetNumber();
                    logger.debug("-------");
                    logger.debug(
                            "sourceComponent instanceof JoinComponent: " + (sourceComponent instanceof JoinComponent));
                    logger.debug("odiAccessStrategy.getJoinNumber(sourceComponent.getName()) "
                            + odiAccessStrategy.getJoinNumber(sourceComponent.getName()));
                    logger.debug("getMaxComponentsPerDataSet(etlOperators.getJoiners(),dataSetNumberJoin) "
                            + getMaxComponentsPerDataSet(etlOperators.getJoiners(dataSetNumber), dataSetNumberJoin));
                    if (sourceComponent instanceof JoinComponent) {
                        if (odiAccessStrategy.getJoinNumber(sourceComponent.getName()) == getMaxComponentsPerDataSet(
                                etlOperators.getJoiners(dataSetNumber), dataSetNumberJoin)) {
                            logger.debug("Connecting source to nextcomponent");
                            // connect only the last join to expression
                            sourceComponent.connectTo(nextComponent);
                        }
                    } else {
                        logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: "
                                + nextComponent.getClass());
                        logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                        sourceComponent.connectTo(nextComponent);
                    }
                }
                if (isNextComponentNotEndOfFlow(nextComponent)) {
                    logger.debug(
                            "JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                    logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                }
            } else {
                // the component is datastore, file or reusablemappingcomponent.
                if (!isNextComponentNotEndOfFlow(nextComponent)) {
                    //
                    if (odiAccessStrategy.getJoinNumber(sourceComponent.getName()) == getMaxComponentsPerDataSet(
                            etlOperators.getJoiners(dataSetNumber), dataSetNumber)) {
                        logger.debug("JoinConnection from: " + sourceComponent.getClass() + " to: "
                                + nextComponent.getClass());
                        logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                        sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                    }
                }
                if (isNextComponentNotEndOfFlow(nextComponent)) {
                    // only handle next component if we haven't reached the end.
                    logger.debug(
                            "JoinConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                    logger.debug("Joinfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                }
            }
        }
    }

    private void handleLookupComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                       IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                       String aliasSourceOrLookup) throws AdapterException, MappingException {
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, LookupComponent.class, source, lookup)) {
            if (odiAccessStrategy.getJoinNumber(sourceComponent.getName()) != lookup.getNumberOfLookupsInDataset()
                    || odiAccessStrategy.getDataSetNumberFromComponentName(sourceComponent) != lookup.getParent()
                    .getParent().getDataSetNumber()) {
                // if it is not the last lookup in the dataset,
                // continue since lookups are daisy chained.
                logger.debug(String.format("number of lookups %1$d %2$d",
                        odiAccessStrategy.getJoinNumber(sourceComponent.getName()),
                        lookup.getNumberOfLookupsInDataset()));
                logger.debug("continue-ing since it is not the last lookup");
                continue;
            }
            logger.debug("LkpConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
            logger.debug("LkpFrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
            if (nextComponent instanceof SetComponent) {
                if (source != null) {
                    addSetOperations(source.getParent().getParent(), source, null, sourceComponent,
                            source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
                } else {
                    addSetOperations(lookup.getParent().getParent().getParent(), null, lookup, sourceComponent,
                            lookup.getParent().getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
                }
                sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            } else {
                // handled in addlookups.
                sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                if (isNextComponentNotEndOfFlow(nextComponent)) {
                    // check to see if we have reached the end of the flow;
                    // after lookups there can't be any datastore components any
                    // more.
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                }
            }
        }

    }

    private void handleFilterComponent(EtlOperators etlOperators, Source source, Lookup lookup,
                                       IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                       String aliasSourceOrLookup) throws AdapterException, MappingException {
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, FilterComponent.class, source, lookup)) {
            if (nextComponent instanceof SetComponent) {
                logger.debug("FltConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("Fltrfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                addSetOperations(source.getParent().getParent(), source, lookup, sourceComponent,
                        source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
                sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            } else if (nextComponent instanceof JoinComponent) {
                logger.debug(
                        "NO FltConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("NO Fltrfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                if (odiAccessStrategy.getJoinNumber(nextComponent.getName()) == getMaxComponentsPerDataSet(
                        etlOperators.getJoiners(dataSetNumber), dataSetNumber)) {
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                }
            } else {
                logger.debug("FltConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("Fltrfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                sourceComponent.connectTo(nextComponent);
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            }
        }
    }

    private void handleDataStoreComponent(EtlOperators etlOperators, final Source source, final Lookup lookup,
                                          final IMapComponent sourceComponent, boolean hasLookups, boolean hasMultipleDataSets, int dataSetNumber,
                                          String aliasSourceOrLookup) throws MappingException {
        int counter = 0;
        for (IMapComponent nextComponent : getNextComponent(etlOperators, sourceComponent, DatastoreComponent.class, source, lookup)) {
            counter++;
            logger.debug("handleDataStoreComponent source != null: " + (source != null));
            logger.debug("handleDataStoreComponent hasLookups: " + (hasLookups));
            if (source != null && hasLookups) {
                // we continue from lookups
                logger.debug("We continue from lookups");
                continue;
            }
            String filterComponentNameWithoutPrefix = odiAccessStrategy.getComponentName(nextComponent);
            filterComponentNameWithoutPrefix = filterComponentNameWithoutPrefix.substring(
                    (ComponentPrefixType.FILTER.getAbbreviation() + "_").length(),
                    filterComponentNameWithoutPrefix.length());
            if (nextComponent instanceof FilterComponent
                    && filterComponentNameWithoutPrefix.equals(odiAccessStrategy.getComponentName(sourceComponent))) {
                // filters only needs to be attached to their source.
                logger.debug("This filter is attached to source.");
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            } else if (nextComponent instanceof FilterComponent
                    && !filterComponentNameWithoutPrefix.equals(odiAccessStrategy.getComponentName(sourceComponent))) {
                // filters only needs to be attached to their source.
                logger.debug("This filter is not attached to source.");
                continue;
            } else if (nextComponent instanceof LookupComponent) {
                // MapConnectorPoint icp = nextComponent.getValue()
                // .getInputConnectorPoint(aliasSourceOrLookup);
                // if (icp == null)
                // icp = nextComponent.getValue()
                // .getNextAvailableInput("");
                logger.debug("RMConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("RMfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                // sourceComponent.connectTo(nextComponent.getValue());
                handleNextComponent(etlOperators, source, lookup, (LookupComponent) nextComponent);
            } else if (nextComponent instanceof JoinComponent) {
                logger.debug("dataset number:" + dataSetNumber + "JoinComponent number: "
                        + odiAccessStrategy.getJoinNumber(nextComponent.getName()) + " number of components: "
                        + getMaxComponentsPerDataSet(etlOperators.getJoiners(dataSetNumber), dataSetNumber));
                int componentsPerDataset = getMaxComponentsPerDataSet(etlOperators.getJoiners(dataSetNumber), dataSetNumber);
                if (odiAccessStrategy.getJoinNumber(nextComponent.getName()) != componentsPerDataset
                        || dataSetNumber != odiAccessStrategy.getDataSetNumberFromComponentName(nextComponent)) {
                    // we only connect the last component of the joins to
                    // the next component;
                    // they are daisy chained.
                    for (IMapComponent a : getNextComponent(etlOperators, sourceComponent, DatastoreComponent.class, source, lookup)) {
                        logger.debug(String.format("valuename: %1s ", a.getName()));
                    }
                    logger.debug("Continueing since joinnumber not equals number of components.");
                    continue;
                }
                if (source != null && hasMultipleDataSets && getMaxComponentsPerDataSet(etlOperators.getJoiners(dataSetNumber),
                        dataSetNumber) == odiAccessStrategy.getJoinNumber(nextComponent.getName())) {
                    logger.debug("from:" + sourceComponent + "to : " + nextComponent.getName());
                    MapConnectorPoint icp = nextComponent
                            .getInputConnectorPoint(odiAccessStrategy.getComponentName(sourceComponent));
                    if (icp == null) {
                        icp = nextComponent.getNextAvailableInput(aliasSourceOrLookup);
                    }
                    sourceComponent.connectTo(icp);
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                } else if ((source != null && hasMultipleDataSets(source, lookup)
                        && source.getParent().getDataSetNumber() != odiAccessStrategy
                        .getDataSetNumberFromComponentName(nextComponent))
                        || (lookup != null && hasMultipleDataSets(source, lookup)
                        && lookup.getParent().getParent().getDataSetNumber() != odiAccessStrategy
                        .getDataSetNumberFromComponentName(nextComponent))) {
                    //
                    logger.debug("this source is not connected to dataset.");
                } else if (hasLookups) {
                    logger.debug("We continue from lookups.");
                } else {
                    final String aliasSource;
                    if (source != null)
                        aliasSource = source.getComponentName();
                    else
                        aliasSource = lookup.getComponentName();
                    assert (source != null);
                    Source driver = source.getParent().getDriverSourceInDataset();
                    String aliasDriver = driver.getComponentName();
                    if (aliasSource.equals(aliasDriver)) {
                        logger.debug("Processing for alias : " + aliasSourceOrLookup);
                        // sourceComponent.connectTo(icp);
                        handleNextComponent(etlOperators, source, lookup, nextComponent);
                        logger.debug("next component from source.");

                    } else if (lookup != null) {
                        MapConnectorPoint icp = nextComponent
                                .getInputConnectorPoint(odiAccessStrategy.getComponentName(sourceComponent));
                        if (icp == null) {
                            icp = nextComponent.getNextAvailableInput(aliasSourceOrLookup);
                        }
                        sourceComponent.connectTo(icp);
                        handleNextComponent(etlOperators, source, lookup, nextComponent);
                        logger.debug("next component from lookup");
                    } else {
                        logger.debug("No further processing.");
                    }
                }
            } else if (nextComponent instanceof SetComponent) {
                logger.debug("DSConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("DSfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                if (source != null) {
                    addSetOperations(source.getParent().getParent(), source, null, sourceComponent,
                            source.getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
                } else if (lookup != null) {
                    addSetOperations(lookup.getParent().getParent().getParent(), null, lookup, sourceComponent,
                            lookup.getParent().getParent().getSetOperator(), etlOperators, aliasSourceOrLookup);
                    sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                }
                logger.debug(aliasSourceOrLookup);
                handleNextComponent(etlOperators, source, lookup, nextComponent);
            } else if (nextComponent instanceof SubqueryFilterComponent) {
                logger.debug("DSConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                sourceComponent.connectTo(((SubqueryFilterComponent) nextComponent).getDriverConnectorPoint());
                if (isNextComponentNotEndOfFlow(nextComponent)) {
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                }

            } else {
                logger.debug("DSConnection from: " + sourceComponent.getClass() + " to: " + nextComponent.getClass());
                logger.debug("DSfrom: " + sourceComponent.getName() + " to: " + nextComponent.getName());
                sourceComponent.connectTo(nextComponent, aliasSourceOrLookup);
                // check to see if we have reached the end,
                // targetdatastore or reusablemapping
                if (isNextComponentNotEndOfFlow(nextComponent)) {
                    handleNextComponent(etlOperators, source, lookup, nextComponent);
                }
                if (counter == 1 && nextComponent instanceof LookupComponent) {
                    // lookups are daisy chained not parallel.
                    break;
                }
            }
        }
    }

    private boolean isNextComponentNotEndOfFlow(IMapComponent mapComponent) {
        return !(mapComponent instanceof DatastoreComponent) && !(mapComponent instanceof ReusableMappingComponent)
                && !(mapComponent instanceof FileComponent) && !(mapComponent instanceof OutputSignature);
    }

    private int getMaxComponentsPerDataSet(List<? extends IMapComponent> joiners, int dataSetNumber) {
        int max = 0;
        for (IMapComponent entry : joiners) {
            if (odiAccessStrategy.getDataSetNumberFromComponentName(entry) == dataSetNumber) {
                int joinNumber = odiAccessStrategy.getJoinNumber(entry.getName());
                if (joinNumber > max) {
                    max = joinNumber;
                }
            }
        }
        return max;
    }

    private boolean sourceHasLookups(final Source source, final Lookup lookup) {
        if (lookup != null) {
            return true;
        }
        boolean hasLookups = false;
        for (Source s : source.getParent().getSources()) {
            if (s.getLookups() != null && s.getLookups().size() > 0) {
                // it is not; if the source has lookups
                // but it is; if one of the sources has lookups
                // Jira; jodi-167
                return true;
            }
        }
        return hasLookups;
    }

    private boolean hasMultipleDataSets(final Source source, final Lookup lookup) {
        if (source != null && source.getParent().getParent().getDatasets().size() > 1) {
            return true;
        } else if (lookup != null && lookup.getParent().getParent().getParent().getDatasets().size() > 1) {
            return true;
        }
        return false;
    }

    /**
     * set the set operation and make sure there is an mapconnector point.
     *
     * @param transformation
     * @param newSource
     * @param setOperatorTypeEnum
     * @param aliasSourceOrLookup
     * @throws AdapterException
     * @throws MappingException
     */
    private void addSetOperations(final Transformation transformation, final Source source, final Lookup lookup,
                                  IMapComponent newSource, SetOperatorTypeEnum setOperatorTypeEnum, EtlOperators etlOperators, String aliasSourceOrLookup)
            throws AdapterException, MappingException {
        if (transformation.getDatasets().size() == 1) {
            return;
        }
        validate(source, lookup);
        int dataSetNumber = source != null ? source.getParent().getDataSetNumber()
                : lookup.getParent().getParent().getDataSetNumber();
        int maxDataSetNumber = transformation.getMaxDatasetNumber();
        SetComponent sc = etlOperators.getSetComponents().get(0);
        MapConnectorPoint icp = sc.createInputConnectorPoint(aliasSourceOrLookup);
        assert (icp != null);
        icp.setName(aliasSourceOrLookup);
        logger.debug("icp name: " + icp.getName());
        //(aliasSourceOrLookup);
        // @TODO check if valid for aggregates
        printStack();
        logger.debug("------->>" + newSource.getClass());
        logger.debug("------->>" + newSource.getName());
        sc.addToSet(newSource);
        if (dataSetNumber % 2 == 0) {
            if (setOperatorTypeEnum.equals(SetOperatorTypeEnum.INTERSECT)) {
                sc.setSetOperationType(icp, "INTERSECT");
            } else if (setOperatorTypeEnum.equals(SetOperatorTypeEnum.MINUS)) {
                sc.setSetOperationType(icp, "MINUS");
            } else if (setOperatorTypeEnum.equals(SetOperatorTypeEnum.UNION)) {
                sc.setSetOperationType(icp, "UNION");
            } else if (setOperatorTypeEnum.equals(SetOperatorTypeEnum.UNION_ALL)) {
                sc.setSetOperationType(icp, "UNION ALL");
            }
        }
        int datasetNumber = 0;
        try {
            datasetNumber = odiAccessStrategy.getDataSetNumberFromComponentName(newSource);
        } catch (NumberFormatException nfe) {
            // for pivots and unpivots standard name is used.  note that dataset number starts at 1, not 0.
            datasetNumber = transformation.getDatasets().indexOf(source.getParent()) + 1;
        }

        logger.debug("datasetNumber: " + dataSetNumber + " :" + maxDataSetNumber);
        //if(datasetNumber == maxDataSetNumber){
        finishSetOperations(icp, transformation, newSource, etlOperators, datasetNumber, maxDataSetNumber);
        //}
        etlOperators.addSetComponent(sc);
    }

    /**
     * @param icp
     * @param transformation
     * @param newSource
     * @throws AdapterException
     * @throws MappingException
     */
    private void finishSetOperations(MapConnectorPoint icp, Transformation transformation, IMapComponent source,
                                     EtlOperators etlOperators, int datasetNumber, int maxDataSetNumber) throws AdapterException, MappingException {
        SetComponent sc = etlOperators.getSetComponents().get(0);
        if (sc != null) {
            for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
                // the translation is necessary since we prefix our source and
                // lookups
                // with the dataset D1 for dataset 1 and D2 for dataset 2 etc.
                String[] translated = getTranslation(tc, transformation);
                //for(int datasetIndex = 0; datasetIndex < tc.getMappingExpressions().size() ; datasetIndex++){
                if (tc.getParent().isAggregateTransformation(datasetNumber)) {
                    sc.setAttributeExpressionText(tc.getName(), "D" + (datasetNumber) + "_" + ComponentPrefixType.AGGREGATE.getAbbreviation() + "." + tc.getName(), icp);
                } else {
                    sc.setAttributeExpressionText(tc.getName(), translated[datasetNumber - 1], icp);
                }
                //}
            }
            if (datasetNumber == maxDataSetNumber) {
                for (MapConnectorPoint mcp : sc.getInputConnectorPoints()) {
                    if (mcp.getName().startsWith("INPUT")) {
                        sc.removeInputConnectorPoint(mcp);
                    }
                }
            }
        }
    }

    private String[] getTranslation(Targetcolumn tc, Transformation transformation) {
        String[] array = new String[tc.getMappingExpressions().size()];
        tc.getMappingExpressions().toArray(array);
        String[] translated = new String[tc.getMappingExpressions().size()];
        int i = 0;
        for (String expression : array) {
            translated[i] = transformation.getDatasets().get(i).translateExpression(expression);
            i++;
        }
        return translated;
    }

    private void printStack() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StringBuilder path = new StringBuilder();
        for (int reversCounter = stackTraceElements.length; reversCounter > 0; reversCounter--) {
            StackTraceElement stackTraceElement = stackTraceElements[reversCounter - 1];
            path.append("/" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName());
        }
        logger.debug(path.toString());
    }


}
