package one.jodi.base.service.odb;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.config.BaseConfigurations;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataModelDescriptor;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.ForeignReference;
import one.jodi.base.service.metadata.Key;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.base.util.Register;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class retrieves the table meta-data from the Oracle Db data dictionary tables.
 */
@Singleton
public class OdbETLProvider implements SchemaMetaDataProvider {

    private final static Logger logger = LogManager.getLogger(OdbETLProvider.class);
    private static final String ERROR_MESSAGE_80820 =
            "Failed to properly close database connection for '%s'. %s";
    private static final String ERROR_MESSAGE_80840 =
            "Fatal error: %s";
    private static final String ERROR_MESSAGE_80850 =
            "Failed to get database connection for '%s'. " +
                    "Verify the database connection. %s";
    private static final String ERROR_MESSAGE_80860 =
            "Failed to get jdbcUrl from properties file.";
    private static final String ERROR_MESSAGE_80870 =
            "No data store in the given database! Verify the database for data store objects.";

    @SuppressWarnings("unused")
    private final BaseConfigurations biProperties;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final DbConnectionUtil dbConnUtil;
    private final OdbMetaDataHelper dbHelper;
    private final List<Pattern> inclusionPattern = new ArrayList<>();
    private final List<Pattern> exclusionPattern = new ArrayList<>();
    private final Map<String, Map<String, DataStoreDescriptor>> models = new HashMap<>();
    /* @Inject @Registered private */ Register register;
    private Connection dbConn;
    private String jdbcUrl;
    private String userName;

    @Inject
    protected OdbETLProvider(final DbConnectionUtil dbConnUtil,
                             final OdbMetaDataHelper dbHelper,
                             final BaseConfigurations biProperties,
                             final ErrorWarningMessageJodi errorWarningMessages) {
        this.dbConnUtil = dbConnUtil;
        this.dbHelper = dbHelper;
        this.biProperties = biProperties;
        this.errorWarningMessages = errorWarningMessages;

        exclusionPattern.addAll(
                biProperties.getTableExclusionPattern()
                        .stream()
                        .map(regex -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                        .collect(Collectors.toList()));
        inclusionPattern.addAll(
                biProperties.getTableInclusionPattern()
                        .stream()
                        .map(regex -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                        .collect(Collectors.toList()));
    }

    @Override
    public void initDBConnection(final String jdbcUrl, final String userName, final String password) {
        if (this.dbConn != null) {
            try {
                this.dbConn.close();
            } catch (SQLException e) {
                logger.error(e);
            }
        }
        this.dbConn = this.dbConnUtil.getDatabaseConnection(
                jdbcUrl,
                userName,
                password);
        this.jdbcUrl = jdbcUrl;
        this.userName = userName;
        this.dbHelper.clearCache();
    }

    @Override
    public void closeDBConnection() {
        try {
            if (!this.dbConn.isClosed()) {
                this.dbConn.close();
            }
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(80820,
                    ERROR_MESSAGE_80820, this.getClass(),
                    this.jdbcUrl, e.getMessage());
            logger.error(msg, e);
        }
    }

    @Override
    public boolean existsProject(String projectCode) {
        return false;
    }

    @Cached
    @Override
    public List<String> getModelCodes() {
        List<String> modelCodes = new ArrayList<>();
        modelCodes.add(this.userName);
        return modelCodes;
    }

    @Cached
    @Override
    public List<DataModelDescriptor> getDataModelDescriptors() {
        List<DataModelDescriptor> dmdList = new ArrayList<>();
        dmdList.add(createDataModelDescriptor());
        return dmdList;
    }

    private boolean matchesRegex(final Pattern pattern, final String input) {
        Matcher m = pattern.matcher(input);
        return m.matches();
    }

    private boolean includeIntoMdel(final String tableName) {
        boolean include = false;
        for (Pattern pattern : this.inclusionPattern) {
            if (matchesRegex(pattern, tableName)) {
                include = true;
                break;
            }
        }
        return include;
    }

    private boolean excludeFromModel(final String tableName) {
        boolean exclude = false;
        for (Pattern pattern : this.exclusionPattern) {
            if (matchesRegex(pattern, tableName)) {
                exclude = true;
                break;
            }
        }
        return exclude;
    }

    private boolean addToModel(final String tableName) {
        boolean add = true;
        if (exclusionPattern.size() > 0 && inclusionPattern.size() > 0) {
            add = includeIntoMdel(tableName) && !excludeFromModel(tableName);
        } else if (exclusionPattern.size() > 0) {
            add = !excludeFromModel(tableName);
        } else if (inclusionPattern.size() > 0) {
            add = includeIntoMdel(tableName);
        }
        if (add) {
            logger.debug("add: " + tableName);
        }

        return add;
    }


    @Cached
    @Override
    public Map<String, DataStoreDescriptor> getDataStoreDescriptorsInModel(
            String modelCode) {
        if (models.get(modelCode) != null) {
            return models.get(modelCode);
        }
        logger.debug("In getDataStoreDescriptorsInModel: " +
                "Retrieving data store details.");
        //get db connection
        if (dbConn == null) {
            String msg = errorWarningMessages.formatMessage(80850,
                    ERROR_MESSAGE_80850, this.getClass(),
                    this.jdbcUrl, "");
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            //TODO raise DbConnectionNotFound exception??
            return null;
        }
        // retrieve all the table names for the given data model
        List<OdbDataStore> dataStoreList;
        try {
            dbHelper.createCache(dbConn);
            dataStoreList = dbHelper.getDataStoreList(dbConn);
            logger.debug("size:" + dataStoreList.size());
        } catch (SQLException | IOException e) {
            String msg = errorWarningMessages.formatMessage(80850,
                    ERROR_MESSAGE_80850, this.getClass(),
                    this.jdbcUrl, e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
            return null;
        }
        if (dataStoreList.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(80870,
                    ERROR_MESSAGE_80870, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return null;
        } else {
            logger.debug(dataStoreList.size() + " data stores found.");
        }

        // create data model descriptor used in model creation
        DataModelDescriptor dmDescr = createDataModelDescriptor();

        Map<String, DataStoreDescriptor> dataStoreDescriptors = new TreeMap<>();
        for (OdbDataStore odbDataStore : dataStoreList) {
            // sample09 contains datastore with 'unsupported' datatypes,
            // policy suggest we should only refactor datastores starting with W_
            // If you try to remove the line below HSGBU will fail.
            if (!addToModel(odbDataStore.getDataStoreName())) {
                logger.debug(odbDataStore.getDataStoreName());
                continue;
            }
            String dataStoreName = odbDataStore.getDataStoreName();
            logger.debug("****Retrieving meta-data for data store: " + dataStoreName);
            try {
                dataStoreDescriptors.put(
                        dataStoreName,
                        createDataStoreDescriptor(dataStoreName,
                                odbDataStore.getDataStoreComments(), dmDescr, dbConn));
            } catch (IOException e) {
                // add 90110 msg here
                String msg = errorWarningMessages.formatMessage(80840,
                        ERROR_MESSAGE_80840, this.getClass(), e.getMessage());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.fatal(msg, e);
                throw new UnRecoverableException(msg, e);
            }
        }
        models.put(modelCode, dataStoreDescriptors);
        return dataStoreDescriptors;
    }

    DataStoreDescriptor createDataStoreDescriptor(final String dataStore,
                                                  final String dataStoreComments,
                                                  final DataModelDescriptor dataModelDescriptor,
                                                  final Connection dbConn) throws IOException {
        try {
            dbHelper.createCache(dbConn);
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(80840,
                    ERROR_MESSAGE_80840, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.fatal(msg, e);
            throw new UnRecoverableException(msg, e);
        }
        //get column meta-data
        final Collection<ColumnMetaData> cols;
        try {
            cols = dbHelper.getColumnMetaData(dataStore, dbConn);
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(80840,
                    ERROR_MESSAGE_80840, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.fatal(msg, e);
            throw new UnRecoverableException(msg, e);
        }
        //get primary key, alternate key & index meta-data
        final List<Key> keys;
        try {
            keys = dbHelper.getKeyandIndexMetaData(dataStore, dbConn);
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(80840,
                    ERROR_MESSAGE_80840, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.fatal(msg, e);
            throw new UnRecoverableException(msg, e);
        }
        //get foreign key and ref cols meta-data
        final List<ForeignReference> fkRefs;
        try {
            fkRefs = dbHelper.getFKRefs(dataStore, dbConn);
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(80840,
                    ERROR_MESSAGE_80840, this.getClass(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.fatal(msg, e);
            throw new UnRecoverableException(msg, e);
        }

        return new DataStoreDescriptor() {

            @Override
            public String getDataStoreName() {
                return dataStore;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public Map<String, Object> getDataStoreFlexfields() {
                return null;
            }

            @Override
            public DataModelDescriptor getDataModelDescriptor() {
                return dataModelDescriptor;
            }

            @Override
            public Collection<ColumnMetaData> getColumnMetaData() {
                return Collections.unmodifiableCollection(cols);
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
                return dataStoreComments;
            }
        };
    }

    private boolean jdbcUrlExists(final String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.length() > 0;
    }

    private DataModelDescriptor createDataModelDescriptor() {
        logger.debug("JDBC Url: " + jdbcUrl);
        if (!jdbcUrlExists(jdbcUrl)) {
            String msg = errorWarningMessages.formatMessage(80860,
                    ERROR_MESSAGE_80860, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            return null;
        }

        final String[] jdbcParts = jdbcUrl.split("/");
        String[] serverPartsString;
        String databaseServiceNameIfNotFound = "";
        try {
            serverPartsString = jdbcParts[2].split(":");
        } catch (ArrayIndexOutOfBoundsException e) {
            // there may be just one / that one of the service name,
            // since this is perfectly fine jdbc url
            // jdbc:oracle:thin:@jodi:1521/FT06
            // vs jdbc:oracle:thin:@jodi:1521/FT06
            final String[] jdbcPartsString = jdbcUrl.split("@");
            serverPartsString = jdbcPartsString[1].split(":");
            serverPartsString[1] = serverPartsString[1].substring(0, serverPartsString[1].indexOf("/"));
            databaseServiceNameIfNotFound = jdbcPartsString[1].substring(jdbcPartsString[1].indexOf("/") + 1).trim();
        }
        final String[] serverParts = serverPartsString;
        final String schemaName = this.userName;

        final String dataServerName = serverParts[0];
        final String port = serverParts[1];
        final String dataBaseServiceName = databaseServiceNameIfNotFound.length() > 0
                ? databaseServiceNameIfNotFound
                : jdbcParts[3];

        return new DataModelDescriptor() {

            @Override
            public String getModelCode() {
                return schemaName;
            }

            @Override
            public Map<String, Object> getModelFlexfields() {
                return null;
            }

            @Override
            public String getPhysicalDataServerName() {
                return dataServerName;
            }

            @Override
            public String getDataServerName() {
                return dataServerName;
            }

            @Override
            public String getDataServerTechnology() {
                return "ORACLE";
            }

            @Override
            public String getSchemaName() {
                return schemaName;
            }

            @Override
            public String getDataBaseServiceName() {
                return dataBaseServiceName;
            }

            @Override
            public int getDataBaseServicePort() {
                return Integer.parseInt(port);
            }
        };
    }

    @Override
    public boolean projectVariableExists(String projectCode, String variableName) {
        return false;
    }

    @Override
    public boolean globalVariableExists(String variableName) {
        return false;
    }


    @Override
    public Map<String, String> translateModelToLogicalSchema() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getColumnNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getTableNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getLogicalSchemaNames() {
        // TODO Auto-generated method stub
        return null;
    }

}
