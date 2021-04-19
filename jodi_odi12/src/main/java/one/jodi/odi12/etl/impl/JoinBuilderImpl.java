package one.jodi.odi12.etl.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.JoinTypeEnum;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.etl.service.interfaces.TransformationException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.DatastoreBuilder;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.FilterBuilder;
import one.jodi.odi12.etl.FlowsBuilder;
import one.jodi.odi12.etl.JoinBuilder;
import one.jodi.odi12.etl.LookupBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.ReusableMapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.FilterComponent;
import oracle.odi.domain.mapping.component.JoinComponent;
import oracle.odi.domain.mapping.component.ReusableMappingComponent;
import oracle.odi.domain.mapping.exception.MapPhysicalException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.topology.OdiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;

public class JoinBuilderImpl implements JoinBuilder {

    private static final Logger logger = LogManager.getLogger(JoinBuilderImpl.class);
    private final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema>
            odiAccessStrategy;
    private final FilterBuilder filterBuilder;
    private final DatastoreBuilder datastoreBuilder;
    private final LookupBuilder lookupBuilder;
    private final FlowsBuilder flowBuilder;
    private final JodiProperties properties;


    @Inject
    protected JoinBuilderImpl(
            final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy,
            final JodiProperties properties, final HashMap<String, MapRootContainer> mappingCache,
            final FilterBuilder filterBuilder, final DatastoreBuilder datastoreBuilder,
            final LookupBuilder lookupBuilder, final FlowsBuilder flowBuilder) {
        this.properties = properties;
        this.odiAccessStrategy = odiAccessStrategy;
        this.filterBuilder = filterBuilder;
        this.datastoreBuilder = datastoreBuilder;
        this.flowBuilder = flowBuilder;
        this.lookupBuilder = lookupBuilder;
    }

    @Override
    public void addDatasets(final MapRootContainer mapping, final Transformation transformation,
                            final boolean journalized, final List<FilterComponent> filterComponentsss,
                            final EtlOperators etlOperators, final boolean useExpressions) throws AdapterException,
            MapPhysicalException, MappingException, TransformationAccessStrategyException, TransformationException,
            ResourceNotFoundException, ResourceFoundAmbiguouslyException {
        /*
         *
         * Add who imported what to the description interface targetcolumn in
         * odi
         */
        if (transformation.getComments() != null) {
            mapping.setDescription(transformation.getComments());
        }
        List<Dataset> dataSets = odiAccessStrategy.findDatasets(mapping);
        logger.debug(transformation.getPackageSequence() + " DataSetSize:" + dataSets.size());
        /*
         * Iterate through the datasets used for union all and other set
         * operators
         */
        for (int currentDatasetIdx = 0; currentDatasetIdx < transformation.getDatasets()
                                                                          .size(); currentDatasetIdx++) {
            one.jodi.etl.internalmodel.Dataset ds = transformation.getDatasets()
                                                                  .get(currentDatasetIdx);
            datastoreBuilder.addDatasource(mapping, ds.getDriverSourceInDataset(), transformation.getPackageSequence(),
                                           journalized, etlOperators);

            // add joined tables and joins
            for (Source joinedTable : ds.findJoinedSourcesInDataset()) {
                String model = joinedTable.getModel();
                // jkm
                if (joinedTable.isTemporary()) {
                    String folder = joinedTable.getParent()
                                               .getParent()
                                               .getFolderName();
                    ReusableMapping joinedreusableMapping =
                            (ReusableMapping) odiAccessStrategy.findMappingsByName(joinedTable.getName(), folder,
                                                                                   properties.getProjectCode());
                    String alias = joinedTable.getComponentName();
                    ReusableMappingComponent sourceComponent =
                            (ReusableMappingComponent) datastoreBuilder.createComponent(mapping, joinedreusableMapping,
                                                                                        false);
                    sourceComponent.setName(alias);
                    logger.debug("joined temp: " + joinedTable.getName() + ": in model" + model);
                } else {
                    OdiDataStore boundTo =
                            odiAccessStrategy.findDataStore(joinedTable.getName(), joinedTable.getModel());
                    assert (boundTo != null);
                    logger.debug("Creating joined datastore " + joinedTable.getAlias());
                    DatastoreComponent dsc =
                            (DatastoreComponent) datastoreBuilder.createComponent(mapping, boundTo, false);
                    dsc.setName(joinedTable.getComponentName());
                    if (transformation.isTemporary()) {
                        ((ReusableMapping) mapping).addComponent(dsc);
                    } else {
                        ((Mapping) mapping).addComponent(dsc);
                    }
                }

                flowBuilder.addFlow(mapping, joinedTable, etlOperators, journalized);
            }
            // filters are added after all sources and joins are added because
            // filters may apply to the entire dataset referring to columns in
            // multiple tables.
            // similarly, we add lookups after the dataset is constructed to
            // allow for
            // off-label usage of Lookup for semi-joins
            addJoins(mapping, transformation, journalized, ds, currentDatasetIdx,
                     etlOperators.getFilterComponents(currentDatasetIdx + 1), etlOperators);
            lookupBuilder.addLookups(mapping, ds.getSources(), journalized,
                                     etlOperators.getJoiners(currentDatasetIdx + 1), etlOperators);
        }
    }


    private void addJoins(final MapRootContainer mapping, final Transformation transformation,
                          final boolean journalized, final one.jodi.etl.internalmodel.Dataset ds,
                          final int currentDatasetIdx, final List<FilterComponent> filterComponents,
                          final EtlOperators etlOperators) throws AdapterException, MappingException,
            TransformationAccessStrategyException, ResourceNotFoundException {
        JoinComponent innerJoin = null;
        IMapComponent firstComponent = null;
        IMapComponent secondComponent = null;

        for (int counter = 0; counter < ds.getSources()
                                          .size(); counter++) {
            Source filteredSource = ds.getSources()
                                      .get(counter);
            filterBuilder.addFilter(mapping, filteredSource, journalized, etlOperators);
            if (counter == 0) {
                Source firstSource = ds.getSources()
                                       .get(counter);
                // find which source the seconds source is joined to.
                if (!firstSource.getFlows()
                                .isEmpty()) {
                    firstComponent = etlOperators.getFlows(firstSource)
                                                 .get(etlOperators.getFlows(firstSource)
                                                                  .size() - 1);
                } else if (firstSource.getFilter() != null && firstSource.getFilter()
                                                                         .length() > 1) {
                    firstComponent = etlOperators.getFilterComponentsByKey(
                            FilterBuilder.getFilterKeyForSource(firstSource, journalized));
                } else {
                    firstComponent = odiAccessStrategy.findSourceComponent(mapping, transformation, firstSource);
                }
                assert (firstComponent != null);
            } else {
                Source secondSource = ds.getSources()
                                        .get(counter);
                //innerJoin = new JoinComponent(mapping, ComponentPrefixType.DATASET.getAbbreviation()
                //		+ (currentDatasetIdx + 1) + "_" + ComponentPrefixType.JOIN.getAbbreviation() + (counter - 1));
                innerJoin = createJoinComponent(mapping, currentDatasetIdx + 1, counter - 1, secondSource.getName());
                if (secondSource.getJoinType()
                                .equals(JoinTypeEnum.INNER)) {
                    innerJoin.setJoinConditionText("1=1");
                    innerJoin.addJoinSource(firstComponent, "1=1");
                    innerJoin.setJoinType(JoinComponent.JOIN_TYPE_INNER);
                } else if (secondSource.getJoinType()
                                       .equals(JoinTypeEnum.LEFT_OUTER)) {
                    innerJoin.setJoinConditionText("1=1");
                    innerJoin.addJoinSource(firstComponent, "1=1");
                    innerJoin.setJoinType(JoinComponent.JOIN_TYPE_LEFT_OUTER);
                } else if (secondSource.getJoinType()
                                       .equals(JoinTypeEnum.FULL)) {
                    innerJoin.setJoinConditionText("1=1");
                    innerJoin.addJoinSource(firstComponent, "1=1");
                    innerJoin.setJoinType(JoinComponent.JOIN_TYPE_FULL_OUTER);
                } else if (secondSource.getJoinType()
                                       .equals(JoinTypeEnum.CROSS)) {
                    innerJoin.addJoinSource(firstComponent, null);
                    innerJoin.setJoinType(JoinComponent.JOIN_TYPE_CROSS);
                } else if (secondSource.getJoinType()
                                       .equals(JoinTypeEnum.NATURAL)) {
                    innerJoin.addJoinSource(firstComponent, "1=1");
                    innerJoin.setJoinType(JoinComponent.JOIN_TYPE_NATURAL);
                }
                assert (firstComponent != null);
                if (secondSource.getFilter() != null && secondSource.getFilter()
                                                                    .length() > 1) {
                    secondComponent = etlOperators.getFilterComponentsByKey(
                            FilterBuilder.getFilterKeyForSource(secondSource, journalized));
                    assert (secondComponent != null);
                } else if (!secondSource.getFlows()
                                        .isEmpty()) {
                    //pick up last Flow item
                    secondComponent = etlOperators.getFlows(secondSource)
                                                  .get(etlOperators.getFlows(secondSource)
                                                                   .size() - 1);
                } else {
                    secondComponent = odiAccessStrategy.findSourceComponent(mapping, transformation, secondSource);
                    assert (secondComponent != null);
                }
                if (innerJoin != null) {
                    String join = secondSource.getParent()
                                              .translateExpression(secondSource.getJoin());
                    if (secondSource.getJoinType()
                                    .equals(JoinTypeEnum.CROSS) || secondSource.getJoinType()
                                                                               .equals(JoinTypeEnum.NATURAL)) {
                        join = null;
                    }
                    innerJoin.addJoinSource(secondComponent, join);
                    if (secondSource.getJoinExecutionLocation() != null) {
                        innerJoin.setExecuteOnHint(
                                mapFromExecutionLocationType(secondSource.getJoinExecutionLocation()));
                    }
                    etlOperators.addJoiner(getNextIncrementalValue(etlOperators.getJoiners(currentDatasetIdx + 1)),
                                           innerJoin);
                }

                if (secondComponent.getComponentTypeName()
                                   .equals("DATASTORE") || secondComponent.getComponentTypeName()
                                                                          .equals("REUSABLEMAPPING") ||
                        secondComponent.getComponentTypeName()
                                       .equals("FILE")) {
                    if (secondSource.getJoinExecutionLocation() != null) {
                        innerJoin.setExecuteOnHint(
                                mapFromExecutionLocationType(secondSource.getJoinExecutionLocation()));
                    }
                }
                // cleanup the component.
                innerJoin.setJoinConditionText(innerJoin.getJoinConditionText()
                                                        .replace("1=1 AND ", ""));
                // end cleanup
                firstComponent = innerJoin;
            }
        }
    }

    private Integer getNextIncrementalValue(final List<?> list) {
        return list.size() + 1;
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

    protected JoinComponent createJoinComponent(MapRootContainer mapping, int datasetIndex, int joinIndex,
                                                String dataStoreName) throws MappingException {
        return new JoinComponent(mapping, ComponentPrefixType.DATASET.getAbbreviation() + datasetIndex + "_" +
                ComponentPrefixType.JOIN.getAbbreviation() + joinIndex + "_" + dataStoreName);
    }
}
