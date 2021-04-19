package one.jodi.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dbunit.Assertion;
import org.dbunit.DBTestCase;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseDataSet;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.*;
import org.dbunit.dataset.filter.DefaultTableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.util.fileloader.FlatXmlDataFileLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class DBUnitHelper extends DBTestCase {
    private static final Logger logger = LogManager.getLogger(DBUnitHelper.class);
    private final String dir;
    private final String dumpFile;
    private final String jdbcDBConnection;
    private final String jdbcDBUsername;
    private final String jdbcDBPassword;
    private final String jdbcDBConnectionREF;
    private final String jdbcDBUsernameREF;
    private final String jdbcDBPasswordREF;

    //
    public DBUnitHelper(String dir, String dumpFile, String driverDBClass,
                        String jdbcDBConnection, String jdbcDBUsername,
                        String jdbcDBPassword, String driverDBClassREF,
                        String jdbcDBConnectionREF, String jdbcDBUsernameREF,
                        String jdbcDBPasswordREF) {
        this.dir = dir;
        this.dumpFile = dumpFile;
        this.jdbcDBConnection = jdbcDBConnection;
        this.jdbcDBUsername = jdbcDBUsername;
        this.jdbcDBPassword = jdbcDBPassword;
        this.jdbcDBConnectionREF = jdbcDBConnectionREF;
        this.jdbcDBUsernameREF = jdbcDBUsernameREF;
        this.jdbcDBPasswordREF = jdbcDBPasswordREF;
    }

    public void fullDatabaseExport() {
        FileOutputStream fos = null;
        File file = new File(dir, dumpFile);
        IDatabaseConnection connection = null;
        try {
            connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
            IDataSet dataset = connection.createDataSet();
            fos = new FileOutputStream(file);
            FlatXmlDataSet.write(dataset, fos);
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (SQLException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    logger.fatal(e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
    }

    public void fullDatabaseImport() {
        File file = new File(dir, dumpFile);
        IDatabaseConnection connection = null;
        try {
            connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
            DefaultTableFilter tableFilter = new DefaultTableFilter();
            tableFilter.includeTable("DWH_STG*");
            @SuppressWarnings("deprecation")
            IDataSet dataSet = new FlatXmlDataSet(file, true);
            FilteredDataSet filteredDS = new FilteredDataSet(tableFilter,
                    dataSet);
            DatabaseOperation.CLEAN_INSERT.execute(connection, filteredDS);
            IDatabaseConnection connectionRef = getDBConnection(jdbcDBConnectionREF, jdbcDBUsernameREF, jdbcDBPasswordREF);
            DefaultTableFilter tableFilter2 = new DefaultTableFilter();
            tableFilter2.excludeTable("DWH_*");
            @SuppressWarnings("deprecation")
            IDataSet dataSetRef = new FlatXmlDataSet(file, true);
            FilteredDataSet filteredDS2 = new FilteredDataSet(tableFilter2,
                    dataSetRef);
            DatabaseOperation.CLEAN_INSERT.execute(connectionRef, filteredDS2);
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (SQLException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }

    }

    public void fullDMTDatabaseImport() {
        File file = new File(dir, dumpFile);
        IDatabaseConnection connection = null;
        try {
            connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
            DefaultTableFilter tableFilter = new DefaultTableFilter();
            tableFilter.includeTable("DWH_DMT*");
            @SuppressWarnings("deprecation")
            IDataSet dataSet = new FlatXmlDataSet(file, true);
            FilteredDataSet filteredDS = new FilteredDataSet(tableFilter,
                    dataSet);
            DatabaseOperation.CLEAN_INSERT.execute(connection, filteredDS);
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (SQLException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private IDatabaseConnection getDBConnection(String jdbcDBConnection, String jdbcDBUsername, String jdbcDBPassword) {
        Connection jdbcConnection = null;
        DatabaseConnection connection = null;
        try {
            jdbcConnection = DriverManager.getConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
            String id = "http://www.dbunit.org/features/qualifiedTableNames";
            String id2 = "http://www.dbunit.org/features/caseSensitiveTableNames";
            connection = new DatabaseConnection(jdbcConnection);
            DatabaseConfig config = connection.getConfig();
            if (!config.getFeature(id)) {
                config.setFeature(id, true);
            }
            if (!config.getFeature(id2)) {
                // this feature is set by new DatabaseDataSet(...,true);
                config.setFeature(id2, true);
            }
            jdbcConnection = DriverManager.getConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
        } catch (SQLException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } finally {
            if (jdbcConnection != null) {
                try {
                    jdbcConnection.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
        assert (connection != null);
        // allow for caching of the connection in a singleton.
        return connection;
    }

    @SuppressWarnings("deprecation")
    public void insertXML() {
        File file = new File(dir, dumpFile);
        IDatabaseConnection connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
        IDataSet dataSet = null;
        try {
            dataSet = new FlatXmlDataSet(file, true);
            DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
        } catch (DataSetException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } catch (SQLException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
    }

    protected IDataSet getDataSet() throws Exception {
        return new DatabaseDataSet(getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword), true);
    }

    protected IDataSet getRefDataSet() throws Exception {
        return new DatabaseDataSet(getDBConnection(jdbcDBConnectionREF, jdbcDBUsernameREF, jdbcDBPasswordREF), true);
    }

    protected DatabaseOperation getSetUpOperation() throws Exception {
        return DatabaseOperation.REFRESH;
    }

    protected DatabaseOperation getTearDownOperation() throws Exception {
        return DatabaseOperation.NONE;
    }

    // @TODO relies on -ea enableassertion as DBUnit does.
    public boolean areEqual(String expectedTableString, String actualTableString) {
        ITable expectedTable;
        try {
            expectedTable = getRefDataSet().getTable(expectedTableString);
        } catch (DataSetException e) {
            logger.fatal(e);
            return false;
        } catch (Exception e) {
            logger.fatal(e);
            return false;
        }
        ITable actualTable;
        try {
            actualTable = getDataSet()
                    .getTable(actualTableString.toUpperCase());
        } catch (DataSetException e) {
            logger.fatal(e);
            return false;
        } catch (Exception e) {
            logger.fatal(e);
            return false;
        }
        try {
            Map<String, StringBuffer> expectedValues = new TreeMap<String, StringBuffer>();
            Map<String, StringBuffer> actualValues = new TreeMap<String, StringBuffer>();
            for (int i = 0; i < expectedTable.getRowCount(); i++) {
                ITableMetaData tabMeta = expectedTable.getTableMetaData();
                StringBuffer row = new StringBuffer("");
                for (Column column : tabMeta.getColumns()) {
                    Object value = expectedTable.getValue(i, column.getColumnName());
                    row.append(column.getColumnName() + value);
                }
                expectedValues.put(row.toString(), row);

            }
            for (int i = 0; i < actualTable.getRowCount(); i++) {
                ITableMetaData tabMeta = actualTable.getTableMetaData();
                StringBuffer row = new StringBuffer("");
                for (Column column : tabMeta.getColumns()) {
                    Object value = actualTable.getValue(i, column.getColumnName());
                    row.append(column.getColumnName() + value);
                }
                actualValues.put(row.toString(), row);
            }
            boolean notEqual = false;
            for (Entry<String, StringBuffer> expected : expectedValues.entrySet()) {
                assert (expected != null);
                assert (expected.getKey() != null);
                assert (expected.getValue() != null);
                assert (actualValues != null);
                assert (actualValues.get(expected.getKey()) != null);
                //StringBuffer ev = expected.getValue();
                String ek = expected.getKey();
                StringBuffer actualV = actualValues.get(ek) != null ? actualValues.get(ek) : new StringBuffer();
                if (!expected.getValue().toString().equalsIgnoreCase(actualV.toString())) {
                    notEqual = true;
                    logger.info(expected.getValue().toString());
                    logger.info(actualV.toString());
                    break;
                }
            }
            return !notEqual;
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            return false;
        }
    }

    public boolean areEqual(File expectedTableXML, String expectedTableString,
                            String actualTableString) throws Exception {
        FlatXmlDataFileLoader loader = new FlatXmlDataFileLoader();
        @SuppressWarnings("deprecation")
        IDataSet ds = loader.getBuilder().build(expectedTableXML.toURL());
        ITable expectedTable = ds.getTable(expectedTableString);
        ITable actualTable = getDataSet().getTable(actualTableString);
        try {
            Assertion.assertEquals(expectedTable, actualTable);
            return true;
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            return false;
        }
    }

    public boolean areEqual(File expectedTableXML, String expectedTableString,
                            String actualTableString, String[] sortColumn) throws Exception {
        assert (sortColumn.length > 0) : "provide at least one column";
        FlatXmlDataFileLoader loader = new FlatXmlDataFileLoader();
        @SuppressWarnings("deprecation")
        IDataSet ds = loader.getBuilder().build(expectedTableXML.toURL());
        ITable expectedUnsorted = ds.getTable(expectedTableString);
        ITable expectedTable = new SortedTable(expectedUnsorted, sortColumn);
        ITable actualUnsorted = getDataSet().getTable(actualTableString);
        ITable actualTable = new SortedTable(actualUnsorted, sortColumn);
        try {
            Assertion.assertEquals(expectedTable, actualTable);
            return true;
        } catch (DatabaseUnitException e) {
            logger.fatal(e);
            return false;
        }
    }

}