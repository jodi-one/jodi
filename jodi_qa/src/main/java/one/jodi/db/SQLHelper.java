package one.jodi.db;

import oracle.odi.setup.TechnologyName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class SQLHelper {
    private final Logger logger = LogManager.getLogger(SQLHelper.class);

    public boolean executedSQLSuccesfully(String sysdbaUser,
                                          String sysdbaPassword, String jdbcUrl, String statement) {
        Connection conn = null;
        Statement stmt = null;
        boolean result;
        try {
            logger.debug("jdbcUrl-->" + jdbcUrl);
            logger.debug("sysdbaUser-->" + sysdbaUser);
            conn = DriverManager.getConnection(jdbcUrl, sysdbaUser,
                    sysdbaPassword);
            stmt = conn.createStatement();
            logger.debug("statement-->" + statement);
            stmt.execute(statement);
            result = true;
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "SQL did not execute succesful.";
            logger.fatal(message, ex);
            result = false;
            throw new RuntimeException(message, ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
        return result;
    }

    public boolean userExists(TechnologyName technologyName, String sysdbaUser,
                              String sysdbaPassword, String jdbcUrl, String dbUser) {
        boolean result = false;
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet restult = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, sysdbaUser, sysdbaPassword);
            logger.info("url: " + jdbcUrl + " user:" + sysdbaUser);
            int number_or_users = 0;
            String query = "";
            if (technologyName.equals(TechnologyName.ORACLE)) {
                query = "select count(*) cnt from all_users where username = ?";
            } else {
                query = "select count(*) cnt from information_schema.system_users where user_name = ?";
            }
            stmt = conn
                    .prepareStatement(query);
            stmt.setString(1, dbUser);
            restult = stmt.executeQuery();
            while (restult.next()) {
                number_or_users = restult.getInt("cnt");
            }
            result = number_or_users == 1;
        } catch (SQLException e) {
            result = false;
        } finally {
            if (restult != null) {
                try {
                    restult.close();
                } catch (SQLException e) {
                    logger.error(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
        return result;
    }

    public boolean deleteUser(TechnologyName technologyName, String sysdbaUser,
                              String sysdbaPassword, String jdbcUrl, String dbUser) {
        boolean result = false;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, sysdbaUser,
                    sysdbaPassword);
            stmt = conn.createStatement();
            logger.info("drop user " + dbUser + " cascade");
            stmt.execute("drop user " + dbUser + " cascade");
            result = true;
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.fatal(e);
            }
            return result;
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Error occurred deleting user, is it currently connected?";
            result = false;
            if (technologyName.equals(TechnologyName.ORACLE)) {
                if (ex.getMessage().contains("ORA-01918")) {
                    logger.warn("User " + dbUser + " not deleted."
                            + message);
                } else {
                    logger.warn("Url: " + jdbcUrl + " not deleted dbuser: " + dbUser);
                    logger.warn(message);
                }
            } else {
                logger.warn("User " + dbUser + " not deleted. "
                        + ex.getMessage());
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
        return result;
    }

    public boolean masterRepostitoryUserIsCreated(
            TechnologyName technologyName, String sysdbaUser,
            String sysdbaPassword, String jdbcUrl,
            String masterRepositoryJdbcUser, String masterRepositoryJdbcPassword) {
        boolean result = false;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, sysdbaUser,
                    sysdbaPassword);
            stmt = conn.createStatement();
            if (technologyName.equals(TechnologyName.ORACLE)) {
                stmt.execute("create user " + masterRepositoryJdbcUser
                        + " identified by " + masterRepositoryJdbcPassword);
                stmt.execute("grant connect,resource to "
                        + masterRepositoryJdbcUser);
                stmt.execute("grant unlimited tablespace to "
                        + masterRepositoryJdbcUser);
            } else if (technologyName.toString().equals("HYPERSONIC_SQL")) {
                stmt.execute("create user " + masterRepositoryJdbcUser
                        + " password " + masterRepositoryJdbcPassword);
                stmt.execute("create SCHEMA " + masterRepositoryJdbcUser
                        + " AUTHORIZATION " + masterRepositoryJdbcUser);
                stmt.execute("ALTER USER " + masterRepositoryJdbcUser
                        + " SET INITIAL SCHEMA " + masterRepositoryJdbcUser);
                stmt.execute("grant dba to " + masterRepositoryJdbcUser);
                // / GRANT ALL ON public.t1 T
            }
            logger.info("User " + masterRepositoryJdbcUser + " created");
            result = true;
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.fatal(e);
            }
        } catch (Exception ex) {
            if (ex.getMessage().contains("ORA-01920")) {
                logger.warn("User " + masterRepositoryJdbcUser
                        + " not created: user exists.");
                result = true;
            } else if (ex.getMessage().contains(
                    "invalid authorization specification")) {
                logger.warn("User " + masterRepositoryJdbcUser
                        + " not created: user exists.");
                result = true;
            } else {
                logger.fatal(ex.getMessage());
                result = false;
            }
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.fatal(e);
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
        return result;
    }

    public boolean workRepostitoryUserIsCreated(TechnologyName technologyName,
                                                String sysdbaUser,

                                                String sysdbaPassword, String jdbcUrl,
                                                String workRepositoryJdbcUsername, String workRepositoryJdbcPassword) {
        boolean result = false;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, sysdbaUser,
                    sysdbaPassword);
            stmt = conn.createStatement();
            if (technologyName.equals(TechnologyName.ORACLE)) {
                stmt.execute("create user " + workRepositoryJdbcUsername
                        + " identified by " + workRepositoryJdbcPassword);
                stmt.execute("grant connect,resource to "
                        + workRepositoryJdbcUsername);
                stmt.execute("grant unlimited tablespace to "
                        + workRepositoryJdbcUsername);
                logger.info("User " + workRepositoryJdbcUsername + " created;");
                result = true;
            } else if (technologyName.toString().equals("HYPERSONIC_SQL")) {
                stmt.execute("create user " + workRepositoryJdbcUsername
                        + " password " + workRepositoryJdbcPassword);
                stmt.execute("create SCHEMA " + workRepositoryJdbcUsername
                        + " AUTHORIZATION " + workRepositoryJdbcUsername);
                stmt.execute("ALTER USER " + workRepositoryJdbcUsername
                        + " SET INITIAL SCHEMA " + workRepositoryJdbcUsername);
                stmt.execute("grant dba to " + workRepositoryJdbcUsername);
                logger.info("User " + workRepositoryJdbcUsername + " created;");
                result = true;
            }
        } catch (Exception ex) {
            if (ex.getMessage().contains("ORA-01920")) {
                logger.fatal("User " + workRepositoryJdbcUsername
                        + " not created: user exists.");
                result = true;
            } else {
                logger.fatal(ex.getMessage());
                result = false;
            }
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.fatal(e);
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.fatal(e);
                }
            }
        }
        return result;
    }
}