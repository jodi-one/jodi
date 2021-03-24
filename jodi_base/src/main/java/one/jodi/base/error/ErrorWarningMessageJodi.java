package one.jodi.base.error;

import java.util.List;
import java.util.SortedMap;

/**
 * Messaging framework
 */
public interface ErrorWarningMessageJodi {

    String EOL = System.getProperty("line.separator");
    String messageHeader = "Messages are displayed in the following formats:" + EOL
            + "1) [message code] - body of the message." + EOL
            + "2) packageSequence - [message code] - body of the message." + EOL;

    void setMetaDataDirectory(String metaDataDirectory);

    SortedMap<Integer, List<String>> getErrorMessages();

    SortedMap<Integer, List<String>> getWarningMessages();

    String formatMessage(int messageCode, String messageCodeString,
                         Class<?> thisClass, Object... args);

    // default message type is both errors and warnings, otherwise use type to
    // suppress or reset
    String printMessages(MESSAGE_TYPE... messageTypeToSuppress);

    void addMessage(int packageSequence, String message,
                    MESSAGE_TYPE messageTypeToAdd);

    void addMessage(String message, MESSAGE_TYPE messageType);

    void clear();

    int getLastSequenceNumber();

    void setLastSequenceNumber(int lastSequenceNumber);

    int assignSequenceNumber();

    boolean existsErrorMessageWithCode(int errorCode);

    boolean existsWarningMessageWithCode(int warningCode);

    enum MESSAGE_TYPE {
        ERRORS, WARNINGS, UNUSED
    }
    //void logMessages();
}
