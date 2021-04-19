package one.jodi.odi12.etl.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi12.etl.DatastoreBuilder;
import one.jodi.odi12.etl.EtlOperators;
import one.jodi.odi12.etl.FlowsBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.IModelObject;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.ReusableMapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.ReusableMappingComponent;
import oracle.odi.domain.mapping.exception.MapPhysicalException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.topology.OdiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class DatastoreBuilderImpl implements DatastoreBuilder {


    private static final Logger logger = LogManager.getLogger(DatastoreBuilderImpl.class);
    private final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema>
            odiAccessStrategy;
    private final FlowsBuilder flowsBuilder;
    private final JodiProperties properties;

    @Inject
    protected DatastoreBuilderImpl(
            final OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent, ReusableMappingComponent, IMapComponent, OdiContext, ILogicalSchema> odiAccessStrategy,
            final JodiProperties properties, final HashMap<String, MapRootContainer> mappingCache,
            final FlowsBuilder flowsBuilder) {
        this.odiAccessStrategy = odiAccessStrategy;
        this.flowsBuilder = flowsBuilder;
        this.properties = properties;
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.DatastoreBuilder#addDatasource(oracle.odi.domain.mapping.MapRootContainer, one.jodi.etl.internalmodel.Source, int, boolean, one.jodi.odi12.etl.EtlOperators)
     */
    @Override
    public void addDatasource(final MapRootContainer mapping, final Source source, final int packageSequence,
                              final boolean journalized, EtlOperators etlOperators) throws AdapterException,
            MapPhysicalException, MappingException, TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        if (source.isTemporary()) {
            addAsTemporaryDatasourceToInterface(mapping, source, packageSequence, journalized, etlOperators);
        } else {
            addAsPermanentDatasourceToInterface(mapping, source, packageSequence, journalized, etlOperators);
        }
    }

    /**
     * @param mapping
     * @param source
     * @param packageSequence
     * @param journalized     @throws AdapterException @throws
     *                        MappingException @throws TransformationAccessStrategyException
     * @throws ResourceNotFoundException
     */
    private void addAsPermanentDatasourceToInterface(final MapRootContainer mapping, final Source source,
                                                     final int packageSequence, final boolean journalized,
                                                     EtlOperators etlOperators) throws AdapterException,
            MappingException, TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        OdiDataStore boundTo = odiAccessStrategy.findDataStore(source.getName(), source.getModel());
        String alias = source.getComponentName();
        IMapComponent sourceComponent = createComponent(mapping, boundTo, false);
        sourceComponent.setName(alias);

        logger.debug("Creating source datastore " + source.getAlias());
        flowsBuilder.addFlow(mapping, source, etlOperators, journalized);

    }

    /**
     * @param mapping
     * @param source
     * @param packageSequence
     * @param journalized     @throws AdapterException @throws
     *                        MapPhysicalException @throws MappingException @throws
     *                        TransformationAccessStrategyException
     * @param etlOperators
     * @throws ResourceFoundAmbiguouslyException
     * @throws ResourceNotFoundException
     */
    private void addAsTemporaryDatasourceToInterface(final MapRootContainer mapping, final Source source,
                                                     final int packageSequence, final boolean journalized,
                                                     EtlOperators etlOperators) throws AdapterException,
            MapPhysicalException, MappingException, TransformationAccessStrategyException, ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        /*
         * the source datastore is a temporary interface
         */
        logger.debug(packageSequence + " Source temp datastore: " + source.getName());
        String alias = source.getComponentName();
        String folder = source.getParent()
                              .getParent()
                              .getFolderName();
        ReusableMapping boundTo = (ReusableMapping) odiAccessStrategy.findMappingsByName(source.getName(), folder,
                                                                                         properties.getProjectCode());
        assert (boundTo != null) : " boundTo not found in folder: " + folder + " for source: " + source.getName();
        ReusableMappingComponent sourceComponent = (ReusableMappingComponent) createComponent(mapping, boundTo, false);
        sourceComponent.setName(alias);
        mapping.addComponent(sourceComponent);

        flowsBuilder.addFlow(mapping, source, etlOperators, journalized);
    }

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.DatastoreBuilder#createComponent(oracle.odi.domain.mapping.MapRootContainer, oracle.odi.domain.adapter.IModelObject, boolean)
     */
    @Override
    public IMapComponent createComponent(final MapRootContainer mapping, final IModelObject boundObject,
                                         final boolean autoJoinEnabled) throws AdapterException, MappingException {
        String type = "";
        if (boundObject instanceof OdiDataStore) {
            if (((OdiDataStore) boundObject).getModel()
                                            .getTechnology()
                                            .getName()
                                            .toLowerCase()
                                            .equals("file")) {
                type = "FILE";
            } else {
                type = "DATASTORE";
            }
        } else {
            type = "REUSABLEMAPPING";
        }
        return mapping.createComponent(type, boundObject, autoJoinEnabled);
    }

}
