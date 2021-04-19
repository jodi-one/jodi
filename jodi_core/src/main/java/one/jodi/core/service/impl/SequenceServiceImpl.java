package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.util.CollectXmlObjectsUtil;
import one.jodi.base.util.CollectXmlObjectsUtilImpl;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.sequences.ObjectFactory;
import one.jodi.core.sequences.Sequences;
import one.jodi.core.service.SequenceService;
import one.jodi.core.service.SequenceServiceException;
import one.jodi.core.validation.sequences.SequenceValidator;
import one.jodi.etl.builder.SequenceEnrichmentBuilder;
import one.jodi.etl.builder.SequenceTransformationBuilder;
import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.service.sequences.SequenceServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SequenceServiceImpl implements SequenceService {

    private static final Logger logger = LogManager.getLogger(SequenceServiceImpl.class);

    private static final String SEQUENCES_FILE = "Sequences";

    private static final String ERROR_MESSAGE_5001 =
            "Exception in serializing sequences details %s.";
    private static final String ERROR_MESSAGE_5002 = "Can't delete %s in directory %s.";

    private final SequenceServiceProvider serviceProvider;
    private final SequenceEnrichmentBuilder enrichmentService;
    private final SequenceTransformationBuilder transformationService;
    private final SequenceValidator validator;
    private final ErrorWarningMessageJodi errorWarningMessageJodi;
    private final FileCollector fileCollector;

    private CollectXmlObjectsUtil<Sequences> collectXMLObjectUtil;

    @Inject
    public SequenceServiceImpl(final SequenceEnrichmentBuilder enrichmentBuilder,
                               final SequenceTransformationBuilder transformationBuilder,
                               final SequenceValidator validator,
                               final FileCollector fileCollector,
                               final ErrorWarningMessageJodi errorWarningMessages,
                               final SequenceServiceProvider serviceProvider) {
        this.enrichmentService = enrichmentBuilder;
        this.transformationService = transformationBuilder;
        this.validator = validator;
        this.fileCollector = fileCollector;
        this.errorWarningMessageJodi = errorWarningMessages;
        this.serviceProvider = serviceProvider;
        this.collectXMLObjectUtil =
                new CollectXmlObjectsUtilImpl<>(ObjectFactory.class,
                        JodiConstants.XSD_FILE_SEQUENCES,
                        errorWarningMessages);
    }

    @Override
    public void create(String metadataDirectory) {
        Path path = Paths.get(metadataDirectory);
        final List<Path> files =
                this.fileCollector.collectInPath(path, SEQUENCES_FILE + "-", ".xml",
                        SEQUENCES_FILE + ".xml");
        this.collectXMLObjectUtil
                .collectObjectsFromFiles(files)
                .values()
                .stream()
                .forEach(s -> createSequences(s));
    }

    protected void createSequences(Sequences sequences) {
        one.jodi.etl.internalmodel.Sequences internalSequences =
                transformationService.transmute(sequences);
        this.enrichmentService.enrich(internalSequences);
        if (this.validator.validate(internalSequences)) {
            internalSequences.getSequences().stream()
                    .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
                    .forEach(iV -> {
                        this.serviceProvider.create(iV);
                    });
        }
    }

    @Override
    public void export(String metadataDirectory) {
        one.jodi.etl.internalmodel.Sequences internalSequences =
                this.serviceProvider.findAll();
        this.enrichmentService.enrich(internalSequences);
        if (this.validator.validate(internalSequences)) {
            Sequences externalSequences =
                    this.transformationService.transmute(internalSequences);
            serialize(metadataDirectory, SEQUENCES_FILE + ".xml", externalSequences);
        } else {
            logger.warn("Sequences not exported since they are not valid.");
        }
    }

    private void serialize(String metadataDirectory, String sequencesFile,
                           Sequences externalSequences) {
        try {
            File file = new File(metadataDirectory, sequencesFile);
            if (file.exists() && !file.delete()) {
                String message =
                        this.errorWarningMessageJodi.formatMessage(5002, ERROR_MESSAGE_5002,
                                this.getClass(),
                                sequencesFile,
                                metadataDirectory);
                errorWarningMessageJodi.addMessage(message,
                        ErrorWarningMessageJodi.MESSAGE_TYPE
                                .ERRORS);
                logger.error(message);
                throw new SequenceServiceException(message);
            }
            JAXBContext jaxbContext =
                    JAXBContext.newInstance(Sequences.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(externalSequences, file);
            logger.info("Sequences exported to : (" + file.getAbsolutePath() + ")");
        } catch (JAXBException e) {
            String msg =
                    this.errorWarningMessageJodi.formatMessage(5001, ERROR_MESSAGE_5001,
                            this.getClass(),
                            e.getMessage() != null ? e.getMessage()
                                    : "Details not provided");
            errorWarningMessageJodi.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg, e);
            throw new SequenceServiceException(msg);
        }
    }

    @Override
    public void delete(final String metadataDirectory) {
        List<Path> files = this.fileCollector.collectInPath(Paths.get(metadataDirectory),
                SEQUENCES_FILE + "-", ".xml",
                SEQUENCES_FILE + ".xml");
        this.collectXMLObjectUtil
                .collectObjectsFromFiles(files)
                .values()
                .stream()
                .map(s -> this.transformationService.transmute(s))
                .forEach(i -> {
                    this.enrichmentService.enrich(i);
                    this.validator.validate(i);
                    doDelete(i);
                });
    }

    private void doDelete(one.jodi.etl.internalmodel.Sequences internalSequences) {
        internalSequences.getSequences().stream()
                .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
                .forEach(s -> delete(s));
    }

    private void delete(Sequence sequence) {
        this.serviceProvider.delete(sequence);
    }

}
