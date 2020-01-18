package one.jodi.base.error;

import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock Class Error - Messaging framework message code tracker
 */
public class Mock_ClassWithError implements ErrorWarningMessageJodiTracker {
    final static Charset ENCODING = StandardCharsets.UTF_8;
    private static final String MESSAGE_CODE = "Message Code";
    private static final String MESSAGE_BODY = "Message Body";
    private static final String NUMBER_OF_LOCATIONS = "# Locations";
    private static final String MESSAGE_LOCATIONS = "Message Locations";
    private static final CSVFormat EXCEL_COLUMN_HEADER = CSVFormat.EXCEL
            .withHeader(MESSAGE_CODE, MESSAGE_BODY, NUMBER_OF_LOCATIONS,
                    MESSAGE_LOCATIONS);
    private static final String CSV = "ErrorWarningJodiTrackerReport.csv";
    private static final String CODE_PREFIX = "ERROR_MESSAGE_";
    private static final String ERROR_SKIP_FILE =
            "Attempt to read from file %1$s failed. Skipping processing of file.";
    private static final String ERROR_MESSAGE_99997 = "Tracker is currently empty.";
    private static final String ERROR_MESSAGE_99998 =
            "%d matches found while %d occurrences found in file for message\n"
                    + " code id %s was not used correctly in class location: %s \n"
                    + "Solution: %s";
    private static final String SOLUTION_99998 =
            "Verify that the id and body match in each instance of usage. E.g. %d, %s";
    private static final String ERROR_MESSAGE_99999 =
            "Tracker contains %d but the message body does not match.\n"
                    + "Message body in tracker: %s\n"
                    + "Message body being added: %s\n"
                    + "Locations in tracker: %s\n"
                    + "Location being added: %s\n"
                    + "Solution: Please assign a new available code.";
    private static final String SEARCH_STRING = "ERROR_MESSAGE_)";
    private static ErrorWarningMessageJodiTracker instance = null;
    private static ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiImpl.getInstance();
    SortedMap<Integer, MessageCode> tracker;
    MessageCode m = null;

    private Mock_ClassWithError() {
        super();
        tracker = new TreeMap<Integer, MessageCode>();
    }

    public synchronized static ErrorWarningMessageJodiTracker getInstance() {
        if (instance == null) {
            instance = new Mock_ClassWithError();
        }
        return instance;
    }

    @Override
    public SortedMap<Integer, MessageCode> getTracker() {
        return Collections.unmodifiableSortedMap(tracker);
    }

    @Override
    public boolean checkTracker(Integer code, String messageCode,
                                String className) {
        boolean ans = false;

        if (tracker.containsKey(code)) {
            if (tracker.get(code).getBody().trim().equals(messageCode.trim())) {
                m = tracker.get(code);
                updateCode(code, messageCode, className);
                ans = true;
            } else {
                try {
                    validateFile(className.getClass());
                } catch (IllegalArgumentException e) {
                    MessageCode m = tracker.get(code);
                    String msg = errorWarningMessages.formatMessage(999990,
                            ERROR_MESSAGE_99999, this.getClass(), code, m
                                    .getBody(), messageCode.trim(), m
                                    .getClassLocations().toString(), className);
                    errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    throw new UsageException(msg);

                } catch (IllegalAccessException e) {
                    MessageCode m = tracker.get(code);
                    String msg = errorWarningMessages.formatMessage(999990,
                            ERROR_MESSAGE_99999, this.getClass(), code, m
                                    .getBody(), messageCode.trim(), m
                                    .getClassLocations().toString(), className);
                    errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    throw new UsageException(msg);
                } catch (ClassNotFoundException e) {
                    MessageCode m = tracker.get(code);
                    String msg = errorWarningMessages.formatMessage(999990,
                            ERROR_MESSAGE_99999, this.getClass(), code, m
                                    .getBody(), messageCode.trim(), m
                                    .getClassLocations().toString(), className);
                    errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    throw new UsageException(msg);
                } catch (NoSuchFieldException e) {
                    MessageCode m = tracker.get(code);
                    String msg = errorWarningMessages.formatMessage(999990,
                            ERROR_MESSAGE_99999, this.getClass(), code, m
                                    .getBody(), messageCode.trim(), m
                                    .getClassLocations().toString(), className);
                    errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    throw new UsageException(msg);
                } catch (SecurityException e) {
                    MessageCode m = tracker.get(code);
                    String msg = errorWarningMessages.formatMessage(999990,
                            ERROR_MESSAGE_99999, this.getClass(), code, m
                                    .getBody(), messageCode.trim(), m
                                    .getClassLocations().toString(), className);
                    errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    throw new UsageException(msg);
                } catch (IOException e) {
                    MessageCode m = tracker.get(code);
                    String msg = errorWarningMessages.formatMessage(999990,
                            ERROR_MESSAGE_99999, this.getClass(), code, m
                                    .getBody(), messageCode.trim(), m
                                    .getClassLocations().toString(), className);
                    errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    throw new UsageException(msg);
                }
                MessageCode m = tracker.get(code);
                String msg = errorWarningMessages.formatMessage(999990,
                        ERROR_MESSAGE_99999, this.getClass(), code,
                        m.getBody(), messageCode.trim(), m.getClassLocations()
                                .toString(), className);
                errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                throw new UsageException(msg);
            }
        } else {
            List<String> classLocations = new ArrayList<String>();
            classLocations.add(className);
            addCode(code, messageCode, classLocations);
            ans = true;
        }
        return ans;
    }

    public void addCode(Integer code, String messageCode,
                        List<String> fileLocations) {
        if (tracker.containsKey(code)) {
            if (tracker.get(code).getBody().trim().equals(messageCode.trim())) {
                m = tracker.get(code);
                updateCode(code, messageCode.trim(), fileLocations);
            } else {
                MessageCode m = tracker.get(code);
                String msg = errorWarningMessages.formatMessage(99999,
                        ERROR_MESSAGE_99999, this.getClass(), code,
                        m.getBody(), messageCode.trim(), m.getClassLocations()
                                .toString(), fileLocations.toString());
                errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                throw new UsageException(msg);
            }
        } else {
            m = new MessageCode(code, messageCode, fileLocations);
            tracker.put(code, m);
        }
    }

    @Override
    public void updateCode(Integer code, String messageCode,
                           String fileLocations) {
        m = tracker.get(code);
        if (m.getId() == code && m.getBody().equals(messageCode)) {
            List<String> locations = m.getClassLocations();
            locations.add(fileLocations);
            m.setClassLocations(locations);
        }
    }

    public void updateCode(Integer code, String messageCode,
                           List<String> fileLocations) {
        m = tracker.get(code);
        if (m.getId() == code && m.getBody().trim().equals(messageCode.trim())) {
            List<String> locations = m.getClassLocations();
            locations.addAll(fileLocations);
            m.setClassLocations(locations);
        }
    }


    public void validateFile(final Class<?> thisClass)
            throws IllegalArgumentException, IllegalAccessException,
            ClassNotFoundException, NoSuchFieldException, SecurityException,
            IOException {
        List<String> declaredMessages = collectDeclaredMessages(thisClass);

        // check usage in classes
        List<MessageCode> declaredMessageCodes = checkMessageCodesUsage(
                declaredMessages, thisClass);

        // need to confirm use of the declared message codes above. If fail, add
        // to issues listing
        List<String> issues = checkUsage(declaredMessageCodes, thisClass);

        // print list of issues found in the class, if any
        if (issues.size() > 0) {
            for (String issue : issues) {
                System.err.println(issue);
            }
        }
    }

    private List<MessageCode> checkMessageCodesUsage(
            List<String> declaredMessages, Class<?> thisClass)
            throws NoSuchFieldException, SecurityException,
            ClassNotFoundException {
        List<MessageCode> declaredMessageCodes = new ArrayList<MessageCode>();
        for (String declaredMessage : declaredMessages) {
            String declaredMessageCode = declaredMessage.split(CODE_PREFIX)[1];
            String temp = null;
            for (int i = 0; i < declaredMessageCode.length(); i++) {
                temp = declaredMessageCode.substring(i);
                if (temp != null && !temp.isEmpty()) {
                    break;
                }
            }
            int messageCode = 0;
            if (temp != null && !temp.isEmpty()) {
                Integer.parseInt(temp);
            }
            Field field = getFieldFromClass(thisClass, declaredMessage);
            String[] fieldPieces = field.toString().split(CODE_PREFIX);
            messageCode = Integer.parseInt(fieldPieces[1]);

            String messageCodeBody = getMessageCodeBody(thisClass,
                    declaredMessage, field);
            List<String> fileLocations = new ArrayList<String>();
            fileLocations.add(thisClass.getName());
            m = new MessageCode(messageCode, messageCodeBody, fileLocations);
            declaredMessageCodes.add(m);
        }
        return declaredMessageCodes;
    }

    private String getMessageCodeBody(Class<?> thisClass,
                                      String declaredMessage, Field field) {
        String messageCodeBody = null;
        try {
            messageCodeBody = field.get(Class.forName(thisClass.getName()))
                    .toString();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return messageCodeBody;
    }

    private Field getFieldFromClass(Class<?> thisClass, String declaredMessage)
            throws NoSuchFieldException, SecurityException,
            ClassNotFoundException {
        Field field = Class.forName(thisClass.getName()).getDeclaredField(
                declaredMessage);
        return field;
    }

    private List<String> collectDeclaredMessages(Class<?> thisClass) {
        List<String> messages = new ArrayList<String>();
        Field[] fieldsInClass = thisClass.getDeclaredFields();
        for (int i = 0; i < fieldsInClass.length; i++) {
            if (fieldsInClass[i].toString().contains(CODE_PREFIX)) {
                String field = fieldsInClass[i].toString().split(
                        thisClass.getName() + ".")[1];
                messages.add(field);
            }
        }
        return messages;
    }

    private List<String> checkUsage(final List<MessageCode> declaredMessageCodes,
                                    final Class<?> thisClass) throws IOException {
        Path path = new File(new File("").getAbsolutePath()).toPath();
        Optional<Path> op = Files.walk(path)
                .filter(p -> p.getFileName().toString()
                        .endsWith("java"))
                .filter(p -> !p.getFileName().toString()
                        .contains("Test") &&
                        p.getFileName().toString()
                                .contains(thisClass.getSimpleName()))
                .findFirst();

        List<String> issues = Collections.emptyList();
        if (op.isPresent()) {
            issues = lookupUsage(op.get().toFile(), declaredMessageCodes);
        }
        return issues;
    }

    private int countMessageUsage(final String fileContents,
                                  final String messageCode) {
        // new Scanner(new File(file)).useDelimiter("\\Z").next();
        Pattern pattern = Pattern.compile(messageCode.trim());
        Matcher matcher = pattern.matcher(fileContents);
        int countMatches = 0;
        if (!messageCode.trim().contains(SEARCH_STRING)) {
            while (matcher.find()) {
                countMatches++;
            }
        }
        return countMatches;
    }

    private List<String> lookupUsage(final File fileName,
                                     final List<MessageCode> messageCodes) {
        String fileContents = null;
        try {
            fileContents = readFile(fileName, ENCODING);
        } catch (IOException e) {
            String msg = errorWarningMessages.formatMessage(0, ERROR_SKIP_FILE,
                    this.getClass(), fileName, e);
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            return Collections.emptyList();
        }
        List<String> mismatchListing = new ArrayList<String>();
        for (MessageCode messageCode : messageCodes) {
            int counter = 0;
            String searchString = getMessageCode(messageCode);
            int occurrenceInFile = 0;
            occurrenceInFile = countMessageUsage(fileContents, CODE_PREFIX +
                    messageCode.getId()) - 1;
            Pattern pattern2 = Pattern.compile(searchString, Pattern.MULTILINE);
            Matcher matcher2 = pattern2.matcher(fileContents.toString());
            while (matcher2.find()) {
                counter++;
            }
            if (counter == 0 || occurrenceInFile != counter) {
                mismatchListing.add(messageCode.getId() + ", "
                        + messageCode.getBody() + " in "
                        + messageCode.getClassLocations().toString());
                String solution = String.format(SOLUTION_99998,
                        messageCode.getId(), CODE_PREFIX + messageCode.getId());
                String msg = errorWarningMessages.formatMessage(99998,
                        ERROR_MESSAGE_99998, this.getClass(), counter,
                        occurrenceInFile, messageCode.getId(), messageCode
                                .getClassLocations().toString(), solution);
                errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                throw new UsageException(msg);

            }
        }
        return mismatchListing;
    }

    private String readFile(File fileName, Charset utf8) throws IOException {
        Path path = fileName.toPath();
        List<String> lines = Files.readAllLines(path, utf8);
        StringBuffer buffer = new StringBuffer();
        for (String line : lines) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    private String getMessageCode(MessageCode messageCode) {
        String codeToString = Integer.toString(messageCode.getId());
        if (messageCode.getId() < 10000) {
            StringBuffer filler = new StringBuffer();
            for (int i = 0; i < 5 - codeToString.length(); i++) {
                filler.append("0");
            }
            String s = filler.toString();
            codeToString = s + Integer.toString(messageCode.getId());
        }
        String declaredMessageCodeVariable = CODE_PREFIX + codeToString;
        return messageCode.getId() + ",(\\s*)" + declaredMessageCodeVariable;
    }

    @Override
    public void printMessageCodes_CodeAndBody() {
        if (tracker.size() > 0) {
            for (MessageCode messageCode : tracker.values()) {
                messageCode.printMessageCodeAndBody();
            }
        } else {
            String msg = errorWarningMessages.formatMessage(99997, ERROR_MESSAGE_99997, this.getClass());
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

    @Override
    public void printMessageCodes() {
        if (tracker.size() > 0) {
            for (MessageCode messageCode : tracker.values()) {
                messageCode.printMessageCodeAndBodyAndLocations();
            }
        } else {
            String msg = errorWarningMessages.formatMessage(99997,
                    ERROR_MESSAGE_99997, this.getClass());
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

    @Override
    public void generateCSVFile() throws IOException {
        CSVPrinter messagePrinter = null;
        if (tracker.size() > 0) {
            String dir = new File("").getAbsolutePath();
            File file = new File(dir, CSV);
            try (BufferedWriter fileOutput = Files.newBufferedWriter(file.toPath(), ENCODING)) {
                messagePrinter = new CSVPrinter(fileOutput, EXCEL_COLUMN_HEADER);

                for (MessageCode messageCode : tracker.values()) {
                    List<String> message = new ArrayList<String>();
                    message.add(String.valueOf(messageCode.getId()));
                    message.add(messageCode.getType().toString());
                    message.add(messageCode.getBody().trim());
                    message.add(String.valueOf(messageCode.getClassLocations()
                            .size()));
                    for (String location : messageCode.getClassLocations()) {
                        message.add(location);
                    }
                    messagePrinter.printRecord(message);
                }
            } finally {
                if (messagePrinter != null) {
                    messagePrinter.flush();
                    messagePrinter.close();
                }
            }
        } else {
            String msg = errorWarningMessages.formatMessage(99997,
                    ERROR_MESSAGE_99997, this.getClass());
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

    @Override
    public void flush() {
        tracker.clear();
    }

    @Override
    public void updateMessageCodeInTracker(int messageIdFromErrorMessage,
                                           MESSAGE_TYPE messageType) {
        MessageCode messageCode = tracker.get(messageIdFromErrorMessage);
        if (messageCode.getType() != messageType) {
            if (messageCode.getType() == MESSAGE_TYPE.UNUSED) {
                messageCode.setType(messageType);
            }
        }
    }

}
