package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.config.ConfigurationException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.XMLParserUtil;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.ObjectFactory;
import one.jodi.core.model.Transformation;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of the MetadataServiceProvider that supports streaming XML
 * by allowing an implementation of the XMLStreamProvider interface to
 * dynamically provide the XML streams.
 *
 */
public class StreamingXMLMetadataServiceProvider implements MetadataServiceProvider {

    private final static Logger logger =
            LogManager.getLogger(StreamingXMLMetadataServiceProvider.class);

    private final static String ERROR_MESSAGE_02060 = "NotImplementedException";
    private final static String ERROR_MESSAGE_02070 =
            "Error parsing transformation from stream %s";

    private final XMLStreamProvider streamProvider;
    private final TransformationBuilder transformationBuilder;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new StreamingXMLMetadataServiceProvider instance.
     *
     * @param streamProvider the stream provider
     * @param properties     the properties
     */
    @Inject
    public StreamingXMLMetadataServiceProvider(final XMLStreamProvider streamProvider,
                                               final JodiProperties properties,
                                               final TransformationBuilder transformationBuilder,
                                               final ErrorWarningMessageJodi errorWarningMessages) {
        this.properties = properties;
        this.streamProvider = streamProvider;
        this.transformationBuilder = transformationBuilder;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * Gets the eTL meta data. This API is not implemented on the
     * StreamingXMLMetadataProvider.
     *
     * @return the package meta data
     */
    @Override
    public List<ETLPackage> getPackages(final boolean journalized) {
        String msg = errorWarningMessages.formatMessage(2060, ERROR_MESSAGE_02060,
                this.getClass());
        errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                MESSAGE_TYPE.ERRORS);
        logger.error(msg);
        throw new NotImplementedException(msg);
    }

    /**
     * Unmarshalls a transformation from each stream provided by the handler.
     *
     * @param handler the handler
     */
    @Override
    public void provideTransformationMetadata(final TransformationMetadataHandler handler) {
        XMLParserUtil<Transformation, ObjectFactory> parser =
                new XMLParserUtil<>(ObjectFactory.class,
                        JodiConstants.getEmbeddedXSDFileNames(),
                        errorWarningMessages);
        while (true) {
            try (InputStream stream = streamProvider.getNextTransformationStream()) {
                if (stream == null) {
                    break;
                }
                Transformation transformation = parseEntity(parser, stream);
                one.jodi.etl.internalmodel.Transformation internalTransformation =
                        transformationBuilder.transmute(transformation, 0);
                handler.handleTransformation(internalTransformation);
            } catch (ConfigurationException e) {
                String msg = errorWarningMessages.formatMessage(2070, ERROR_MESSAGE_02070,
                        this.getClass(), e);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                break;
            } catch (IOException e1) {
                String msg = errorWarningMessages.formatMessage(2070, ERROR_MESSAGE_02070,
                        this.getClass(), e1);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                break;
            }
        }
    }

    /**
     * Gets the transformation from the provided stream.
     *
     * @param xmlFile the xml stream containing the transformation.
     * @return the transformation parsed from the stream
     */
    public one.jodi.etl.internalmodel.Transformation getTransformation(
            final InputStream xmlFile,
            final int packageSequence) {
        XMLParserUtil<Transformation, ObjectFactory> parser =
                new XMLParserUtil<>(ObjectFactory.class,
                        JodiConstants.getEmbeddedXSDFileNames(),
                        errorWarningMessages);
        try {
            Transformation transformation = parseEntity(parser, xmlFile);
            return transformationBuilder.transmute(transformation, packageSequence);
        } catch (ConfigurationException e) {
            String msg = errorWarningMessages.formatMessage(2070, ERROR_MESSAGE_02070,
                    this.getClass(), e);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Parses an entity from the XML stream.
     *
     * @param parser the parser used to unmarshall the entity
     * @param stream the stream containing the entity
     * @return the parsed entity
     * @throws ConfigurationException
     */
    protected <T, O> T parseEntity(final XMLParserUtil<T, O> parser,
                                   final InputStream stream)
            throws ConfigurationException {
        return parser.loadObjectFromXMLAndValidate(stream,
                properties.getProperty("xml.xsd.interfaces"));
    }

    @Override
    public List<ETLPackageHeader> getPackageHeaders(boolean journalized) {
        String msg = errorWarningMessages.formatMessage(2060, ERROR_MESSAGE_02060,
                this.getClass());
        errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                MESSAGE_TYPE.ERRORS);
        logger.error(msg);
        throw new NotImplementedException(msg);
    }

    @Override
    public List<one.jodi.etl.internalmodel.Transformation> getInternaTransformations() {
        return Collections.EMPTY_LIST;
    }

    public interface XMLStreamProvider {

        /**
         * Closes the xml stream.
         *
         * @param stream the stream
         */
        void disposeXMLStream(InputStream stream);

        /**
         * Gets the eTL metadata stream.
         *
         * @return the eTL metadata stream
         */
        InputStream getETLMetadataStream();

        /**
         * Returns the next transformation stream. Returns null if no more streams available.
         *
         * @return the next transformation stream
         */
        InputStream getNextTransformationStream();
    }


}
