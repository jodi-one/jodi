package one.jodi.base.service.odb;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Establish a connection to the Oracle database.
 */
public class DbConnectionUtil {

    private final static Logger LOGGER = LogManager.getLogger(DbConnectionUtil.class);

    private static final String ERROR_MESSAGE_80800 =
            "Database connection failed for '%s'. Verify the connection string. %s";

    private static final String ERROR_MESSAGE_80810 =
            "Failed to get database connection for '%s'. Verify the database connection.";

    private static final String ERROR_MESSAGE_80820 =
            "Failed to properly close database connection for '%s'. %s";

    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    DbConnectionUtil(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    public Connection getDatabaseConnection(final String jdbcUrl,
                                            final String schemaName,
                                            final String password) {
        LOGGER.debug("Getting db connection for jdbcUrl: " + jdbcUrl + " and schema: " + schemaName);
        Connection connection;
        try {
            connection = DriverManager.getConnection(jdbcUrl, schemaName, password);
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(80800,
                    ERROR_MESSAGE_80800, this.getClass(), jdbcUrl, e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            LOGGER.fatal(msg, e);
            throw new UnRecoverableException(msg, e);
        }
        try {
            if (connection != null && connection.isClosed()) {
                connection = null;
                String msg = errorWarningMessages.formatMessage(80810,
                        ERROR_MESSAGE_80810, this.getClass(), jdbcUrl);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                LOGGER.fatal(msg);
                throw new UnRecoverableException(msg);
            }
        } catch (SQLException e) {
            String msg = errorWarningMessages.formatMessage(80820,
                    ERROR_MESSAGE_80820, this.getClass(), jdbcUrl, e.getMessage());
            LOGGER.error(msg, e);
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ec) {
                String msg2 = errorWarningMessages.formatMessage(80820,
                        ERROR_MESSAGE_80820, this.getClass(), jdbcUrl,
                        ec.getMessage());
                LOGGER.error(msg2);
            } finally {
                connection = null;
            }
        }
        // errors were previously logged
        if (connection == null) {
            String msg = errorWarningMessages.formatMessage(80810,
                    ERROR_MESSAGE_80810, this.getClass(), jdbcUrl);
            LOGGER.fatal(msg);
            throw new UnRecoverableException(msg);
        }
        return connection;
    }

}
