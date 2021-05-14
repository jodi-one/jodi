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
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.SortedTable;
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

   private IDatabaseConnection connection;

   private IDatabaseConnection connectionRef;

   //
   public DBUnitHelper(final String dir, final String dumpFile, final String driverDBClass,
                       final String jdbcDBConnection, final String jdbcDBUsername, final String jdbcDBPassword,
                       final String driverDBClassREF, final String jdbcDBConnectionREF, final String jdbcDBUsernameREF,
                       final String jdbcDBPasswordREF) {
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
      final File file = new File(dir, dumpFile);
      IDatabaseConnection connection = null;
      try {
         connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
         final IDataSet dataset = connection.createDataSet();
         fos = new FileOutputStream(file);
         FlatXmlDataSet.write(dataset, fos);
      } catch (final DatabaseUnitException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final SQLException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final IOException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } finally {
         if (fos != null) {
            try {
               fos.close();
            } catch (final IOException e) {
               // TODO Auto-generated catch block
               logger.fatal(e);
            }
         }
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e) {
               logger.fatal(e);
            }
         }
      }
   }

   public void fullDatabaseImport() {
      final File file = new File(dir, dumpFile);
      IDatabaseConnection connection = null;
      try {
         logger.info(String.format("url : %s username %s", jdbcDBConnection, jdbcDBUsername));
         connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
         final DefaultTableFilter tableFilter = new DefaultTableFilter();
         tableFilter.includeTable("DWH_STG*");
         @SuppressWarnings("deprecation") final IDataSet dataSet = new FlatXmlDataSet(file, true);
         final FilteredDataSet filteredDS = new FilteredDataSet(tableFilter, dataSet);
         DatabaseOperation.CLEAN_INSERT.execute(connection, filteredDS);
         final IDatabaseConnection connectionRef =
                 getDBConnection(jdbcDBConnectionREF, jdbcDBUsernameREF, jdbcDBPasswordREF);
         final DefaultTableFilter tableFilter2 = new DefaultTableFilter();
         tableFilter2.excludeTable("DWH_*");
         @SuppressWarnings("deprecation") final IDataSet dataSetRef = new FlatXmlDataSet(file, true);
         final FilteredDataSet filteredDS2 = new FilteredDataSet(tableFilter2, dataSetRef);
         DatabaseOperation.CLEAN_INSERT.execute(connectionRef, filteredDS2);
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e) {
               logger.fatal(e);
            }
         }
         if (connectionRef != null) {
            try {
               connectionRef.close();
            } catch (final SQLException e) {
               logger.fatal(e);
            }
         }
      } catch (final DatabaseUnitException e) {
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e1) {
               logger.fatal(e1);
            }
         }
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final SQLException e) {
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e2) {
               logger.fatal(e2);
            }
         }
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final IOException e) {
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e3) {
               logger.fatal(e3);
            }
         }
         logger.fatal(e);
         throw new RuntimeException(e);
      } finally {
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e) {
               logger.fatal(e);
            }
         }
      }

   }

   public void fullDMTDatabaseImport() {
      final File file = new File(dir, dumpFile);
      IDatabaseConnection connection = null;
      try {
         connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
         final DefaultTableFilter tableFilter = new DefaultTableFilter();
         tableFilter.includeTable("DWH_DMT*");
         @SuppressWarnings("deprecation") final IDataSet dataSet = new FlatXmlDataSet(file, true);
         final FilteredDataSet filteredDS = new FilteredDataSet(tableFilter, dataSet);
         DatabaseOperation.CLEAN_INSERT.execute(connection, filteredDS);
      } catch (final DatabaseUnitException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final SQLException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final IOException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } finally {
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e) {
               logger.fatal(e);
            }
         }
      }
   }

   @SuppressWarnings("deprecation")
   private IDatabaseConnection getDBConnection(final String jdbcDBConnection, final String jdbcDBUsername,
                                               final String jdbcDBPassword) {
      Connection jdbcConnection = null;
      DatabaseConnection connection = null;
      try {
         jdbcConnection = DriverManager.getConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
         final String id = "http://www.dbunit.org/features/qualifiedTableNames";
         final String id2 = "http://www.dbunit.org/features/caseSensitiveTableNames";
         connection = new DatabaseConnection(jdbcConnection);
         final DatabaseConfig config = connection.getConfig();
         if (!config.getFeature(id)) {
            config.setFeature(id, true);
         }
         if (!config.getFeature(id2)) {
            // this feature is set by new DatabaseDataSet(...,true);
            config.setFeature(id2, true);
         }
         //jdbcConnection = DriverManager.getConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
      } catch (final SQLException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final DatabaseUnitException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } finally {
//         if (jdbcConnection != null) {
//            try {
//               jdbcConnection.close();
//            } catch (final SQLException e) {
//               logger.fatal(e);
//            }
//         }
      }
      assert (connection != null);
      // allow for caching of the connection in a singleton.
      return connection;
   }

   @SuppressWarnings("deprecation")
   public void insertXML() {
      final File file = new File(dir, dumpFile);
      final IDatabaseConnection connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
      IDataSet dataSet = null;
      try {
         dataSet = new FlatXmlDataSet(file, true);
         DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
      } catch (final DataSetException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final IOException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final DatabaseUnitException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } catch (final SQLException e) {
         logger.fatal(e);
         throw new RuntimeException(e);
      } finally {
         if (connection != null) {
            try {
               connection.close();
            } catch (final SQLException e) {
               logger.fatal(e);
            }
         }
      }
   }

   @Override
   protected IDataSet getDataSet() throws Exception {
      if (this.connection == null || this.connection.getConnection()
                                                    .isClosed()) {
         this.connection = getDBConnection(jdbcDBConnection, jdbcDBUsername, jdbcDBPassword);
      }
      return new DatabaseDataSet(connection, true);
   }

   protected IDataSet getRefDataSet() throws Exception {
      if (this.connectionRef == null || this.connectionRef.getConnection()
                                                          .isClosed()) {
         this.connectionRef = getDBConnection(jdbcDBConnectionREF, jdbcDBUsernameREF, jdbcDBPasswordREF);
      }
      return new DatabaseDataSet(connectionRef, true);
   }

   @Override
   protected DatabaseOperation getSetUpOperation() throws Exception {
      return DatabaseOperation.REFRESH;
   }

   @Override
   protected DatabaseOperation getTearDownOperation() throws Exception {
      return DatabaseOperation.NONE;
   }

   // @TODO relies on -ea enableassertion as DBUnit does.
   public boolean areEqual(final String expectedTableString, final String actualTableString) {
      final ITable expectedTable;
      try {
         expectedTable = getRefDataSet().getTable(expectedTableString);
      } catch (final DataSetException e) {
         logger.fatal(e);
         return false;
      } catch (final Exception e) {
         logger.fatal(e);
         return false;
      }
      final ITable actualTable;
      try {
         actualTable = getDataSet().getTable(actualTableString.toUpperCase());
      } catch (final DataSetException e) {
         logger.fatal(e);
         return false;
      } catch (final Exception e) {
         logger.fatal(e);
         return false;
      }
      try {
         final Map<String, StringBuffer> expectedValues = new TreeMap<>();
         final Map<String, StringBuffer> actualValues = new TreeMap<>();
         for (int i = 0; i < expectedTable.getRowCount(); i++) {
            final ITableMetaData tabMeta = expectedTable.getTableMetaData();
            final StringBuffer row = new StringBuffer("");
            for (final Column column : tabMeta.getColumns()) {
               final Object value = expectedTable.getValue(i, column.getColumnName());
               row.append(column.getColumnName() + value);
            }
            expectedValues.put(row.toString(), row);

         }
         for (int i = 0; i < actualTable.getRowCount(); i++) {
            final ITableMetaData tabMeta = actualTable.getTableMetaData();
            final StringBuffer row = new StringBuffer("");
            for (final Column column : tabMeta.getColumns()) {
               final Object value = actualTable.getValue(i, column.getColumnName());
               row.append(column.getColumnName() + value);
            }
            actualValues.put(row.toString(), row);
         }
         boolean notEqual = false;
         for (final Entry<String, StringBuffer> expected : expectedValues.entrySet()) {
            assert (expected != null);
            assert (expected.getKey() != null);
            assert (expected.getValue() != null);
            assert (actualValues != null);
            assert (actualValues.get(expected.getKey()) != null);
            //StringBuffer ev = expected.getValue();
            final String ek = expected.getKey();
            final StringBuffer actualV = actualValues.get(ek) != null ? actualValues.get(ek) : new StringBuffer();
            if (!expected.getValue()
                         .toString()
                         .equalsIgnoreCase(actualV.toString())) {
               notEqual = true;
               logger.info(expected.getValue()
                                   .toString());
               logger.info(actualV.toString());
               break;
            }
         }
         try {
            this.connection.close();
         } catch (final Exception e) {
         }
         try {
            this.connectionRef.close();
         } catch (final Exception e) {
         }
         return !notEqual;
      } catch (final DatabaseUnitException e) {
         logger.fatal(e);
         return false;
      }
   }

   public boolean areEqual(final File expectedTableXML, final String expectedTableString,
                           final String actualTableString) throws Exception {
      final FlatXmlDataFileLoader loader = new FlatXmlDataFileLoader();
      @SuppressWarnings("deprecation") final IDataSet ds = loader.getBuilder()
                                                                 .build(expectedTableXML.toURL());
      final ITable expectedTable = ds.getTable(expectedTableString);
      final ITable actualTable = getDataSet().getTable(actualTableString);
      try {
         Assertion.assertEquals(expectedTable, actualTable);
         return true;
      } catch (final DatabaseUnitException e) {
         logger.fatal(e);
         return false;
      }
   }

   public boolean areEqual(final File expectedTableXML, final String expectedTableString,
                           final String actualTableString, final String[] sortColumn) throws Exception {
      assert (sortColumn.length > 0) : "provide at least one column";
      final FlatXmlDataFileLoader loader = new FlatXmlDataFileLoader();
      @SuppressWarnings("deprecation") final IDataSet ds = loader.getBuilder()
                                                                 .build(expectedTableXML.toURL());
      final ITable expectedUnsorted = ds.getTable(expectedTableString);
      final ITable expectedTable = new SortedTable(expectedUnsorted, sortColumn);
      final ITable actualUnsorted = getDataSet().getTable(actualTableString);
      final ITable actualTable = new SortedTable(actualUnsorted, sortColumn);
      try {
         Assertion.assertEquals(expectedTable, actualTable);
         return true;
      } catch (final DatabaseUnitException e) {
         logger.fatal(e);
         return false;
      }
   }

}