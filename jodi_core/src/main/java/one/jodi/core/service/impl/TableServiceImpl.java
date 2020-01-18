package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.model.types.*;
import one.jodi.base.util.StringUtils;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.service.ModelValidator;
import one.jodi.core.service.TableService;
import one.jodi.etl.service.table.ColumnDefaultBehaviors;
import one.jodi.etl.service.table.TableDefaultBehaviors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 */
public class TableServiceImpl implements TableService {

    private final static Logger logger = LogManager.getLogger(TableServiceImpl.class);

    private final static boolean NO_FLOW_CHECK = false;
    private final static boolean NO_MANDATORY = false;
    private final static boolean NO_UPDATE = false;
    private final static boolean NO_SELECT = false;

    private final DatabaseMetadataService databaseMetadataService;
    private final JodiProperties jodiProperties;
    private final ModelValidator modelValidator;

    //
    @Inject
    public TableServiceImpl(final DatabaseMetadataService databaseMetadataService,
                            final JodiProperties jodiProperties,
                            final ModelValidator modelValidator) {
        this.databaseMetadataService = databaseMetadataService;
        this.jodiProperties = jodiProperties;
        this.modelValidator = modelValidator;
    }

    /**
     * Create a table with default behaviors.
     *
     * @param dataStore
     * @param columns
     * @return
     */
    private TableDefaultBehaviors createNewTableDefaultBehaviors(
            final DataStore dataStore,
            final List<ColumnDefaultBehaviors> columns) {

        return new TableDefaultBehaviors() {

            @Override
            public String getModel() {
                return dataStore.getDataModel().getModelCode();
            }

            @Override
            public String getTableName() {
                return dataStore.getDataStoreName();
            }

            @Override
            public String getDefaultAlias() {
                return dataStore.getDataStoreName();
            }

            @Override
            public OlapType getOlapType() {
                OlapType value = null;
                if (StringUtils.equalsIgnoreCase(dataStore.getDataStoreType()
                        .toString(), DataStoreType.FACT.toString())) {
                    value = OlapType.FACT;
                } else if (StringUtils.equalsIgnoreCase(dataStore
                        .getDataStoreType().toString(), DataStoreType.DIMENSION
                        .toString())) {
                    value = OlapType.DIMENSION;
                } else if (StringUtils.equalsIgnoreCase(dataStore
                                .getDataStoreType().toString(),
                        DataStoreType.SLOWLY_CHANGING_DIMENSION.toString())) {
                    value = OlapType.SLOWLY_CHANGING_DIMENSION;
                }
                return value;
            }

            @Override
            public List<ColumnDefaultBehaviors> getColumnDefaultBehaviors() {
                return columns;
            }

            @Override
            public boolean isConnectorModel() {
                return databaseMetadataService.isConnectorModel(dataStore
                        .getDataModel().getModelCode());
            }
        };
    }

    /**
     * Create the column default behaviors.
     *
     * @param setColumnName
     * @param setSCDType
     * @param setFlowCheckEnabled
     * @param setMandatory
     * @param setStaticCheckEnabled
     * @param setDataServiceAllowUpdate
     * @param setDataServiceAllowSelect
     * @param setInDatabase
     * @return
     */
    private ColumnDefaultBehaviors createNewColumnFlags(
            final String setColumnName, final SCDType setSCDType,
            final boolean setFlowCheckEnabled, final boolean setMandatory,
            final boolean setStaticCheckEnabled,
            final boolean setDataServiceAllowUpdate,
            final boolean setDataServiceAllowSelect, final boolean setInDatabase) {

        return new ColumnDefaultBehaviors() {

            @Override
            public String getColumnName() {
                return setColumnName;
            }

            @Override
            public String getScdType() {
                return setSCDType.toString();
            }

            @Override
            public boolean isFlowCheckEnabled() {
                return setFlowCheckEnabled;
            }

            @Override
            public boolean isMandatory() {
                return setMandatory;
            }

            @Override
            public boolean isStaticCheckEnabled() {
                return setStaticCheckEnabled;
            }

            @Override
            public boolean isDataServiceAllowUpdate() {
                return setDataServiceAllowUpdate;
            }

            @Override
            public boolean isDataServiceAllowSelect() {
                return setDataServiceAllowSelect;
            }

            @Override
            public boolean isInDatabase() {
                boolean value = false;
                if (setInDatabase) {
                    value = true;
                }
                return value;
            }
        };
    }

    private boolean hasDataMartPrefix(final DataStore dataStore) {
        boolean found = false;
        for (String prefix : jodiProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)) {
            if (dataStore.getDataStoreName().startsWith(prefix)) {
                found = true;
            }
        }
        return found;
    }

    /**
     * currently only have settings for alterTables...need to add isSCD and add
     * code for alterSCDTables() business rules too...handleAlterSCD2()
     * <p>
     * Only the alterSCDTables() use the odiKeyColumn.
     * <p>
     * default behaviors
     */
    @SuppressWarnings({"deprecation"})
    @Override
    public List<TableDefaultBehaviors> assembleDefaultBehaviors() {
        List<TableDefaultBehaviors> tablesToChange =
                new ArrayList<>();

        List<DataStore> dataStoreListing = getDataStores();
        for (DataStore dataStore : dataStoreListing) {
            String modelCode;
            logger.debug("-----------------------------");
            logger.debug("There are two checks:");
            logger.debug("Check for Alternate key W_<TABLE_NAME>_D_U1");
            logger.debug("Check for columns with "
                    + jodiProperties.getRowidColumnName() + " postfix");
            logger.debug("Check for DATE columns with precision 7");
            logger.debug("-----------------------------");

            if (modelValidator.doCheck(dataStore)) {
                modelCode = dataStore.getDataModel().getModelCode();
                logger.debug(dataStore.getDataStoreName());

                if (hasDataMartPrefix(dataStore) &&
                        dataStore.getDataStoreType() == DataStoreType.FACT &&
                        //dataStore.getDataStoreName().endsWith(
                        //			  jodiProperties.getProperty(JodiConstants.FACT_SUFFIX)) &&
                        !databaseMetadataService.isSourceModel(modelCode)) {
                    tablesToChange.add(assembleTableDefaultBehaviors(dataStore));
                } else if (dataStore.getDataStoreType() ==
                        DataStoreType.SLOWLY_CHANGING_DIMENSION) {
                    tablesToChange.add(assembleSCDTableDefaultBehaviors(dataStore));
                } else if (dataStore.getDataStoreType() == DataStoreType.DIMENSION) {
                    tablesToChange.add(assembleTableDefaultBehaviors(dataStore));
                }
            }
        }
        return tablesToChange;
    }

    /**
     * Assemble all the table default behaviors for a given datastore.
     *
     * @param dataStore
     * @return
     */
    private TableDefaultBehaviors assembleTableDefaultBehaviors(final DataStore dataStore) {
        List<ColumnDefaultBehaviors> columnsToAlter =
                new ArrayList<>();
        for (DataStoreKey dataStorekey : dataStore.getDataStoreKeys()) {
            for (String column : dataStorekey.getColumns()) {
                assert (column != null) : "malformed model";
                if (column.equalsIgnoreCase(jodiProperties.getRowidColumnName())) {
                    columnsToAlter.add(createNewColumnFlags(column, SCDType.SURROGATE_KEY,
                            NO_FLOW_CHECK, NO_MANDATORY, true, NO_UPDATE, NO_SELECT,
                            dataStorekey.existsInDatabase()));
                    break;
                }
            }
        }
        return createNewTableDefaultBehaviors(dataStore, columnsToAlter);
    }

    private List<String> getUpdateKeyColumns(final DataStore datastore) {
        List<String> keyColumns = new ArrayList<>();
        datastore.getDataStoreKeys().stream().filter(key -> key.getName()
                .equalsIgnoreCase(
                        datastore
                                .getDataStoreName() +
                                "_U1"))
                .forEach(key -> {
                    for (String column : key.getColumns()) {
                        assert (column != null) : "misformed key";
                        if (!column.equalsIgnoreCase(
                                jodiProperties
                                        .getProperty(JodiConstants.EFFECTIVE_DATE)) &&
                                !column.equalsIgnoreCase(
                                        jodiProperties
                                                .getProperty(JodiConstants.EXPIRATION_DATE)) &&
                                !column.equalsIgnoreCase(
                                        jodiProperties
                                                .getProperty(JodiConstants.CURRENT_FLG)) &&
                                !column.equalsIgnoreCase(jodiProperties
                                        .getRowidColumnName()) &&
                                !column.equalsIgnoreCase(
                                        jodiProperties
                                                .getProperty(JodiConstants.W_INSERT_DT)) &&
                                !column.equalsIgnoreCase(
                                        jodiProperties
                                                .getProperty(JodiConstants.W_UPDATE_DT)) &&
                                !column.equalsIgnoreCase(
                                        jodiProperties
                                                .getProperty(JodiConstants.ETL_PROC_WID))) {
                            if (!keyColumns.contains(column)) {
                                keyColumns.add(column);
                            }
                        }
                    }
                });
        return keyColumns;
    }

    /**
     * Assemble all the SCD table default behaviors for a given datastore.
     *
     * @param dataStore
     * @return
     */
    private TableDefaultBehaviors assembleSCDTableDefaultBehaviors(final DataStore dataStore) {
        List<ColumnDefaultBehaviors> columnsToAlter =
                new ArrayList<>();
        List<String> keyColumns = getUpdateKeyColumns(dataStore);
        for (Entry<String, DataStoreColumn> entity : dataStore.getColumns().entrySet()) {
            ColumnDefaultBehaviors behavior;
            String columnName = entity.getKey();
            assert (columnName != null) : "incorrect model";
            if (columnName.equalsIgnoreCase(jodiProperties
                    .getProperty(JodiConstants.EFFECTIVE_DATE))) {
                behavior = createNewColumnFlags(columnName, SCDType.START_TIMESTAMP,
                        NO_FLOW_CHECK, NO_MANDATORY, true, NO_UPDATE, NO_SELECT, false);
            } else if (columnName.equalsIgnoreCase(jodiProperties
                    .getProperty(JodiConstants.EXPIRATION_DATE))) {
                behavior = createNewColumnFlags(columnName, SCDType.END_TIMESTAMP,
                        NO_FLOW_CHECK, NO_MANDATORY, true, NO_UPDATE, NO_SELECT, false);
            } else if (columnName.equalsIgnoreCase(jodiProperties
                    .getProperty(JodiConstants.CURRENT_FLG))) {
                behavior = createNewColumnFlags(columnName, SCDType.CURRENT_RECORD_FLAG,
                        NO_FLOW_CHECK, NO_MANDATORY, false, NO_UPDATE, NO_SELECT, false);
            } else if (columnName.equalsIgnoreCase(jodiProperties.getRowidColumnName())) {
                behavior = createNewColumnFlags(columnName, SCDType.SURROGATE_KEY,
                        NO_FLOW_CHECK, NO_MANDATORY, false, NO_UPDATE, NO_SELECT, false);
            } else if (columnName.equalsIgnoreCase(jodiProperties
                    .getProperty(JodiConstants.ETL_PROC_WID)) ||
                    columnName.equalsIgnoreCase(jodiProperties
                            .getProperty(JodiConstants.W_INSERT_DT)) ||
                    columnName.equalsIgnoreCase(jodiProperties
                            .getProperty(JodiConstants.W_UPDATE_DT))) {
                behavior = createNewColumnFlags(columnName, SCDType.OVERWRITE_ON_CHANGE,
                        NO_FLOW_CHECK, NO_MANDATORY, false, NO_UPDATE, NO_SELECT, false);
            } else if (keyColumns.contains(columnName)) {
                behavior = createNewColumnFlags(columnName, SCDType.NATURAL_KEY,
                        NO_FLOW_CHECK, NO_MANDATORY, false, NO_UPDATE, NO_SELECT, false);
            } else {
                behavior = createNewColumnFlags(columnName, SCDType.ADD_ROW_ON_CHANGE,
                        NO_FLOW_CHECK, NO_MANDATORY, false, NO_UPDATE, NO_SELECT, false);
            }
            columnsToAlter.add(behavior);
        }
        return createNewTableDefaultBehaviors(dataStore, columnsToAlter);
    }

    /**
     * Gather all the DataStores.
     *
     * @return list of datastores
     */
    @SuppressWarnings("deprecation")
    public List<DataStore> getDataStores() {
        List<DataStore> dataStoreListing = new ArrayList<>();
        databaseMetadataService.getConfiguredModels().stream()
                .filter(model -> !model.isIgnoredByHeuristics())
                .forEach(model -> dataStoreListing.addAll(databaseMetadataService
                        .getAllDataStoresInModel(model
                                .getCode()).values()));
        return dataStoreListing;
    }

}
