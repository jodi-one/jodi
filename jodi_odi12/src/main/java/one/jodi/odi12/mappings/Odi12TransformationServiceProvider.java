package one.jodi.odi12.mappings;

import com.google.inject.Inject;
import one.jodi.base.annotations.DevMode;
import one.jodi.base.config.JodiPropertyNotFoundException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.etl.service.interfaces.TransformationException;
import one.jodi.etl.service.interfaces.TransformationServiceProvider;
import one.jodi.logging.OdiLogHandler;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi.interfaces.ResourceCreationException;
import one.jodi.odi12.etl.*;
import one.jodi.odi12.flow.FlowStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionDefinition;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.IModelObject;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.*;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.*;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MapPhysicalException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.mapping.physical.MapPhysicalDesign;
import oracle.odi.domain.mapping.properties.PropertyException;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.root.RootIssue;
import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiTechnology;
import oracle.odi.mapping.generation.GenerationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;

/*
 *
 * @param <T>
 *            is Mapping or ReusableMapping
 */
@Singleton
public class Odi12TransformationServiceProvider<T extends MapRootContainer> implements TransformationServiceProvider {
    private final static String ERROR_MESSAGE_09999 = "Interface: %1$d %2$s: Validation reported '%3$s' with message '%4$s' with description '%5$s' with key '%6$s'.";

    private final static String UseUniqueTemporaryObjectNames = "odi12.useUniqueTemporaryObjectNames";
    private final static Logger logger = LogManager.getLogger(Odi12TransformationServiceProvider.class);
    private final OdiInstance odiInstance;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final boolean isDevMode;
    private final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy;
    private final HashMap<String, Transformation> transformationCache;
    private final FlowStrategy flowStrategy;
    private final FlowsBuilder flowBuilder;
    private final JoinBuilder joinBuilder;
    private final ExpressionsBuilder expressionsBuilder;
    private final SetBuilder setBuilder;
    private final FlagsBuilder flagsBuilder;
    private final AggregateBuilder aggregateBuilder;
    private final DistinctBuilder distinctBuilder;
    private final KMBuilder kmBuilder;

    @Inject
    protected Odi12TransformationServiceProvider(final OdiInstance odiInstance,
                                                 final JodiProperties properties, final ErrorWarningMessageJodi errorWarningMessages,
                                                 final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy,
                                                 final @DevMode Boolean devMode, final FlowStrategy flowStrategy,
                                                 final FlowsBuilder flowBuilder,
                                                 final JoinBuilder joinBuilder,
                                                 final SetBuilder setBuilder,
                                                 final ExpressionsBuilder expressionsBuilder,
                                                 final FlagsBuilder flagsBuilder,
                                                 final AggregateBuilder aggregateBuilder,
                                                 final DistinctBuilder distinctBuilder,
                                                 final KMBuilder kmBuilder) {
        this.odiInstance = odiInstance;
        this.properties = properties;
        this.errorWarningMessages = errorWarningMessages;
        this.odiAccessStrategy = odiAccessStrategy;
        this.isDevMode = devMode;
        this.transformationCache = new HashMap<>();
        this.flowStrategy = flowStrategy;
        this.flowBuilder = flowBuilder;
        this.joinBuilder = joinBuilder;
        this.setBuilder = setBuilder;
        this.expressionsBuilder = expressionsBuilder;
        this.flagsBuilder = flagsBuilder;
        this.aggregateBuilder = aggregateBuilder;
        this.distinctBuilder = distinctBuilder;
        this.kmBuilder = kmBuilder;
        /**
         * Findbugs reports about this part:
         * LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE: Potential lost logger changes
         * due to weak reference in OpenJDK OpenJDK introduces a potential
         * incompatibility. In particular, the java.util.logging.Logger behavior
         * has changed. Instead of using strong references, it now uses weak
         * references internally. That's a reasonable change, but unfortunately
         * some code relies on the old behavior - when changing logger
         * configuration, it simply drops the logger reference. That means that
         * the garbage collector is free to reclaim that memory, which means
         * that the logger configuration is lost.
         *
         * Since this is not a singleton and insert in the constructor, this is
         * not an issue for now.
         *
         */
        // Set ODI logging to severe
        // this seem to be causeing memory leak
        // be careful in re-enabling this.
        java.util.logging.Logger odiLogger = java.util.logging.Logger.getLogger("oracle.odi");
        odiLogger.setLevel(Level.OFF);
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("oracle.odi");
        Handler[] handlers = globalLogger.getHandlers();
        for (Handler handler : handlers) {
            globalLogger.removeHandler(handler);
        }
    }

    /**
     * Entry point for creating an transformation.
     *
     * @param transformation derived from textual specifications. @param isJournalizedData
     *                       whether to load only journalized data @param packageSequence a
     *                       number representing a sequence number of order in which the
     *                       transformation should be loaded.
     * @return Transformation
     * @throws TransformationException exception during generating mappings
     */
    @Override
    public Transformation createTransformation(final Transformation transformation, final boolean isJournalizedData,
                                               final int packageSequence) throws TransformationException {
        boolean useExpressions = transformation.useExpressions();

        if (isDevMode) {
            logger.info("Processing: " + transformation.getPackageSequence() + " : " + transformation.getName()
                    + " in folder : " + transformation.getFolderName() + " is async: " + transformation.isAsynchronous());
            logger.debug(String.format("Creating new interface '%d' '%s' in folder '%s'.",
                    transformation.getPackageSequence(), transformation.getName(), transformation.getFolderName()));
        }

        final EtlOperators etlOperators = new EtlOperators(null, null,
                new ArrayList<>(),
                new ArrayList<>(), null,
                new TreeMap<>(), null,
                new ArrayList<>(), new HashMap<>(),
                odiAccessStrategy);

        //
        MapRootContainer mapping = null;
        {
            // do not change this transaction control,
            // it has impact on performance.
            final DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
            final ITransactionManager tm = odiInstance.getTransactionManager();
            final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
            final IOdiEntityManager tme = odiInstance.getTransactionalEntityManager();
            try {
                if (transformation.isTemporary()) {
                    mapping = createNewReusableMapping(transformation, isJournalizedData);
                    tme.persist(mapping);
                } else {
                    mapping = createNewMapping(transformation, isJournalizedData);
                    tme.persist(mapping);
                }
                tm.commit(txnStatus);
            } catch (ResourceFoundAmbiguouslyException | ResourceNotFoundException | ResourceCreationException
                    | MappingException | TransformationAccessStrategyException e) {
                tm.rollback(txnStatus);
                tm.commit(txnStatus);
                logger.error("OdiTransformationAccessStrategyException", e);
                throw new TransformationException("OdiTransformationAccessStrategyException", e);
            } finally {
                tme.close();
            }
        }
        // do not change this transaction control,
        // it has impact on performance.
        final DefaultTransactionDefinition txnDef1 = new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
        final ITransactionManager tm1 = odiInstance.getTransactionManager();
        final ITransactionStatus txnStatus1 = tm1.getTransaction(txnDef1);
        final IOdiEntityManager tme1 = odiInstance.getTransactionalEntityManager();
        try {
            logger.debug("ODI FOLDER NAME = " + transformation.getFolderName());

            // create the target component, if the targetcomponent is not a
            // datastore,
            // but a resuable mapping component (output signature) then then
            // datastore is null.
            OdiDataStore targetDataStore = createTargetComponents(mapping, transformation, etlOperators);

            // the add distinct, aggregate and targetexpressions,
            // just add the empty components onto the canvas of the mapping
            addDistinct(mapping, transformation, etlOperators);
            for (int dataSetIndex = 0; dataSetIndex < transformation.getDatasets().size(); dataSetIndex++) {
                aggregateBuilder.addAggregate(mapping, transformation, etlOperators, (dataSetIndex + 1));
            }
            setBuilder.addSetComponent(transformation, mapping, useExpressions, etlOperators);

            // if we use expressions a targetexpression is added.
            // it is just like the other add distinct add aggregate and add
            // setcomponent methods,
            // just an empty component. The component itself hasn't been
            // 'decorated' meaning,
            // the details of disctinct aggregate and set are not yet composed.
            expressionsBuilder.addTargetExpressions(mapping, useExpressions, etlOperators);

            // the datasets are added; meaning all sources and lookups are added
            // to the mappings,
            // and also joins and left outer joins are added to the mapping,
            // however the components are not yet 'connected' to each other.
            joinBuilder.addDatasets(mapping, transformation, isJournalizedData, etlOperators.getAllFilterComponents(), etlOperators,
                    useExpressions);

            // the start flow strategy connects all the components.
            // first source a is joined to source b then lookup a is left joined
            // to the inner join,
            // of source a and b then the setcomponents are joined then the
            // targetexpressions then the distinct
            // then the aggregated and then the targetcolumns
            startFlowStrategy(mapping, transformation, etlOperators, flowStrategy, useExpressions);

            // now we have connected components, however what is not set is the
            // details of each component,
            // meaning if we have aggregate the group by clause is not set, if
            // we have setcomponents the attributes are not set etc
            // the setting of the details of the components happends below.

            // set details, here row locator.
            flowBuilder.setFlows(transformation, etlOperators.getFlows(), useExpressions);

            // the details of the aggregate are set for instance the aggregate
            // column and the group by columns
            for (int dataSetIndex = 0; dataSetIndex < transformation.getDatasets().size(); dataSetIndex++) {
                aggregateBuilder.setAggregate(transformation, etlOperators.getAggregateComponents(dataSetIndex + 1), (dataSetIndex + 1));
            }

            // if it is a distinct mapping the distict component is populated.
            distinctBuilder.setDistinct(transformation, etlOperators.getDistinctComponents(), useExpressions);
            // here we create the expression component.

            expressionsBuilder.createExpressionComponent(mapping, transformation, etlOperators.getTargetExpressions(), useExpressions);

            // here the mapping fields are populated from the targetcolumns
            // textual specification.
            expressionsBuilder.setMappingFields(mapping, transformation, etlOperators.getTargetComponents(), useExpressions,
                    targetDataStore);

            // here the flags are set
            flagsBuilder.setFlags(transformation, etlOperators.getTargetComponents());

            initializeExecutionLocation(etlOperators.getTargetComponents().get(0), transformation);
            initializeUserDefinedFields(mapping, transformation, etlOperators.getTargetComponents());
            setSubSelects(mapping, transformation, isJournalizedData);

            if (properties.isUpdateable()) {
                logger.debug(transformation.getPackageSequence() + " merge");
                mergeTransformation(mapping, transformation.getName(), packageSequence);
            } else {
                logger.debug(transformation.getPackageSequence() + " persist");
                persistMapping(mapping, transformation.getName(), packageSequence);
            }

            setJournalized(mapping, transformation, isJournalizedData);
            setPhysicalProperties(mapping, etlOperators.getTargetComponents());
            setBeginAndEndMapping(mapping, transformation, etlOperators.getTargetComponents());

            // LKM, IKM and CKM are set
            // moved to bottom for PGBU where a IKM wasn't set on first iteration
            // however when updated it was set.
            setKnowledgeModules(mapping, transformation, isJournalizedData, etlOperators.getTargetComponents());

            validatePostTransformation(mapping, transformation);

            tme1.persist(mapping);
            tm1.commit(txnStatus1);
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            tm1.rollback(txnStatus1);
            throw new TransformationException("MapComponentException", e);
        } catch (MapPhysicalException e) {
            logger.error("MapPhysicalException", e);
            tm1.rollback(txnStatus1);
            throw new TransformationException("MapPhysicalException", e);
        } catch (PropertyException e) {
            logger.error("PropertyException", e);
            tm1.rollback(txnStatus1);
            throw new TransformationException("PropertyException", e);
        } catch (AdapterException e) {
            logger.error("AdapterException", e);
            tm1.rollback(txnStatus1);
            throw new TransformationException("AdapterException", e);
        } catch (MappingException e) {
            logger.error("MappingException", e);
            tm1.rollback(txnStatus1);
            throw new TransformationException("MappingException", e);
        } catch (GenerationException e) {
            logger.error("GenerationException", e);
            tm1.rollback(txnStatus1);
            throw new TransformationException("GenerationException", e);
        } catch (TransformationAccessStrategyException | ResourceNotFoundException | ResourceFoundAmbiguouslyException e) {
            logger.error("OdiTransformationAccessStrategyException", e);
            tm1.rollback(txnStatus1);
            throw new TransformationException("OdiTransformationAccessStrategyException", e);
        } finally {
            tme1.close();
        }
        // store the enriched model in a cache
        transformationCache.put(transformation.getName(), transformation);
        // store the actual generated mappings in a cache for fast retrieval.
        // mapping = null;
        return transformation;
    }

    /**
     * Add the empty distinct component if used.
     *
     * @param mapping
     * @param transformation
     * @param etlOperators
     * @throws AdapterException @throws MappingException
     */
    private void addDistinct(final MapRootContainer mapping, final Transformation transformation,
                             final EtlOperators etlOperators) throws AdapterException, MappingException {
        if (transformation.getMappings().isDistinct()) {
            logger.debug("Setting interfaceoptions");
            logger.debug("Distinct operator:" + transformation.getMappings().isDistinct());
            etlOperators.addDistinctComponents(
                    new DistinctComponent(mapping, ComponentPrefixType.DISTINCT.getAbbreviation()));
        }
    }


    /**
     * The startFlowStrategy connects all items together, previously they where
     * just added and unconnected. after this method they are connected, the
     * flowstrategy determines how things are connected.
     * <p>
     * Connections are always daisy chained. Meaning a component has at maximumn
     * 2 inputs, except for setoperations who may have more inputs.
     *
     * @param mapping
     * @param transformation
     * @param etlOperators
     * @param flowStrategy   @param useExpressions @throws
     *                       MappingException @throws TransformationAccessStrategyException
     * @throws ResourceNotFoundException
     */
    private void startFlowStrategy(final MapRootContainer mapping, final Transformation transformation,
                                   final EtlOperators etlOperators, final FlowStrategy flowStrategy, final boolean useExpressions)
            throws MappingException, TransformationAccessStrategyException, ResourceNotFoundException {
        for (one.jodi.etl.internalmodel.Dataset ds : transformation.getDatasets()) {
            Iterator<Source> iterator = ds.getSources().iterator();
            if (iterator.hasNext()) {
                Source firstSource = iterator.next();
                IMapComponent sourceComponent = odiAccessStrategy.findSourceComponent(mapping, transformation,
                        firstSource);
                if (!firstSource.isTemporary()) {
                    assert (sourceComponent != null);
                    if (sourceComponent instanceof DatastoreComponent) {
                        // get the next component for the source and handle it
                        // do this only on the first source
                        flowStrategy.handleNextComponent(etlOperators, firstSource, null, sourceComponent);
                    } else if (sourceComponent instanceof FileComponent) {
                        // get the next component for source and handle it
                        // do this only on the first source
                        flowStrategy.handleNextComponent(etlOperators, firstSource, null,
                                (FileComponent) sourceComponent);
                    } else if (sourceComponent instanceof ReusableMappingComponent) {
                        // get the next component for source and handle it
                        // do this only on the first source
                        flowStrategy.handleNextComponent(etlOperators, firstSource, null,
                                (ReusableMappingComponent) sourceComponent);
                    }
                } else {
                    assert (sourceComponent != null);
                    // get the next component for source and handle it
                    // do this only on the first source
                    flowStrategy.handleNextComponent(etlOperators, firstSource, null, sourceComponent);
                }
            }
            for (Source source : ds.getSources()) {
                for (Lookup lookup : source.getLookups()) {
                    IMapComponent lookupComponent = odiAccessStrategy.findLookupComponent(mapping, transformation,
                            lookup);
                    assert (lookupComponent != null);
                    // get the next component for lookup and handle it
                    if (lookupComponent instanceof DatastoreComponent) {
                        flowStrategy.handleNextComponent(etlOperators, null, lookup, lookupComponent);
                    } else if (lookupComponent instanceof FileComponent) {
                        flowStrategy.handleNextComponent(etlOperators, null, lookup, (FileComponent) lookupComponent);
                    } else if (lookupComponent instanceof ReusableMappingComponent) {
                        flowStrategy.handleNextComponent(etlOperators, null, lookup, lookupComponent);
                    }
                }
            }
        }
    }


    /**
     * Set other IKM and LKM and CKM options; do this only once.
     *
     * @param mapping          odi mapping
     * @param transformation   transfomation from textual expressions
     * @param journalized      indicating journalization
     * @param targetComponents targetcomponents
     * @throws MappingException        exception during creating of mapping
     * @throws GenerationException     exception during generation
     * @throws AdapterException        exception from adapter
     * @throws TransformationException exception while creating transformation in etl subsystem
     */
    protected void setKnowledgeModules(final MapRootContainer mapping, final Transformation transformation,
                                       final boolean journalized, final List<IMapComponent> targetComponents)
            throws MappingException, AdapterException, GenerationException, TransformationException {
        kmBuilder.setLKM(mapping, transformation);
        kmBuilder.setIKM(mapping, transformation, targetComponents);
        kmBuilder.setCKM(mapping, transformation);
    }


    /**
     * @param mapping        mapping
     * @param transformation transformation from textual specifications
     * @throws AdapterException exception from adapter
     * @throws MappingException exception from mapping
     */
    private void initializeExecutionLocation(final IMapComponent mapping, final Transformation transformation)
            throws AdapterException, MappingException {
        HashMap<String, MapAttribute> db = getMapExpressions(mapping);
        for (Targetcolumn targetColumn : transformation.getMappings().getTargetColumns()) {
            for (int expressionIndex = 0; expressionIndex < targetColumn.getMappingExpressions()
                    .size(); expressionIndex++) {
                MapExpression.ExecuteOnLocation el = mapFromExecutionLocationType(
                        targetColumn.getExecutionLocations().get(expressionIndex));
                if (!mapping.getTypeName().equals("OUTPUTSIGNATURE")) {
                    initializeColumnExecutionLocation(transformation, db.get(targetColumn.getName()),
                            targetColumn.getName(), el, targetColumn);
                }
            }
        }
    }

    /**
     * @param mapping
     * @param transformation
     * @param targetComponents
     * @throws AdapterException @throws MappingException @throws
     *                          TransformationException
     */
    private void initializeUserDefinedFields(final MapRootContainer mapping, final Transformation transformation,
                                             List<IMapComponent> targetComponents) throws AdapterException, MappingException, TransformationException {
        for (MapAttribute ma : targetComponents.get(0).getAttributes()) {
            Targetcolumn tc = getTargetColumn(transformation, ma.getName());
            for (UserDefinedFlag udf : tc.getUserDefinedFlags()) {
                logger.debug("Udf name: " + udf.getName() + " value: " + udf.getValue());
                ma.setPropertyValue(getUDProperty(udf.getNumber()), udf.getValue());
            }
        }
    }

    /**
     * If one of the targetcolumns uses analytical functions, we set subselect
     * to true, but if one of the textual specifications, has subselect set to
     * true then it is always set to true. We only set subselects on reusable
     * mappings.
     *
     * @param mapping
     * @param transformation
     * @param journalized
     * @throws MapComponentException @throws PropertyException
     */
    private void setSubSelects(final MapRootContainer mapping, final Transformation transformation,
                               final boolean journalized) throws MapComponentException, PropertyException {
        List<String> db = new ArrayList<>();
        for (one.jodi.etl.internalmodel.Dataset internalDataSet : transformation.getDatasets()) {
            // create a "db" of objects on which the issubselect should be set.
            for (Source internalSource : internalDataSet.getSources()) {
                // Subselect is set to true in the xml file.
                if (internalSource.isSubSelect() && internalSource.isTemporary()) {
                    String tablename = internalSource.getComponentName();
                    db.add(tablename);
                }
                if (internalSource.getLookups() != null) {
                    internalSource.getLookups().stream().filter(
                            internalLookup -> internalLookup.isSubSelect() &&
                                    internalLookup.isTemporary())
                            .forEach(internalLookup -> {
                                String tablename =
                                        internalLookup.getComponentName();
                                db.add(tablename);
                            });
                }
            }
        }
        // iterate to all components and when found in the "subselect database"
        // apply the subselect.
        for (IMapComponent sds : mapping.getAllComponents()) {
            if (sds instanceof ReusableMappingComponent) {
                Transformation reusableMapping = transformationCache.get(sds.getBoundObjectName());
                assert (reusableMapping != null) : "Can't find reusable mapping for setting subselects, in development you may unset the -ea flag.";
                boolean isAnalytical = false;
                if (reusableMapping != null) {
                    for (Targetcolumn tc : reusableMapping.getMappings().getTargetColumns()) {
                        for (int dataSetIndex = 0; dataSetIndex < tc.getMappingExpressions().size(); dataSetIndex++) {
                            if (tc.isAnalyticalFunction(dataSetIndex + 1)) {
                                isAnalytical = true;
                            }
                        }
                    }
                }
                if (isAnalytical) {
                    ((ReusableMappingComponent) sds).setSubSelect(true);
                }
            }
            // Model not taken into account since temp interfaces do not
            // have models
            String odiKey = odiAccessStrategy.getComponentName(sds);
            if (db.contains(odiKey) && sds instanceof ReusableMappingComponent) {
                ((ReusableMappingComponent) sds).setSubSelect(true);
            }
        }
    }

    /**
     * Crud operation on the mapping to merge it.
     *
     * @param mapping
     * @param transformationName
     * @param packageSequence
     */
    private void mergeTransformation(final MapRootContainer mapping, final String transformationName,
                                     final int packageSequence) {
        logger.debug("-- merge.");
        java.util.logging.Logger javautillogger = java.util.logging.Logger.getLogger("oracle.odi.interfaces.interactive.support.InteractiveInterfaceHelperWithActions");
        javautillogger.setLevel(Level.INFO);
        OdiLogHandler odiHandler = new OdiLogHandler(errorWarningMessages);
        odiHandler.setLevel(Level.INFO);
        javautillogger.addHandler(odiHandler);
        odiInstance.getTransactionalEntityManager().persist(mapping);
        javautillogger.removeHandler(odiHandler);
        odiHandler.flush();
    }

    /**
     * CRUD on the mapping to create it.
     *
     * @param mapping
     * @param interfaceName
     * @param packageSequence
     */
    private void persistMapping(final MapRootContainer mapping, final String interfaceName, final int packageSequence) {
        // Persist the interface
        java.util.logging.Logger javautillogger = java.util.logging.Logger.getLogger("oracle.odi.interfaces.interactive.support.InteractiveInterfaceHelperWithActions");
        javautillogger.setLevel(Level.INFO);
        OdiLogHandler odiHandler = new OdiLogHandler(errorWarningMessages);
        odiHandler.setLevel(Level.INFO);
        javautillogger.addHandler(odiHandler);
        if (properties.isUpdateable()) {
            logger.debug("-- updateable.");
            // helper.preparePersist();
        } else {
            logger.debug("-- not updateable changed.");
            // needs to be merge for ODI12C.
            odiInstance.getTransactionalEntityManager().persist(mapping);
            if (odiHandler.containsLogMessage("critical")) {
                javautillogger.removeHandler(odiHandler);
                odiHandler.flush();
            }
        }
        javautillogger.removeHandler(odiHandler);
        odiHandler.flush();
    }

    /**
     * Set the journalized data only flag on used source or lookup
     *
     * @param mapping
     * @param transformation
     * @param journalized
     * @throws AdapterException          @throws MappingException @throws
     *                                   TransformationAccessStrategyException
     * @throws ResourceNotFoundException
     */
    private void setJournalized(final MapRootContainer mapping, final Transformation transformation,
                                final boolean journalized) throws AdapterException, MappingException, TransformationAccessStrategyException,
            ResourceNotFoundException {
        if (!journalized) {
            return;
        }
        for (one.jodi.etl.internalmodel.Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                if (!source.isTemporary()) {
                    IMapComponent sourceDS = odiAccessStrategy.findSourceComponent(mapping, transformation, source);
                    if (sourceDS instanceof DatastoreComponent) {
                        logger.debug("Processing: " + source.getName() + " is journalized: " + source.isJournalized());
                        ((DatastoreComponent) sourceDS).setJournalizedDataOnly(source.isJournalized());
                        if (source.isJournalized()) {
                            ((DatastoreComponent) sourceDS).setJournalizedFilter(source.getJournalizedFilters());
                        }
                        logger.debug("Processed: " + source.getName() + " is journalized: " + ((DatastoreComponent) sourceDS).isJournalized());
                    }
                }
                for (Lookup lookup : source.getLookups()) {
                    if (!lookup.isTemporary()) {
                        logger.debug("Processing: " + lookup.getLookupDataStore() + " is journalized: " + lookup.isJournalized());
                        IMapComponent sourceLookupDS = odiAccessStrategy.findLookupComponent(mapping, transformation,
                                lookup);
                        ((DatastoreComponent) sourceLookupDS).setJournalizedDataOnly(lookup.isJournalized());
                        if (lookup.isJournalized()) {
                            ((DatastoreComponent) sourceLookupDS).setJournalizedFilter(lookup.getJournalizedFilters());
                        }
                        logger.debug("Processed: " + lookup.getLookupDataStore() + " is journalized: " + ((DatastoreComponent) sourceLookupDS).isJournalized());
                    }
                }
            }
        }
    }

    /**
     * This methods set the optimization context from properties file, for a
     * specific mapping. Reusable mappings do not have an optimization context
     * set.
     *
     * @param mapping
     * @param targetComponents
     * @throws MapPhysicalException
     * @throws PropertyException
     * @throws ResourceNotFoundException
     */
    private void setPhysicalProperties(final MapRootContainer mapping, final List<IMapComponent> targetComponents)
            throws MapPhysicalException, PropertyException, ResourceNotFoundException {
        if (!targetComponents.get(0).getTypeName().equals("OUTPUTSIGNATURE")) {
            OdiContext odiContext = odiAccessStrategy.findOdiContext(properties.getProperty("odi.context"));
            MapPhysicalDesign physicalDesign = ((Mapping) mapping).getPhysicalDesign(0);
            physicalDesign.getProperty("OPTIMIZATION_CONTEXT").setValue(odiContext.getName());
            for (String propKey : properties.getPropertyKeys()) {
                if (propKey.equals(UseUniqueTemporaryObjectNames)) {
                    boolean supportConncurrentExecution = Boolean.valueOf(properties.getProperty(UseUniqueTemporaryObjectNames));
                    if (supportConncurrentExecution) {
                        physicalDesign.setPropertyValue("SUPPORT_CONCURRENT_EXECUTION", true);
                    } else {
                        physicalDesign.setPropertyValue("SUPPORT_CONCURRENT_EXECUTION", false);
                    }
                }
            }
//			for( Property p :  physicalDesign.getProperties()){
//				logger.info("Property: "+ p.getName());
//			}
        }
    }

    /**
     * This method delegates validation of the created mapping to ODI. Some of
     * the warning messages are ignored by default.
     *
     * @param mapping
     * @param transformation
     */
    private void validatePostTransformation(final MapRootContainer mapping, final Transformation transformation) {
        List<RootIssue> issues = new ArrayList<>();
        // TODO 12C make hashset of to be ignored rootissues
        boolean debug = true;
        mapping.clearIssueCache();
        boolean result = mapping.validate(issues, debug);
        if (!issues.isEmpty()) {
            for (RootIssue issue : issues) {
                try {
                    issue.getMessage();
                } catch (ArrayIndexOutOfBoundsException e) {
                    logger.debug("Issue caught ArrayIndexOutOfBoundsException | NPE.");
                    continue;
                }
                if (issue.getLevel().toString().contains("INFO") || issue.getMessage().contains("NEXTVAL")
                        || issue.getMessage().contains("nextval")
                        || issue.getMessage().contains(
                        "contains a filter expression that does not reference a different schema on each side of the equality operator")
                        || issue.getMessage().contains(
                        "JNPE at oracle.odi.domain.mapping.MapConnectorPoint.getProjectorOrMultiInputSource")
                        || (issue.getMessage().contains("Reusable mapping")
                        && issue.getMessage().contains("for reusable mapping component")
                        && issue.getMessage().contains("contains errors or warnings."))
                        || issue.getMessage().contains("has type CROSS, so its non-blank Join Condition will be ignored")
                ) {
                    logger.debug(issue.getLevel() + " message:" + issue.getMessage() + " issue:"
                            + issue.getDescription() + " key:" + issue.getMessageKey());
                } else if (issue.getLevel().toString().contains("WARNING")) {
                    String msg = errorWarningMessages.formatMessage(9999, ERROR_MESSAGE_09999, this.getClass(),
                            transformation.getPackageSequence(), transformation.getName(), issue.getLevel(),
                            issue.getMessage(), issue.getDescription(), issue.getMessageKey());
                    errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.WARNINGS);
                } else {
                    String msg = errorWarningMessages.formatMessage(9999, ERROR_MESSAGE_09999, this.getClass(),
                            transformation.getPackageSequence(), transformation.getName(), issue.getLevel(),
                            issue.getMessage(), issue.getDescription(), issue.getMessageKey());
                    logger.error(msg);
                    errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                }
            }
            if (!result) {
                // TODO validate more strictly
                // throw new OdiTransformationException("Issues during
                // creation.");
            }
        }
        mapping.clearIssueCache();
    }


    /***
     * Delete mapping or reusable mapping.
     *
     * @param name
     *            of the mapping to delete
     *
     * @throws TransformationException exception generating mappings
     *
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteTransformation(final String name, String folder)
            throws TransformationException {
        try {
            logger.debug("Trying to delete in project: " + properties.getProjectCode() + " name: " + name + " folder: " + folder);
            odiAccessStrategy.deleteMappingsByName(name, folder, properties.getProjectCode());
        } catch (ResourceNotFoundException e) {
            //logger.error("Cannot remove mapping with name " + name + "; mapping not found.", e);
            //throw new TransformationException("OdiTransformationAccessStrategyException", e);
        } catch (ResourceFoundAmbiguouslyException e) {
            //logger.error("Cannot remove mapping with name " + name + "; multiple mappings with same name exist.", e);
            throw new TransformationException("OdiTransformationAccessStrategyException", e);
        }
    }


    /**
     * @param number
     * @return the key of the User Defined Field e.g. UD_1
     */
    private String getUDProperty(final int number) {
        return "UD_" + number;
    }

    // set exec location

    /**
     * @param transformation
     * @param mapAttribute
     * @param columnName
     * @param execLocation   @param targetColumn @throws
     *                       AdapterException @throws MappingException
     */
    private void initializeColumnExecutionLocation(final Transformation transformation, final MapAttribute mapAttribute,
                                                   final String columnName, final MapExpression.ExecuteOnLocation execLocation,
                                                   final Targetcolumn targetColumn) throws AdapterException, MappingException {
        if (execLocation != null) {
            mapAttribute.setExecuteOnHint(execLocation);
        }
    }

    /**
     * @param mapping odi mapping
     * @return HashMap<String, MapAttribute> with key is the name of the
     * mapattribute, and value the mapattribute itself.
     * @throws AdapterException exception from the adapter
     * @throws MappingException exception from the mapping
     */
    private HashMap<String, MapAttribute> getMapExpressions(final IMapComponent mapping)
            throws AdapterException, MappingException {
        HashMap<String, MapAttribute> db = new HashMap<>();
        for (MapAttribute me : mapping.getAttributes()) {
            db.put(me.getName(), me);
        }
        return db;
    }

    /**
     * CRUD to create a new resuable mapping object, and create the folder if
     * not present.
     *
     * @param transformation    transformation
     * @param isJournalizedData indicating journalization
     * @return odi reusable mapping
     * @throws MappingException                      exception while creating mapping
     * @throws TransformationAccessStrategyException exception while searching for textual specifications of a mapping
     * @throws TransformationException               exeption while generating from textual specifications
     * @throws ResourceNotFoundException             exception while searching for a resource
     * @throws ResourceFoundAmbiguouslyException     exception while searching for a resource; mutliple items found
     * @throws ResourceCreationException             exception while creating resource
     */

    protected ReusableMapping createNewReusableMapping(final Transformation transformation,
                                                       final boolean isJournalizedData)
            throws MappingException, TransformationAccessStrategyException,
            TransformationException, ResourceFoundAmbiguouslyException,
            ResourceNotFoundException, ResourceCreationException {
        logger.debug("Generating in folder: " + transformation.getFolderName());
        OdiFolder folder;
        try {
            folder = odiAccessStrategy.findFolderByName(transformation.getFolderName(),
                    properties.getProjectCode());
        } catch (ResourceNotFoundException e1) {
            logger.info("Adding folder.");
            folder = odiAccessStrategy.addFolder(transformation.getFolderName());
        }
        ReusableMapping reusableMapping = null;
        if (properties.isUpdateable()) {
            try {
                reusableMapping = (ReusableMapping)
                        odiAccessStrategy.findMappingsByName(transformation.getName(),
                                transformation.getFolderName(),
                                properties.getProjectCode());
            } catch (ResourceNotFoundException rnfe) {
                reusableMapping = new ReusableMapping(transformation.getName(), folder);
            }
        } else {
            reusableMapping = new ReusableMapping(transformation.getName(), folder);
        }
        if (reusableMapping == null) {
            reusableMapping = new ReusableMapping(transformation.getName(), folder);
        }
        assert (odiInstance != null) : "OdiInstance is null.";
        assert (reusableMapping != null) : "Reusablemapping is null.";
        assert (transformation.getName() != null) : "Reusablemapping name is null.";
        return reusableMapping;
    }

    /**
     * CRUD to create now Mapping and folder if not exists.
     *
     * @param transformation    transformation
     * @param isJournalizedData indicating journalization
     * @return odi mapping
     * @throws TransformationAccessStrategyException exception accessing transformations
     * @throws AdapterException                      exception from etl subsytem adapster
     * @throws MappingException                      exception from etl subsystem
     * @throws TransformationException               exception from textual specifications
     * @throws ResourceFoundAmbiguouslyException     exception while searching for a resource
     * @throws ResourceCreationException             exception while creating a resource
     */
    protected Mapping createNewMapping(final Transformation transformation, final boolean isJournalizedData)
            throws TransformationAccessStrategyException, AdapterException, MappingException, TransformationException,
            ResourceFoundAmbiguouslyException, ResourceCreationException {
        logger.debug("Generating in folder: " + transformation.getFolderName());
        OdiFolder folder;
        try {
            folder = odiAccessStrategy.findFolderByName(transformation.getFolderName(),
                    properties.getProjectCode());
        } catch (ResourceNotFoundException e1) {
            logger.info("Adding folder.");
            folder = odiAccessStrategy.addFolder(transformation.getFolderName());
        }
        Mapping mapping = null;
        if (properties.isUpdateable()) {
            // already done, no need to truncate twice
            //truncateInterfaces(transformation);
            try {
                mapping = (Mapping) odiAccessStrategy.findMappingsByName(transformation.getName(),
                        transformation.getFolderName(), properties.getProjectCode());
            } catch (ResourceNotFoundException e) {
                mapping = constructMapping(folder, transformation.getName());
            }
        }
        if (mapping == null) {
            mapping = constructMapping(folder, transformation.getName());
        }
        assert (odiInstance != null) : "OdiInstance is null.";
        assert (mapping != null) : "Mapping is null.";
        assert (transformation.getName() != null) : "Mapping name is null.";
        return mapping;
    }

    /**
     * @param folder
     * @param interfaceName
     * @return @throws AdapterException @throws MappingException
     */
    private Mapping constructMapping(final OdiFolder folder, final String interfaceName)
            throws AdapterException, MappingException {
        assert (folder != null);
        return new Mapping(interfaceName, folder);
    }

    /**
     * Create a FILECOMPONENT, DATASTORE_COMPONENT or REUSABLEMAPPING_COMPONENT.
     *
     * @param mapping
     * @param boundObject
     * @param autoJoinEnabled
     * @return @throws AdapterException @throws MappingException
     */
    private IMapComponent createComponent(final MapRootContainer mapping, final IModelObject boundObject,
                                          final boolean autoJoinEnabled) throws AdapterException, MappingException {
        String type = "";
        if (boundObject instanceof OdiDataStore) {
            if (((OdiDataStore) boundObject).getModel().getTechnology().getName().toLowerCase().equals("file")) {
                type = "FILE";
            } else {
                type = "DATASTORE";
            }
        } else {
            type = "REUSABLEMAPPING";
        }
        return mapping.createComponent(type, boundObject, autoJoinEnabled);
    }


    /**
     * Return the transformations that were created.
     */
    @Override
    public HashMap<String, Transformation> getTransformationCache() {
        return transformationCache;
    }

    /**
     * Remove all mapcomponents from an interface.
     *
     * @param transformationName name of the transformation
     * @param foldername         name of the folder of the transformation
     * @throws TransformationException exception during transformation
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void truncateInterfaces(String transformationName, String foldername)
            throws TransformationException {
        try {
            MapRootContainer mapping = odiAccessStrategy.findMappingsByName(transformationName,
                    foldername, properties.getProjectCode());
            if (mapping != null) {
                Iterator<IMapComponent> iter = mapping.getAllComponents().iterator();
                while (iter.hasNext()) {
                    IMapComponent mc = iter.next();
                    mc.removeAllConnections();
                    try {
                        mapping.removeComponent(mc, true);
                    } catch (NullPointerException npe) {
                        logger.debug(npe);
                    }
                    iter.remove();
                }
                mapping.clearProperties();
                if (mapping instanceof Mapping) {
                    ((Mapping) mapping).clearPhysicalDesigns();
                }
            }
            logger.debug("successfully truncated: " + transformationName);
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationException("MapComponentException", e);
        } catch (AdapterException e) {
            logger.error("AdapterException", e);
            throw new TransformationException("AdapterException", e);
        } catch (MappingException e) {
            logger.error("MappingException", e);
            throw new TransformationException("MappingException", e);
        } catch (ResourceNotFoundException | ResourceFoundAmbiguouslyException e) {
            // Nothing to do
            logger.debug("ResourceNotFoundException on truncate: " + transformationName + " in folder; " + foldername);
        }
    }


    /**
     * @param executionLocation
     * @return
     */
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
     * @param mapping
     * @param transformation
     * @param etlOperators
     * @return @throws AdapterException @throws
     * MappingException @throws TransformationAccessStrategyException
     * @throws ResourceNotFoundException
     */
    private OdiDataStore createTargetComponents(final MapRootContainer mapping, final Transformation transformation,
                                                final EtlOperators etlOperators) throws AdapterException, MappingException,
            TransformationAccessStrategyException, ResourceNotFoundException {
        if (transformation.isTemporary()) {
            etlOperators.addTargetComponents(new OutputSignature(mapping, transformation.getName()));
            MapConnectorPoint icp = etlOperators.getTargetComponents().get(0).getInputConnectorPoint(0);
            for (Targetcolumn targetColumn : transformation.getMappings().getTargetColumns()) {
                String prefix = "";
                if (transformation.getMappings().isDistinct()) {
                    prefix = ComponentPrefixType.DISTINCT.getAbbreviation();
                } else if (transformation.useExpressions()) {
                    prefix = ComponentPrefixType.TARGET_EXPRESSIONS.getAbbreviation();
                } else if (transformation.getDatasets().size() > 1) {
                    prefix = ComponentPrefixType.SETCOMPONENT.getAbbreviation();
                } else if (transformation.useExpressions()) {
                    prefix = ComponentPrefixType.TARGET_EXPRESSIONS.getAbbreviation();
                } else if (transformation.getMappings().isAggregateTransformation(1)) {
                    prefix = ComponentPrefixType.AGGREGATE.getAbbreviation();
                }
                String expression = prefix + "." + targetColumn.getName();
                if (targetColumn.getMappingExpressions() == null || targetColumn.getMappingExpressions().get(0) == null
                        || targetColumn.getMappingExpressions().get(0).trim() == null
                        || targetColumn.getMappingExpressions().get(0).trim().toLowerCase().equals("null")) {
                    expression = " null ";
                }
                icp.createAttribute(targetColumn.getName()).setExpressionText(expression);
            }
            return null;
        } else {
            OdiDataStore targetDatastore = odiAccessStrategy.findDataStore(
                    transformation.getMappings().getTargetDataStore(), transformation.getMappings().getModel());
            assert (targetDatastore != null) : "TargetDS not found.";
            boolean autoJoinEnabled = false;
            etlOperators.addTargetComponents(createComponent(mapping, targetDatastore, autoJoinEnabled));
            assert (etlOperators.getTargetComponents().get(0) != null) : "targetComponent not found.";
            return targetDatastore;
        }
    }

    private void setBeginAndEndMapping(MapRootContainer mapping, Transformation transformation, List<IMapComponent> targetComponents) throws MapPhysicalException, PropertyException {
        if (!targetComponents.get(0).getTypeName().equals("OUTPUTSIGNATURE") &&
                (transformation.getBeginMappingCommand() != null || transformation.getEndMappingCommand() != null)) {
            MapPhysicalDesign physicalDesign = ((Mapping) mapping).getPhysicalDesign(0);
            if (transformation.getBeginMappingCommand() != null) {
                OdiTechnology tech = odiAccessStrategy.findTechnologyByCode(transformation.getBeginMappingCommand().getTechnology());
                assert (tech != null) : "Technology not found with name : " + transformation.getBeginMappingCommand().getTechnology();
                physicalDesign.setBeginCmd(transformation.getBeginMappingCommand().getText());
                physicalDesign.setBeginCmdTechnology(tech);
                String model;
                try {
                    model = transformation.getBeginMappingCommand().getModel();
                } catch (JodiPropertyNotFoundException dpnfe) {
                    model = null;
                }
                if (model != null)
                    physicalDesign.setBeginCmdLogicalSchema((ILogicalSchema) odiAccessStrategy.findLogicalSchemaByName(model));
            }
            if (transformation.getEndMappingCommand() != null) {
                OdiTechnology tech = odiAccessStrategy.findTechnologyByCode(transformation.getEndMappingCommand().getTechnology());
                assert (tech != null) : "Technology not found with name : " + transformation.getEndMappingCommand().getTechnology();
                physicalDesign.setEndCmd(transformation.getEndMappingCommand().getText());
                physicalDesign.setEndCmdTechnology(tech);
                String model;
                try {
                    model = transformation.getEndMappingCommand().getModel();
                } catch (JodiPropertyNotFoundException dpnfe) {
                    model = null;
                }
                if (model != null)
                    physicalDesign.setEndCmdLogicalSchema((ILogicalSchema) odiAccessStrategy.findLogicalSchemaByName(model));
            }
        }
    }

    private Targetcolumn getTargetColumn(final Transformation transformation,
                                         final String targetColumnName) throws TransformationException {
        for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
            if (tc.getName().equals(targetColumnName)) {
                return tc;
            }
        }
        assert (false); // shouldn't be here;
        throw new TransformationException("Can't find targetcolumn " + targetColumnName);
    }

}