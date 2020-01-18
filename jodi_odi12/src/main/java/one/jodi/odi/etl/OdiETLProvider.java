package one.jodi.odi.etl;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.service.metadata.*;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.odi.common.FlexfieldUtil;
import one.jodi.odi.common.OdiConstants;
import one.jodi.odi.packages.OdiBasePackageServiceProvider;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.flexfields.IFlexFieldValue;
import oracle.odi.domain.model.*;
import oracle.odi.domain.model.finder.IOdiColumnFinder;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.finder.IOdiCKMFinder;
import oracle.odi.domain.project.finder.IOdiIKMFinder;
import oracle.odi.domain.project.finder.IOdiLKMFinder;
import oracle.odi.domain.project.finder.IOdiVariableFinder;
import oracle.odi.domain.topology.*;
import oracle.odi.domain.topology.AbstractOdiDataServer.IConnectionSettings;
import oracle.odi.domain.topology.finder.IOdiContextFinder;
import oracle.odi.domain.topology.finder.IOdiFlexFieldFinder;
import oracle.odi.domain.topology.finder.IOdiLogicalSchemaFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Entry point to the ETL subsystem.
 *
 */
@Singleton
public class OdiETLProvider implements SchemaMetaDataProvider, OdiCommon {

    private final static String ERROR_MESSAGE_03380 =
            "Cannot find IKM with name: %s, projectCode: %s.";

    private final static String ERROR_MESSAGE_03390 =
            "Cannot find JKM with name: %s, projectCode: %s.";

    private final static String ERROR_MESSAGE_03400 =
            "Cannot find CKM with name: %s, projectCode: %s.";

    private final static String ERROR_MESSAGE_03410 =
            "Cannot find LKM with name: %s, projectCode: %s.";

    private final static Logger logger = LogManager.getLogger(OdiETLProvider.class);

    private final OdiInstance odiInstance;
    private final FlexfieldUtil<OdiModel> modelFlexFieldUtil;
    private final FlexfieldUtil<OdiDataStore> dataStoreFlexFieldUtil;
    private final OdiVariableAccessStrategy odiVariableService;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private final Map<String, String> modelsAndSchemas = new HashMap<String, String>();

    @Inject
    protected OdiETLProvider(final OdiInstance odiInstance,
                             final FlexfieldUtil<OdiModel> modelFlexFieldUtil,
                             final FlexfieldUtil<OdiDataStore> dataStoreFlexFieldUtil,
                             final OdiVariableAccessStrategy odiVariableService,
                             final JodiProperties properties,
                             final ModelPropertiesProvider modelPropProvider,
                             final ErrorWarningMessageJodi errorWarningMessages) {
        this.odiInstance = odiInstance;
        this.modelFlexFieldUtil = modelFlexFieldUtil;
        this.dataStoreFlexFieldUtil = dataStoreFlexFieldUtil;
        this.odiVariableService = odiVariableService;
        this.properties = properties;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Cached
    @Override
    public boolean existsProject(final String projectCode) {
        boolean found = false;

        @SuppressWarnings("unchecked")
        Collection<OdiProject> c = odiInstance.getTransactionalEntityManager()
                .getFinder(OdiProject.class)
                .findAll();
        for (OdiProject candidate : c) {
            if (candidate.getCode().equals(projectCode)) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Cached
    public OdiKM<?> findIKMByName(String name, String projectCode) {
        Collection<? extends OdiKM<?>> ikm =
                ((IOdiIKMFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiIKM.class))
                        .findByName(name, projectCode);
        if (ikm == null || ikm.isEmpty()) {
            errorWarningMessages.addMessage("Can't find IKM '" + name + "'", MESSAGE_TYPE.ERRORS);
        }
        try {
            return (ikm == null || ikm.isEmpty() ? null : ikm.iterator().next());
        } catch (NoSuchElementException nsee) {
            String msg =
                    errorWarningMessages.formatMessage(3380, ERROR_MESSAGE_03380,
                            this.getClass(), name, projectCode);
            logger.error(msg, nsee);
            throw new UnRecoverableException(msg, nsee);
        }
    }

    @Override
    public OdiKM<?> findIKMByName(String name) {
        Collection<? extends OdiKM<?>> ikm =
                ((IOdiIKMFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiIKM.class))
                        .findGlobalByName(name);
        if (ikm == null || ikm.isEmpty()) {
            errorWarningMessages.addMessage("Can't find global IKM '" + name + "'", MESSAGE_TYPE.ERRORS);
        }
        try {
            return (ikm == null || ikm.isEmpty() ? null : ikm.iterator().next());
        } catch (NoSuchElementException nsee) {
            String msg =
                    errorWarningMessages.formatMessage(3380, ERROR_MESSAGE_03380,
                            this.getClass(), name, "global");
            logger.error(msg, nsee);
            throw new UnRecoverableException(msg, nsee);
        }
    }

    @Cached
    public OdiKM<?> findJKMByName(String name, String projectCode) {
        Collection<? extends OdiKM<?>> jkm =
                ((IOdiIKMFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiJKM.class))
                        .findByName(name, projectCode);
        try {
            return (jkm == null || jkm.isEmpty() ? null : jkm.iterator().next());
        } catch (NoSuchElementException nsee) {
            String msg =
                    errorWarningMessages.formatMessage(3390, ERROR_MESSAGE_03390,
                            this.getClass(), name, projectCode);
            logger.error(msg, nsee);
            throw new UnRecoverableException(msg);
        }
    }

    @Cached
    public OdiKM<?> findCKMByName(String name, String projectCode) {
        Collection<OdiCKM> ckm =
                ((IOdiCKMFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiCKM.class))
                        .findByName(name, projectCode);

        if (ckm == null || ckm.isEmpty()) {
            errorWarningMessages.addMessage("Can't find CKM '" + name + "'", MESSAGE_TYPE.ERRORS);
        }

        OdiCKM ckmChoosen = null;
        try {
            ckmChoosen = (ckm == null || ckm.isEmpty() ? null : ckm.iterator().next());
        } catch (NoSuchElementException nse) {
            String msg =
                    errorWarningMessages.formatMessage(3400, ERROR_MESSAGE_03400,
                            this.getClass(), name, projectCode);
            logger.error(msg, nse);
            throw new UnRecoverableException(msg, nse);
        }
        return ckmChoosen;
    }

    @Override
    public OdiKM<?> findCKMByName(String name) {
        Collection<OdiCKM> ckm =
                ((IOdiCKMFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiCKM.class))
                        .findGlobalByName(name);

        if (ckm == null || ckm.isEmpty()) {
            errorWarningMessages.addMessage("Can't find CKM '" + name + "'", MESSAGE_TYPE.ERRORS);
        }

        OdiCKM ckmChoosen = null;
        try {
            ckmChoosen = (ckm == null || ckm.isEmpty() ? null : ckm.iterator().next());
        } catch (NoSuchElementException nse) {
            String msg =
                    errorWarningMessages.formatMessage(3400, ERROR_MESSAGE_03400,
                            this.getClass(), name, "global");
            logger.error(msg, nse);
            throw new UnRecoverableException(msg, nse);
        }
        return ckmChoosen;
    }

    @Cached
    public OdiKM<?> findLKMByName(String name, String projectCode) {
        Collection<OdiLKM> lkm =
                ((IOdiLKMFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiLKM.class))
                        .findByName(name, projectCode);
        if (lkm == null || lkm.isEmpty()) {
            errorWarningMessages.addMessage("Can't find LKM '" + name + "'", MESSAGE_TYPE.ERRORS);
        }
        logger.debug("attempting to find LKM by name = " + name);
        try {
            return (lkm == null || lkm.isEmpty() ? null : lkm.iterator().next());
        } catch (NoSuchElementException nsee) {
            String msg =
                    errorWarningMessages.formatMessage(3410, ERROR_MESSAGE_03410,
                            this.getClass(), name, projectCode);
            logger.error(msg, nsee);
            throw new UnRecoverableException(msg, nsee);
        }
    }

    @Override
    public OdiKM<?> findLKMByName(final String name) {
        Collection<OdiLKM> lkm =
                ((IOdiLKMFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiLKM.class))
                        .findAllGlobals().stream().filter(n -> n.getName().equalsIgnoreCase(name))
                        .collect(Collectors.toList());
        if (lkm == null || lkm.isEmpty()) {
            if (!name.endsWith(".GLOBAL")) {
                lkm =
                        ((IOdiLKMFinder) odiInstance.getTransactionalEntityManager()
                                .getFinder(OdiLKM.class))
                                .findAllGlobals().stream().filter(n -> n.getName().equalsIgnoreCase(name + ".GLOBAL"))
                                .collect(Collectors.toList());
            }
        }
        if (lkm == null || lkm.isEmpty()) {
            Collection<OdiLKM> lkmOptions =
                    ((IOdiLKMFinder) odiInstance.getTransactionalEntityManager()
                            .getFinder(OdiLKM.class))
                            .findAllGlobals();
            StringBuilder options = new StringBuilder();
            lkmOptions.stream().forEach(l -> options.append(l.getName() + ","));
            errorWarningMessages.addMessage("Can't find LKM '" + name + "' valid options are " + options.toString() + '.', MESSAGE_TYPE.ERRORS);
        }
        logger.debug("attempting to find LKM by name = " + name);
        try {
            return (lkm == null || lkm.isEmpty() ? null : lkm.iterator().next());
        } catch (NoSuchElementException nsee) {
            String msg =
                    errorWarningMessages.formatMessage(3410, ERROR_MESSAGE_03410,
                            this.getClass(), name, "global");
            logger.error(msg, nsee);
            throw new UnRecoverableException(msg, nsee);
        }
    }

    private ColumnMetaData createColumnMetaData(final OdiColumn odiColumn) {
        IOdiFlexFieldFinder finder =
                ((IOdiFlexFieldFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiFlexField.class));
        odiColumn.initFlexFields(finder);

        ColumnMetaData result = new ColumnMetaData() {

            @Override
            public String getColumnDataType() {
                String dataType = null;
                if (odiColumn.getDataType() != null) {
                    dataType = odiColumn.getDataType().getName();
                }
                return dataType;
            }

            @Override
            public SlowlyChangingDataType getColumnSCDType() {
                if (odiColumn.getScdType() != null) {
                    if (odiColumn.getScdType().toString().equals("SK"))
                        return SlowlyChangingDataType.SURROGATE_KEY;
                    else if (odiColumn.getScdType().toString().equals("NK"))
                        return SlowlyChangingDataType.NATURAL_KEY;
                    else if (odiColumn.getScdType().toString().equals("OC"))
                        return SlowlyChangingDataType.OVERWRITE_ON_CHANGE;
                    else if (odiColumn.getScdType().toString().equals("IR"))
                        return SlowlyChangingDataType.ADD_ROW_ON_CHANGE;
                    else if (odiColumn.getScdType().toString().equals("CR"))
                        return SlowlyChangingDataType.CURRENT_RECORD_FLAG;
                    else if (odiColumn.getScdType().toString().equals("ST"))
                        return SlowlyChangingDataType.START_TIMESTAMP;
                    else if (odiColumn.getScdType().toString().equals("ET"))
                        return SlowlyChangingDataType.END_TIMESTAMP;
                    else
                        return SlowlyChangingDataType.valueOf(odiColumn
                                .getScdType().toString());
                }
                return null;
            }

            @Override
            public boolean hasNotNullConstraint() {
                return odiColumn.isMandatory();
            }

            @Override
            public Map<String, Object> getFlexFieldValues() {
                Map<String, Object> result = new HashMap<>();
                for (IFlexFieldValue fv : odiColumn.getFlexFieldsValues()) {
                    result.put(fv.getName(), fv.getValue());
                }
                return Collections.unmodifiableMap(result);
            }

            @Override
            public String getName() {
                return odiColumn.getName();
            }

            @Override
            public int getLength() {
                return odiColumn.getLength() != null ? odiColumn.getLength() : 0;
            }

            @Override
            public int getScale() {
                return odiColumn.getScale() != null ? odiColumn.getScale() : 0;
            }

            @Override
            public String getDescription() {
                return odiColumn.getDescription();
            }

            @Override
            public int getPosition() {
                return odiColumn.getPosition();
            }

            @Override
            public String getDataStoreName() {
                return odiColumn.getDataStore().getName();
            }
        };

        return result;
    }

    @Cached
    @Override
    public List<String> getModelCodes() {

        List<String> projectModels = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Collection<OdiModel> odiModels = odiInstance.getTransactionalEntityManager()
                .getFinder(OdiModel.class)
                .findAll();
        projectModels.addAll(odiModels.stream()
                .map(OdiModel::getCode)
                .collect(Collectors.toList()));

        return projectModels;
    }

    private Key.KeyType mapKeyType(OdiKey.KeyType type) {

        Key.KeyType result;
        switch (type) {
            case PRIMARY_KEY:
                result = Key.KeyType.PRIMARY;
                break;
            case ALTERNATE_KEY:
                result = Key.KeyType.ALTERNATE;
                break;
            default:
                result = Key.KeyType.INDEX;
        }
        return result;
    }

    private Key createKey(final OdiKey odiKey) {
        final List<String> columnNames = new ArrayList<>();
        for (OdiColumn column : odiKey.getColumns()) {
            columnNames.add(column.getName());
        }
        return new Key() {
            @Override
            public String getName() {
                return odiKey.getName();
            }

            @Override
            public KeyType getType() {
                return mapKeyType(odiKey.getKeyType());
            }

            @Override
            public List<String> getColumns() {
                return Collections.unmodifiableList(columnNames);
            }

            @Override
            public boolean existsInDatabase() {
                return odiKey.isInDatabase();
            }

            @Override
            public boolean isEnabledInDatabase() {
                return odiKey.isActive();
            }

            @Override
            public String getDataStoreName() {
                return odiKey.getDataStore().getName();
            }

            @Override
            public void setDataStoreName(String datastoreName) {
            }
        };
    }

    private ForeignReference createFKReference(final OdiReference odiFKRefs) {

        final List<ForeignReference.RefColumns> refColumns = new ArrayList<>();
        for (final ReferenceColumn refColumn : odiFKRefs.getReferenceColumns()) {
            // we do not return complex reference types for now - only FK
            // relationships
            if (odiFKRefs.getReferenceType() != OdiReference.ReferenceType
                    .COMPLEX_REFERENCE) {
                refColumns.add(new ForeignReference.RefColumns() {
                    @Override
                    public String getForeignKeyColumnName() {
                        return refColumn.getForeignKeyColumn().getName();
                    }

                    @Override
                    public String getPrimaryKeyColumnName() {
                        return refColumn.getPrimaryKeyColumn().getName();
                    }
                });
            }
        }

        return new ForeignReference() {
            @Override
            public String getName() {
                return odiFKRefs.getName();
            }

            @Override
            public String getPrimaryKeyDataStoreName() {
                return odiFKRefs.getPrimaryDataStore().getName();
            }

            @Override
            public String getPrimaryKeyDataStoreModelCode() {
                return odiFKRefs.getPrimaryDataStore().getModel().getCode();
            }

            @Override
            public List<RefColumns> getReferenceColumns() {
                return refColumns;
            }

            @Override
            public boolean isEnabledInDatabase() {
                return odiFKRefs.isActive();
            }
        };
    }

    private DataStoreDescriptor createDescriptor(
            final OdiDataStore dataStore,
            final OdiModel model,
            final DataModelDescriptor dataModelDescriptor,
            final boolean isTemporary) {
        // collect Column information
        final Collection<ColumnMetaData> columns = new ArrayList<>();
        for (OdiColumn odiColumn : dataStore.getColumns()) {
            columns.add(createColumnMetaData(odiColumn));
        }

        // collect Key information
        final List<Key> keys = new ArrayList<>();
        for (final OdiKey odiKey : dataStore.getKeys()) {
            keys.add(createKey(odiKey));
        }

        // collect FK constraints; they are associated with the table that
        // contains
        // foreign keys
        final List<ForeignReference> fkRefs = new ArrayList<>();
        for (OdiReference odiFKRefs : dataStore.getOutboundReferences()) {
            fkRefs.add(createFKReference(odiFKRefs));
        }

        DataStoreDescriptor descriptor = new DataStoreDescriptor() {

            @Override
            public String getDataStoreName() {
                return dataStore.getName();
            }

            @Override
            public boolean isTemporary() {
                return isTemporary;
            }

            @Override
            public Map<String, Object> getDataStoreFlexfields() {
                return Collections.unmodifiableMap(
                        dataStoreFlexFieldUtil.getFlexFieldValues(dataStore));
            }

            @Override
            public DataModelDescriptor getDataModelDescriptor() {
                return dataModelDescriptor;
            }

            @Override
            public Collection<ColumnMetaData> getColumnMetaData() {
                return Collections.unmodifiableCollection(columns);
            }

            @Override
            public List<Key> getKeys() {
                return Collections.unmodifiableList(keys);
            }

            @Override
            public List<ForeignReference> getFKRelationships() {
                return Collections.unmodifiableList(fkRefs);
            }

            @Override
            public String getDescription() {
                return dataStore.getDescription();
            }
        };

        return descriptor;
    }

    @Cached
    public OdiModel getOdiModel(final String modelCode) {
        @SuppressWarnings("unchecked")
        Collection<OdiModel> odiModels = odiInstance.getTransactionalEntityManager()
                .getFinder(OdiModel.class)
                .findAll();
        for (OdiModel model : odiModels) {
            if (model.getCode().equals(modelCode)) {
                return model;
            }
        }
        String error = String.format("Model '%1$s' not found.", modelCode);
        throw new RuntimeException(error);
    }

    private DataModelDescriptor createDataModelDescriptor(final OdiModel model) {

        DataModelDescriptor dmDesc = new DataModelDescriptor() {

            @Override
            public String getModelCode() {
                return model.getCode();
            }

            @Override
            public Map<String, Object> getModelFlexfields() {
                return Collections.unmodifiableMap(
                        modelFlexFieldUtil.getFlexFieldValues(model));
            }

            @Override
            public String getPhysicalDataServerName() {
                String targetContext = properties.getProperty(OdiConstants.ODI_CONTEXT);
                OdiContext context = getContext(targetContext);
                String url = "defaultserver";
                try {
                    IConnectionSettings connection =
                            model.getLogicalSchema()
                                    .getPhysicalSchema(context)
                                    .getDataServer()
                                    .getConnectionSettings();
                    if (connection instanceof AbstractOdiDataServer.JdbcSettings) {
                        url = ((AbstractOdiDataServer.JdbcSettings) connection).getJdbcUrl();
                        String[] parts = url.split(":");
                        url = parts.length > 4 ? parts[3].replace("@", "") : url;
                    }
                } catch (NoSuchMethodError e) {
                    // method not found exception from odi12c while running from
                    // command line.
                    logger.debug(e);
                }
                return url;
            }

            @Override
            public String getDataServerName() {
                String targetContext = properties.getProperty(OdiConstants.ODI_CONTEXT);
                OdiContext context = getContext(targetContext);
                OdiLogicalSchema ls = model.getLogicalSchema();
                // if you remove this, test from odi12 command line sample06
                String name = "defaultDataServerName";
                try {
                    OdiPhysicalSchema ps = ls.getPhysicalSchema(context);
                    OdiDataServer ds = ps.getDataServer();
                    name = ds.getName();
                } catch (NoSuchMethodError e) {
                    // method not found exception from odi12c while running from
                    // command line.
                    logger.debug(e);
                } catch (NullPointerException npe) {
                    String msg = "Dataserver name not found for logical schema " +
                            (ls != null ? ls.getName() + "," : "unknown,") + " set the logical schema for this model, for context "
                            + (context != null ? context.getName() : " unknown");
                    logger.error(msg, npe);
                    throw new NullPointerException(msg);
                }
                return name;
            }

            @Override
            public String getDataServerTechnology() {
                return model.getTechnology().getName();
            }

            @Override
            public String getSchemaName() {
                String targetContext = properties
                        .getProperty(OdiConstants.ODI_CONTEXT);
                OdiContext context = getContext(targetContext);
                String schemaName = "defaultSchemaName";
                try {
                    OdiPhysicalSchema pschema = model.getLogicalSchema()
                            .getPhysicalSchema(context);
                    // it is the default schema name of the dmt.
                    schemaName = pschema.getSchemaName();
                } catch (Exception e) {
                    // an overbroadly catch, on purpose,
                    // due to different exceptions in different ODI versions.
                    //
                    // method not found exception from odi12c while running from
                    // command line.
                    // use the modelName as schema name for for instance MySQL.
                    schemaName = model.getName();
                    logger.debug(e);
                }
                return schemaName;
            }

            @Override
            public String getDataBaseServiceName() {
                String targetContext = properties.getProperty(OdiConstants.ODI_CONTEXT);
                OdiContext context = getContext(targetContext);
                String dbName = "defaultSchemaName";
                try {
                    IConnectionSettings connection = model.getLogicalSchema()
                            .getPhysicalSchema(context)
                            .getDataServer()
                            .getConnectionSettings();
                    if (connection instanceof AbstractOdiDataServer.JdbcSettings) {
                        dbName = ((AbstractOdiDataServer.JdbcSettings) connection).getJdbcUrl();
                        String[] parts = dbName.split(":");
                        dbName = parts.length > 4
                                ? parts[4].substring(parts[4].indexOf("/") + 1,
                                parts[4].length())
                                : dbName;
                    }
                } catch (NoSuchMethodError e) {
                    // method not found exception from odi12c while running from
                    // command line.
                    logger.debug(e);
                }
                return dbName;
            }

            @Override
            public int getDataBaseServicePort() {
                String targetContext = properties.getProperty(OdiConstants.ODI_CONTEXT);
                OdiContext context = getContext(targetContext);
                String port = "defaultSchemaName";
                int portInt = 0;
                try {
                    IConnectionSettings connection = model.getLogicalSchema()
                            .getPhysicalSchema(context)
                            .getDataServer()
                            .getConnectionSettings();
                    if (connection instanceof AbstractOdiDataServer.JdbcSettings) {
                        port = ((AbstractOdiDataServer.JdbcSettings) connection).getJdbcUrl();
                        String[] parts = port.split(":");
                        try {
                            port = parts.length > 4
                                    ? parts[4].substring(0, parts[4].indexOf("/"))
                                    : port;
                            portInt = Integer.parseInt(port);
                        } catch (NumberFormatException nfe) {
                            logger.debug("Port not applicable for this server.");
                        } catch (StringIndexOutOfBoundsException se) {
                            // old sid style jdbc url
                            port = parts.length > 4 ? parts[4] : port;
                        }
                    }
                } catch (NoSuchMethodError e) {
                    // method not found exception from odi12c while running from
                    // command line.
                    logger.debug(e);
                }
                return portInt;
            }
        };
        return dmDesc;
    }

    @Cached
    private OdiContext getContext(String code) {
        IOdiContextFinder finder =
                (IOdiContextFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiContext.class);
        OdiContext context = finder.findByCode(code);
        assert (context != null) : "Wrong context provided.";
        return context;
    }

    @Cached
    @Override
    public List<DataModelDescriptor> getDataModelDescriptors() {

        List<DataModelDescriptor> dmdList = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Collection<OdiModel> odiModels = odiInstance.getTransactionalEntityManager()
                .getFinder(OdiModel.class)
                .findAll();
        for (OdiModel model : odiModels) {
            dmdList.add(createDataModelDescriptor(model));
        }
        return dmdList;
    }

    @Cached
    @Override
    public Map<String, DataStoreDescriptor> getDataStoreDescriptorsInModel(
            final String modelCode) {

        // Follows the following strategy:
        // 1) Get all models in repository and find named model
        // 2) Get all data stores via the global sub-model
        // 3) for each data store that is not temporary create a descriptor
        // and add in return list

        // for OHI to avoid 'old style' datastores created from temporary
        // interfaces
        // here the pattern is repeated 'SXX'.
        final Pattern tempNameTableMatcher = Pattern.compile(
                this.properties.getTemporaryInterfacesRegex());

        OdiModel idModel = getOdiModel(modelCode);
        DataModelDescriptor dmDescr = createDataModelDescriptor(idModel);

        Map<String, DataStoreDescriptor> descriptors = new TreeMap<>();
        if (idModel != null) {
            // Retrieve data store descriptors from the global model
            for (OdiDataStore dataStore : idModel.getGlobalSubModel().getDataStores()) {
                if (!tempNameTableMatcher.matcher(dataStore.getName()).find()) {
                    descriptors.put(dataStore.getName(),
                            createDescriptor(dataStore, idModel, dmDescr, false));
                }
            }

            // Retrieve data store descriptors from sub-models as well
            for (OdiSubModel subModel : idModel.getSubModels()) {
                for (OdiDataStore dataStore : subModel.getDataStores()) {
                    descriptors.put(dataStore.getName(),
                            createDescriptor(dataStore, idModel, dmDescr, false));
                }
            }
        }

        return descriptors;
    }

    @Override
    public void initDBConnection(String jdbcUrl, String username, String password) {
    }

    @Override
    public void closeDBConnection() {
    }

    @Override
    public boolean projectVariableExists(final String projectCode,
                                         final String variableName) {
        OdiVariable odiVariable =
                odiVariableService.findProjectVariable(variableName, projectCode);
        if (odiVariable != null && odiVariable.getLogicalSchema() == null) {
            String msg = errorWarningMessages.formatMessage(80801,
                    OdiBasePackageServiceProvider.ERROR_MESSAGE_80801,
                    this.getClass(), variableName);
            throw new RuntimeException(msg);
        }

        return (odiVariable != null);
    }

    @Override
    public Map<String, String> translateModelToLogicalSchema() {
        if (modelsAndSchemas.size() == 0) {
            IOdiModelFinder finder =
                    (IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                            .getFinder(OdiModel.class);
            @SuppressWarnings("unchecked")
            Collection<OdiModel> models = finder.findAll();
            for (OdiModel m : models) {
                modelsAndSchemas.put(m.getCode(), m.getLogicalSchema().getName());
            }
            return modelsAndSchemas;
        } else {
            return modelsAndSchemas;
        }
    }

    @Override
    public Set<String> getLogicalSchemaNames() {
        IOdiLogicalSchemaFinder finder =
                (IOdiLogicalSchemaFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiLogicalSchema.class);
        @SuppressWarnings("unchecked")
        Collection<OdiLogicalSchema> schemas = finder.findAll();
        return schemas.stream()
                .map(s -> s.getName())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getTableNames() {
        IOdiDataStoreFinder finder =
                (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiDataStore.class);
        @SuppressWarnings("unchecked")
        Collection<OdiDataStore> schemas = finder.findAll();
        return schemas.stream()
                .map(s -> s.getModel().getLogicalSchema().getName() + "." +
                        s.getName())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getColumnNames() {
        IOdiColumnFinder finder =
                (IOdiColumnFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiColumn.class);
        @SuppressWarnings("unchecked")
        Collection<OdiColumn> schemas = finder.findAll();
        return schemas.stream()
                .map(s -> s.getDataStore().getModel().getLogicalSchema().getName() +
                        "." + s.getDataStore().getName() + "." + s.getName())
                .collect(Collectors.toSet());
    }

    @Override
    @Cached
    public boolean globalVariableExists(String variableName) {
        IOdiVariableFinder finder =
                (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiVariable.class);
        OdiVariable variable = finder.findGlobalByName(variableName);
        return variable != null;
    }
}
