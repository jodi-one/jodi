package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.util.CollectXmlObjectsUtil;
import one.jodi.base.util.CollectXmlObjectsUtilImpl;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.constraints.ConstraintType;
import one.jodi.core.constraints.Constraints;
import one.jodi.core.constraints.ObjectFactory;
import one.jodi.core.service.ConstraintService;
import one.jodi.etl.internalmodel.Constraint;
import one.jodi.etl.service.constraints.ConstraintEnrichmentService;
import one.jodi.etl.service.constraints.ConstraintServiceProvider;
import one.jodi.etl.service.constraints.ConstraintTransformationService;
import one.jodi.etl.service.constraints.ConstraintValidationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConstraintServiceImpl implements ConstraintService {

    private static final Logger logger = LogManager.getLogger(ConstraintServiceImpl.class);

    private static final String CONSTRAINT_FILE = "Constraints";

    private static final String ERROR_MESSAGE_8001 =
            "Exception in serializing constraints.";
    private static final String ERROR_MESSAGE_8002 = "Can't delete %s in directory %s.";
    private static final String ERROR_MESSAGE_8003 =
            "No Constraint files were discovered in metadata directory; files must be " +
                    "named either Constraints.xml or Constraints-*.xml";

    private final ConstraintServiceProvider serviceProvider;
    private final ConstraintTransformationService transformationService;
    private final ConstraintEnrichmentService enrichingService;
    private final ConstraintValidationService validationService;
    private final FileCollector fileCollector;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private CollectXmlObjectsUtil<Constraints> collectXMLObjectUtil;

    @Inject
    public ConstraintServiceImpl(final FileCollector fileCollector,
                                 final ErrorWarningMessageJodi errorWarningMessages,
                                 final ConstraintTransformationService transformationService,
                                 final ConstraintServiceProvider serviceProvider,
                                 final ConstraintEnrichmentService enrichingService,
                                 final ConstraintValidationService validationService,
                                 final JodiProperties properties) {

        this.fileCollector = fileCollector;
        this.errorWarningMessages = errorWarningMessages;
        this.serviceProvider = serviceProvider;
        this.transformationService = transformationService;
        this.enrichingService = enrichingService;
        this.validationService = validationService;
        this.collectXMLObjectUtil =
                new CollectXmlObjectsUtilImpl<>(ObjectFactory.class,
                        JodiConstants.XSD_FILE_CONSTRAINTS,
                        errorWarningMessages);
    }

    private Map<Path, List<ConstraintType>> collectConstraints(final List<Path> files) {
        return this.collectXMLObjectUtil
                .collectObjectsFromFiles(files)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(),
                        e -> e.getValue()
                                .getConstraint()
                                .stream()
                                .map(jaxb -> jaxb.getValue())
                                .collect(Collectors.toList())));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected void createConstraints(Map<Path, List<ConstraintType>> constraints) {
        List<Constraint> internalConstraints =
                constraints.entrySet()
                        .stream()
                        .flatMap(entry -> {
                            return entry.getValue()
                                    .stream()
                                    .map(c -> new AbstractMap.SimpleImmutableEntry<>(
                                            entry.getKey().toString(),
                                            c))
                                    .collect(Collectors.toList()).stream();
                        }).map(entry -> transformationService.transform(entry.getKey(),
                        entry.getValue()))
                        .peek(enrichingService::enrich)
                        .collect(Collectors.toList());
        if (validationService.validate(internalConstraints)) {
            internalConstraints.forEach(serviceProvider::create);
        } else {
            logger.warn("Constraints not valid");
        }
    }

    @Override
    public void create(String metadataDirectory) {
        Path path = Paths.get(metadataDirectory);
        final List<Path> files =
                fileCollector.collectInPath(path, CONSTRAINT_FILE + "-", ".xml",
                        CONSTRAINT_FILE + ".xml");
        if (files.size() == 0) {
            logger.debug(ERROR_MESSAGE_8003);
            return;
        }

        files.forEach(f -> {
            logger.info("Constraints created from (" + f.toFile().getAbsolutePath() + ")");
        });

        Map<Path, List<ConstraintType>> constraints = collectConstraints(files);
        createConstraints(constraints);
    }

    @Override
    public void export(String metadataDirectory, boolean exportDefinedInDatabase) {

        List<Constraint> constraints = serviceProvider.findAll().stream().filter(c -> {
            return exportDefinedInDatabase || !c.isDefinedInDatabase();
        }).collect(Collectors.toList());

        constraints.forEach(enrichingService::reduce);
        Constraints constraint = transformationService.transform(constraints);

        serialize(metadataDirectory, CONSTRAINT_FILE + ".xml", constraint);

    }

    private void serialize(final String metadataDirectory, final String filename,
                           final Constraints constraint) {
        try {
            File file = new File(metadataDirectory, filename);
            if (file.exists() && !file.delete()) {
                String message =
                        this.errorWarningMessages.formatMessage(8002, ERROR_MESSAGE_8002,
                                this.getClass(), filename,
                                metadataDirectory);
                logger.error(message);
                errorWarningMessages.addMessage(message,
                        ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                throw new RuntimeException(message);
            }
            JAXBContext jaxbContext =
                    JAXBContext.newInstance(Constraints.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty("jaxb.noNamespaceSchemaLocation",
                    "jodi-constraints.v1.0.xsd");

            jaxbMarshaller.marshal(constraint, file);
            logger.info("Constraints exported to : (" + file.getAbsolutePath() + ")");
        } catch (JAXBException e) {
            String message =
                    this.errorWarningMessages.formatMessage(8001, ERROR_MESSAGE_8001,
                            this.getClass());
            logger.error(message, e);
            errorWarningMessages.addMessage(message,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(message);
        }
    }

    @Override
    public void delete(final String metadataDirectory) {
        List<Path> files =
                fileCollector.collectInPath(Paths.get(metadataDirectory),
                        CONSTRAINT_FILE + "-", ".xml",
                        CONSTRAINT_FILE + ".xml");
        if (files.size() == 0) {
            logger.debug(ERROR_MESSAGE_8003);
        }
        Map<Path, List<ConstraintType>> constraints = collectConstraints(files);

        files.forEach(f -> {
            logger.info("Deleting constraints from (" + f.toFile().getAbsolutePath() + ")");
        });

        List<Constraint> internalConstraints =
                constraints.entrySet().stream().flatMap(entry -> {
                    return entry.getValue().stream()
                            .map(c -> new AbstractMap.SimpleImmutableEntry<>(
                                    entry.getKey().toString(),
                                    c))
                            .collect(Collectors.toList()).stream();
                }).map(entry -> transformationService.transform(entry.getKey(),
                        entry.getValue()))
                        .peek(enrichingService::enrich).collect(Collectors.toList());
        validationService.validate(internalConstraints);
        internalConstraints.forEach(serviceProvider::delete);
    }

}
