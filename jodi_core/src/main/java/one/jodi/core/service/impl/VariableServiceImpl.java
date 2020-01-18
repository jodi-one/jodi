package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.util.CollectXmlObjectsUtil;
import one.jodi.base.util.CollectXmlObjectsUtilImpl;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.ValidationException;
import one.jodi.core.service.VariableService;
import one.jodi.core.service.VariableServiceException;
import one.jodi.core.validation.variables.VariableValidator;
import one.jodi.core.variables.ObjectFactory;
import one.jodi.core.variables.Variables;
import one.jodi.etl.builder.VariableEnrichmentBuilder;
import one.jodi.etl.builder.VariableTransformationBuilder;
import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.service.variables.VariableServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class VariableServiceImpl implements VariableService {

    private final static Logger logger = LogManager.getLogger(VariableServiceImpl.class);

    private final static String VARIABLES_FILE = "Variables";

    private final static String ERROR_MESSAGE_6001 =
            "Exception in serializing sequences details %s.";
    private final static String ERROR_MESSAGE_6002 = "Can't delete %s in directory %s.";

    private final VariableServiceProvider serviceProvider;
    private final VariableEnrichmentBuilder enrichmentService;
    private final VariableTransformationBuilder transformationService;
    private final VariableValidator validator;
    private final FileCollector fileCollector;
    private final JodiProperties jodiProperties;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private CollectXmlObjectsUtil<Variables> collectXMLObjectUtil;

    @Inject
    public VariableServiceImpl(final VariableServiceProvider serviceProvider,
                               final VariableEnrichmentBuilder enrichmentBuilder,
                               final VariableTransformationBuilder transformationBuilder,
                               final VariableValidator validator,
                               final JodiProperties jodiProperties,
                               final FileCollector fileCollector,
                               final ErrorWarningMessageJodi errorWarningMessages) {
        this.enrichmentService = enrichmentBuilder;
        this.transformationService = transformationBuilder;
        this.validator = validator;
        this.jodiProperties = jodiProperties;
        this.fileCollector = fileCollector;
        this.errorWarningMessages = errorWarningMessages;
        this.serviceProvider = serviceProvider;

        this.collectXMLObjectUtil =
                new CollectXmlObjectsUtilImpl<>(ObjectFactory.class,
                        JodiConstants.XSD_FILE_VARIABLES,
                        errorWarningMessages);
    }

    private void createVariables(final Variables variables) {
        one.jodi.etl.internalmodel.Variables internalVariables =
                transformationService.transmute(variables);
        this.enrichmentService.enrich(internalVariables);
        if (this.validator.validate(internalVariables)) {
            internalVariables.getVariables()
                    .forEach(iV -> {
                        this.serviceProvider
                                .create(iV, jodiProperties.getProjectCode(),
                                        DATE_FORMAT);
                    });
        }
    }

    @Override
    public void create(final String metadataDirectory) {
        Path path = Paths.get(metadataDirectory);
        final List<Path> files =
                fileCollector.collectInPath(path, VARIABLES_FILE + "-", ".xml",
                        VARIABLES_FILE + ".xml");
        this.collectXMLObjectUtil.collectObjectsFromFiles(files)
                .values()
                .stream()
                .forEach(v -> createVariables(v));
    }

    private void serialize(final String metadataDirectory, final String variablesFile,
                           final Variables allVariables) {
        try {
            File file = new File(metadataDirectory, variablesFile);
            if (file.exists() && !file.delete()) {
                String message = this.errorWarningMessages.formatMessage(6002,
                        ERROR_MESSAGE_6002, this.getClass(),
                        variablesFile, metadataDirectory);

                errorWarningMessages.addMessage(message,
                        ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                logger.error(message);
                throw new VariableServiceException(message);
            }

            // add temporary target directory if it does not exist
            File targetFolder = new File(metadataDirectory);
            if (!targetFolder.exists()) {
                targetFolder.mkdir();
            }

            JAXBContext jaxbContext =
                    JAXBContext.newInstance(Variables.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(allVariables, file);
            logger.info("Wrote variables to file : (" + file.getAbsolutePath() + ")");
        } catch (JAXBException e) {
            String message = this.errorWarningMessages.formatMessage(6001,
                    ERROR_MESSAGE_6001, this.getClass(),
                    e.getMessage() != null ? e.getMessage()
                            : "Details not provided");
            errorWarningMessages.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(message, e);
            throw new ValidationException(message);
        }
    }

    @Override
    public void export(final String metadataDirectory) {
        one.jodi.etl.internalmodel.Variables internalVariables =
                this.serviceProvider.findAll();
        enrichmentService.enrich(internalVariables);
        if (this.validator.validate(internalVariables)) {
            Variables allExternalVariables =
                    this.transformationService.transmute(internalVariables);
            serialize(metadataDirectory, VARIABLES_FILE + ".xml", allExternalVariables);
        }
    }

    private void delete(Variable internalVariable) {
        this.serviceProvider.delete(internalVariable, jodiProperties.getProjectCode());
    }

    private void doDelete(one.jodi.etl.internalmodel.Variables internalVariables) {
        internalVariables.getVariables().forEach(iv -> delete(iv));
    }

    @Override
    public void delete(String metadataDirectory) {
        Path path = Paths.get(metadataDirectory);
        List<Path> files = fileCollector.collectInPath(path, VARIABLES_FILE + "-", ".xml",
                VARIABLES_FILE + ".xml");
        this.collectXMLObjectUtil
                .collectObjectsFromFiles(files)
                .values()
                .stream()
                .map(v -> this.transformationService.transmute(v))
                .forEach(i -> {
                    this.enrichmentService.enrich(i);
                    this.validator.validate(i);
                    doDelete(i);
                });
    }

}