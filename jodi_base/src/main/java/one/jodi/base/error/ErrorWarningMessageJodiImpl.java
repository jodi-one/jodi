package one.jodi.base.error;

import one.jodi.base.exception.UnRecoverableException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Implementation of the Messaging framework {@link ErrorWarningMessageJodi} interface.
 */
public class ErrorWarningMessageJodiImpl implements ErrorWarningMessageJodi {

   public static final int PackageSequenceGlobal = -1;

   private static final Logger LOGGER = LogManager.getLogger(ErrorWarningMessageJodiImpl.class);
   private static final String FORMAT_HEADER = "[%05d] ";
   private static final String ERROR_MESSAGE_99996 =
           "Error in construction error message from string '%s'. Message code string " +
                   "contains %d parameters while %s parameters are entered.";
   private static ErrorWarningMessageJodi error = null;

   private final SortedMap<Integer, List<String>> errorMessages;
   private final SortedMap<Integer, List<String>> warningMessages;
   private final SortedMap<String, File> files = new TreeMap<>();
   //has state
   private int lastSequenceNumber;
   private String metaDataDirectory;

   /**
    * Constructor used as part of the Singleton pattern.
    */
   private ErrorWarningMessageJodiImpl() {
      super();
      errorMessages = new TreeMap<>();
      warningMessages = new TreeMap<>();

      if (EOL.length() > 10) {
         throw new UnRecoverableException(
                 "Possible DoS Attack via method call " + "System.getProperty(\"line.separator\")");
      }
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
               files.put(f.getName(), f);
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
            report.append(messageHeader)
                  .append(EOL);
            LOGGER.warn(messageHeader);
            if (errorMessages.size() > 0) {
               report.append("----  ERRORS  ----------------------------")
                     .append(EOL);
               LOGGER.error("----  ERRORS  ----------------------------");
               report.append(printByMessageType(Level.ERROR, errorMessages, wString));
            }
            if (warningMessages.size() > 0) {
               wString = "(warning)";
               report.append("----  WARNINGS  ----------------------------")
                     .append(EOL);
               LOGGER.warn("----  WARNINGS  ----------------------------");
               report.append(printByMessageType(Level.WARN, warningMessages, wString));
            }
            LOGGER.warn("-------------------------------------------------------" + EOL);
         }
      } else {
         if (!amessagesToSuppress.contains(MESSAGE_TYPE.ERRORS)) {
            // suppress warnings and only print errors
            if (errorMessages.size() > 0) {
               report.append(messageHeader)
                     .append(EOL);
               LOGGER.error(messageHeader);
               report.append("----  ERRORS  ----------------------------")
                     .append(EOL);
               LOGGER.error("----  ERRORS  ----------------------------");
               report.append(printByMessageType(Level.ERROR, getErrorMessages(), wString));
               if (warningMessages.size() == 0) {
                  report.append("-------------------------------------------------------")
                        .append(EOL);
                  LOGGER.error("-------------------------------------------------------" + EOL);
               }
            }
         }
         if (!amessagesToSuppress.contains(MESSAGE_TYPE.WARNINGS)) {
            // suppress errors and only print warnings
            if (warningMessages.size() > 0) {
               wString = "(warning)";
               if (errorMessages.size() < 1) {
                  report.append(messageHeader)
                        .append(EOL);
                  LOGGER.warn(messageHeader);
               }
               report.append("----  WARNINGS  ----------------------------")
                     .append(EOL);
               LOGGER.warn("----  WARNINGS  ----------------------------");
               report.append(printByMessageType(Level.WARN, getWarningMessages(), wString));
               LOGGER.warn("-------------------------------------------------------" + EOL);
               report.append("-------------------------------------------------------")
                     .append(EOL)
                     .append(EOL);
            }
         }
      }
      return report.toString();
   }

   private synchronized String printByMessageType(final Level level, final Map<Integer, List<String>> map,
                                                  final String warning) {
      StringBuilder report = new StringBuilder();
      SortedMap<Integer, List<String>> updatedMap = new TreeMap<>();
      for (Integer packageSequence : map.keySet()) {
         for (String message : map.get(packageSequence)) {
            int messageCode = getMessageIdFromErrorMessage(message);
            String packageSequenceString = (packageSequence == PackageSequenceGlobal) ? "" : packageSequence + " - ";
            if (warning == null) {
               List<String> messages;
               if (!updatedMap.containsKey(messageCode)) {
                  messages = new ArrayList<>();
               } else {
                  messages = updatedMap.get(messageCode);
               }
               messages.add(packageSequenceString.concat(message));
               updatedMap.put(messageCode, messages);
            } else {
               List<String> messages;
               if (!updatedMap.containsKey(messageCode)) {
                  messages = new ArrayList<>();
               } else {
                  messages = updatedMap.get(messageCode);
               }
               messages.add(packageSequenceString.concat(message)
                                                 .concat(warning));
               updatedMap.put(messageCode, messages);
            }
         }
      }
      for (List<String> messages : updatedMap.values()) {
         for (String message : messages) {
            if (level.equals(Level.WARN)) {
               LOGGER.warn(message);
            } else {
               LOGGER.error(message);
            }
            report.append(message)
                  .append(EOL);
         }
      }
      return report.toString();
   }

   @Override
   public void addMessage(final String errorMessage, final MESSAGE_TYPE messageType) {
      addMessage(PackageSequenceGlobal, errorMessage, messageType);
   }

   private boolean checkIfMessageAlreadyExist(String errorMessage, MESSAGE_TYPE messageType) {
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

   private boolean searchMessageMap(final SortedMap<Integer, List<String>> messageMap, final String errorMessage) {
      for (List<String> messages : messageMap.values()) {
         for (String message : messages) {
            if (message.equals(errorMessage)) {
               return true;
            }
         }
      }
      return false;
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
   public void addMessage(final int packageSequence, String errorMessage, final MESSAGE_TYPE messageType) {
      if (files.size() == 0 && this.metaDataDirectory != null) {
         initializeFiles(this.metaDataDirectory);
      }
      if (packageSequence > 0) {
         List<File> matching = files.values()
                                    .stream()
                                    .filter(file -> file.getName()
                                                        .startsWith(packageSequence + ""))
                                    .collect(Collectors.toList());
         if (matching.size() == 1) {
            errorMessage = errorMessage + "(" + matching.get(0)
                                                        .getAbsolutePath() + ")";
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

   private void addMessages(final SortedMap<Integer, List<String>> errorMessages, final int packageSequence,
                            final String errorMessage) {
      List<String> list = errorMessages.computeIfAbsent(packageSequence, k -> new ArrayList<>());
      list.add(errorMessage);
   }

   @Override
   public String formatMessage(final int messageCode, final String messageFormatString, final Class<?> classLocation,
                               final Object... args) {
      if (validate(messageCode, messageFormatString, args)) {
         return String.format(FORMAT_HEADER, messageCode) + String.format(messageFormatString, args);
      } else {
         long countVariables = messageFormatString.chars()
                                                  .filter(ch -> ch == '%')
                                                  .count();
         String cleanMessage = messageFormatString.replace("%", "");
         String msg =
                 formatMessage(99996, ERROR_MESSAGE_99996, this.getClass(), cleanMessage, countVariables, args.length);
         addMessage(msg, MESSAGE_TYPE.ERRORS);
         return msg;
      }
   }

   public boolean validate(final int messageCode, final String messageCodeString, final Object[] args) {
      // number of occurrences of %
      long count = messageCodeString.chars()
                                    .filter(ch -> ch == '%')
                                    .count();

      // simple case the same number of parameters in args and message code string
      if (count == args.length) {
         return true;
      } else {
         // less simple case: parameters are used multiple times within message
         String[] parameters = messageCodeString.split("\"");
         long hits = Arrays.stream(parameters)
                           .filter(p -> p.startsWith("%"))
                           .map(p -> p.substring(1))
                           .distinct()
                           .count();

         return hits == args.length;
      }
   }

   @Override
   public void clear() {
      setLastSequenceNumber(0);
      if (errorMessages.isEmpty()) {
         LOGGER.debug("No errors to be removed.");
      } else {
         int size = errorMessages.size();
         errorMessages.clear();
         LOGGER.debug("Error messages removed. " + size);
      }
      if (warningMessages.isEmpty()) {
         LOGGER.debug("No warnings to be removed.");
      } else {
         int size = warningMessages.size();
         warningMessages.clear();
         LOGGER.debug("Warning messages removed. " + size);
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

   private boolean messageWithCodeExists(final Collection<List<String>> messages, final int code) {
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
}
