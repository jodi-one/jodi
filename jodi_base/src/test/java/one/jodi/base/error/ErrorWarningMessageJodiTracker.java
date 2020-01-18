package one.jodi.base.error;

import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

/**
 * Messaging framework tracker interface
 */
public interface ErrorWarningMessageJodiTracker {
    public SortedMap<Integer, MessageCode> getTracker();

    public boolean checkTracker(Integer code, String messageCode,
                                String fileLocation);

    public void addCode(Integer code, String messageCode,
                        List<String> fileLocations);

    public void updateCode(Integer code, String messageCode,
                           String fileLocations);

    public void validateFile(Class<?> thisClass)
            throws IllegalArgumentException, IllegalAccessException,
            ClassNotFoundException, NoSuchFieldException, SecurityException,
            IOException;

    public void printMessageCodes();

    public void printMessageCodes_CodeAndBody(); // only code and body, no locations

    public void generateCSVFile() throws IOException;

    public void flush();

    public void updateMessageCodeInTracker(int messageIdFromErrorMessage,
                                           MESSAGE_TYPE messageType);

}
