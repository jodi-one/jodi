package one.jodi.base.error;

import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ErrorWarningMessageJodiImplTest<Field> {

    private final static Logger logger =
            LogManager.getLogger(ErrorWarningMessageJodiImplTest.class);
    private final static String EOL = System.getProperty("line.separator");
    ErrorWarningMessageJodi fixture = ErrorWarningMessageJodiHelper
            .getTestErrorWarningMessages();
    ErrorWarningMessageJodiTracker tracker = ErrorWarningMessageJodiTrackerImpl.getInstance();
    Class<?> testClass = ErrorWarningMessageJodiImpl.class;
    int messageCode = 0;
    int counter = 0;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
        fixture.clear();
        tracker.flush();
    }

    @Test
    public void testErrorWarningMessageJodiImpl() {
        assertEquals(0, fixture.getErrorMessages().size());
        assertEquals(0, fixture.getWarningMessages().size());
    }

    @Test
    public void testCopyMessages() {
        fixture.clear();
        String expected = ErrorWarningMessageJodi.messageHeader + EOL
                + "----  ERRORS  ----------------------------" + EOL
                + "[00001] This is a test message." + EOL
                + "----  WARNINGS  ----------------------------" + EOL
                + "[00201] This is a test message.(warning)" + EOL
                + "-------------------------------------------------------" + EOL + EOL;

        // add messages
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        List<String> value = new ArrayList<String>();
        String testMessage = "This is a test message.";
        value.add("[00001] " + testMessage);
        expectedMessages.put(key, value);
        messageCode = 1;
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);

        SortedMap<Integer, List<String>> expectedMessages1 = new TreeMap<Integer, List<String>>();
        value = new ArrayList<String>();
        testMessage = "This is a test message.";
        value.add("[00201] " + testMessage);
        expectedMessages1.put(key, value);
        messageCode = 201;
        formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.WARNINGS);
        fixture.printMessages();

        String message = "";
        try {
            message = fixture.printMessages(MESSAGE_TYPE.UNUSED);
            logger.info("1:'" + message + "'");
            logger.info("2:'" + expected + "'");

        } catch (Exception e) {
            fail();
        }

        for (int i = 0; i < expected.length(); i++) {
            if (expected.charAt(i) != (message.charAt(i))) {
                System.err.println(i + ": " + expected.charAt(i) + " "
                        + message.charAt(i));
            }
        }

        // compare the copy holder with fixture
        assertEquals(expected, message);

    }

    @Test
    public void testGetErrorMessages() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        List<String> value = new ArrayList<String>();
        String testMessage = "This is a test message.";
        value.add("[00001] " + testMessage);
        expectedMessages.put(key, value);
        messageCode = 1;
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);
        assertEquals(expectedMessages, fixture.getErrorMessages());
        assertEquals(1, fixture.getErrorMessages().size());

    }

    @Test
    public void testGetWarningMessages() {
        assertEquals(0, fixture.getWarningMessages().size());

        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        List<String> value = new ArrayList<String>();
        String testMessage = "This is a test message.";
        value.add("[00002] " + testMessage);
        messageCode = 2;
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        expectedMessages.put(key, value);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.WARNINGS);
        fixture.printMessages(MESSAGE_TYPE.ERRORS);
        assertEquals(expectedMessages, fixture.getWarningMessages());

        assertEquals(1, fixture.getWarningMessages().size());
    }

    @Test
    public void testPrintAllMessages() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        List<String> value = new ArrayList<String>();
        messageCode += 10;
        String testMessage = "Print all messages.";
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        fixture.addMessage(1, formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(1, value);

        value = new ArrayList<String>();
        testMessage = "Print all messages.";
        messageCode += 10;
        formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        fixture.addMessage(2, formattedMessage, MESSAGE_TYPE.WARNINGS);
        value.add(formattedMessage);
        expectedMessages.put(2, value);
        fixture.printMessages();

        SortedMap<Integer, List<String>> actualResults = new TreeMap<Integer, List<String>>();
        actualResults.putAll(fixture.getErrorMessages());
        actualResults.putAll(fixture.getWarningMessages());
        assertEquals(expectedMessages, actualResults);
    }

    @Test
    public void testPrintErrorMessages() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        messageCode += 11;
        List<String> value = new ArrayList<String>();
        String testMessage = "Print error message.";
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        fixture.printMessages(MESSAGE_TYPE.WARNINGS);
        assertEquals(expectedMessages, fixture.getErrorMessages());
    }

    @Test
    public void testPrintWarningMessages() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        List<String> value = new ArrayList<String>();
        messageCode += 12;
        String testMessage = "Print warning message.";
        String formattedMessage = fixture.formatMessage(messageCode,
                testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.WARNINGS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        fixture.printMessages(MESSAGE_TYPE.ERRORS);
        assertEquals(expectedMessages, fixture.getWarningMessages());

    }

    @Test
    public void testAddMessageWithoutPackageSequence() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        List<String> value = new ArrayList<String>();
        messageCode += 13;
        String testMessage = "Adding a message message without a package sequence.";
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, this.getClass(), key);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        assertEquals(expectedMessages, fixture.getErrorMessages());
    }

    @Test
    public void testAddMessageWithPackageSequence() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = 25;
        List<String> value = new ArrayList<String>();
        messageCode += 14;
        String testMessage = "Adding a message with package sequence %s.";
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass, key);
        fixture.addMessage(25, formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        assertEquals(expectedMessages, fixture.getErrorMessages());
    }

    @Test
    public void testParameterCount() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = 25;
        List<String> value = new ArrayList<String>();
        messageCode += 14;
        String testMessage = "Adding a message with package sequence %s.";
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass, key, 0);
        logger.info(formattedMessage);
        assertEquals(formattedMessage.contains("Message code string contains 1 parameters while 2 parameters are entered"), true);
        //assertEquals("[99996] Error in construction error message from string 'Adding a message with package sequence s.'. Message code string contains 1 parameters while 2 parameters are entered.")
        //assertEquals("[99996] Message code string contains 1 parameters while 2 parameters are entered.", formattedMessage);
        fixture.addMessage(25, formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        assertNotEquals(expectedMessages, fixture.getErrorMessages());
        fixture.clear();
    }

    @Test
    public void testAddMessageUsingDecimalInput() {
        fixture.clear();
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = 25;
        List<String> value = new ArrayList<String>();
        messageCode = 9;
        String testMessage = "Adding a message with package sequence %d.";
        logger.info(testMessage);
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass, key);
        fixture.addMessage(25, formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        assertEquals(expectedMessages, fixture.getErrorMessages());
    }

    @Test
    public void testFormatMessageLessThanTenThousand() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        List<String> value = new ArrayList<String>();
        String testMessage = "This is a test message for code less than 10000.";
        messageCode += 15;
        String formattedMessage = fixture.formatMessage(messageCode, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        assertEquals(expectedMessages, fixture.getErrorMessages());
    }

    @Test
    public void testFormatMessageTenThousandOrGreater() {
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = -1;
        List<String> value = new ArrayList<String>();
        String testMessage = "This is a test message for code greatter than 10000.";
        String formattedMessage = fixture.formatMessage(100010, testMessage,
                this.getClass());
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);
        value.add(formattedMessage);
        expectedMessages.put(key, value);
        assertEquals(expectedMessages, fixture.getErrorMessages());
    }

    @Test
    public void testFlush() {
        int expectedMessageCode = 10001;
        SortedMap<Integer, List<String>> expectedMessages = new TreeMap<Integer, List<String>>();
        int key = expectedMessageCode;
        List<String> value = new ArrayList<String>();
        String testMessage = "[" + expectedMessageCode
                + "] This is a test message.";
        value.add(testMessage);
        expectedMessages.put(key, value);
        fixture.addMessage(expectedMessageCode, testMessage,
                MESSAGE_TYPE.ERRORS);
        assertEquals(0, fixture.getLastSequenceNumber());
        assertEquals(1, fixture.getErrorMessages().size());
        assertEquals(0, fixture.getWarningMessages().size());
        fixture.clear();
        assertEquals(0, fixture.getLastSequenceNumber());
        assertEquals(0, fixture.getErrorMessages().size());
        assertEquals(0, fixture.getWarningMessages().size());
    }

    @Test
    public void testGetLastSequenceNumber() {
        int setValue = 3;
        fixture.setLastSequenceNumber(setValue);
        assertEquals(setValue, fixture.getLastSequenceNumber());
    }

    @Test
    public void testSetLastSequenceNumber() {
        int setValue = 9;
        fixture.setLastSequenceNumber(setValue);
        assertEquals(setValue, fixture.getLastSequenceNumber());
    }

    @Test
    public void testAssignSequenceNumber() {
        int setValue = 12;
        fixture.setLastSequenceNumber(setValue);
        int expectedValue = fixture.getLastSequenceNumber() + 10;
        assertEquals(expectedValue, fixture.assignSequenceNumber());
    }

    //@Test
    public void testMessageCodeUsageInJodiCore() throws IOException {
        String filePath = new File("").getAbsolutePath();
        File directoryToTest = new File(filePath);
        String initialSearchString = "String ERROR_MESSAGE_";
        List<String> files = getFiles(directoryToTest, initialSearchString);

        // verify usage of message codes within each file
        List<String> messageCodes = new ArrayList<String>();
        List<Integer> mismatches = new ArrayList<Integer>();
        for (String file : files) {
            messageCodes = getMessageCodes(file, initialSearchString);
            mismatches.addAll(lookupUsage(file, messageCodes));
        }
        for (Integer errorNumber : mismatches) {
            System.err.println("Incorrectly formed error message #" + errorNumber);
        }
        assertEquals(0, mismatches.size());
    }

    private int countMessageUsage(String file, String messageCode)
            throws FileNotFoundException {
        int countMatches = 0;
        try (
                Scanner s = new Scanner(new File(file))) {
            String fileContents = s.useDelimiter("\\Z")
                    .next();
            Pattern pattern = Pattern.compile(messageCode.trim());
            Matcher matcher = pattern.matcher(fileContents);

            //if (!messageCode.trim().contains("private final static String")) {
            if (!messageCode.trim().contains("String ERROR_MESSAGE_")) {
                while (matcher.find()) {
                    countMatches++;
                }
            }
        }
        return countMatches;
    }

    List<Integer> lookupUsage(String file, List<String> messageCodes)
            throws FileNotFoundException {
        List<Integer> mismatchListing = new ArrayList<Integer>();
        try (
                Scanner s = new Scanner(new File(file))) {
            String fileContents = s.useDelimiter("\\Z")
                    .next();
            for (String messageCode : messageCodes) {
                try {
                    messageCode = messageCode.trim();
                    int counter = 0;
                    int code = getMessageCodeValue(messageCode);
                    String searchString = code + "\\s*,\\s*" + messageCode;
                    Pattern pattern2 = Pattern.compile(searchString, Pattern.MULTILINE);
                    Matcher matcher2 = pattern2.matcher(fileContents.trim());
                    while (matcher2.find()) {
                        counter++;
                    }
                    if (counter == 0) {
                        mismatchListing.add(code);
                    }
                    /** uncomment for debugging an imbalance in the usage and counter on line 363
                     if (countMessageUsage(file, messageCode) > 2){
                     logger.info("message usage: " +
                     countMessageUsage(file, messageCode) +
                     " message code: " + messageCode + " counter:  " + counter);
                     }*/
                    assertEquals("unexpected use of error message detected in file " +
                                    file + "\n for message code " + messageCode,
                            countMessageUsage(file, messageCode) - 1, counter);
                } catch (NumberFormatException e) {
                    // ignore this message as it is not conforming to the expected format
                }
            }
        }
        return mismatchListing;
    }

    int getMessageCodeValue(String messageCode) {
        String code = messageCode.split("_")[2];
        String temp = null;
        for (int i = 0; i < 5; i++) {
            if (code.charAt(i) != '0') {
                temp = code.substring(i).trim();
                break;
            }
        }
        return Integer.valueOf(temp);
    }

    List<String> getMessageCodes(String file, String searchString) {
        List<String> messageCodes = new ArrayList<String>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            try {
                for (String line; (line = br.readLine()) != null; ) {
                    if (line.contains(searchString)) {
                        // strip out the message code
                        String delimiter = "=";
                        String[] pieces = line.split(delimiter);
                        for (String piece : pieces) {
                            if (piece.contains(searchString)) {
                                delimiter = "String ";
                                pieces = piece.split(delimiter);
                                messageCodes.add(pieces[1]);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return messageCodes;
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
    public void testExistsErrorCode() {
        String testMessage = "This is a test message for code less than 10000.";

        String formattedMessage = fixture.formatMessage(5, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);

        formattedMessage = fixture.formatMessage(2000, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);

        formattedMessage = fixture.formatMessage(12000, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.ERRORS);

        assertTrue(fixture.existsErrorMessageWithCode(005));
        assertFalse(fixture.existsErrorMessageWithCode(6));
        assertTrue(fixture.existsErrorMessageWithCode(2000));
        assertTrue(fixture.existsErrorMessageWithCode(12000));
    }

    @Test
    public void testExistsWarningCode() {
        String testMessage = "This is a test message for code less than 10000.";

        String formattedMessage = fixture.formatMessage(5, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.WARNINGS);

        formattedMessage = fixture.formatMessage(2000, testMessage, testClass);
        fixture.addMessage(formattedMessage, MESSAGE_TYPE.WARNINGS);

        assertTrue(fixture.existsWarningMessageWithCode(5));
        assertFalse(fixture.existsWarningMessageWithCode(6));
        assertTrue(fixture.existsWarningMessageWithCode(2000));
    }

}
