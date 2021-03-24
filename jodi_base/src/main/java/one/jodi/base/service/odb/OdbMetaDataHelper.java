package one.jodi.base.service.odb;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.DataTypeService;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.ForeignReference;
import one.jodi.base.service.metadata.Key;
import one.jodi.base.service.metadata.SlowlyChangingDataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class retrieves the table meta-data from the Oracle Db data dictionary
 * tables.
 */
class OdbMetaDataHelper {
    private static final Logger LOGGER = LogManager.getLogger(OdbMetaDataHelper.class);
    private static final String ERROR_MESSAGE_80840 = "Fatal error: %s";
    private static final String ERROR_MESSAGE_82000 =
            "Invalid Data type '%1$s' for column '%2$s' in table '%3$s'. " +
                    "This column is not added to the derived table";
    private static final String ERROR_MESSAGE_82010 = "Failed to retrieve foreign key meta-data. %s";
    private static final String ERROR_MESSAGE_82020 = "Failed to retrieve index meta-data. %s";
    private static final String ERROR_MESSAGE_82030 = "Failed to retrieve column information: %s";
    private static final String ERROR_MESSAGE_82040 = "Failed to retrieve tables. %s";
    private static final String ERROR_MESSAGE_82050 = "Failed to close the statement. %s";

    protected static final String sqlTableQuery = " SELECT * FROM  ( SELECT uo.object_name AS TabName, " +
            "NVL(umv.comments, utc.comments ) AS TabComments FROM  user_objects uo LEFT JOIN user_tab_comments utc " +
            "ON uo.object_name = utc.table_name LEFT JOIN user_mview_comments umv ON uo.object_name = umv.mview_name " +
            "WHERE uo.object_type IN ('TABLE','VIEW') UNION ALL SELECT uo.object_name AS TabName, utc.comments " +
            "AS TabComments FROM  user_objects uo INNER JOIN user_synonyms us ON us.synonym_name = uo.object_name " +
            "AND us.table_owner = USER LEFT JOIN user_tab_comments utc ON us.table_name = utc.table_name " +
            "WHERE uo.object_type LIKE 'SYNONYM' ) ORDER BY TabName";
    protected static final String colQuery = "SELECT "
            + "				  us.synonym_name," + "				  cols.table_name,"
            + "				  cols.column_name    AS ColumnName,"
            + "				  cols.data_type      AS DataType,"
            + "				  cols.data_length    AS DataLength,"
            + "				  cols.data_scale     AS DataScale,"
            + "				  cols.data_precision AS DataPrecision,"
            + "				  cols.nullable       AS IsNullable,"
            + "				  cols.column_id      AS ColPosition,"
            + "				  colcmnts.comments   AS ColCmnts" + "				FROM"
            + "				  user_tab_cols cols" + "				LEFT JOIN user_synonyms us"
            + "				ON" + "				  (" + "				    us.table_name = cols.table_name"
            + "				  )" + "				LEFT JOIN user_col_comments colcmnts" + "				ON"
            + "				  cols.table_name    = colcmnts.table_name"
            + "				AND cols.column_name = colcmnts.column_name "
            + "				order by 2,1";
    protected static final String indexQuery = "SELECT"
            + "			  inds.table_name, "
            + "			  us.synonym_name,"
            + "			  uic.index_name  AS IndexName,"
            + "			  uic.column_name AS IndColName,"
            + "			  inds.status     AS IsEnabled,"
            + "			  row_number() over( partition BY inds.table_name, uic.index_name order by uic.column_name DESC) isafterlast_col,"
            + "			  row_number() over( partition BY inds.table_name order by uic.index_name DESC, uic.column_name DESC) isafterlast_tab"
            + "			FROM" + "			  user_ind_columns uic"
            + "			JOIN user_indexes inds" + "			ON"
            + "			  inds.index_name = uic.index_name"
            + "			LEFT JOIN user_synonyms us" + "			ON" + "			  ("
            + "			    us.table_name = inds.table_name" + "			  )"
            + "			ORDER BY" + "			  inds.table_name," + "			  uic.index_name,"
            + "			  uic.column_name";
    protected static final String keysQuery = "SELECT "
            + "			  ucc.table_name,"
            + "			  us.synonym_name,"
            + "			  ucc.owner Owner,"
            + "			  ucc.constraint_name ConstraintName,"
            + "			  uc.constraint_type ConstraintType,"
            + "			  ucc.column_name KeyColName,"
            + "			  uc.Status IsEnabled,"
            + "			  row_number() over( partition by ucc.table_name, uc.constraint_type, ucc.constraint_name order by ucc.column_name desc) isafterlast_col,"
            + "			  row_number() over( partition by ucc.table_name order by uc.constraint_type desc, ucc.constraint_name desc, ucc.column_name desc) isafterlast_tab"
            + "			FROM" + "			  user_cons_columns ucc"
            + "			JOIN user_constraints uc" + "			ON"
            + "			  ucc.owner             = uc.owner"
            + "			AND ucc.constraint_name = uc.constraint_name"
            + "			LEFT JOIN user_synonyms us" + "			ON" + "			  ("
            + "			    us.table_name = ucc.table_name" + "			  )" + "			where uc.constraint_type IN ('P','U')" + "			ORDER BY"
            + "			  ucc.table_name," + "			  uc.constraint_type,"
            + "			  ucc.constraint_name," + "			  ucc.column_name";
    // 1 line sql since it needs to be cut and paste in editor anyway.
    protected static final String fkeysQuery = " SELECT ucc.table_name, us.synonym_name synonym_name, ucc.owner owner, ucc.column_name AS fkcolumnname, ucc.constraint_name AS fkeyname, uc.status isenabled, NVL(refus.synonym_name, refcons.table_name) reftablename, CASE WHEN refcons.constraint_type = 'P' THEN refcons.constraint_name ELSE NULL END AS refconstraintname, refucc.column_name AS pkcolumnname, row_number() over( partition BY ucc.table_name, ucc.constraint_name order by ucc.position DESC) isafterlast_col, row_number() over( partition BY ucc.table_name order by ucc.constraint_name DESC) isafterlast_tab FROM  user_cons_columns ucc INNER JOIN user_constraints uc ON ( ucc.owner = uc.owner AND ucc.constraint_name = uc.constraint_name ) LEFT JOIN user_synonyms us ON ( uc.table_name = us.table_name ) INNER JOIN user_constraints refcons ON ( uc.r_owner = refcons.owner AND uc.r_constraint_name = refcons.constraint_name ) LEFT JOIN user_synonyms refus ON ( refus.table_name = refcons.table_name ) LEFT JOIN user_cons_columns refucc ON ( refcons.table_name = refucc.table_name AND refcons.constraint_name = refucc.constraint_name AND ucc.position = REFUCC.POSITION ) ORDER BY ucc.table_name, ucc.constraint_name, ucc.position";
    protected static final String viewQuery = "	SELECT" + "	  view_name,"
            + "	  CASE" + "	    WHEN text_length > 4000" + "	    THEN 'N'"
            + "	    ELSE 'Y'" + "	  END AS valid," + "	  text" + "	FROM"
            + "	  user_views order by 1";

    private final DataTypeService dataTypeService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private Map<String, Collection<ColumnMetaData>> mapobdMetaColumnTypes =
            new LinkedHashMap<>();
    private Map<String, List<Key>> mapobdMetaKeyTypes =
            new LinkedHashMap<>();
    private Map<String, List<ForeignReference>> mapobdMetaFKeyTypes =
            new LinkedHashMap<>();
    private List<OdbDataStore> tables = new ArrayList<>();
    private List<String> tablesNames = new ArrayList<>();

    @Inject
    OdbMetaDataHelper(final DataTypeService dataTypeService,
                      final ErrorWarningMessageJodi errorWarningMessages) {
        this.dataTypeService = dataTypeService;
        this.errorWarningMessages = errorWarningMessages;
    }

    public List<OdbDataStore> getDataStoreList(Connection dbConn)
            throws SQLException {
        try {
            if (tables.size() == 0) {
                createCache(dbConn);
            }
        } catch (IOException e) {
            String msg = errorWarningMessages.formatMessage(80840,
                    ERROR_MESSAGE_80840, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            LOGGER.fatal(msg, e);
            throw new UnRecoverableException(msg, e);
        }
        LOGGER.debug("tables size:" + tables.size());
        return tables;
    }

    private void createCacheDataStoreList(Connection dbConn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery(sqlTableQuery);
            if (rs != null) {
                while (rs.next()) {
                    OdbDataStore tbl = new OdbDataStore(rs.getString("TabName"),
                            rs.getString("TabComments"));
                    tables.add(tbl);
                    tablesNames.add(rs.getString("TabName"));
                }
                rs.close();
            }
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(82040,
                    ERROR_MESSAGE_82040, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            LOGGER.error(msg, e);
            throw (e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    String msg = errorWarningMessages.formatMessage(82050,
                            ERROR_MESSAGE_82050, this.getClass(), e.getMessage());
                    LOGGER.error(msg, e);
                }
            }
        }
    }

    public void createCache(Connection dbConn) throws SQLException, IOException {
        if (tables.size() == 0) {
            createCacheDataStoreList(dbConn);
        }
        if (mapobdMetaColumnTypes.size() > 0) {
            return;
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = dbConn.prepareStatement(colQuery);
            // Retrieve columns
            rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String colName = rs.getString("ColumnName");
                    String type = rs.getString("DataType");
                    int length = rs.getInt("DataLength");
                    int scale = rs.getInt("DataScale");
                    int pos = rs.getInt("ColPosition");
                    String comments = rs.getString("ColCmnts");
                    String aDataStore = rs.getString("table_name");
                    String synonym_name = rs.getString("synonym_name");
                    final boolean isNullable = rs.getString("IsNullable").equals("Y");

                    // Handle types that contain parameters
                    if (type.contains("(")) {
                        type = type.substring(0, type.indexOf("("));
                    }
                    String mappedType = dataTypeService.getMappedType(type);
                    if (mappedType == null) {
                        // don't process unsupported / unknown types
                        String msg = errorWarningMessages.formatMessage(82000,
                                ERROR_MESSAGE_82000, this.getClass(),
                                type, colName, aDataStore);
//                  errorWarningMessages.addMessage(
//                                 errorWarningMessages.assignSequenceNumber(), 
//                                 msg, MESSAGE_TYPE.WARNINGS);
                        LOGGER.warn(msg);
                        continue;
                    } else if (mappedType.equals("DOUBLE") &&
                            scale == 0 && length >= 1 && length <= 9) {
                        // convert DOUBLE into INT if scale is 0 and precision is in range
                        mappedType = "INTEGER";
                    }

                    ColumnMetaData column = createColumnMetaData(aDataStore,
                            scale, pos, colName, length, comments, mappedType, isNullable);
                    if (mapobdMetaColumnTypes.get(aDataStore) != null) {
                        Collection<ColumnMetaData> columns = mapobdMetaColumnTypes
                                .get(aDataStore);
                        columns.add(column);
                        mapobdMetaColumnTypes.put(aDataStore, columns);
                    } else {
                        Collection<ColumnMetaData> columns = new ArrayList<>();
                        columns.add(column);
                        mapobdMetaColumnTypes.put(aDataStore, columns);
                    }
                    // store synonyms as well
                    if (synonym_name != null && synonym_name.length() > 1) {
                        if (mapobdMetaColumnTypes.get(synonym_name) != null) {
                            Collection<ColumnMetaData> columns = mapobdMetaColumnTypes
                                    .get(synonym_name);
                            columns.add(column);
                            mapobdMetaColumnTypes.put(synonym_name, columns);
                        } else {
                            Collection<ColumnMetaData> columns = new ArrayList<>();
                            columns.add(column);
                            mapobdMetaColumnTypes.put(synonym_name, columns);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(82030,
                    ERROR_MESSAGE_82030, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            LOGGER.error(msg, e);
            throw (e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to clean up record set.", e);
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                String msg = errorWarningMessages.formatMessage(82050,
                        ERROR_MESSAGE_82050, this.getClass(), e.getMessage());
                LOGGER.error(msg, e);
            }
        }
        createCacheKeys(dbConn);
        createCacheFKeys(dbConn);
    }

    private void createCacheKeys(Connection dbConn) throws SQLException {
        if (mapobdMetaKeyTypes.size() > 0) {
            return;
        }
        // Retrieve indexes, PK & Alternate Keys
        String indexName = "";
        List<Key> keys = new ArrayList<>();
        List<String> indexCols = new ArrayList<>();
        boolean indexEnabled = false;
        int colCounter = 0; // to determine when to create the key object & add it to the key list
        try (PreparedStatement statement = dbConn.prepareStatement(indexQuery); ResultSet rs = statement.executeQuery()) {
            // Retrieve index
            if (rs != null) {
                while (rs.next()) {
                    // else found multiple constraint columns for the same constraint
                    if (!indexName.equalsIgnoreCase(rs.getString("IndexName"))) {
                        if (colCounter != 0) {
                            // add previous constraint to the list
                            keys.add(createKey(rs.getString("table_name"),
                                    indexName, indexEnabled, indexCols,
                                    Key.KeyType.INDEX));
                            if (rs.getString("synonym_name") != null) {
                                keys.add(createKey(
                                        rs.getString("synonym_name"),
                                        indexName, indexEnabled, indexCols,
                                        Key.KeyType.INDEX));
                            }
                            indexCols = new ArrayList<>();
                            colCounter = 0;
                        }
                        indexName = rs.getString("IndexName");
                        LOGGER.debug("Retrieving index meta-data: " + indexName);
                        indexEnabled = rs.getString("IsEnabled").equals("VALID");
                    }
                    indexCols.add(rs.getString("IndColName"));

                    colCounter++;
                    if (rs.getInt("isafterlast_col") == 1) {
                        // add current constraint to the list if is the last
                        // record in
                        // the resultset
                        keys.add(createKey(rs.getString("table_name"),
                                indexName, indexEnabled, indexCols,
                                Key.KeyType.INDEX));
                        if (rs.getString("synonym_name") != null) {
                            keys.add(createKey(rs.getString("synonym_name"),
                                    indexName, indexEnabled, indexCols,
                                    Key.KeyType.INDEX));
                        }
                    }
                    if (rs.getInt("isafterlast_tab") == 1) {
                        if (rs.getString("synonym_name") != null) {
                            mapobdMetaKeyTypes.put(
                                    rs.getString("synonym_name"), keys);
                        }
                        mapobdMetaKeyTypes
                                .put(rs.getString("table_Name"), keys);
                        keys = new ArrayList<>();
                    }
                }
            }
            // Retrieve PK, UK, FK with ref columns
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(82020,
                    ERROR_MESSAGE_82020, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            LOGGER.error(msg, e);
            throw (e);
        }

        try (PreparedStatement statement = dbConn.prepareStatement(keysQuery); ResultSet rs = statement.executeQuery()) {
            List<String> keyCols = new ArrayList<>();
            if (rs != null) {
                while (rs.next()) {
                    // get next constraint.
                    String keyName = rs.getString("ConstraintName");
                    LOGGER.debug("Retrieving index meta-data: " + keyName);
                    boolean enabled = rs.getString("IsEnabled").equals("ENABLED");
                    Key.KeyType keyType = mapKeyType(rs.getString("ConstraintType"));
                    keyCols.add(rs.getString("KeyColName"));
                    if (rs.getInt("isafterlast_col") == 1) {
                        // add current constraint to the list if itis the last
                        // record in
                        // the result set
                        if (rs.getString("synonym_name") != null) {
                            keys.add(createKey(rs.getString("synonym_name"),
                                    keyName, enabled, keyCols, keyType));
                        }
                        keys.add(createKey(rs.getString("table_name"), keyName,
                                enabled, keyCols, keyType));
                        keyCols = new ArrayList<>();
                    }
                    if (rs.getInt("isafterlast_tab") == 1) {
                        if (mapobdMetaKeyTypes.get(rs.getString("table_Name")) != null) {
                            keys.addAll(mapobdMetaKeyTypes.get(rs
                                    .getString("table_Name")));
                        }
                        if (mapobdMetaKeyTypes
                                .get(rs.getString("synonym_name")) != null) {
                            keys.addAll(mapobdMetaKeyTypes.get(rs
                                    .getString("synonym_name")));
                        }
                        if (rs.getString("synonym_name") != null) {
                            mapobdMetaKeyTypes.put(
                                    rs.getString("synonym_name"), keys);
                        }
                        mapobdMetaKeyTypes
                                .put(rs.getString("table_Name"), keys);
                        keys = new ArrayList<>();
                    }
                }
            }
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(82020,
                    ERROR_MESSAGE_82020, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            LOGGER.error(msg, e);
            throw (e);
        }
    }

    private void createCacheFKeys(final Connection dbConn) throws SQLException {
        if (mapobdMetaFKeyTypes.size() > 0) {
            return;
        }
        // Retrieve FK with ref columns
        List<ForeignReference> fkRefs = new ArrayList<>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = dbConn.prepareStatement(fkeysQuery);
            rs = stmt.executeQuery();
            List<ForeignReference.RefColumns> refs = new ArrayList<>();
            if (rs != null) {
                while (rs.next()) {
                    String constraintName = rs.getString("FKeyName");
                    LOGGER.debug("Retrieving foregn key meta-data: " + constraintName);
                    boolean enabled = rs.getString("IsEnabled").equals("ENABLED");
                    refs.addAll(getReferenceColumns(rs.getString("FKColumnName"),
                            rs.getString("PKColumnName")));
                    if (rs.getInt("isafterlast_col") == 1) {
                        fkRefs.add(createFKReference(constraintName, enabled,
                                rs.getString("Owner"),
                                rs.getString("RefTableName"), refs));
                        LOGGER.debug("refs size: " + refs.size());
                        for (ForeignReference.RefColumns r : refs) {
                            LOGGER.debug(rs.getString("FKEYNAME") + " : " +
                                    rs.getString("REFTABLENAME") + ":" +
                                    r.getForeignKeyColumnName() + ":" +
                                    r.getPrimaryKeyColumnName());
                        }
                        LOGGER.debug("---------");
                        refs = new ArrayList<>();
                    }
                    if (rs.getInt("isafterlast_tab") == 1) {
                        if (rs.getString("synonym_name") != null) {
                            mapobdMetaFKeyTypes.put(rs.getString("synonym_name"), fkRefs);
                        }
                        mapobdMetaFKeyTypes.put(rs.getString("table_Name"), fkRefs);
                        fkRefs = new ArrayList<>();
                    }
                }
            }
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(82010, ERROR_MESSAGE_82010,
                    this.getClass(), e);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            LOGGER.error(msg, e);
            throw (e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to clean up record set.", e);
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                String msg = errorWarningMessages.formatMessage(82050,
                        ERROR_MESSAGE_82050, this.getClass(),
                        e.getMessage());
                LOGGER.error(msg, e);
            }
        }
    }

    public Collection<ColumnMetaData> getColumnMetaData(final String dataStore, final Connection dbConn)
            throws SQLException {
        LOGGER.debug("dataStore:" + dataStore);
        Collection<ColumnMetaData> cMetaData = Collections.emptyList();
        if (mapobdMetaColumnTypes.get(dataStore) != null) {
            cMetaData = mapobdMetaColumnTypes.get(dataStore);
        }
        return cMetaData;
    }

    private ColumnMetaData createColumnMetaData(final String dataStoreName,
                                                final int scale, final int pos,
                                                final String colName, final int length,
                                                final String comments, final String mappedType,
                                                final boolean isNullable) {
        return new ColumnMetaData() {
            @Override
            public boolean hasNotNullConstraint() {
                return !isNullable;
            }

            @Override
            public int getScale() {
                return scale;
            }

            @Override
            public int getPosition() {
                return pos;
            }

            @Override
            public String getName() {
                return colName;
            }

            @Override
            public int getLength() {
                return length;
            }

            @Override
            public Map<String, Object> getFlexFieldValues() {
                return null;
            }

            @Override
            public String getDescription() {
                return comments;
            }

            @Override
            public SlowlyChangingDataType getColumnSCDType() {
                return null;
            }

            @Override
            public String getColumnDataType() {
                return mappedType;
            }

            @Override
            public String getDataStoreName() {
                return dataStoreName;
            }
        };
    }

    public List<Key> getKeyandIndexMetaData(String dataStore, Connection dbConn)
            throws SQLException {
        if (mapobdMetaKeyTypes.get(dataStore) == null) {
            return new ArrayList<>();
        } else {
            return mapobdMetaKeyTypes.get(dataStore);
        }
    }

    private Key.KeyType mapKeyType(String mapType) {
        Key.KeyType result;
        if (mapType.equalsIgnoreCase("P")) {
            result = Key.KeyType.PRIMARY;
        } else if (mapType.equalsIgnoreCase("U")) {
            result = Key.KeyType.ALTERNATE;
        } else {
            result = Key.KeyType.INDEX;
        }
        return result;
    }

    private Key createKey(final String dataStoreName, final String name,
                          final boolean enabled, final List<String> cols,
                          final Key.KeyType constraintType) {
        return new Key() {
            @Override
            public boolean isEnabledInDatabase() {
                return enabled;
            }

            @Override
            public KeyType getType() {
                return constraintType;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public List<String> getColumns() {
                return cols;
            }

            @Override
            public boolean existsInDatabase() {
                return true;
            }

            @Override
            public String getDataStoreName() {
                return dataStoreName;
            }

            @Override
            public void setDataStoreName(String datastoreName) {
            }
        };
    }

    public List<ForeignReference> getFKRefs(String dataStore, Connection dbConn)
            throws SQLException {
        if (mapobdMetaFKeyTypes.get(dataStore) == null) {
            return new ArrayList<>();
        } else {
            return mapobdMetaFKeyTypes.get(dataStore);
        }
    }

    private List<ForeignReference.RefColumns> getReferenceColumns(
            final String fkColName, final String pkColName) {
        List<ForeignReference.RefColumns> refColumns = new ArrayList<>();
        refColumns.add(new ForeignReference.RefColumns() {
            @Override
            public String getForeignKeyColumnName() {
                return fkColName;
            }

            @Override
            public String getPrimaryKeyColumnName() {
                return pkColName;
            }
        });
        return refColumns;
    }

    private ForeignReference createFKReference(final String name,
                                               final boolean enabled, final String schemaName,
                                               final String dataStoreName,
                                               final List<ForeignReference.RefColumns> refColumns) {
        return new ForeignReference() {
            @Override
            public boolean isEnabledInDatabase() {
                return enabled;
            }

            @Override
            public List<RefColumns> getReferenceColumns() {
                return refColumns;
            }

            @Override
            public String getPrimaryKeyDataStoreName() {
                return dataStoreName;
            }

            @Override
            public String getPrimaryKeyDataStoreModelCode() {
                return schemaName;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    public void clearCache() {
        mapobdMetaColumnTypes = new LinkedHashMap<>();
        mapobdMetaKeyTypes = new LinkedHashMap<>();
        mapobdMetaFKeyTypes = new LinkedHashMap<>();
        tables = new ArrayList<>();
        tablesNames = new ArrayList<>();
    }
}