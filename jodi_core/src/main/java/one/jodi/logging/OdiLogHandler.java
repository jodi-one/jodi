package one.jodi.logging;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class OdiLogHandler extends Handler {

    private final static Logger logger = LogManager.getLogger(OdiLogHandler.class);
    private final static String ERROR_MESSAGE_00240 = "Fatal error: %s";
    private final ErrorWarningMessageJodi errorWarningMessages;
    private StringBuffer odiLog = new StringBuffer("");
    private java.util.logging.Formatter formatter = new java.util.logging.SimpleFormatter();

    public OdiLogHandler(final ErrorWarningMessageJodi errorWarningMessages) {
        setFormatter(formatter);
        this.errorWarningMessages = errorWarningMessages;
    }

    public void publish(LogRecord record) {
        if (record.getMessage().contains("critical")) {
            String msg = errorWarningMessages.formatMessage(240,
                    ERROR_MESSAGE_00240, this.getClass(), record);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            ErrorReport.addErrorLine(0, formatter.formatMessage(record));
        } else {
            logger.info(formatter.formatMessage(record));
        }
        odiLog.append(formatter.formatMessage(record));
    }

    public void flush() {
        odiLog = new StringBuffer("");
    }

    public void close() throws SecurityException {
        odiLog = new StringBuffer("");
    }

    public boolean containsLogMessage(String needle) {
        if (odiLog.toString().indexOf(needle) > 0) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return odiLog.toString();
    }

}
