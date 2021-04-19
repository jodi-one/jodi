package one.jodi.core.metadata;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.context.Context;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.model.types.LazyCreation;
import one.jodi.base.model.types.ModelSolutionLayerType;
import one.jodi.base.model.types.impl.DataModelImpl;
import one.jodi.base.model.types.impl.DataStoreColumnImpl;
import one.jodi.base.model.types.impl.DataStoreImpl;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataModelDescriptor;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.base.service.schema.DatabaseSchemaServiceImpl;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.extensions.strategies.NoModelFoundException;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.service.SubsystemServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static one.jodi.base.util.StringUtils.endsWithIgnoreCase;

public class DatabaseMetadataServiceImpl extends DatabaseSchemaServiceImpl
        implements DatabaseMetadataService, LazyCreation {

    private static final Logger LOGGER = LogManager.getLogger(DatabaseMetadataServiceImpl.class);

    private static final String ERROR_MESSAGE_00030 = "Unknown data model %s.";

    private final SchemaMetaDataProvider etlProvider;
    private final SubsystemServiceProvider subsystemServiceProvider;
    private final Context context;
    private final JodiProperties jodiProperties;
    private final ModelPropertiesProvider modelPropertiesProvider;
    private final Pattern tempNameTableMatcher;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * @param jodiProperties - reference to the Jodi properties files that are injected
     * @param etlProvider    - refers to a service for accessing functionality of the ETL
     *                       tool
     */
    @Inject
    public DatabaseMetadataServiceImpl(final SchemaMetaDataProvider etlProvider,
                                       final SubsystemServiceProvider subsystemServiceProvider,
                                       final Context context, final JodiProperties jodiProperties,
                                       final ModelPropertiesProvider modelPropertiesProvider,
                                       final ErrorWarningMessageJodi errorWarningMessages) {
        super(etlProvider, context, errorWarningMessages);
        this.etlProvider = etlProvider;
        this.subsystemServiceProvider = subsystemServiceProvider;
        this.context = context;
        this.jodiProperties = jodiProperties;
        this.modelPropertiesProvider = modelPropertiesProvider;
        this.errorWarningMessages = errorWarningMessages;
        tempNameTableMatcher = Pattern.compile(this.jodiProperties.getTemporaryInterfacesRegex());
    }

    @Override
    public List<ModelProperties> getConfiguredModels() {
        return modelPropertiesProvider.getConfiguredModels();
    }

    @Override
    public Map<String, PropertyValueHolder> getCoreProperties() {
        List<String> exclusions = subsystemServiceProvider.getPropertyNameExclusionList();

        // Skips keys that are explicitly referenced in the OdiConstants class as they refer to ODI-specific details
        // that should not be made available in execution contexts.
        Map<String, PropertyValueHolder> collect = jodiProperties.getPropertyKeys().stream()
                .filter(key -> !exclusions.contains(key))
                .collect(Collectors.toMap(key -> key, jodiProperties::getPropertyValueHolder));
        return Collections.unmodifiableMap(collect);
    }

    private ModelProperties findModelPropertyByCode(final String modelCode) {
        return modelPropertiesProvider.getConfiguredModels().stream()
                .filter(Objects::nonNull)
                .filter(mp -> modelCode.equals(mp.getCode()))
                .findAny()
                .orElse(null);
    }

    @Override
    public List<DataStore> findDataStoreInAllModels(final String dataStoreName) {
        // find data stores in different models that have the name provided in
        // the dataStoreName parameter
        final List<DataStore> foundDataStores = new ArrayList<>();

        for (String modelCode : etlProvider.getModelCodes()) {
            Map<String, DataStoreDescriptor> d = etlProvider
                    .getDataStoreDescriptorsInModel(modelCode);
            if (d.containsKey(dataStoreName)) {
                foundDataStores.add(findOrCreateDataStore(d.get(dataStoreName)));
            }
        }

        // add temporary tables that are registered with the cache
        final List<DataStore> registeredTemporaryDataStores = context.getAllTempTables();
        foundDataStores.addAll(registeredTemporaryDataStores.stream()
                .filter(tds -> dataStoreName.equals(tds.getDataStoreName()))
                .collect(Collectors.toList()));

        return Collections.unmodifiableList(foundDataStores);
    }

    private DataModelDescriptor findModelDescriptor(final String modelCode) {
        return etlProvider.getDataModelDescriptors().stream()
                .filter(Objects::nonNull)
                .filter(dmd -> dmd.getModelCode().equals(modelCode))
                .findAny()
                .orElse(null);
    }

    @Override
    public boolean isTemporaryTransformation(String tableName) {
        return tempNameTableMatcher.matcher(tableName).find();
    }

    @Override
    public DataStore getSourceDataStoreInModel(final String dataStoreName,
                                               final String modelCode) {
        // consider temporary interfaces
        return getDataStoreInModel(dataStoreName, modelCode, null, true);
    }

    @Override
    public DataStoreType getDataStoreType(final String dataStoreName) {
        DataStoreType dsType = DataStoreType.UNKNOWN;
        if (isOfTypeSCD2(dataStoreName)) {
            dsType = DataStoreType.SLOWLY_CHANGING_DIMENSION;
        } else if (isFact(dataStoreName)) {
            dsType = DataStoreType.FACT;
        } else if (isDimension(dataStoreName)) {
            dsType = DataStoreType.DIMENSION;
        } else if (isHelper(dataStoreName)) {
            dsType = DataStoreType.HELPER;
        }
        return dsType;
    }

    private boolean isDimension(String targetDataStore) {
        boolean hasDataMartPrefix = jodiProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX).stream()
                .anyMatch(targetDataStore::startsWith);
        return hasDataMartPrefix && endsWithIgnoreCase(targetDataStore,
                jodiProperties.getProperty(JodiConstants.DIMENSION_SUFFIX));
    }

    private boolean isFact(String targetDataStore) {
        boolean hasDataMartPrefix = jodiProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX).stream()
                .anyMatch(targetDataStore::startsWith);
        return hasDataMartPrefix && jodiProperties.getPropertyList(JodiConstants.FACT_SUFFIX).stream()
                .anyMatch(fs -> endsWithIgnoreCase(targetDataStore, fs));
    }

    private boolean isHelper(String targetDataStore) {
        boolean hasDataMartPrefix = jodiProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX).stream()
                .anyMatch(targetDataStore::startsWith);
        return hasDataMartPrefix
                && endsWithIgnoreCase(targetDataStore, jodiProperties.getProperty(JodiConstants.HELPER_SUFFIX));
    }

    @Override
    public boolean isSourceModel(String modelCode) {
        return isTypeModel(modelCode, ModelSolutionLayerType.SOURCE.getSolutionLayerName());
    }

    @Override
    public boolean isConnectorModel(String modelCode) {
        return isTypeModel(modelCode, ModelSolutionLayerType.EDW_SDS.getSolutionLayerName());
    }

    private boolean isTypeModel(String modelCode, String modelTypeFlag) {
        return getConfiguredModels().stream()
                .filter(Objects::nonNull)
                .filter(mp -> mp.getCode().equalsIgnoreCase(modelCode))
                .findAny()
                .map(ModelProperties::getLayer)
                .map(layer -> layer.equalsIgnoreCase(modelTypeFlag))
                .orElse(false);
    }

    @Cached
    @Override
    public boolean isOfTypeSCD2(final String aOdiDataStoreName) {

        // TODO - assumes that Data Store is only in one of the star models or
        // have similar characteristics. We should generalize this by
        // passing in the model associated with the data store.
        List<ModelProperties> mpList = modelPropertiesProvider
                .getConfiguredModels(Arrays.asList(ModelSolutionLayerType.STAR.getSolutionLayerName(), "dm"));

        Optional<DataStoreDescriptor> odiDataStore = mpList.stream()
                .map(ModelProperties::getCode)
                .map(etlProvider::getDataStoreDescriptorsInModel)
                .map(dsMap -> dsMap.get(aOdiDataStoreName))
                .filter(Objects::nonNull)
                .findFirst();

        if (!odiDataStore.isPresent()) {
            LOGGER.debug("Datastore not found:");
            return false;
        }
        String effectiveDate = jodiProperties.getProperty(JodiConstants.EFFECTIVE_DATE);
        boolean isSCD2Type = odiDataStore.get().getColumnMetaData().stream()
                .map(ColumnMetaData::getName)
                .anyMatch(effectiveDate::equalsIgnoreCase);

        LOGGER.debug("Datastore :" + aOdiDataStoreName + " is of type SCD2: " + isSCD2Type);
        return isSCD2Type;
    }

    @Override
    public DataStore getTargetDataStoreInModel(Mappings mappings) {
        return getDataStoreInModel(mappings.getTargetDataStore(),
                mappings.getModel(), mappings, false);
    }

    private DataStore getDataStoreInModel(final String dataStoreName,
                                          final String modelCode,
                                          final Mappings mappings,
                                          boolean isSourceModelAndDoesntNeedColumns) {
        assert (modelCode != null) : "Model code must be defined in this method";
        String mappingsInfo = (mappings != null)
                ? " mappings:" + mappings.getTargetDataStore()
                : "";
        LOGGER.debug("getDataStoreInModel ---> dataStoreName:" + dataStoreName +
                " modelCode:" + modelCode + mappingsInfo);

        DataStore foundDataStore;
        if (isTemporaryTransformation(dataStoreName)) {
            LOGGER.debug("getDataStoreInModel is temp.");
            DataModelDescriptor dataModelDesc = findModelDescriptor(modelCode);
            foundDataStore = createTemporaryDataStore(dataStoreName,
                    dataModelDesc, mappings,
                    isSourceModelAndDoesntNeedColumns);
        } else {
            LOGGER.debug("getDataStoreInModel is not temp. ");
            foundDataStore = getDataStoreInModel(dataStoreName, modelCode);
        }
        return foundDataStore;
    }

    private DataStore createTemporaryDataStore(final String dataStoreName,
                                               final DataModelDescriptor dataModelDesc,
                                               final Mappings mappings,
                                               final boolean isSourceModelAndDoesntNeedColumns) {
        final DataStore cachedDataStore = context.getDataStore(dataStoreName,
                dataModelDesc.getModelCode());

        DataStore ds;
        if (cachedDataStore != null) {
            // return cached data store
            ds = cachedDataStore;
        } else {
            // find or create Data Model
            DataModel dataModel = findOrCreateDataModel(dataModelDesc);
            // create new data store and cache

            // temporary table creation required that the data store reference
            // to be passed to the columns; this requires the use of anonymous
            // subclass and the initializer builder to create everything in one go
            final Map<String, DataStoreColumn> noColumns = Collections.emptyMap();
            ds = new DataStoreImpl(dataStoreName, noColumns, dataModel) {
                private static final long serialVersionUID = 1L;

                //initialize class by setting columns
                {
                    setColumns(getTempTableColumnMetaData(this, mappings));
                }
            };

            if (!isSourceModelAndDoesntNeedColumns) {
                context.addDataStore(ds);
            }
        }
        return ds;
    }

    /**
     * Create meta data for all columns in the Mapping specification for
     * temporary table
     *
     * @param mappings defines the name and type of a temporary table columns
     * @return Map<String, DataStoreColumn> e.g. ColumnName, Datastorecolumn
     */
    private Map<String, DataStoreColumn> getTempTableColumnMetaData(final DataStore parent, final Mappings mappings) {
        Map<String, DataStoreColumn> result = new HashMap<>();
        int position = 1;
        if (mappings != null) {
            for (Targetcolumn column : mappings.getTargetColumns()) {
                result.put(column.getName(),
                        createColumnMetaData(parent, column, position++));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private DataStoreColumn createColumnMetaData(final DataStore parent,
                                                 final Targetcolumn column,
                                                 final int position) {
        boolean mandatory = (column.isMandatory() != null) ? column.isMandatory() : false;
        return new DataStoreColumnImpl(parent, column.getName(), column.getLength(),
                column.getScale(), column.getDataType(), null, mandatory, null, position);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected DataModel createDataModel(final DataModelDescriptor descriptor) {
        ModelProperties modelProperties = findModelPropertyByCode(descriptor.getModelCode());
        String layerName = null;
        boolean ignoredByHeuristics = false;
        if (modelProperties != null) {
            layerName = modelProperties.getLayer();
            ignoredByHeuristics = modelProperties.isIgnoredByHeuristics();
        } else {
            LOGGER.debug("Model properties not found for code:  " + descriptor.getModelCode());
        }

        return new DataModelImpl(descriptor.getModelCode(),
                descriptor.getDataServerName(),
                descriptor.getPhysicalDataServerName(),
                descriptor.getDataServerTechnology(),
                descriptor.getSchemaName(),
                descriptor.getModelFlexfields(),
                layerName,
                ignoredByHeuristics,
                descriptor.getDataBaseServiceName(),
                descriptor.getDataBaseServicePort());
    }

    @Override
    public DataModel getDataModel(String modelCode) {
        return etlProvider.getDataModelDescriptors().stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getModelCode().equalsIgnoreCase(modelCode))
                .findAny()
                .map(this::findOrCreateDataModel)
                .orElseThrow(() -> {
                    String msg = errorWarningMessages.formatMessage(30, ERROR_MESSAGE_00030, this.getClass(), modelCode);
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                    LOGGER.error(msg);
                    return new NoModelFoundException(msg);
                });
    }

    @Override
    public void initializeDbConnection(String jdbcUrl, String userName, String password) {
        etlProvider.initDBConnection(jdbcUrl, userName, password);
    }

    @Override
    public void closeDbConnection() {
        etlProvider.closeDBConnection();
    }

    @Override
    public boolean projectVariableExists(final String projectCode, final String variableName) {
        return etlProvider.projectVariableExists(projectCode, variableName);
    }

    @Override
    public boolean globalVariableExists(String variableName) {
        return etlProvider.globalVariableExists(variableName);
    }

    @Override
    public Map<String, String> retrieveModelsFromCache() {
        return etlProvider.translateModelToLogicalSchema();
    }

    @Override
    public Set<String> getColumnNames() {
        return etlProvider.getColumnNames();
    }

    @Override
    public Set<String> getTableNames() {
        return etlProvider.getTableNames();
    }

    @Override
    public Set<String> getSchemaNames() {
        return etlProvider.getLogicalSchemaNames();
    }
}
