package one.jodi.base.error;

import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ErrorWarningMessageJodiTrackerImplTest {
    private static final String CODE_PREFIX = "ERROR_MESSAGE_";
    private static final String DELIMITER_STRING = "String ";
    private static final String M_TYPE = "MESSAGE_TYPE.";

    int code = 1;
    ErrorWarningMessageJodiTracker fixture = ErrorWarningMessageJodiTrackerImpl
            .getInstance();
    ErrorWarningMessageJodiImplTest<?> testMethods;
    MessageCode m = null;

    List<String> fileLocations = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fileLocations.add(this.getClass().getName());
    }

    @After
    public void tearDown() throws Exception {
        fixture.flush();
    }

    @Test
    public void testCheckTrackerContainCodeBodyMatch() {
        code += 9;
        String messageCode = "This is a test message for adding a single message code.";
        fixture.addCode(code, messageCode, fileLocations);
        fixture.checkTracker(code, messageCode, fileLocations.get(0));
    }

    @Test
    public void testCheckTrackerDoesNotContainCode() {
        code += 8;
        String messageCode = "This is a test message for adding a single message code.";
        fixture.addCode(code, messageCode, fileLocations);
        int newCode = 2;
        fixture.checkTracker(newCode, messageCode, fileLocations.get(0));
        assertEquals(2, fixture.getTracker().size());
    }

    @Test
    public void testCheckTracker_Mismatch() {
        UsageException exception = null;
        code += 8;
        String messageCode = "This is a test message for adding a single message code.";
        fixture.addCode(code, messageCode, fileLocations);
        String messageCode2 = messageCode + " New message.";
        String expectedError = "[99999] Tracker contains " + code
                + " but the message body does not match.\n"
                + "Message body in tracker: "
                + fixture.getTracker().get(code).getBody() + "\n"
                + "Message body being added: " + messageCode2 + "\n"
                + "Locations in tracker: "
                + fixture.getTracker().get(code).getClassLocations() + "\n"
                + "Location being added: " + fileLocations.get(0) + "\n"
                + "Solution: Please assign a new available code.";
        try {
            fixture.checkTracker(code, messageCode2, fileLocations.get(0));
        } catch (UsageException u) {
            exception = u;
        }
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testAddCode() {
        int count = 4;
        for (int code = 1; code < count; code++) {
            String messageCode = "This is a test message for adding a single message code.";
            fixture.addCode(code, messageCode, fileLocations);
        }
        assertEquals(count - 1, fixture.getTracker().size());
    }

    /**
     * Testing addition of a message code that is already added to the tracker
     * and body of new does not match what is already being used in the tracker.
     */
    @Test
    public void testAddCode_MismatchedBody() {
        UsageException e = null;
        code += 6;
        String messageCode = "This is a test message for adding a single message code.";
        fixture.addCode(code, messageCode, fileLocations);
        String messageCode2 = messageCode + " again.";
        String expectedError = "[99999] Tracker contains " + code
                + " but the message body does not match.\n"
                + "Message body in tracker: "
                + fixture.getTracker().get(code).getBody() + "\n"
                + "Message body being added: " + messageCode2 + "\n"
                + "Locations in tracker: "
                + fixture.getTracker().get(code).getClassLocations() + "\n"
                + "Location being added: " + fileLocations + "\n"
                + "Solution: Please assign a new available code.";

        try {
            fixture.addCode(code, messageCode2, fileLocations);
        } catch (UsageException u) {
            e = u;
        }
        assertEquals(expectedError, e.getMessage());
    }

    @Test
    public void testAddCode_AddNewLocation() {
        code += 5;
        String messageCode = "First test message for updating a single message code.";
        fixture.addCode(code, messageCode, fileLocations);
        assertEquals(1, fixture.getTracker().size());

        // add a second message code of the same with new location
        String newFileLocation = this.getClass().getName() + "test";
        List<String> locations = new ArrayList<String>();
        locations.add(newFileLocation);
        fixture.addCode(code, messageCode, locations);
        assertEquals(1, fixture.getTracker().size());

        m = fixture.getTracker().get(code);
        assertEquals(2, m.getClassLocations().size());
    }

    @Test
    public void testUpdateCode_Failure() {
        fixture.flush();
        code += 4;
        String messageCode = "First test message for updating a single message code.";
        fixture.addCode(code, messageCode, fileLocations);
        assertEquals(1, fixture.getTracker().size());

        // add a second message code of the same with new location
        messageCode = "Second test message for updating a single message code.";
        String newFileLocation = this.getClass().getName() + "test";
        fixture.updateCode(code, messageCode, newFileLocation);
        assertEquals(1, fixture.getTracker().size());

        m = fixture.getTracker().get(code);
        assertEquals(1, m.getClassLocations().size());
    }

    @Test
    public <T> void testValidateFile_Success() {
        try {
            fixture.validateFile(ErrorWarningMessageJodiTrackerImpl.class);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public <T> void testValidateFile_Failure() throws IOException {
        int code = 99999;

        UsageException exception = null;
        String solution = "Verify that the id and body match in each instance of usage. E.g. %d, %s";
        List<String> location = new ArrayList<String>();
        location.add("one.jodi.base.error.Mock_ClassWithError");

        String expectedError = "[99998] 1 matches found while 8 occurrences found in file for message\n"
                + " code id "
                + code
                + " was not used correctly in class location: "
                + location
                + " \n"
                + "Solution: "
                + String.format(solution, code, CODE_PREFIX + code);
        try {
            validateCode(Mock_ClassWithError.class);
        } catch (UsageException u) {
            exception = u;
        }
        assertEquals(expectedError, exception.getMessage());
    }

    private void validateCode(Class<Mock_ClassWithError> class1) {
        try {
            fixture.validateFile(class1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPrintMessageCodes_EmptyTracker() {
        Exception exception = null;
        String expectedError = "[99997] Tracker is currently empty.";
        try {
            fixture.printMessageCodes();
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testPrintMessageCodes_testData() {
        testAddCode();
        fixture.printMessageCodes();
    }

    @Test
    public void testPrintMessageCodes_testData_failure() {
        UsageException exception = null;
        code = 1;
        testAddCode();
        String messageCode = "Different message body";
        List<String> locations = new ArrayList<String>();
        locations.add(this.getClass().getName());
        String expectedError = "[99999] Tracker contains " + code
                + " but the message body does not match.\n"
                + "Message body in tracker: "
                + fixture.getTracker().get(1).getBody() + "\n"
                + "Message body being added: " + messageCode + "\n"
                + "Locations in tracker: "
                + fixture.getTracker().get(1).getClassLocations() + "\n"
                + "Location being added: " + locations + "\n"
                + "Solution: Please assign a new available code.";

        try {
            fixture.addCode(code, messageCode, fileLocations);
            fixture.printMessageCodes();
        } catch (UsageException u) {
            exception = u;
        }
        assertEquals(expectedError, exception.getMessage());
    }

    //@Test
    public void testPrintMessageCodes_messagesInUse() throws IOException {
        collectMessageCodesInUse();
        fixture.printMessageCodes();
    }

    //@Test
    public void testPrintMessageCodes_messagesInUse_lessLocations() throws IOException {
        collectMessageCodesInUse();
        fixture.printMessageCodes_CodeAndBody();
    }

    private void collectMessageCodesInUse() throws IOException {
        List<String> files = filesUsingFramework();
        for (String file : files) {
            getMessageCodes(file, DELIMITER_STRING + CODE_PREFIX);
        }
    }

    private void getMessageCodes(String file, String searchString) {
        List<MessageCode> messageCodes = new ArrayList<MessageCode>();
        StringBuffer stringBuffer = new StringBuffer();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null; ) {
                stringBuffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String entireFile = stringBuffer.toString();
        List<String> linesInFile = new ArrayList<String>();
        String[] fileContents = entireFile.split(";");
        for (String fileContent : fileContents) {
            linesInFile.add(fileContent);
        }
        for (String lineInFile : linesInFile) {
            lineInFile = lineInFile.concat(";");
            if (lineInFile.contains(searchString)) {
                // strip out the message code
                try {
                    String[] pieces = lineInFile.split(DELIMITER_STRING);
                    String rawMessage = pieces[1];
                    String[] rawMessagePieces = rawMessage.split("=");
                    int messageCode = Integer.parseInt(rawMessagePieces[0].trim()
                            .split("_")[2]);
                    List<String> locations = new ArrayList<String>();
                    locations.add(file);
                    fixture.addCode(messageCode, rawMessagePieces[1], locations);
                    messageCodes.add(new MessageCode(messageCode, rawMessagePieces[1], locations));
                } catch (NumberFormatException e) {
                    // ignore message
                }
            }
        }
        //System.out.println(messageCodes.size());
        for (MessageCode messageCode : messageCodes) {
            for (int i = 0; i < fileContents.length; i++) {
                if (fileContents[i].contains(messageCode.getId() + ", ")) {
                    // one line
                    if (fileContents[i].contains("addErrorMessage")) {
                        fixture.updateMessageCodeInTracker(messageCode.getId(), MESSAGE_TYPE.ERRORS);
                    } else if (fileContents[i].contains("addWarningMessage")) {
                        fixture.updateMessageCodeInTracker(messageCode.getId(), MESSAGE_TYPE.WARNINGS);
                    }

                    // two line-- formatMessage() followed by addMessage()
                    String nextLine = fileContents[i + 1];
                    if (nextLine.contains(M_TYPE)) {
                        String lineA = nextLine.split(M_TYPE)[1].replace(")", "");
                        if (lineA.contains("ERRORS")) {
                            fixture.updateMessageCodeInTracker(messageCode.getId(), MESSAGE_TYPE.ERRORS);
                        } else if (lineA.contains("WARNINGS")) {
                            fixture.updateMessageCodeInTracker(messageCode.getId(), MESSAGE_TYPE.WARNINGS);
                        }
                    }
                }
            }
        }
    }

    private List<String> filesUsingFramework() throws IOException {
        // jodi_core
        String filePath = new File("").getAbsolutePath();
        File directoryToTest = new File(filePath);
        String initialSearchString = "String ERROR_MESSAGE_";
        List<String> files = getFiles(directoryToTest, initialSearchString);

        // jodi_rpd
        String filePath2 = new File(filePath).getParent() + File.separator
                + "jodi_rpd";
        directoryToTest = new File(filePath2);
        files.addAll(getFiles(directoryToTest, initialSearchString));

        // jodi_odi12
        String filePath3 = new File(filePath).getParent() + File.separator
                + "jodi_odi12";
        directoryToTest = new File(filePath3);
        files.addAll(getFiles(directoryToTest, initialSearchString));
        return files;
    }

    public List<String> getFiles(final File directoryToTest,
                                 final String initialSearchString) throws IOException {
        List<String> classesToTest =
                Files.walk(directoryToTest.toPath())
                        .filter(p -> p.getFileName().toString().endsWith("java"))
                        .filter(p -> !new File(p.toAbsolutePath().toString())
                                .isDirectory() &&
                                !p.getFileName().toString().contains("Test") &&
                                errorWarningFrameworkUtilizedInThisFile(p,
                                        initialSearchString))
                        .map(p -> p.toAbsolutePath().toString())
                        .collect(Collectors.toList());

        return classesToTest;
    }

    private boolean errorWarningFrameworkUtilizedInThisFile(
            final Path path,
            final String searchString) {
        boolean used = false;
        try {
            used = Files.lines(path).anyMatch(s -> s.contains(searchString));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return used;
    }

    @Test
    public void testGenerateEmptyCSVFile() throws IOException {
        Exception exception = null;
        String expectedError = "[99997] Tracker is currently empty.";
        try {
            fixture.generateCSVFile();
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(expectedError, exception.getMessage());
    }

    //@Test
    public void testGenerateCSVFile() throws IOException {
        collectMessageCodesInUse();
        fixture.generateCSVFile();
    }

    @Test
    public void testFlush() {
        int count = 4;
        for (int code = 1; code < count; code++) {
            String messageCode = "This is a test message for adding a single message code.";
            fixture.addCode(code, messageCode, fileLocations);
        }
        assertEquals(count - 1, fixture.getTracker().size());

        fixture.flush();
        assertEquals(0, fixture.getTracker().size());

    }
}
