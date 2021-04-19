package one.jodi.odi12.etl.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.ComponentPrefixType;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.LookupTypeEnum;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.DatastoreBuilder;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.LookupBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.ReusableMapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.JoinComponent;
import oracle.odi.domain.mapping.component.LookupComponent;
import oracle.odi.domain.mapping.component.LookupComponent.NO_MATCH_ROWS;
import oracle.odi.domain.mapping.component.ReusableMappingComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.topology.OdiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;

public class LookupBuilderImpl implements LookupBuilder {

    private static final Logger logger = LogManager.getLogger(LookupBuilderImpl.class);
    private static final String LookupMultipleMatchedRows = "jodi.lookup.multiple_match_rows";
    private final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema>
            odiAccessStrategy;
    private final JodiProperties properties;
    private final DatastoreBuilder datastoreBuilder;


    @Inject
    protected LookupBuilderImpl(
            final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy,
            final HashMap<String, MapRootContainer> mappingCache, final JodiProperties properties,
            final DatastoreBuilder datastoreBuilder) {
        this.odiAccessStrategy = odiAccessStrategy;
        this.properties = properties;
        this.datastoreBuilder = datastoreBuilder;
    }


    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.LookupBuilder#addLookups(oracle.odi.domain.mapping.MapRootContainer, java.util.List, boolean, java.util.List, one.jodi.odi12.etl.EtlOperators)
     */
    @Override
    public void addLookups(final MapRootContainer mapping, final List<Source> sources, final boolean journalized,
                           final List<JoinComponent> joiners, final EtlOperators etlOperators) throws AdapterException,
            MappingException, TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        IMapComponent lastLookupProcessed = null;
        int datasetLookupCounter = 0;
        for (Source source : sources) {
            IMapComponent firstComponent = null;
            if (source.getLookups() != null && sources.size() == 1) {
                if (source.getFilter() != null && source.getFilter()
                                                        .length() > 2) {
                    // get the filtercomponent.
                    // source has filter so anything joins to that source,
                    // should be attached to filter
                    firstComponent = odiAccessStrategy.getComponentByName(mapping,
                                                                          ComponentPrefixType.FILTER.getAbbreviation() +
                                                                                  "_" + source.getComponentName());
                    logger.debug("firstComponent" + firstComponent.getName());
                } else if (source.getParent()
                                 .getSources()
                                 .size() == 1) {
                    // there is only 1 source in the dataset
                    // so we and we don't have a filter
                    // so anything attached to it should be the source itself
                    if (source.getFlows()
                              .size() > 0) {
                        // If there are flows use the last flow item as input
                        firstComponent = etlOperators.getFlows(source)
                                                     .get(etlOperators.getFlows(source)
                                                                      .size() - 1);
                    } else {
                        firstComponent = odiAccessStrategy.findSourceComponent(mapping, source.getParent()
                                                                                              .getParent(), source);
                    }

                    logger.debug("firstComponent" + firstComponent.getName());
                }
            } else {
                // the source has lookups and there are multiple sources
                // we add the first lookups to the last joinComponent
                int dataSetNumber = sources.get(0)
                                           .getParent()
                                           .getDataSetNumber();
                if (datasetLookupCounter == 0) {
                    firstComponent = getLastJoinerOfDataSet(joiners, dataSetNumber);
                } else {
                    // attach to last lookup
                    for (LookupComponent previousLookup : etlOperators.getLookups(dataSetNumber)) {
                        if (odiAccessStrategy.getDataSetNumberFromComponentName(previousLookup) == dataSetNumber) {
                            firstComponent = previousLookup;
                        }
                    }
                }
                logger.debug("firstComponent" + firstComponent.getName());
            }
            for (int lookupCounter = 0; lookupCounter < source.getLookups()
                                                              .size(); lookupCounter++) {
                Lookup lookup = source.getLookups()
                                      .get(lookupCounter);
                datasetLookupCounter++;
                logger.debug("firstComponent was:" + firstComponent.getName());
                if (lookupCounter == 0) {
                    firstComponent =
                            addLookupComponent(mapping, firstComponent, lookup, datasetLookupCounter, etlOperators);
                    lastLookupProcessed = firstComponent;
                } else {
                    firstComponent = addLookupComponent(mapping, lastLookupProcessed, lookup, datasetLookupCounter,
                                                        etlOperators);
                    lastLookupProcessed = firstComponent;
                }
                logger.debug("firstComponent is:" + firstComponent.getName());
            }
        }
    }

    /**
     * @param mapping
     * @param firstComponent being either source, filter or joiner @param lookup the lookup
     *                       component to add from the internal model @param counter the
     *                       counter of the lookup in the dataset starting with 1 @param
     *                       etlOperators all operators within the mapping @return @throws
     *                       AdapterException @throws MappingException @throws
     *                       TransformationAccessStrategyException
     * @throws ResourceFoundAmbiguouslyException
     * @throws ResourceNotFoundException
     */
    private LookupComponent addLookupComponent(final MapRootContainer mapping, IMapComponent firstComponent,
                                               Lookup lookup, final int counter, final EtlOperators etlOperators) throws
            AdapterException, MappingException, TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        IMapComponent secondComponent = null;
        String alias = lookup.getComponentName();
        if (!lookup.isTemporary()) {
            OdiDataStore lookupDataStore =
                    odiAccessStrategy.findDataStore(lookup.getLookupDataStore(), lookup.getModel());
            assert (lookupDataStore != null);
            secondComponent = datastoreBuilder.createComponent(mapping, lookupDataStore, false);
        } else {
            String folder = lookup.getParent()
                                  .getParent()
                                  .getParent()
                                  .getFolderName();
            ReusableMapping lookupDataStore =
                    (ReusableMapping) odiAccessStrategy.findMappingsByName(lookup.getLookupDataStore(), folder,
                                                                           properties.getProjectCode());
            assert (lookupDataStore != null);
            secondComponent = datastoreBuilder.createComponent(mapping, lookupDataStore, false);
        }
        assert (secondComponent != null);
        mapping.addComponent(secondComponent);
        secondComponent.setName(alias);

        LookupComponent lookupComponent = createLookup(mapping, lookup, firstComponent, secondComponent, counter);
        // debug
        logger.debug(
                "lookupComponent created: " + lookupComponent.getName() + " attached to: " + firstComponent.getName());
        etlOperators.addLookup(lookupComponent);
        return lookupComponent;
    }

    /**
     * @param mapping
     * @param lookup  from the internal model @param driver source where the lookup
     *                is joined to; source, filter, joiner or lookup
     *                component @param lookupComponent @param counter of the lookup
     *                within the dataset @return @throws AdapterException @throws
     *                MappingException
     */
    private LookupComponent createLookup(final MapRootContainer mapping, final Lookup lookup,
                                         final IMapComponent driver, final IMapComponent lookupComponent,
                                         final int counter) throws AdapterException, MappingException {

        String name = ComponentPrefixType.DATASET.getAbbreviation() + lookup.getParent()
                                                                            .getParent()
                                                                            .getDataSetNumber() + "_" +
                ComponentPrefixType.LOOKUP.getAbbreviation() + counter + "_" + lookup.getLookupDataStore();
        LookupComponent leftJoin = this.createLookupComponent(mapping, name);
        leftJoin.setJoinConditionText("1=1");
        leftJoin.setJoinType(JoinComponent.JOIN_TYPE_LEFT_OUTER);
        leftJoin.setDriverComponent(driver);


        // Cannot set row matching on reusable components.
        if (!lookup.isTemporary() && !lookup.getParent()
                                            .isTemporary() && !lookup.getParent()
                                                                     .getParent()
                                                                     .getParent()
                                                                     .isTemporary()) {
            String lookupType = LookupComponent.LookupType.ANY_ROW.name();
            if (properties.getPropertyKeys()
                          .contains(LookupMultipleMatchedRows)) {
                lookupType = properties.getProperty(LookupMultipleMatchedRows)
                                       .trim();
            }
            if (lookup.getLookupType() != null && !lookup.getLookupType()
                                                         .equals(LookupTypeEnum.SCALAR)) {
                lookupType = lookup.getLookupType()
                                   .name();
            }
            logger.debug("Setting Lookup " + lookup.getAlias() + " lookup type = " + lookupType);
            leftJoin.setLookupType(LookupComponent.LookupType.valueOf(lookupType));

        }

        if (lookup.getDefaultRowColumns()
                  .size() > 0) {
            leftJoin.setNoMatchRows(NO_MATCH_ROWS.DEFAULT_VALUES);

            lookup.getDefaultRowColumns()
                  .forEach((k, v) -> {
                      try {
                          leftJoin.setLookupAttributeDefaultValue(k, lookup.getParent()
                                                                           .getParent()
                                                                           .translateExpression(v));
                      } catch (MappingException me) {
                          throw new RuntimeException(me);
                      }
                  });
        }

        leftJoin.addJoinSource(lookupComponent, lookup.getParent()
                                                      .getParent()
                                                      .translateExpression(lookup.getJoin()));
        if (lookup.getJoinExecutionLocation() != null) {
            leftJoin.setExecuteOnHint(mapFromExecutionLocationType(lookup.getJoinExecutionLocation()));
        }
        leftJoin.setJoinConditionText(leftJoin.getJoinConditionText()
                                              .replace("1=1 AND ", ""));
        return leftJoin;
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


    /**
     * The lookups should be joined to the lasted joiner in case of multiple
     * sources per dataset.
     *
     * @param joiners
     * @param dataSetNumber
     * @return
     */
    private IMapComponent getLastJoinerOfDataSet(final List<JoinComponent> joiners, final int dataSetNumber) {
        IMapComponent last = null;
        for (JoinComponent entry : joiners) {
            if (odiAccessStrategy.getDataSetNumberFromComponentName(entry) == dataSetNumber) {
                last = entry;
            }
        }
        assert (last != null); // shouldn't be here.
        return last;
    }

    protected LookupComponent createLookupComponent(final MapRootContainer mapping, String name) throws
            MappingException {
        return new LookupComponent(mapping, name);
    }
}
