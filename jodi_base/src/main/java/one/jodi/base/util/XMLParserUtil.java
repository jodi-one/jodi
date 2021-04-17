package one.jodi.base.util;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

public class XMLParserUtil<T, O> {

    private final static String EOL = System.getProperty("line.separator");
    private final static Logger logger = LogManager.getLogger(XMLParserUtil.class);

    private final static String ERROR_MESSAGE_07021 =
            "Please specify your xsd location correctly for %s.";

    private final static String ERROR_MESSAGE_81000 =
            "FATAL: error while initializing JAXB parser. %s";
    private final static String ERROR_MESSAGE_81010 =
            "Validation Error at line %d of file %s:" + EOL + "%s";
    private final static String ERROR_MESSAGE_81020 =
            "FATAL: xml is invalid: %s";
    private final static String ERROR_MESSAGE_81030 =
            "FATAL: xml in file '%1$s' is invalid: %2$s";
    private final static String ERROR_MESSAGE_81040 =
            "Unknown fatal exception during parsing of XML stream: %s";
    private final static String ERROR_MESSAGE_81050 =
            "Unknown fatal exception during parsing of XML file '%1$s': %2$s";

    private final static String ERROR_MESSAGE_81060 =
            "Unable to find XSD schema for validation. We proceed without validation. %s %s.";
    private final static String ERROR_MESSAGE_81070 =
            "Validation Warning at line %d of file %s:" + EOL + "%s";
    private final static String ERROR_MESSAGE_81080 =
            "Invalid filename; %s : can't report where exactly errors occur.";
    private final static String ERROR_MESSAGE_81090 =
            "The xsd base file location is empty while initializing JAXB.";
    private final static String ERROR_MESSAGE_81100 =
            "Classloader error result in fatal failure.";


    private final Class<O> objectFactory;
    private final List<String> xsdFileNames;
    private final ErrorWarningMessageJodi errorWarningMessages;

    public XMLParserUtil(final Class<O> objectFactory,
                         final List<String> xsdFileNames,
                         final ErrorWarningMessageJodi errorWarningMessages) {
        this.objectFactory = objectFactory;
        this.xsdFileNames = xsdFileNames;
        this.errorWarningMessages = errorWarningMessages;
        if (EOL.length() > 10) {
            throw new UnRecoverableException("Possible DoS Attack via method call " +
                    "System.getProperty(\"line.separator\")");
        }
    }

    private Unmarshaller initializeJAXB(String xsdBaseFile) {
        Unmarshaller u;
        try {
            JAXBContext jc = JAXBContext.newInstance(objectFactory);
            u = jc.createUnmarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
            String friendlyMessage = e.getMessage() != null ? e.getMessage()
                    : "Unknown cause";
            String msg = errorWarningMessages.formatMessage(81000,
                    ERROR_MESSAGE_81000, this.getClass(), friendlyMessage);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg, e);
        }
        try {
            SchemaFactory schemaFactory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema;
            if (xsdBaseFile == null) {
                String msg = errorWarningMessages.formatMessage(81090,
                        ERROR_MESSAGE_81090, this.getClass());
                errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                logger.fatal(msg);
                throw new UnRecoverableException(msg);
            }
            if (this.xsdFileNames.contains(xsdBaseFile)) {
                // in production we read from jar
                ClassLoader cl = this.objectFactory.getClassLoader();
                if (cl == null) {
                    String msg = errorWarningMessages.formatMessage(81100,
                            ERROR_MESSAGE_81100, this.getClass());
                    errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
                    logger.fatal(msg);
                    throw new UnRecoverableException(msg);
                }
                try (InputStream inputStream = cl.getResourceAsStream(xsdBaseFile)) {
                    Source schemaFile = new StreamSource(inputStream);
                    schema = schemaFactory.newSchema(schemaFile);
                }
            } else {
                // for development or  JODIConstants.XSD_FILE_MODEL
                // or  JODIConstants.XSD_FILE_PACKAGES we read from filesystem
                schema = schemaFactory.newSchema(getXsdFileFromFileSystem(xsdBaseFile));
            }
            u.setSchema(schema);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            String msg = errorWarningMessages.formatMessage(81060,
                    ERROR_MESSAGE_81060, this.getClass(),
                    getXsdFileFromFileSystem(xsdBaseFile).toString(), e.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
            throw new UnRecoverableException(msg, e);
        }
        return u;
    }

    private Source getXsdFileFromFileSystem(String xsdBaseFile) {
        Source schemaFile;
        // for jodi_etl, since this class is in base we go to jodi_core
        File file = Paths.get(xsdBaseFile).toFile();
        logger.debug("Reading xsdBaseFile: " + xsdBaseFile);
        //File file = new File(location);
        if (file.exists()) {
            schemaFile = new StreamSource(file);
        } else {
            String msg = errorWarningMessages.formatMessage(7021,
                    ERROR_MESSAGE_07021, this.getClass(), xsdBaseFile);
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            logger.fatal(msg);
            throw new UnRecoverableException(msg);
        }
        logger.debug("Read xsd from: " + file.getAbsolutePath());
        return schemaFile;
    }

    public T loadObjectFromXMLAndValidate(final InputStream xmlStream,
                                          final String XSDLocation) {
        return loadObjectFromXMLAndValidate(xmlStream, XSDLocation, null);
    }

    @SuppressWarnings("unchecked")
    public T loadObjectFromXMLAndValidate(final InputStream xmlStream,
                                          final String xsdBaseFileName,
                                          final String pathName) {
        if (pathName != null && !new File(pathName).exists()) {
            String msg = errorWarningMessages.formatMessage(81080,
                    ERROR_MESSAGE_81080, this.getClass(), pathName);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
        }
        //assert(fileName != null) : "Filename to validate needs to be set.";


        ValidationEventHandler veh = new JodiValidationEventHandler(pathName);
        T object;

        Unmarshaller u = initializeJAXB(xsdBaseFileName);
        try {
            u.setEventHandler(veh);
            object = (T) u.unmarshal(xmlStream);
        } catch (JAXBException je) {
            logger.error(String.format("Error processing file '%1$s'.", pathName), je);
            String friendlyMessage;
            if (je.getMessage() != null) {
                friendlyMessage = je.getMessage();
            } else if (je.getLinkedException().getMessage() != null) {
                friendlyMessage = je.getLinkedException().getMessage();
            } else {
                friendlyMessage = "Unknown cause";
            }
            String msg;
            if (pathName == null) {
                msg = errorWarningMessages.formatMessage(81020,
                        ERROR_MESSAGE_81020, this.getClass(), friendlyMessage);
            } else {
                msg = errorWarningMessages.formatMessage(81030,
                        ERROR_MESSAGE_81030, this.getClass(), pathName, friendlyMessage);
            }
            logger.error(msg, je);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg, je);
        } catch (RuntimeException ex) {
            String friendlyMessage = ex.getMessage() != null ? ex.getMessage()
                    : "Unknown cause";
            String msg;
            if (pathName == null) {
                msg = errorWarningMessages.formatMessage(81040,
                        ERROR_MESSAGE_81040, this.getClass(), friendlyMessage);
            } else {
                msg = errorWarningMessages.formatMessage(81050,
                        ERROR_MESSAGE_81050, this.getClass(), pathName, friendlyMessage);
            }
            logger.error(msg, ex);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg, ex);
        }
        return object;
    }

    private class JodiValidationEventHandler implements ValidationEventHandler {

        private final String fileName;

        public JodiValidationEventHandler(final String fileName) {
            super();
            this.fileName = fileName;
        }

        @Override
        public boolean handleEvent(ValidationEvent event) {
            int severity = event.getSeverity();
            if (severity == ValidationEvent.WARNING) {
                String msg = errorWarningMessages.formatMessage(81070,
                        ERROR_MESSAGE_81070, this.getClass(),
                        event.getLocator().getLineNumber(),
                        fileName, event.getMessage());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.WARNINGS);
                return true;
            }

            if (severity == ValidationEvent.ERROR ||
                    severity == ValidationEvent.FATAL_ERROR) {
                String msg = errorWarningMessages.formatMessage(81010,
                        ERROR_MESSAGE_81010, this.getClass(),
                        event.getLocator().getLineNumber(),
                        fileName, event.getMessage());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                return false;
            }
            return true;
        }
    }

}
