package one.jodi.db;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import oracle.odi.setup.TechnologyName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class SQLHelper {
   private static final Logger LOG = LogManager.getLogger(SQLHelper.class);

   public boolean executedSQLSuccesfully(final String sysdbaUser, final String sysdbaPassword, final String jdbcUrl,
                                         final String statement) {
      LOG.info("jdbcUrl-->" + jdbcUrl);
      LOG.info("sysdbaUser-->" + sysdbaUser);
      final Properties info = new Properties();
      info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, sysdbaUser);
      info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, sysdbaPassword);
      info.put(OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20");
      info.put(OracleConnection.CONNECTION_PROPERTY_TNS_ADMIN,
               "/mnt/filesystem-jodione/oracle/git/opc/src/main/resources/wallet_jodiAtp/");
//               "/home/opc/projects/opc/src/main/resources/wallet_jodiAtp/");
      OracleDataSource ods;
      try {
         ods = new OracleDataSource();
         ods.setURL(jdbcUrl);
         ods.setConnectionProperties(info);
      } catch (final SQLException e) {
         throw new RuntimeException(e);
      }
      try (OracleConnection conn = (OracleConnection) ods.getConnection(); Statement stmt = conn.createStatement()) {
         LOG.info("statement-->" + statement);
         stmt.execute(statement);
         return true;
      } catch (final Exception ex) {
         final String message = ex.getMessage() != null ? ex.getMessage() : "SQL did not execute successful.";
         LOG.fatal(message, ex);
         throw new RuntimeException(message, ex);
      }
   }

   public boolean userExists(final TechnologyName technologyName, final String sysdbaUser, final String sysdbaPassword,
                             final String jdbcUrl, final String dbUser) {
      LOG.info("url: " + jdbcUrl + " user:" + sysdbaUser);
      int number_or_users = 0;
      String query;
      if (technologyName.equals(TechnologyName.ORACLE)) {
         query = "select count(*) cnt from all_users where username = ?";
      } else {
         query = "select count(*) cnt from information_schema.system_users where user_name = ?";
      }
      try (Connection conn = DriverManager.getConnection(jdbcUrl, sysdbaUser, sysdbaPassword);
           PreparedStatement stmt = conn.prepareStatement(query)) {
         stmt.setString(1, dbUser);
         try (ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
               number_or_users = resultSet.getInt("cnt");
            }
            return number_or_users == 1;
         }
      } catch (SQLException e) {
         LOG.fatal(e);
         return false;
      }
   }

   public boolean deleteUser(final TechnologyName technologyName, final String sysdbaUser, final String sysdbaPassword,
                             final String jdbcUrl, final String dbUser) {
      try (Connection conn = DriverManager.getConnection(jdbcUrl, sysdbaUser, sysdbaPassword);
           Statement stmt = conn.createStatement()) {
         LOG.info("drop user " + dbUser + " cascade");
         stmt.execute("drop user " + dbUser + " cascade");
         return true;
      } catch (final Exception ex) {
         final String message =
                 ex.getMessage() != null ? ex.getMessage() : "Error occurred deleting user, is it currently connected?";
         if (technologyName.equals(TechnologyName.ORACLE)) {
            if (message.contains("ORA-01918")) {
               LOG.warn("User " + dbUser + " not deleted." + message);
            } else {
               LOG.warn("Url: " + jdbcUrl + " not deleted dbuser: " + dbUser);
               LOG.warn(message);
            }
         } else {
            LOG.warn("User " + dbUser + " not deleted. " + ex.getMessage());
         }
      }
      return false;
   }

   public boolean masterRepostitoryUserIsCreated(final TechnologyName technologyName, final String sysdbaUser,
                                                 final String sysdbaPassword, final String jdbcUrl,
                                                 final String masterRepositoryJdbcUser,
                                                 final String masterRepositoryJdbcPassword) {
      try (Connection conn = DriverManager.getConnection(jdbcUrl, sysdbaUser, sysdbaPassword);
           Statement stmt = conn.createStatement()) {
         if (technologyName.equals(TechnologyName.ORACLE)) {
            stmt.execute("create user " + masterRepositoryJdbcUser + " identified by " + masterRepositoryJdbcPassword);
            stmt.execute("grant connect,resource to " + masterRepositoryJdbcUser);
            stmt.execute("grant unlimited tablespace to " + masterRepositoryJdbcUser);
         } else if (technologyName.toString()
                                  .equals("HYPERSONIC_SQL")) {
            stmt.execute("create user " + masterRepositoryJdbcUser + " password " + masterRepositoryJdbcPassword);
            stmt.execute("create SCHEMA " + masterRepositoryJdbcUser + " AUTHORIZATION " + masterRepositoryJdbcUser);
            stmt.execute("ALTER USER " + masterRepositoryJdbcUser + " SET INITIAL SCHEMA " + masterRepositoryJdbcUser);
            stmt.execute("grant dba to " + masterRepositoryJdbcUser);
            // / GRANT ALL ON public.t1 T
         }
         LOG.info("User " + masterRepositoryJdbcUser + " created");
         return true;
      } catch (final Exception ex) {
         boolean result;
         if (ex.getMessage()
               .contains("ORA-01920")) {
            LOG.warn("User " + masterRepositoryJdbcUser + " not created: user exists.");
            result = true;
         } else if (ex.getMessage()
                      .contains("invalid authorization specification")) {
            LOG.warn("User " + masterRepositoryJdbcUser + " not created: user exists.");
            result = true;
         } else {
            LOG.fatal(ex.getMessage());
            result = false;
         }
         return result;
      }
   }

   public boolean workRepostitoryUserIsCreated(final TechnologyName technologyName, final String sysdbaUser,

                                               final String sysdbaPassword, final String jdbcUrl,
                                               final String workRepositoryJdbcUsername,
                                               final String workRepositoryJdbcPassword) {
      boolean result = false;
      try (Connection conn = DriverManager.getConnection(jdbcUrl, sysdbaUser, sysdbaPassword);
           Statement stmt = conn.createStatement()) {
         if (technologyName.equals(TechnologyName.ORACLE)) {
            stmt.execute("create user " + workRepositoryJdbcUsername + " identified by " + workRepositoryJdbcPassword);
            stmt.execute("grant connect,resource to " + workRepositoryJdbcUsername);
            stmt.execute("grant unlimited tablespace to " + workRepositoryJdbcUsername);
            LOG.info("User " + workRepositoryJdbcUsername + " created;");
            result = true;
         } else if (technologyName.toString()
                                  .equals("HYPERSONIC_SQL")) {
            stmt.execute("create user " + workRepositoryJdbcUsername + " password " + workRepositoryJdbcPassword);
            stmt.execute(
                    "create SCHEMA " + workRepositoryJdbcUsername + " AUTHORIZATION " + workRepositoryJdbcUsername);
            stmt.execute(
                    "ALTER USER " + workRepositoryJdbcUsername + " SET INITIAL SCHEMA " + workRepositoryJdbcUsername);
            stmt.execute("grant dba to " + workRepositoryJdbcUsername);
            LOG.info("User " + workRepositoryJdbcUsername + " created;");
            result = true;
         }
      } catch (final Exception ex) {
         if (ex.getMessage()
               .contains("ORA-01920")) {
            LOG.fatal("User " + workRepositoryJdbcUsername + " not created: user exists.");
            result = true;
         } else {
            LOG.fatal(ex.getMessage());
            result = false;
         }
      }
      return result;
   }
}
