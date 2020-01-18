package one.jodi.odi12.mappings;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi.interfaces.ResourceCreationException;
import one.jodi.odi12.folder.Odi12FolderServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.topology.ILogicalSchema;
import oracle.odi.domain.mapping.*;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.*;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MapPhysicalException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.mapping.finder.IReusableMappingFinder;
import oracle.odi.domain.mapping.physical.ExecutionUnit;
import oracle.odi.domain.mapping.physical.MapPhysicalDesign;
import oracle.odi.domain.mapping.physical.MapPhysicalNode;
import oracle.odi.domain.mapping.properties.Property;
import oracle.odi.domain.mapping.properties.PropertyException;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.runtime.loadplan.finder.IOdiLoadPlanFinder;
import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.OdiTechnology;
import oracle.odi.domain.topology.finder.IOdiContextFinder;
import oracle.odi.domain.topology.finder.IOdiLogicalSchemaFinder;
import oracle.odi.domain.topology.finder.IOdiTechnologyFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Stream.concat;

public class Odi12TransformationAccessStrategy implements
        OdiTransformationAccessStrategy<MapRootContainer, Dataset, DatastoreComponent,
                ReusableMappingComponent, IMapComponent,
                OdiContext, ILogicalSchema> {

    private final static Logger logger =
            LogManager.getLogger(Odi12TransformationAccessStrategy.class);

    private final OdiInstance odiInstance;
    private final Odi12FolderServiceProvider folderService;
    private final JodiProperties properties;
    private final EnrichingBuilder enrichBuilder;

    private final HashMap<String, OdiDataStore> cacheDataStores = new HashMap<>();
    private final HashMap<String, OdiModel> cacheModels = new HashMap<>();

    @Inject
    Odi12TransformationAccessStrategy(final OdiInstance odiInstance,
                                      final Odi12FolderServiceProvider folderService,
                                      final EnrichingBuilder enrichBuilder,
                                      final JodiProperties properties,
                                      final ErrorWarningMessageJodi errorWarningMessages) {
        this.odiInstance = odiInstance;
        this.folderService = folderService;
        this.properties = properties;
        this.enrichBuilder = enrichBuilder;
    }

    @Cached
    @Override
    public OdiProject findProject(final String projectCode)
            throws ResourceNotFoundException {
        final IOdiProjectFinder finder =
                ((IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiProject.class));
        if (finder.findByCode(projectCode) == null) {
            throw new ResourceNotFoundException("Cannot find ODI project with code " +
                    projectCode);
        }
        return finder.findByCode(projectCode);
    }

    @Override
    @Cached
    public OdiContext findOdiContext(final String code) throws ResourceNotFoundException {
        final IOdiContextFinder finder =
                ((IOdiContextFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiContext.class));
        OdiContext odiContext = finder.findByCode(code);
        if (odiContext == null) {
            throw new ResourceNotFoundException("No ODI context can be found with code " +
                    code);
        }
        return odiContext;
    }

    @Cached
    @Override
    public OdiTechnology findTechnologyByCode(final String technology) {
        IOdiTechnologyFinder techFinder =
                (IOdiTechnologyFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiTechnology.class);
        return techFinder.findByCode(technology);
    }

    @Cached
    @Override
    public ILogicalSchema findLogicalSchemaByName(final String model) {
        IOdiLogicalSchemaFinder modelFinder =
                (IOdiLogicalSchemaFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiLogicalSchema.class);
        return modelFinder.findByName(model);
    }

    @Override
    public OdiFolder findFolderByName(final String folderName, final String project)
            throws ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        Optional<OdiFolder> found = folderService.findFolderByName(folderName, project);
        if (!found.isPresent()) {
            throw new ResourceNotFoundException("No folder found with name '" +
                    folderName + "'.");
        }
        return found.get();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiFolder addFolder(final String folderPath) throws ResourceCreationException {
        Map<String, OdiFolder> folders =
                folderService.findOrCreateFolders(Arrays.asList(folderPath),
                        properties.getProjectCode());
        if (folders.get(folderPath) != null) {
            logger.info(String.format("created folder '%s'.", folderPath));
        }
        return folders.get(folderPath);
    }

    @SuppressWarnings("unchecked")
    private Collection<Mapping> getMappings() {
        final IMappingFinder finder =
                ((IMappingFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(Mapping.class));
        return finder.findAll();
    }

    @SuppressWarnings("unchecked")
    private Collection<ReusableMapping> getReusableMappings() {
        final IReusableMappingFinder finder =
                ((IReusableMappingFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(ReusableMapping.class));
        return finder.findAll();
    }

    @Override
    public Collection<MapRootContainer> findMappingsByProject(final String projectCode) {
        return concat(getMappings().stream(), getReusableMappings().stream())
                .filter(m -> m.getProject().getCode().equals(projectCode))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<MapRootContainer> findMappingsByFolder(final String projectCode,
                                                             final String folder) {
        Collection<MapRootContainer> mappings = Collections.emptyList();
        // identify (nested) folder first and then find Mapping
        try {
            OdiFolder cFolder = findFolderByName(folder, projectCode);
            mappings = (Collection<MapRootContainer>) (Collection<?>) cFolder.getMappings();
        } catch (ResourceNotFoundException | ResourceFoundAmbiguouslyException e) {
            // ignore and do not return mappings
        }
        return mappings;
    }

    @Override
    public MapRootContainer findMappingsByName(final String lookupDataStore,
                                               final String projectCode)
            throws ResourceNotFoundException, ResourceFoundAmbiguouslyException {
        return findMappingsByName(lookupDataStore, null, projectCode);
    }

    @Override
    public MapRootContainer findMappingsByName(final String name, final String folder,
                                               final String projectCode)
            throws ResourceFoundAmbiguouslyException, ResourceNotFoundException {

        if (enrichBuilder.isTemporaryTransformation(name)) {
            return findReusableMapping(name, folder, projectCode);
        } else {
            return findMapping(name, folder, projectCode);
        }
    }

    private MapRootContainer findMapping(final String name, final String folder,
                                         final String projectCode)
            throws ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        final IMappingFinder finder =
                ((IMappingFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(Mapping.class));

        // identify (nested) folder first and then find Mapping
        if (folder != null) {
            OdiFolder cFolder = findFolderByName(folder, projectCode);
            return finder.findByName(cFolder, name);
        }

        // folder name not supplied. Identify map by name only
        final Collection<Mapping> c = finder.findByName(name, projectCode);
        if (c.size() > 1) {
            throw (new ResourceFoundAmbiguouslyException(
                    String.format("Found multiple (%3$s) mappings in project '%1$s' " +
                            "with name '%2$s'.", projectCode, name, c.size())));
        } else if (c.size() < 1) {
            throw new ResourceNotFoundException(
                    String.format("Found no mapping in project '%1$s' with name '%2$s'.",
                            projectCode, name));
        } else {
            return c.iterator().next();
        }
    }

    private MapRootContainer findReusableMapping(final String name, final String folder,
                                                 final String projectCode)
            throws ResourceNotFoundException {
        IReusableMappingFinder finderRM =
                ((IReusableMappingFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(ReusableMapping.class));
        if (folder != null) {
            ReusableMapping m = null;
            try {
                OdiFolder cFolder = findFolderByName(folder, projectCode);
                m = finderRM.findByName(cFolder, name);
            } catch (ResourceFoundAmbiguouslyException e) {
                throw new RuntimeException();
            }
            return m;
        } else {
            @SuppressWarnings("unchecked")
            Collection<ReusableMapping> all = (Collection<ReusableMapping>) finderRM.findAll();
            for (ReusableMapping m : all) {
                if (m.getName().equals(name) && m.getProject().getCode().equals(projectCode)) {
                    logger.debug(String.format("Found 1 mapping in project '%1$s' with " +
                            "name '%2$s'.", projectCode, name));
                    return m;
                }
            }
            throw new ResourceNotFoundException(
                    String.format("Found no mapping in project '%1$s' with name '%2$s'.",
                            projectCode, name));
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteMappingsByName(final String name, final String folder,
                                     final String projectCode)
            throws ResourceNotFoundException,
            ResourceFoundAmbiguouslyException {
        logger.debug("Attempting to delete mapping with name " + name +
                " in project code " + projectCode);
        MapRootContainer mapping = findMappingsByName(name, folder, projectCode);
        if (mapping != null && mapping.getName() != null) {
            logger.debug(String.format("Removing 1 mapping in project '%1$s' with " +
                    "name '%2$s'.", projectCode, name));
            odiInstance.getTransactionalEntityManager().remove(mapping);
        }
    }

    @Override
    public Collection<Dataset> findAllDataSets(final MapRootContainer mapping)
            throws TransformationAccessStrategyException {
        final Collection<Dataset> dss = new ArrayList<>();
        try {
            dss.addAll(mapping.getAllComponents().stream()
                    .filter(mc -> mc.getTypeName().equals("DATASET"))
                    .map(mc -> (Dataset) mc).collect(Collectors.toList()));
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException(e);
        }
        return dss;
    }

    @Override
    public boolean isOneOfTheSourcesDerived(final MapRootContainer mapping)
            throws TransformationAccessStrategyException {
        final boolean result = false;
        try {
            for (IMapComponent source : mapping.getSources()) {
                for (Property property : source.getAllProperties()) {
                    if (property.getName().equals("SUBSELECT_ENABLED")) {
                        if (property.getValue().equals(true)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException(e);
        } catch (PropertyException e) {
            logger.error("PropertyException", e);
            throw new TransformationAccessStrategyException(e);
        }
        return result;
    }

    @Override
    public boolean areAllDatastoresJoinedNaturally(final MapRootContainer mapping)
            throws TransformationAccessStrategyException {
        final boolean result = true;
        try {
            for (IMapComponent mc : mapping.getAllComponents()) {
                if (mc.getTypeName().equals("JOIN")) {
                    JoinComponent join = (JoinComponent) mc;
                    if (!join.getJoinType().toUpperCase().contains("NATURAL")) {
                        return false;
                    }
                }
            }
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException(e);
        } catch (PropertyException e) {
            logger.error("PropertyException", e);
            throw new TransformationAccessStrategyException(e);
        }
        return result;
    }

    @Override
    public Set<String> findStagingAreas(final MapRootContainer mapping, final String contextCode) {
        final Set<String> logicalschemaNames = new HashSet<>();
        if (mapping instanceof Mapping) {
            for (MapPhysicalDesign pd : ((Mapping) mapping).getPhysicalDesigns()) {
                logicalschemaNames.addAll(
                        pd.getStagingExUnitList().stream().map(ExecutionUnit::getName)
                                .collect(Collectors.toList()));
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return logicalschemaNames;
    }

    @Override
    public boolean validateDataSetRelation(final MapRootContainer mapping)
            throws TransformationAccessStrategyException {
        try {
            for (Object o : findAllDataSets(mapping)) {
                Dataset dataset = (Dataset) o;
                for (MapAttribute me : dataset.getAttributes()) {
                    if (!me.getExpression().getText().startsWith(dataset.getSources().get(0).getAlias())) {
                        return false;
                    }
                }
            }
        } catch (AdapterException e) {
            logger.error("AdapterException", e);
            throw new TransformationAccessStrategyException(e);
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException(e);
        } catch (MappingException e) {
            logger.error("MappingException", e);
            throw new TransformationAccessStrategyException(e);
        } catch (Exception e) {
            logger.error("Exception", e);
            throw new TransformationAccessStrategyException(e);
        }
        return true;
    }

    @Cached
    @Override
    public int findGlobalVersion() {
        final int version;
        if (odiInstance.getWorkRepository().getLegacyConnectionDef().getProductVersion().isBefore11G())
            version = 10;
        else if (odiInstance.getWorkRepository().getLegacyConnectionDef().getProductVersion().toString()
                .startsWith("11."))
            version = 11;
        else if (odiInstance.getWorkRepository().getLegacyConnectionDef().getProductVersion().toString()
                .startsWith("12."))
            version = 12;
        else
            version = -1;
        // logger.debug("Version:"+odiInstance.getWorkRepository().getLegacyConnectionDef().getProductVersion().toString());
        return version;
    }

    @Override
    public List<Dataset> findDatasets(final MapRootContainer map) throws TransformationAccessStrategyException {
        final List<Dataset> dss = new ArrayList<>();
        try {
            dss.addAll(map.getAllComponents().stream()
                    .filter(mc -> mc.getTypeName().equals("DATASET"))
                    .map(mc -> (Dataset) mc).collect(Collectors.toList()));
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException("MapComponentException", e);
        }
        return dss;
    }

    //@Cached
    @Override
    public OdiDataStore findDataStore(final String tableName, final String model) throws ResourceNotFoundException {
        if (cacheDataStores.size() == 0) {
            initCache();
        }
        OdiDataStore odiJoinedDatastore = cacheDataStores.get(model + "." + tableName);
//		final OdiDataStore odiJoinedDatastore = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
//				.getFinder(OdiDataStore.class)).findByName(tableName, model);
//		assert(odiJoinedDatastore != null);
        if (odiJoinedDatastore == null) {
            throw new ResourceNotFoundException("Joined datastore can't be null.");
        }
        return odiJoinedDatastore;
    }

    private void initCache() {
        @SuppressWarnings("unchecked")
        Collection<OdiDataStore> allDs =
                (Collection<OdiDataStore>) ((IOdiDataStoreFinder)
                        odiInstance.getTransactionalEntityManager()
                                .getFinder(OdiDataStore.class))
                        .findAll();
        for (OdiDataStore ds : allDs) {
            cacheDataStores.put(ds.getModel().getCode() + "." + ds.getName(), ds);
        }
    }

    private void initCacheModel() {
        @SuppressWarnings("unchecked")
        Collection<OdiModel> allModels =
                (Collection<OdiModel>) ((IOdiModelFinder)
                        odiInstance.getTransactionalEntityManager()
                                .getFinder(OdiModel.class))
                        .findAll();
        for (OdiModel m : allModels) {
            cacheModels.put(m.getCode(), m);
        }
    }

    @Cached
    @Override
    public DatastoreComponent findSourceLookupDataStore(final MapRootContainer odiInterface,
                                                        final Transformation transformation, final int dataSetIndex, final Lookup lookup,
                                                        final int dataSetNumberOfLoookup) throws TransformationAccessStrategyException {
        throw new UnsupportedOperationException();
    }

    @Cached
    @Override
    public DatastoreComponent findSourceDataStore(final MapRootContainer mapping, final Transformation transformation,
                                                  final int dataSetIndex, final Source source) throws TransformationAccessStrategyException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IMapComponent findSourceComponent(final MapRootContainer mapping,
                                             final SubQuery subquery) throws TransformationAccessStrategyException {
        final String alias = subquery.getName();
        // if the loops don't find anything it is not signalled...
        try {
            for (IMapComponent mc : mapping.getAllComponents()) {
                if (mc.getTypeName().equals("DATASTORE")) {
                    DatastoreComponent sourceDataStore = (DatastoreComponent) mc;
                    if (alias.equals(getComponentName(sourceDataStore))) {
                        return sourceDataStore;
                    }
                } else if (mc.getTypeName().equals("FILE")) {
                    FileComponent sourceDataStore = (FileComponent) mc;
                    if (alias.equals(getComponentName(sourceDataStore))) {
                        return sourceDataStore;
                    }
                } else if (mc.getTypeName().equals("REUSABLEMAPPING")) {
                    ReusableMappingComponent rmc = (ReusableMappingComponent) mc;
                    // length of
                    // journalizing
                    // prefix = 2
                    String aliasRmc = getComponentName(rmc);
                    if (alias.equals(aliasRmc)) {
                        return rmc;
                    }
                }
            }
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException("ODI error encountered finding source component", e);
        }
        logger.fatal(subquery.getName() + " was not found.");
        assert (false);
        throw new TransformationAccessStrategyException("No source component found.");
    }

    @Override
    public IMapComponent findSourceComponent(final MapRootContainer mapping, final Transformation transformation,
                                             final Source source) throws TransformationAccessStrategyException {
        final String alias = source.getComponentName();
        // if the loops don't find anything it is not signalled...
        try {
            for (IMapComponent mc : mapping.getAllComponents()) {
                if (mc.getTypeName().equals("DATASTORE")) {
                    DatastoreComponent sourceDataStore = (DatastoreComponent) mc;
                    if (alias.equals(getComponentName(sourceDataStore))) {
                        return sourceDataStore;
                    }
                } else if (mc.getTypeName().equals("FILE")) {
                    FileComponent sourceDataStore = (FileComponent) mc;
                    if (alias.equals(getComponentName(sourceDataStore))) {
                        return sourceDataStore;
                    }
                } else if (mc.getTypeName().equals("REUSABLEMAPPING")) {
                    ReusableMappingComponent rmc = (ReusableMappingComponent) mc;
                    // length of
                    // journalizing
                    // prefix = 2
                    String aliasRmc = getComponentName(rmc);
                    if (alias.equals(aliasRmc)) {
                        return rmc;
                    }
                }
            }
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException("ODI error encountered finding source component", e);
        }
        logger.fatal(source.getComponentName() + " was not found.");
        assert (false);
        throw new TransformationAccessStrategyException("No source component found.");
    }

    @Cached
    @Override
    public OdiModel findModel(final String code) {
        if (cacheModels.size() == 0) {
            initCacheModel();
        }
        return cacheModels.get(code);
    }

    @Override
    public IMapComponent findLookupComponent(final MapRootContainer mapping, final Transformation transformation,
                                             final Lookup lookup) throws TransformationAccessStrategyException {
        final String alias = lookup.getComponentName();
        try {
            for (IMapComponent mc : mapping.getAllComponents()) {
                if (mc.getTypeName().equals("DATASTORE")) {
                    DatastoreComponent sourceDataStore = (DatastoreComponent) mc;
                    String aliasFound = getComponentName(sourceDataStore);
                    if (alias.equals(aliasFound)) {
                        return sourceDataStore;
                    }
                } else if (mc.getTypeName().equals("FILE")) {
                    FileComponent sourceDataStore = (FileComponent) mc;
                    String aliasFound = getComponentName(sourceDataStore);
                    if (alias.equals(aliasFound)) {
                        return sourceDataStore;
                    }
                } else if (mc.getTypeName().equals("REUSABLEMAPPING")) {
                    ReusableMappingComponent rmc = (ReusableMappingComponent) mc;
                    String aliasRmc = getComponentName(rmc);
                    if (alias.equals(aliasRmc)) {
                        return rmc;
                    }
                }
            }
        } catch (MapComponentException e) {
            logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException("MapComponentException", e);
        }
        logger.fatal(lookup.getLookupDataStore() + " was not found.");
        assert (false);
        return null;
    }

    @Cached
    @Override
    public String getComponentName(final IMapComponent sds) {
        return sds.getAlias() != null ? sds.getAlias() : sds.getName();
    }

    @Override
    public int findSequenceNumberFromDataSet(final Dataset dataSet) {
        int seq = -1;
        String name = dataSet.getName();
        int index = name.lastIndexOf(JodiProperties.DATASETSEPERATOR);

        try {
            if ((index >= 0) && (index < name.length())) {
                seq = Integer.parseInt(name.substring(index + 1, name.length()));
            }
        } catch (NumberFormatException e) {
            // We simply eat this exception since this indicates that the
            // remainder
            // of the string was not a sequence number
            logger.info("Exception parsing sequence number", e);
        }
        return seq;
    }

    @Override
    public int getJoinNumber(final String name) {
//		final String number = name.substring(name.lastIndexOf("_") + 2, name.length());
//		return Integer.parseInt(number);
        int startIndex = name.indexOf("_") + 2;
        String withoutDataSet = name.substring(startIndex, name.length());
        final String number;
        if (withoutDataSet.indexOf("_") > -1) {
            number = withoutDataSet.substring(0, withoutDataSet.indexOf("_"));
        } else {
            number = withoutDataSet;
        }
        // it is either;
        // D1_L2 or D1_L2_W_CUSTOMER_D
        //final String number = name.substring(startIndex, name.length());
        return Integer.parseInt(number);
    }

    @Override
    public int getDataSetNumberFromComponentName(final IMapComponent value) {
        String name = getComponentName(value);
        if (getComponentName(value).startsWith(ComponentPrefixType.FILTER.getAbbreviation() + "_")) {
            name = name.substring(2, name.length());
        }

        return Integer.parseInt(name.substring(1, 2));
    }

    @Override
    public IMapComponent getComponentByName(final MapRootContainer mapping, final String name)
            throws TransformationAccessStrategyException, ResourceNotFoundException {
        try {
            for (IMapComponent mc : mapping.getAllComponents()) {
                if (mc.getName().equals(name)) {
                    return mc;
                }
            }
        } catch (MapComponentException e) {
            // logger.error("MapComponentException", e);
            throw new TransformationAccessStrategyException("MapComponentException", e);
        }
        // assert (false) : "Mapcomponent not found.";
        throw new ResourceNotFoundException("MapComponent with name '" + name + "' not found");
    }

    @Override
    public boolean checkThatAllTargetsHaveCKMName(MapRootContainer mapping, String ckmName)
            throws TransformationAccessStrategyException {
        boolean result = false;
        if (mapping instanceof Mapping) {
            for (MapPhysicalDesign pd : ((Mapping) mapping).getPhysicalDesigns()) {
                try {
                    for (ExecutionUnit teu : pd.getTargetExecutionUnits()) {
                        for (MapPhysicalNode target : teu.getTargetNodes()) {
                            if (target.getCheckKM().getName().equals(ckmName)) {
                                result = true;
                            } else {
                                return false;
                            }
                        }
                    }
                } catch (AdapterException e) {
                    // logger.error("AdapterException", e);
                    throw new TransformationAccessStrategyException("MapComponentException", e);
                } catch (MappingException e) {
                    // logger.error("MappingException", e);
                    throw new TransformationAccessStrategyException("MappingException", e);
                }
            }
        }
        logger.debug("result: " + result);
        return result;
    }

    @Override
    public boolean checkThatAllTargetsHaveIKMName(MapRootContainer mapping, String ckmName)
            throws TransformationAccessStrategyException {
        boolean result = false;
        if (mapping instanceof Mapping) {
            for (MapPhysicalDesign pd : ((Mapping) mapping).getPhysicalDesigns()) {
                try {
                    for (ExecutionUnit teu : pd.getTargetExecutionUnits()) {
                        for (MapPhysicalNode target : teu.getTargetNodes()) {
                            if (target.getIKM().getName().equals(ckmName)) {
                                result = true;
                            } else {
                                return false;
                            }
                        }
                    }
                } catch (AdapterException e) {
                    logger.error("AdapterException", e);
                    throw new TransformationAccessStrategyException("AdapterException", e);
                } catch (MappingException e) {
                    logger.error("MappingException", e);
                    throw new TransformationAccessStrategyException("MappingException", e);
                }
            }
        }
        return result;
    }

    @Override
    public boolean checkThatAllTargetsHaveLKMName(MapRootContainer mapping, String ckmName)
            throws TransformationAccessStrategyException {
        boolean result = false;
        if (mapping instanceof Mapping) {
            MapPhysicalDesign physicalDesign;
            try {
                physicalDesign = ((Mapping) mapping).getPhysicalDesign(0);
            } catch (MapPhysicalException e) {
                logger.error("MapPhysicalException", e);
                throw new TransformationAccessStrategyException("MapPhysicalException", e);
            }
            for (MapPhysicalNode node : physicalDesign.getAllAPNodes()) {
                if (node.getLKM().getName().equals(ckmName)) {
                    result = true;
                } else {
                    return false;
                }
            }
        }
        return result;
    }

    @Override
    public boolean isDistinctMapping(MapRootContainer mapping) throws Exception {
        boolean result = false;
        for (IMapComponent comp : mapping.getAllComponents()) {
            if (comp instanceof DistinctComponent) {
                return true;
            }
        }
        return result;
    }

    private MapAttribute findColumnByName(final IMapComponent target, final String columnName)
            throws MappingException, ResourceNotFoundException {
        for (MapAttribute ma : target.getAttributes()) {
            if (ma.getName().equals(columnName)) {
                return ma;
            }
        }

        throw new ResourceNotFoundException("Mapping " + target.getName() + " has no column with name " + columnName);
    }

    @Override
    public Map<String, Boolean> getFlags(final String projectCode, final String mappingName, final String columnName)
            throws ResourceNotFoundException, ResourceFoundAmbiguouslyException, TransformationAccessStrategyException {
        Map<String, Boolean> flags = new HashMap<>();

        try {
            Mapping mapping = (Mapping) findMappingsByName(mappingName, projectCode);
            if (mapping == null) {
                // throw new TransformationAccessStrategyException("Mapping not
                // found");
            }
            MapAttribute column = null;
            for (IMapComponent target : mapping.getTargets()) {
                column = findColumnByName(target, columnName);
                break;
            }

            // artifact of findbugs - despite fact that findColumnByName throws
            // exception when col not found.
            if (column != null) {
                // add mandatory flag
                flags.put(MANDATORY, (Boolean) column.isCheckNotNullIndicator());
                // add insert flag
                flags.put(INSERT, (Boolean) column.isInsertIndicator());
                // add update flag
                flags.put(UPDATE, (Boolean) column.isUpdateIndicator());
                // add update key flag
                flags.put(KEY, (Boolean) column.isKeyIndicator());
            }

            // add values of user defined flags
            for (int i = 0; i < 10; i++) {
                Boolean ud = (Boolean) column.getPropertyValue("UD_" + (i + 1));
                flags.put("UD" + (i + 1), ud);
            }
        } catch (MappingException e) {
            throw new TransformationAccessStrategyException("ODI Exception encountered during flag retrieval", e);
        }

        return flags;
    }

    @Override
    public Map<String, String> getFilterExecutionLocations(MapRootContainer mapping)
            throws TransformationAccessStrategyException {
        try {
            Map<String, String> filterExecutionLocations = new HashMap<>();
            for (IMapComponent component : mapping.getAllComponentsOfType(FilterComponent.COMPONENT_TYPE_NAME)) {
                FilterComponent filter = (FilterComponent) component;
                filterExecutionLocations.put(filter.getName(), filter.getExecuteOnHint().name());
            }
            return filterExecutionLocations;
        } catch (MappingException me) {
            logger.error("MappingException", me);
            throw new TransformationAccessStrategyException(me);
        }
    }

    @Override
    public Map<String, String> getJoinExecutionLocations(MapRootContainer mapping)
            throws TransformationAccessStrategyException {
        try {
            Map<String, String> joinExecutionLocations = new HashMap<>();
            for (IMapComponent component : mapping.getAllComponentsOfType(JoinComponent.COMPONENT_TYPE_NAME)) {
                JoinComponent join = (JoinComponent) component;
                joinExecutionLocations.put(join.getName(), join.getExecuteOnHint().name());
            }
            return joinExecutionLocations;
        } catch (MappingException me) {
            logger.error("MappingException", me);
            throw new TransformationAccessStrategyException(me);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<OdiLoadPlan> findAllLoadPlans() {
        IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiLoadPlan.class);
        return loadPlanFinder.findAll();
    }

    @Override
    public boolean areAllSourcesNotJournalised(MapRootContainer interf) {
        boolean result = true;
        try {
            for (IMapComponent component : interf.getSources()) {
                if (component instanceof DatastoreComponent) {
                    if (((DatastoreComponent) component).isJournalized()) {
                        logger.info("Component is journalized:" + component.getName() + " of mapping: " + interf.getName());
                        return false;
                    }
                }
            }
        } catch (MapComponentException e) {
            logger.error(e);
            return false;
        } catch (AdapterException e) {
            logger.error(e);
            return false;
        } catch (MappingException e) {
            logger.error(e);
            return false;
        }
        return result;
    }

    @Override
    public boolean areAllSourcesJournalised(MapRootContainer interf) {
        boolean result = true;
        try {
            for (IMapComponent component : interf.getSources()) {
                if (component instanceof DatastoreComponent) {
                    if (!((DatastoreComponent) component).isJournalized()) {
                        logger.info("Component is not journalized:" + component.getName() + " of mapping: " + interf.getName());
                        return false;
                    }
                }
            }
        } catch (MapComponentException e) {
            logger.error(e);
            return false;
        } catch (AdapterException e) {
            logger.error(e);
            return false;
        } catch (MappingException e) {
            logger.error(e);
            return false;
        }
        return result;
    }

    @Override
    public String getBeginOrEndMappingText(MapRootContainer mapping, String beginOrEnd) {
        MapPhysicalDesign physicalDesign;
        try {
            physicalDesign = ((Mapping) mapping).getPhysicalDesign(0);
            if (beginOrEnd.toLowerCase().equals("begin")) {
                return physicalDesign.getBeginCmd().getText();
            } else if (beginOrEnd.toLowerCase().equals("end")) {
                return physicalDesign.getEndCmd().getText();
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (PropertyException e) {
            return null;
        } catch (MapPhysicalException e) {
            return null;
        }
    }

    @Override
    public String getBeginOrEndMappingLocationCode(MapRootContainer mapping, String beginOrEnd) {
        MapPhysicalDesign physicalDesign;
        try {
            physicalDesign = ((Mapping) mapping).getPhysicalDesign(0);
            if (beginOrEnd.toLowerCase().equals("begin")) {
                return ((OdiLogicalSchema) physicalDesign.getProperty("BEGIN_MAPPING_CMD_LOCATION").getValue()).getName();
            } else if (beginOrEnd.toLowerCase().equals("end")) {
                return ((OdiLogicalSchema) physicalDesign.getProperty("END_MAPPING_CMD_LOCATION").getValue()).getName();
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (PropertyException e) {
            return null;
        } catch (AdapterException e) {
            return null;
        } catch (MapPhysicalException e) {
            return null;
        }
    }

    @Override
    public String getBeginOrEndMappingTechnologyCode(MapRootContainer mapping, String beginOrEnd) {
        MapPhysicalDesign physicalDesign;
        try {
            physicalDesign = ((Mapping) mapping).getPhysicalDesign(0);
            if (beginOrEnd.toLowerCase().equals("begin")) {
                return ((OdiTechnology) physicalDesign.getProperty("BEGIN_MAPPING_CMD_TECH").getValue()).getInternalName();
            } else if (beginOrEnd.toLowerCase().equals("end")) {
                return ((OdiTechnology) physicalDesign.getProperty("END_MAPPING_CMD_TECH").getValue()).getInternalName();
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (PropertyException e) {
            return null;
        } catch (AdapterException e) {
            return null;
        } catch (MapPhysicalException e) {
            return null;
        }
    }

    @Override
    public Map<String, String> getSubQueryExecutionLocation(MapRootContainer mapping)
            throws TransformationAccessStrategyException {
        try {
            Map<String, String> subqueryExecutionLocations = new HashMap<>();
            for (IMapComponent component : mapping.getAllComponentsOfType(SubqueryFilterComponent.COMPONENT_TYPE_NAME)) {
                SubqueryFilterComponent filter = (SubqueryFilterComponent) component;
                subqueryExecutionLocations.put(filter.getName(), filter.getExecuteOnHint().name());
            }
            return subqueryExecutionLocations;
        } catch (MappingException me) {
            logger.error("MappingException", me);
            throw new TransformationAccessStrategyException(me);
        }
    }

}