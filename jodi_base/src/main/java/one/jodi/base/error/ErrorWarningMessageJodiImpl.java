package one.jodi.base.error;

import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.Register;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the Messaging framework {@link ErrorWarningMessageJodi} interface.
 *
 */
public class ErrorWarningMessageJodiImpl implements ErrorWarningMessageJodi {

    public final static int PackageSequenceGlobal = -1;
    private final static Logger logger =
            LogManager.getLogger(ErrorWarningMessageJodiImpl.class);
    private static final String FORMAT_HEADER = "[%05d] ";
    private static final String ERROR_MESSAGE_99996 =
            "Error in construction error message from string '%s'. Message code string " +
                    "contains %d parameters while %s parameters are entered.";
    private static ErrorWarningMessageJodi error = null;
    Register register;
    private int lastSequenceNumber;
    private SortedMap<Integer, List<String>> errorMessages;
    private SortedMap<Integer, List<String>> warningMessages;
    private SortedMap<String, File> files = new TreeMap<>();
    //has state
    private String metaDataDirectory;

    /**
     * Constructor used as part of the Singleton pattern.
     */
    private ErrorWarningMessageJodiImpl() {
        super();
        errorMessages = new TreeMap<>();
        warningMessages = new TreeMap<>();

        if (EOL.length() > 10) {
            throw new UnRecoverableException("Possible DoS Attack via method call " +
                    "System.getProperty(\"line.separator\")");
        }
    }

    private ErrorWarningMessageJodiImpl(final ErrorWarningMessageJodi original) {
        super();
        // clone operation! Not simply copy of Map as it may not be deleted
        // but simply cleared
        errorMessages =
                new TreeMap<>(original.getErrorMessages());
        warningMessages =
                new TreeMap<>(original.getWarningMessages());
    }

    /**
     * Singleton pattern
     * <p>
     * A singleton instance is created before Guice-based injection is enabled.
     * This instance will be handed over to the Guice framework as part of the
     * bootstrapping mechanism and is subsequently injected in all other services
     * when requested.
     *
     * @return errorWarningMessageJodi instance of the error / warning system
     */
    public synchronized static ErrorWarningMessageJodi getInstance() {
        if (error == null) {
            error = new ErrorWarningMessageJodiImpl();
        }
        return error;
    }

    private void initializeFiles(final String metadataFolder) {
        File dir = new File(metadataFolder);
        if (dir.isDirectory()) {
            File[] xmlFiles = dir.listFiles();
            if (xmlFiles == null) {
                return;
            }
            for (File f : xmlFiles) {
                if (f.isFile()) {
                    files.put(f.getName(),
                            f);
                } else {
                    // recurse
                    initializeFiles(f.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public SortedMap<Integer, List<String>> getErrorMessages() {
        return Collections.unmodifiableSortedMap(errorMessages);
    }

    @Override
    public SortedMap<Integer, List<String>> getWarningMessages() {
        return Collections.unmodifiableSortedMap(warningMessages);
    }

    @Override
    public synchronized String printMessages(final MESSAGE_TYPE... messagesToSuppress) {
        StringBuilder report = new StringBuilder();
        List<MESSAGE_TYPE> amessagesToSuppress = Arrays.asList(messagesToSuppress);
        String wString = null;
        if (messagesToSuppress.length == 0) {
            // default
            if (errorMessages.size() > 0 || warningMessages.size() > 0) {
                report.append(messageHeader + EOL);
                logger.warn(messageHeader);
                if (errorMessages.size() > 0) {
                    report.append("----  ERRORS  ----------------------------" + EOL);
                    logger.error("----  ERRORS  ----------------------------");
                    report.append(printByMessageType(Level.ERROR, errorMessages, wString, true));
                }
                if (warningMessages.size() > 0) {
                    wString = "(warning)";
                    report.append("----  WARNINGS  ----------------------------" + EOL);
                    logger.warn("----  WARNINGS  ----------------------------");
                    report.append(printByMessageType(Level.WARN, warningMessages, wString,
                            false));
                }
                logger.warn("-------------------------------------------------------" + EOL);
            }
        } else if (messagesToSuppress.length > 0) {
            if (!amessagesToSuppress.contains(MESSAGE_TYPE.ERRORS) || amessagesToSuppress.isEmpty()) {
                // suppress warnings and only print errors
                if (errorMessages.size() > 0) {
                    report.append(messageHeader + EOL);
                    logger.error(messageHeader);
                    report.append("----  ERRORS  ----------------------------" + EOL);
                    logger.error("----  ERRORS  ----------------------------");
                    report.append(printByMessageType(Level.ERROR, getErrorMessages(),
                            wString, true));
                    if (warningMessages.size() == 0) {
                        report.append("-------------------------------------------------------" + EOL);
                        logger.error("-------------------------------------------------------" + EOL);
                    }
                }
            }
            if (!amessagesToSuppress.contains(MESSAGE_TYPE.WARNINGS) || amessagesToSuppress.isEmpty()) {
                // suppress errors and only print warnings
                if (warningMessages.size() > 0) {
                    wString = "(warning)";
                    if (errorMessages.size() < 1) {
                        report.append(messageHeader + EOL);
                        logger.warn(messageHeader);
                    }
                    report.append("----  WARNINGS  ----------------------------" + EOL);
                    logger.warn("----  WARNINGS  ----------------------------");
                    report.append(printByMessageType(Level.WARN, getWarningMessages(),
                            wString, false));
                    logger.warn("-------------------------------------------------------" + EOL);
                    report.append("-------------------------------------------------------" + EOL + EOL);
                }
            }

        }
        return report.toString();
    }

    private synchronized String printByMessageType(final Level level,
                                                   final Map<Integer, List<String>> map,
                                                   final String warning,
                                                   final boolean isError) {
        StringBuilder report = new StringBuilder();
        SortedMap<Integer, List<String>> updatedMap = new TreeMap<>();
        for (Integer packageSequence : map.keySet()) {
            for (String message : map.get(packageSequence)) {
                int messageCode = getMessageIdFromErrorMessage(message);
                String packageSequenceString = (packageSequence == PackageSequenceGlobal) ? ""
                        : packageSequence + " - ";
                if (warning == null) {
                    if (!updatedMap.containsKey(messageCode)) {
                        List<String> messages = new ArrayList<>();
                        messages.add(packageSequenceString.concat(message));
                        updatedMap.put(messageCode, messages);
                    } else {
                        List<String> messages = updatedMap.get(messageCode);
                        messages.add(packageSequenceString.concat(message));
                        updatedMap.put(messageCode, messages);
                    }
                } else {
                    if (!updatedMap.containsKey(messageCode)) {
                        List<String> messages = new ArrayList<>();
                        messages.add(packageSequenceString.concat(message)
                                .concat(warning));
                        updatedMap.put(messageCode, messages);
                    } else {
                        List<String> messages = updatedMap.get(messageCode);
                        messages.add(packageSequenceString.concat(message)
                                .concat(warning));
                        updatedMap.put(messageCode, messages);
                    }
                }
            }
        }
        for (List<String> messages : updatedMap.values()) {
            for (String message : messages) {
                if (level.equals(Level.WARN)) {
                    logger.warn(message);
                } else {
                    logger.error(message);
                }
                report.append(message + EOL);
            }
        }
        return report.toString();
    }

    @Override
    public void addMessage(final String errorMessage, final MESSAGE_TYPE messageType) {
        addMessage(PackageSequenceGlobal, errorMessage, messageType);
    }

    private boolean checkIfMessageAlreadyExist(String errorMessage,
                                               MESSAGE_TYPE messageType) {
        boolean messageAlreadyExist = false;
        if (messageType.equals(MESSAGE_TYPE.ERRORS)) {
            // search errorMessages for an occurrence of errorMessage
            if (searchMessageMap(errorMessages, errorMessage)) {
                messageAlreadyExist = true;
            }
        } else if (messageType.equals(MESSAGE_TYPE.WARNINGS)) {
            // search warningMessages for an occurrence of errorMessage
            if (searchMessageMap(warningMessages, errorMessage)) {
                messageAlreadyExist = true;
            }
        }

        return messageAlreadyExist;
    }

    private boolean searchMessageMap(final SortedMap<Integer, List<String>> messageMap,
                                     final String errorMessage) {
        boolean messageFound = false;
        for (List<String> messages : messageMap.values()) {
            for (String message : messages) {
                if (message.equals(errorMessage)) {
                    messageFound = true;
                }
            }

        }
        return messageFound;
    }

    private int getMessageIdFromErrorMessage(final String errorMessage) {
        String temp = (errorMessage.split("]")[0]).replace("[", "");

        StringBuilder buf = new StringBuilder();
        char[] tempChar = temp.toCharArray();

        boolean firstDigitFound = false;
        for (char c : tempChar) {
            //i++;
            if (c != '0' && !firstDigitFound) {
                firstDigitFound = true;
                buf.append(c);

            } else if (firstDigitFound) {
                buf.append(c);
            }
        }

        try {
            return Integer.parseInt(buf.toString());
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    @Override
    public void addMessage(final int packageSequence, String errorMessage,
                           final MESSAGE_TYPE messageType) {
        if (files.size() == 0 && this.metaDataDirectory != null) {
            initializeFiles(this.metaDataDirectory);
        }
        if (packageSequence > 0) {
            List<File> matching = files.entrySet().stream().filter(f -> f.getValue().getName()
                    .startsWith(
                            packageSequence +
                                    ""))
                    .map(Map.Entry<String, File>::getValue)
                    .collect(Collectors.toList());
            if (matching.size() == 1) {
                errorMessage = errorMessage + "(" +
                        matching.get(0).getAbsolutePath() + ")";
            }
        }
        if (messageType.equals(MESSAGE_TYPE.ERRORS)) {
            if (!checkIfMessageAlreadyExist(errorMessage, messageType)) {
                addMessages(errorMessages, packageSequence, errorMessage);
            }
        } else if (messageType.equals(MESSAGE_TYPE.WARNINGS)) {
            if (!checkIfMessageAlreadyExist(errorMessage, messageType)) {
                addMessages(warningMessages, packageSequence, errorMessage);
            }
        }
    }

    private void addMessages(final SortedMap<Integer, List<String>> errorMessages,
                             final int packageSequence, final String errorMessage) {
        List<String> list = errorMessages.get(packageSequence);
        if (list == null) {
            list = new ArrayList<>();
            errorMessages.put(packageSequence, list);
        }
        list.add(errorMessage);
    }

    @Override
    public String formatMessage(final int messageCode, final String messageFormatString,
                                final Class<?> classLocation, final Object... args) {
        if (validate(messageCode, messageFormatString, args)) {
            return String.format(FORMAT_HEADER, messageCode) +
                    String.format(messageFormatString, args);
        } else {
            long countcountVariables = messageFormatString.chars()
                    .filter(ch -> ch == '%')
                    .count();
            String cleanMessage = messageFormatString.replace("%", "");
            String msg = formatMessage(99996, ERROR_MESSAGE_99996, this.getClass(),
                    cleanMessage, countcountVariables, args.length);
            addMessage(msg, MESSAGE_TYPE.ERRORS);
            return msg;
        }
    }

    public boolean validate(final int messageCode, final String messageCodeString,
                            final Object[] args) {
        boolean match = false;

        // number of occurences of %
        long count = messageCodeString.chars().filter(ch -> ch == '%').count();

        // simple case the same number of parameters in args and message code string
        //if(count == Long.valueOf(args.length+"")){
        if (count == args.length) {
            match = true;
        } else {
            // less simple case: parameters are used multiple times within
            // message
            String[] parameters = messageCodeString.split("\"");
            List<String> params = new ArrayList<String>();
            for (String p : parameters) {
                if (p.startsWith("%") &&
                        !params.contains(p.substring(1, p.length()))) {
                    params.add(p.substring(1, p.length()));
                }
            }
            if (params.size() == args.length) {
                match = true;
            }
        }
        return match;
    }

    @Override
    public void clear() {
        setLastSequenceNumber(0);
        if (errorMessages.isEmpty()) {
            logger.debug("No errors to be removed.");
        } else {
            int size = errorMessages.size();
            errorMessages.clear();
            logger.debug("Error messages removed. " + size);
        }
        if (warningMessages.isEmpty()) {
            logger.debug("No warnings to be removed.");
        } else {
            int size = warningMessages.size();
            warningMessages.clear();
            logger.debug("Warning messages removed. " + size);
        }
    }

    private boolean hasPrefix(final List<String> messages, final String prefix) {
        boolean match = false;
        for (String msg : messages) {
            if (msg.startsWith(prefix)) {
                match = true;
                break;
            }
        }
        return match;
    }

    private boolean messageWithCodeExists(final Collection<List<String>> messages,
                                          final int code) {
        boolean found = false;
        String prefix = "[" + String.format("%05d", code) + "]";

        for (List<String> msgs : messages) {
            if (hasPrefix(msgs, prefix)) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
    public boolean existsErrorMessageWithCode(final int errorCode) {
        return messageWithCodeExists(this.errorMessages.values(), errorCode);
    }

    @Override
    public boolean existsWarningMessageWithCode(final int warningCode) {
        return messageWithCodeExists(this.warningMessages.values(), warningCode);
    }

    @Override
    public int getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    @Override
    public void setLastSequenceNumber(final int lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }

    @Override
    public int assignSequenceNumber() {
        return this.lastSequenceNumber += 10;
    }

    @Override
    public void setMetaDataDirectory(final String metaDataDirectory) {
        this.metaDataDirectory = metaDataDirectory;
    }

//	@Override
//	public synchronized void logMessages() {
//		// set log off and back on again for the console
//		Logger logger = LogManager.getLogger();
//		Map<String, Appender> appenderMap = 
//		        ((org.apache.logging.log4j.core.Logger) logger).getAppenders();
////		 Set<Appender> consoleAppenders = appenderMap.values().stream()
////				.filter( e -> e.getClass().getName().equals("org.apache.logging.log4j.core.appender.ConsoleAppender"))
////				.collect(Collectors.toSet());
////		appenderMap.entrySet().stream()
////		.peek( e -> logger.info("trying to stp : " +e.getValue().getClass().getName() + " -> "+ e.getValue().getHandler().getClass().getName()))
////		.filter( e -> e.getValue().getClass().getName().equals("org.apache.logging.log4j.core.appender.ConsoleAppender"))
////		.peek( e -> logger.info("Stopping : " +e.getValue().getName()))
////		.forEach( a ->  ((org.apache.logging.log4j.core.Logger) logger).get a.getValue()));
//
//		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
//		Configuration config = ctx.getConfiguration();
//		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
//		loggerConfig.setLevel(level);
//		
//		
//		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss - E");
//		String date = String.format("ErrorReport as per %s.", dateFormat.format(new Date()));
//		// default
//		if (errorMessages.size() > 0 || warningMessages.size() > 0) {
//			logger.info(date);
//			if (errorMessages.size() > 0) {
//				logger.info("----  ERRORS  ----------------------------");
//				logByMessageType(errorMessages);
//			}
//			if (warningMessages.size() > 0) {
//				logger.info("----  WARNINGS  ----------------------------");
//				logByMessageType(warningMessages);
//			}
//			logger.info("-------------------------------------------------------" + EOL);
//		}
//		consoleAppenders.stream().forEach( a-> ((org.apache.logging.log4j.core.Logger) logger).addAppender(a));
//		((org.apache.logging.log4j.core.Logger) logger).addAppender();
//	}
//	
//	private  synchronized void logByMessageType(SortedMap<Integer, List<String>> errorMessages) {
//		for(Entry<Integer, List<String>> entry :  errorMessages.entrySet()){ 
//			logger.info(String.format("%d - %s",  entry.getKey(), entry.getValue()));
//		}
//	}
}
