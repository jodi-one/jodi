package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.model.types.DataStoreKey;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.model.types.SCDType;
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

   private static final Logger logger = LogManager.getLogger(TableServiceImpl.class);

   private static final boolean NO_FLOW_CHECK = false;
   private static final boolean NO_MANDATORY = false;
   private static final boolean NO_UPDATE = false;
   private static final boolean NO_SELECT = false;

   private final DatabaseMetadataService databaseMetadataService;
   private final JodiProperties jodiProperties;
   private final ModelValidator modelValidator;

   //
   @Inject
   public TableServiceImpl(final DatabaseMetadataService databaseMetadataService, final JodiProperties jodiProperties,
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
   private TableDefaultBehaviors createNewTableDefaultBehaviors(final DataStore dataStore,
                                                                final List<ColumnDefaultBehaviors> columns) {

      return new TableDefaultBehaviors() {

         @Override
         public String getModel() {
            return dataStore.getDataModel()
                            .getModelCode();
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
            if (isDetectedAsScd(dataStore)) {
               value = OlapType.SLOWLY_CHANGING_DIMENSION;
            } else if (dataStore.getDataStoreType()
                                .toString()
                                .equalsIgnoreCase(DataStoreType.FACT.toString())) {
               value = OlapType.FACT;
            } else if (dataStore.getDataStoreType()
                                .toString()
                                .equalsIgnoreCase(DataStoreType.DIMENSION.toString())) {
               value = OlapType.DIMENSION;
            } else if (dataStore.getDataStoreType()
                                .toString()
                                .equalsIgnoreCase(DataStoreType.SLOWLY_CHANGING_DIMENSION.toString())) {
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
            return databaseMetadataService.isConnectorModel(dataStore.getDataModel()
                                                                     .getModelCode());
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
   private ColumnDefaultBehaviors createNewColumnFlags(final String setColumnName, final SCDType setSCDType,
                                                       final boolean setFlowCheckEnabled, final boolean setMandatory,
                                                       final boolean setStaticCheckEnabled,
                                                       final boolean setDataServiceAllowUpdate,
                                                       final boolean setDataServiceAllowSelect,
                                                       final boolean setInDatabase) {

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
      for (final String prefix : jodiProperties.getPropertyList(JodiConstants.DATA_MART_PREFIX)) {
         if (dataStore.getDataStoreName()
                      .startsWith(prefix)) {
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
      final List<TableDefaultBehaviors> tablesToChange = new ArrayList<>();

      final List<DataStore> dataStoreListing = getDataStores();
      for (final DataStore dataStore : dataStoreListing) {
         final String modelCode;

         modelCode = dataStore.getDataModel()
                              .getModelCode();
         logger.debug(dataStore.getDataStoreName());

         if (hasDataMartPrefix(dataStore) && dataStore.getDataStoreType() == DataStoreType.FACT &&
                 //dataStore.getDataStoreName().endsWith(
                 //			  jodiProperties.getProperty(JodiConstants.FACT_SUFFIX)) &&
                 !databaseMetadataService.isSourceModel(modelCode)) {
            tablesToChange.add(assembleTableDefaultBehaviors(dataStore));
            logger.info(String.format("Table %s detected as OLAP type %s", dataStore.getDataStoreName(),
                                      assembleTableDefaultBehaviors(dataStore).getOlapType()));
         } else if (dataStore.getDataStoreType() == DataStoreType.SLOWLY_CHANGING_DIMENSION ||
                 isDetectedAsScd(dataStore)) {
            // if (modelValidator.doCheck(dataStore)) {
            // only check for U1 for SCD2 for performance
            logger.info("-----------------------------");
            logger.info("There are two checks:");
            logger.info("Check for Alternate key W_<TABLE_NAME>_D_U1");
            logger.info("Check for columns with " + jodiProperties.getRowidColumnName() + " postfix");
            logger.info("Check for DATE columns with precision 7");
            tablesToChange.add(assembleSCDTableDefaultBehaviors(dataStore));
            logger.info(String.format("Table %s detected as OLAP type %s", dataStore.getDataStoreName(),
                                      assembleTableDefaultBehaviors(dataStore).getOlapType()));
//            } else {
//               logger.info(String.format(
//                       "Table %s detected as OLAP type %s but no Alternate key with name <TABLE_NAME>_U1 found.",
//                       dataStore.getDataStoreName(), assembleTableDefaultBehaviors(dataStore).getOlapType()));
//            }
         } else if (dataStore.getDataStoreType() == DataStoreType.DIMENSION) {
            tablesToChange.add(assembleTableDefaultBehaviors(dataStore));
            logger.info(String.format("Table %s detected as OLAP type %s", dataStore.getDataStoreName(),
                                      assembleTableDefaultBehaviors(dataStore).getOlapType()));
         }
      }
      logger.info("-----------------------------");
      return tablesToChange;
   }

   public boolean isDetectedAsScd(final DataStore dataStore) {
      boolean hasEFFECTIVE_DATE = false;
      boolean hasEXPIRATION_DATE = false;
      boolean hasCURRENT_RECORD_FLAG = false;
      boolean hasROW_WID = false;
      final List<String> keyColumns = getUpdateKeyColumns(dataStore);

      for (final Entry<String, DataStoreColumn> entity : dataStore.getColumns()
                                                                  .entrySet()) {
         final ColumnDefaultBehaviors behavior;
         final String columnName = entity.getKey();
         assert (columnName != null) : "incorrect model";
         if (columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.EFFECTIVE_DATE))) {
            hasEFFECTIVE_DATE = true;
            logger.info(" dataStore: " + dataStore.getDataStoreName() + "hasEFFECTIVE_DATE " + hasEFFECTIVE_DATE);
         } else if (columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.EXPIRATION_DATE))) {
            hasEXPIRATION_DATE = true;
            logger.info(" dataStore: " + dataStore.getDataStoreName() + "hasEXPIRATION_DATE " + hasEXPIRATION_DATE);
         } else if (columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.CURRENT_FLG))) {
            hasCURRENT_RECORD_FLAG = true;
            logger.info(
                    " dataStore: " + dataStore.getDataStoreName() + "hasCURRENT_RECORD_FLAG " + hasCURRENT_RECORD_FLAG);
         } else if (columnName.equalsIgnoreCase(jodiProperties.getRowidColumnName())) {
            hasROW_WID = true;
            logger.info(" dataStore: " + dataStore.getDataStoreName() + "hasROW_WID " + hasROW_WID);
         } else if (keyColumns.contains(columnName)) {
            keyColumns.add(entity.getKey());
         }
      }
      logger.info(" isDetectedAsScd: " + dataStore.getDataStoreName() + " " +
                          (hasEFFECTIVE_DATE && hasEXPIRATION_DATE && hasCURRENT_RECORD_FLAG && hasROW_WID &&
                                  keyColumns.size() > 1));
      return hasEFFECTIVE_DATE && hasEXPIRATION_DATE && hasCURRENT_RECORD_FLAG && hasROW_WID && keyColumns.size() > 1;
   }

   /**
    * Assemble all the table default behaviors for a given datastore.
    *
    * @param dataStore
    * @return
    */
   private TableDefaultBehaviors assembleTableDefaultBehaviors(final DataStore dataStore) {
      final List<ColumnDefaultBehaviors> columnsToAlter = new ArrayList<>();
      for (final DataStoreKey dataStorekey : dataStore.getDataStoreKeys()) {
         for (final String column : dataStorekey.getColumns()) {
            assert (column != null) : "malformed model";
            if (column.equalsIgnoreCase(jodiProperties.getRowidColumnName())) {
               columnsToAlter.add(
                       createNewColumnFlags(column, SCDType.SURROGATE_KEY, NO_FLOW_CHECK, NO_MANDATORY, true, NO_UPDATE,
                                            NO_SELECT, dataStorekey.existsInDatabase()));
               break;
            }
         }
      }
      return createNewTableDefaultBehaviors(dataStore, columnsToAlter);
   }

   private List<String> getUpdateKeyColumns(final DataStore datastore) {
      final List<String> keyColumns = new ArrayList<>();
      datastore.getDataStoreKeys()
               .stream()
               .filter(key -> key.getName()
                                 .equalsIgnoreCase(datastore.getDataStoreName() + "_U1"))
               .forEach(key -> {
                  for (final String column : key.getColumns()) {
                     assert (column != null) : "misformed key";
                     if (!column.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.EFFECTIVE_DATE)) &&
                             !column.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.EXPIRATION_DATE)) &&
                             !column.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.CURRENT_FLG)) &&
                             !column.equalsIgnoreCase(jodiProperties.getRowidColumnName()) &&
                             !column.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.W_INSERT_DT)) &&
                             !column.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.W_UPDATE_DT)) &&
                             !column.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.ETL_PROC_WID))) {
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
      final List<ColumnDefaultBehaviors> columnsToAlter = new ArrayList<>();
      final List<String> keyColumns = getUpdateKeyColumns(dataStore);
      for (final Entry<String, DataStoreColumn> entity : dataStore.getColumns()
                                                                  .entrySet()) {
         final ColumnDefaultBehaviors behavior;
         final String columnName = entity.getKey();
         assert (columnName != null) : "incorrect model";
         if (columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.EFFECTIVE_DATE))) {
            behavior = createNewColumnFlags(columnName, SCDType.START_TIMESTAMP, NO_FLOW_CHECK, NO_MANDATORY, true,
                                            NO_UPDATE, NO_SELECT, false);
         } else if (columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.EXPIRATION_DATE))) {
            behavior = createNewColumnFlags(columnName, SCDType.END_TIMESTAMP, NO_FLOW_CHECK, NO_MANDATORY, true,
                                            NO_UPDATE, NO_SELECT, false);
         } else if (columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.CURRENT_FLG))) {
            behavior = createNewColumnFlags(columnName, SCDType.CURRENT_RECORD_FLAG, NO_FLOW_CHECK, NO_MANDATORY, false,
                                            NO_UPDATE, NO_SELECT, false);
         } else if (columnName.equalsIgnoreCase(jodiProperties.getRowidColumnName())) {
            behavior = createNewColumnFlags(columnName, SCDType.SURROGATE_KEY, NO_FLOW_CHECK, NO_MANDATORY, false,
                                            NO_UPDATE, NO_SELECT, false);
         } else if (columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.ETL_PROC_WID)) ||
                 columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.W_INSERT_DT)) ||
                 columnName.equalsIgnoreCase(jodiProperties.getProperty(JodiConstants.W_UPDATE_DT))) {
            behavior = createNewColumnFlags(columnName, SCDType.OVERWRITE_ON_CHANGE, NO_FLOW_CHECK, NO_MANDATORY, false,
                                            NO_UPDATE, NO_SELECT, false);
         } else if (keyColumns.contains(columnName)) {
            behavior =
                    createNewColumnFlags(columnName, SCDType.NATURAL_KEY, NO_FLOW_CHECK, NO_MANDATORY, false, NO_UPDATE,
                                         NO_SELECT, false);
         } else {
            behavior = createNewColumnFlags(columnName, SCDType.ADD_ROW_ON_CHANGE, NO_FLOW_CHECK, NO_MANDATORY, false,
                                            NO_UPDATE, NO_SELECT, false);
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
   @Override
   @SuppressWarnings("deprecation")
   public List<DataStore> getDataStores() {
      final List<DataStore> dataStoreListing = new ArrayList<>();
      databaseMetadataService.getConfiguredModels()
                             .stream()
                             .filter(model -> !model.isIgnoredByHeuristics())
                             .forEach(model -> dataStoreListing.addAll(
                                     databaseMetadataService.getAllDataStoresInModel(model.getCode())
                                                            .values()));
      return dataStoreListing;
   }

}
