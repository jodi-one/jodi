package one.jodi.odi.table;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.base.util.StringUtils;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.etl.service.table.ColumnDefaultBehaviors;
import one.jodi.etl.service.table.TableDefaultBehaviors;
import one.jodi.etl.service.table.TableServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.model.OdiColumn;
import oracle.odi.domain.model.OdiColumn.ScdType;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiDataStore.OlapType;
import oracle.odi.domain.model.OdiKey;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.OdiReference;
import oracle.odi.domain.model.ReferenceColumn;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.model.finder.IOdiReferenceFinder;
import oracle.odi.domain.project.IOptionValue;
import oracle.odi.domain.project.OdiJKM;
import oracle.odi.domain.project.finder.IOdiJKMFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OdiTableServiceImpl implements TableServiceProvider {

    private static final Logger logger = LogManager.getLogger(OdiTableServiceImpl.class);
    private static final String ERROR_MESSAGE_01210 =
            "Matching column '%s' has been identified but not found in " + "datastore '%s'.";
    private static final String ERROR_MESSAGE_01220 =
            "ERROR: deleting Key: %s try deleting any packages that include " + "this interface. %s.";
    private static final String ERROR_MESSAGE_01230 =
            "Cannot find JKM with name '%1$s' in project '%2$s', is it " + "imported?";
    private static final String ERROR_MESSAGE_06000 = "OdiReference has no index for column: %s.%s";
    private static final String ERROR_MESSAGE_06001 = "OdiReference %s of table %s is not defined in database.";
    private static final String ERROR_MESSAGE_06002 = "OdiReference %s of table %s doens't have primary datastore.";
    protected final OdiInstance odiInstance;
    protected final SchemaMetaDataProvider odiUtils;
    private final JodiProperties properties;
    private final ModelPropertiesProvider modelPropProvider;
    private final ErrorWarningMessageJodi errorWarningMessages;


    @Inject
    protected OdiTableServiceImpl(OdiInstance instance, SchemaMetaDataProvider odiUtils,
                                  final JodiProperties properties, final ModelPropertiesProvider modelPropProvider,
                                  final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.odiInstance = instance;
        this.odiUtils = odiUtils;
        this.properties = properties;
        this.modelPropProvider = modelPropProvider;
        this.errorWarningMessages = errorWarningMessages;
    }

    /*
     * (non-Javadoc)
     *
     * @see one.jodi.table.OdiAlterTables#alterTables()
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void alterTables(List<TableDefaultBehaviors> tablesToChange) {
        @SuppressWarnings("unchecked") Collection<OdiDataStore> odiDetailDatastore =
                odiInstance.getTransactionalEntityManager()
                           .findAll(OdiDataStore.class);

        for (TableDefaultBehaviors tdb : tablesToChange) {

            OdiDataStore matchingOdiDataStore = getMatchingDataStore(tdb, odiDetailDatastore);
            assert (matchingOdiDataStore != null);
            assert (tdb.getDefaultAlias() != null);
            assert (matchingOdiDataStore.getName() != null);
            if (tdb.getDefaultAlias()
                   .equals(tdb.getTableName())) {
                matchingOdiDataStore.setDefaultAlias(matchingOdiDataStore.getName());
            }
            for (OdiKey odiKey : matchingOdiDataStore.getKeys()) {
                alterKeyDefinedInDatabase(odiKey, matchingOdiDataStore, tdb);
            }
            if (tdb.getOlapType() != null) {
                matchingOdiDataStore.setOlapType(convertToOdiCoding(tdb.getOlapType()));
            }
            odiInstance.getTransactionalEntityManager()
                       .merge(matchingOdiDataStore);

            OdiColumn matchingColumn = null;
            for (ColumnDefaultBehaviors cdb : tdb.getColumnDefaultBehaviors()) {
                assert (!cdb.getColumnName()
                            .isEmpty()) : "Model error";
                matchingColumn = getMatchingOdiColumn(matchingOdiDataStore, cdb.getColumnName());
                if (matchingColumn == null) {
                    String message = errorWarningMessages.formatMessage(1210, ERROR_MESSAGE_01210, this.getClass(),
                                                                        cdb.getColumnName(), matchingOdiDataStore);
                    errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
                    throw new AssertionError(message);
                }

                if (cdb.getScdType()
                       .equals("SURROGATE_KEY")) {
                    matchingColumn.setFlowCheckEnabled(cdb.isFlowCheckEnabled());
                    matchingColumn.setMandatory(cdb.isMandatory());
                    matchingColumn.setStaticCheckEnabled(cdb.isStaticCheckEnabled());
                    matchingColumn.setDataServiceAllowInsert(true);
                    matchingColumn.setDataServiceAllowUpdate(cdb.isDataServiceAllowUpdate());
                    matchingColumn.setDataServiceAllowSelect(cdb.isDataServiceAllowSelect());
                }
            }

            if (tdb.isConnectorModel()) {
                Collection<OdiReference> odiReferences =
                        findReferenceByPrimaryDataStoreId(matchingOdiDataStore.getDataStoreId());

                logger.info("odiReferences: " + odiReferences.size());
                for (OdiReference odiReference : odiReferences) {
                    alterReferenceInConnector(matchingOdiDataStore, odiReference);
                }
            }
        }
    }

    private OlapType convertToOdiCoding(TableDefaultBehaviors.OlapType olapType) {
        OlapType value = null;
        if (olapType == TableDefaultBehaviors.OlapType.FACT) {
            value = OlapType.FACT_TABLE;
        } else if (olapType == TableDefaultBehaviors.OlapType.DIMENSION) {
            value = OlapType.DIMENSION;
        } else if (olapType == TableDefaultBehaviors.OlapType.SLOWLY_CHANGING_DIMENSION) {
            value = OlapType.SLOWLY_CHANGING_DIMENSION;
        }
        return value;
    }

    private void alterKeyDefinedInDatabase(OdiKey odiKey, OdiDataStore matchingOdiDataStore,
                                           TableDefaultBehaviors tdb) {

        for (ColumnDefaultBehaviors cdb : tdb.getColumnDefaultBehaviors()) {
            if (!cdb.isInDatabase()) {
                logger.debug("ColumnName: " + cdb.getColumnName() + " is not defined in the database altering it.");
            }
            odiKey.setInDatabase(true);
            odiInstance.getTransactionalEntityManager()
                       .merge(matchingOdiDataStore);
        }
    }

    private OdiDataStore getMatchingDataStore(TableDefaultBehaviors tdb, Collection<OdiDataStore> odiDetailDatastore) {
        OdiDataStore matchingOdiDataStore = null;
        for (OdiDataStore odiDataStore : odiDetailDatastore) {
            if (odiDataStore.getName()
                            .equals(tdb.getTableName())) {
                matchingOdiDataStore = odiDataStore;
                logger.debug("Get matching dataStore, matchingOdiDataStore: " + matchingOdiDataStore);
            }
        }
        return matchingOdiDataStore;
    }

    /**
     * @param o
     * @param columnName
     * @return
     */
    private OdiColumn getMatchingOdiColumn(OdiDataStore odiDataStore, String columnName) {
        OdiColumn matchingColumn = null;
        for (OdiColumn odiColumn : odiDataStore.getColumns()) {
            if (odiColumn.getName()
                         .equals(columnName)) {
                matchingColumn = odiColumn;
                logger.debug("Get matching column, matchingColumn: " + matchingColumn);
            }
        }
        return matchingColumn;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setCDCDescriptor(String dataStoreName, String dataModelCode, int order) {
        IOdiDataStoreFinder finder = (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                      .getFinder(OdiDataStore.class);
        OdiDataStore odiDataStore = finder.findByName(dataStoreName, dataModelCode);
        logger.debug(String.format("Setting CDC descriptor to true for datastore '%1$s'.", dataStoreName));
        odiDataStore.setCdcDescriptor(new OdiDataStore.CdcDescriptor(true, order));
        odiInstance.getTransactionalEntityManager()
                   .merge(odiDataStore);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void resetCDCDescriptor() {
        IOdiDataStoreFinder finder = (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                      .getFinder(OdiDataStore.class);
        @SuppressWarnings("unchecked") Collection<OdiDataStore> odiDataStores = finder.findAll();
        for (OdiDataStore dataStore : odiDataStores) {
            logger.debug(String.format("Setting CDC descriptor to false for datastore '%1$s'.", dataStore.getName()));
            dataStore.setCdcDescriptor(new OdiDataStore.CdcDescriptor(false, 0));
            odiInstance.getTransactionalEntityManager()
                       .merge(dataStore);
        }
    }

    // @TODO implement changes for subscriber.
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setSubscriber(String dataStoreName, String dataModelCode, List<String> subscribers) {
        IOdiDataStoreFinder finder = (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                      .getFinder(OdiDataStore.class);
        OdiDataStore odiDataStore = finder.findByName(dataStoreName, dataModelCode);
        logger.debug(String.format("Setting subscriber to for datastore '%1$s'.", dataStoreName));
        odiInstance.getTransactionalEntityManager()
                   .merge(odiDataStore);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setJKMOptions(String modelCode, Map<String, Object> jkmOptions) {
        OdiModel odiModel = ((IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                                                          .getFinder(OdiModel.class)).findByCode(modelCode);
        int jkmCounter = 0;
        for (IOptionValue jkmOption : odiModel.getJKMOptions()) {
            for (Entry<String, Object> entryOption : jkmOptions.entrySet()) {
                if (entryOption.getKey()
                               .equalsIgnoreCase(jkmOption.getName())) {
                    if (Boolean.TRUE.equals(entryOption.getValue()) || Boolean.FALSE.equals(entryOption.getValue())) {
                        odiModel.getJKMOptions()
                                .get(jkmCounter)
                                .setValue(Boolean.parseBoolean(entryOption.getValue() + ""));
                        logger.debug("JKMOption:" + entryOption.getKey() + " set to value:" + entryOption.getValue());
                    } else {
                        odiModel.getJKMOptions()
                                .get(jkmCounter)
                                .setValue(entryOption.getValue() + "");
                        logger.debug("JKMOption:" + entryOption.getKey() + " set to value:" + entryOption.getValue());
                    }
                }
            }
            jkmCounter++;
        }
    }


    /**
     * Remove ForeignKey from Connector tables since data is loaded truncate
     * insert In the connector layer, the 1 on 1 tables, one would not need any
     * Foreign Keys, currently the PK are preserved, mainly as a extra check for
     * consistency, since then an unique index is also created.
     *
     * @param odiDatastore
     * @param odiReference
     */

    private void alterReferenceInConnector(OdiDataStore odiDatastore, OdiReference odiReference) {
        logger.debug("Removing Foreign Key: " + odiReference.getName());
        odiInstance.getTransactionalEntityManager()
                   .remove(odiReference);
    }

    /**
     * There are two checks: 1) OdiCheckTable.doCheck() 2) Indexes for Foreign
     * Keys 3) Indexes for Foreign Keys defined in database( Performance )
     */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void checkTables() {
        @SuppressWarnings("unchecked") Collection<OdiReference> odiReferences =
                odiInstance.getTransactionalEntityManager()
                           .findAll(OdiReference.class);
        for (OdiReference odiReference : odiReferences) {
            boolean hasDataMartPrefix = false;
            for (String dmp : properties.getPropertyList(JodiConstants.DATA_MART_PREFIX)) {
                if (odiReference.getPrimaryDataStore() == null) {
                    String message = errorWarningMessages.formatMessage(6001, ERROR_MESSAGE_06002, this.getClass(),
                                                                        odiReference.getName(),
                                                                        odiReference.getForeignDataStore()
                                                                                    .getName());
                    errorWarningMessages.addMessage(message, MESSAGE_TYPE.WARNINGS);
                    continue;
                }
                if (odiReference.getPrimaryDataStore()
                                .getName()
                                .startsWith(dmp)) {
                    hasDataMartPrefix = true;
                }
            }
            if (odiReference.getPrimaryDataStore() != null && hasDataMartPrefix) {
                Collection<ReferenceColumn> refcolumns = odiReference.getReferenceColumns();
                for (ReferenceColumn refCol : refcolumns) {
                    OdiColumn odiRefColumn = refCol.getForeignKeyColumn();
                    boolean found = false;
                    for (OdiKey odiKey : odiReference.getForeignDataStore()
                                                     .getKeys()) {
                        if (odiKey.getKeyType()
                                  .equals(OdiKey.KeyType.INDEX)) {
                            for (OdiColumn indexColumn : odiKey.getColumns()) {
                                if (indexColumn.equals(odiRefColumn)) {
                                    found = true;
                                }
                            }
                        }
                    }
                    if (!found) {
                        String message = errorWarningMessages.formatMessage(6000, ERROR_MESSAGE_06000, this.getClass(),
                                                                            odiReference.getForeignDataStore()
                                                                                        .getName(),
                                                                            odiRefColumn.getName());
                        errorWarningMessages.addMessage(message, MESSAGE_TYPE.WARNINGS);
                    }
                }
                if (odiReference.getReferenceType() != OdiReference.ReferenceType.DB_REFERENCE) {
                    String message = errorWarningMessages.formatMessage(6001, ERROR_MESSAGE_06001, this.getClass(),
                                                                        odiReference.getName(),
                                                                        odiReference.getForeignDataStore()
                                                                                    .getName());
                    errorWarningMessages.addMessage(message, MESSAGE_TYPE.WARNINGS);
                }
            }
        }
    }

    private Collection<OdiReference> findReferenceByPrimaryDataStoreId(final Number dataStoreId) {
        return ((IOdiReferenceFinder) odiInstance.getTransactionalEntityManager()
                                                 .getFinder(OdiReference.class)).findByPrimaryDataStore(dataStoreId);
    }

    private OdiColumn findColumn(final OdiDataStore odiDataStore, final ColumnDefaultBehaviors c) {
        OdiColumn found = null;
        for (OdiColumn column : odiDataStore.getColumns()) {
            if (column.getName()
                      .equalsIgnoreCase(c.getColumnName())) {
                found = column;
                break;
            }
        }
        return found;
    }

    private void setSCDType2Behavior(final TableDefaultBehaviors tdb, final Map<String, OdiDataStore> datastores) {
        final String key = tdb.getModel() + "." + tdb.getTableName();
        final OdiDataStore odiDataStore = datastores.get(key);
        assert (odiDataStore != null) : "Datastore must exist per previous analysis";

        logger.debug("SCD2 Dimension Datastore: " + tdb.getTableName());
        odiDataStore.setOlapType(OdiDataStore.OlapType.SLOWLY_CHANGING_DIMENSION);
        logger.debug("Now OlapType:" + odiDataStore.getOlapType());
        odiInstance.getTransactionalEntityManager()
                   .merge(odiDataStore);

        for (ColumnDefaultBehaviors c : tdb.getColumnDefaultBehaviors()) {
            final OdiColumn odiColumn = findColumn(odiDataStore, c);
            assert (odiColumn != null) : "Column must exist per previous analysis";

            odiColumn.setScdType(getScdTypeValue(c.getScdType()));
            odiColumn.setFlowCheckEnabled(c.isFlowCheckEnabled());
            odiColumn.setMandatory(c.isMandatory());
            odiColumn.setStaticCheckEnabled(c.isStaticCheckEnabled());

            logger.debug(odiDataStore.getName() + ":" + odiColumn.getName() + ":" + odiDataStore.getOlapType() + ":" +
                                 odiColumn.getScdType());
            odiInstance.getTransactionalEntityManager()
                       .merge(odiDataStore);
        }
    }

    /**
     * This methods changes datastores with an EFFECTIVE_DATE, into a Slowly
     * Changing Dimension. Therefore any table with an EFFECTIVE_DATE will be
     * regarded as SCD2.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void alterSCDTables(List<TableDefaultBehaviors> tablesToChange) {

        @SuppressWarnings("unchecked") Collection<OdiDataStore> odiDetailDatastore =
                odiInstance.getTransactionalEntityManager()
                           .findAll(OdiDataStore.class);
        if (odiDetailDatastore == null || odiDetailDatastore.isEmpty()) {
            return;
        }

        // create map with model.name as key and datastore as value
        Map<String, OdiDataStore> datastores = new HashMap<>();
        for (OdiDataStore ods : odiDetailDatastore) {
            datastores.put(ods.getModel()
                              .getCode() + "." + ods.getName(), ods);
        }

        tablesToChange.stream()
                      .filter(tdb -> tdb.getOlapType() == TableDefaultBehaviors.OlapType.SLOWLY_CHANGING_DIMENSION)
                      .forEach(tdb -> setSCDType2Behavior(tdb, datastores));
    }

    /**
     * Given a SCDType value in string format return the matching etl layer
     * type.
     *
     * @param ScdType as string
     * @return ScdType as enum
     */
    private ScdType getScdTypeValue(String scdType) {
        ScdType scdTypeValue = null;
        if (StringUtils.equals(scdType, "SURROGATE_KEY")) {
            scdTypeValue = ScdType.SURROGATE_KEY;
        } else if (StringUtils.equals(scdType, "NATURAL_KEY")) {
            scdTypeValue = ScdType.NATURAL_KEY;
        } else if (StringUtils.equals(scdType, "OVERWRITE_ON_CHANGE")) {
            scdTypeValue = ScdType.OVERWRITE_ON_CHANGE;
        } else if (StringUtils.equals(scdType, "ADD_ROW_ON_CHANGE")) {
            scdTypeValue = ScdType.ADD_ROW_ON_CHANGE;
        } else if (StringUtils.equals(scdType, "CURRENT_RECORD_FLAG")) {
            scdTypeValue = ScdType.CURRENT_RECORD_FLAG;
        } else if (StringUtils.equals(scdType, "START_TIMESTAMP")) {
            scdTypeValue = ScdType.START_TIMESTAMP;
        } else if (StringUtils.equals(scdType, "END_TIMESTAMP")) {
            scdTypeValue = ScdType.END_TIMESTAMP;
        }
        return scdTypeValue;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * one.jodi.table.OdiDeleteReferences#deleteReferencesByModel(java
     * .lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteReferencesByModel(String odiModel) {
        try {
            deleteReferenceByModel(odiModel);
            deleteIndexesByModel(odiModel);
        } catch (RuntimeException re) {
            String message = errorWarningMessages.formatMessage(1220, ERROR_MESSAGE_01220, this.getClass(), odiModel,
                                                                re.getMessage());
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
            logger.error(message, re);
            throw new RuntimeException(message, re);
        }
    }

    private void deleteReferenceByModel(String name) {
        @SuppressWarnings("unchecked") Collection<OdiReference> odireferences =
                ((IOdiReferenceFinder) odiInstance.getTransactionalEntityManager()
                                                  .getFinder(OdiReference.class)).findAll();

        if (!odireferences.isEmpty()) {
            for (OdiReference odireference : odireferences) {
                if (odireference == null || odireference.getForeignDataStore() == null ||
                        odireference.getForeignDataStore()
                                    .getModel() == null || odireference.getForeignDataStore()
                                                                       .getModel()
                                                                       .getCode() == null) {
                    continue;
                }
                if (odireference.getForeignDataStore()
                                .getModel()
                                .getCode()
                                .equals(name)) {
                    logger.info("Model: " + name + ":reference:" + odireference.getForeignDataStore()
                                                                               .getModel()
                                                                               .getCode());
                    odiInstance.getTransactionalEntityManager()
                               .remove(odireference);

                    logger.info("Removed reference: " + odireference.getName() + " from model: " + name +
                                        " from datastore:" + odireference.getForeignDataStore()
                                                                         .getName());

                }
            }
        }
    }


    private void deleteIndexesByModel(final String model) {
        IOdiDataStoreFinder finder = (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                                                                      .getFinder(OdiDataStore.class);
        Collection<OdiDataStore> allDs = finder.findByModel(model);

        // DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
        // ITransactionManager tm = odiInstance.getTransactionManager();
        // IOdiEntityManager tme = odiInstance.getTransactionalEntityManager();
        // ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        for (OdiDataStore ds : allDs) {
            if (ds.getKeys() == null) {
                continue;
            }
            for (OdiKey key : ds.getKeys()) {
                if (key != null && key.getKeyType() != null && key.getKeyType()
                                                                  .equals(oracle.odi.domain.model.OdiKey.KeyType.INDEX)) {
                    logger.info("Index: " + key.getName() + " :reference:" + ds.getModel()
                                                                               .getCode() + "." + ds.getName());
                    odiInstance.getTransactionalEntityManager()
                               .remove(key);
                }
            }
        }
        // tm.commit(txnStatus);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setJKM(String modelCode, String jkmName) {
        OdiModel odiModel = ((IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                                                          .getFinder(OdiModel.class)).findByCode(modelCode);
        Collection<OdiJKM> odiJKMs = ((IOdiJKMFinder) odiInstance.getTransactionalEntityManager()
                                                                 .getFinder(OdiJKM.class)).findByName(jkmName,
                                                                                                      properties.getProjectCode());
        if (odiJKMs.size() != 1) {
            String message = errorWarningMessages.formatMessage(1230, ERROR_MESSAGE_01230, this.getClass(), jkmName,
                                                                properties.getProjectCode());
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(message);
        }
        OdiJKM odiJKM = odiJKMs.iterator()
                               .next();
        odiModel.setJKM(odiJKM);
        odiInstance.getTransactionalEntityManager()
                   .persist(odiModel);
        logger.info(String.format("JKM '%1$s' set for model '%2$s' in project '%3$s'.", odiJKM.getName(), modelCode,
                                  properties.getProjectCode()));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void resetJKMs() {
        List<ModelProperties> models = modelPropProvider.getConfiguredModels();
        for (ModelProperties modelp : models) {
            OdiModel odiModel = ((IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                                                              .getFinder(OdiModel.class)).findByCode(modelp.getCode());
            odiModel.setJKM(null);
            odiInstance.getTransactionalEntityManager()
                       .persist(odiModel);
            logger.info(String.format("Resetting JKMs  for model '%1$s' in project '%2$s'.", modelp.getCode(),
                                      properties.getProjectCode()));

        }
    }
}
