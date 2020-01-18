package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.util.CollectXmlObjectsUtil;
import one.jodi.base.util.CollectXmlObjectsUtilImpl;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.procedure.ObjectFactory;
import one.jodi.core.procedure.Procedure;
import one.jodi.core.service.ProcedureException;
import one.jodi.core.service.ProcedureService;
import one.jodi.etl.builder.ProcedureTransformationBuilder;
import one.jodi.etl.internalmodel.procedure.ProcedureHeader;
import one.jodi.etl.internalmodel.procedure.ProcedureInternal;
import one.jodi.etl.service.procedure.ProcedureServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcedureServiceImpl implements ProcedureService {

    private final static Logger logger =
            LogManager.getLogger(ProcedureServiceImpl.class);

    private final static String PROCEDURE_FILE_NAME = "Procedure";

    private final static String ERROR_MESSAGE_61010 =
            "Exception while %1$s procedure definitions from file: %2$s. " +
                    "The following procedures were not processed: %3$s.";

    private final String projectCode;
    private final FileCollector fileCollector;
    private final ProcedureTransformationBuilder procTransformationBuilder;
    private final ProcedureServiceProvider procServiceProvider;
    private final ErrorWarningMessageJodi errorWarningMessageJodi;

    private CollectXmlObjectsUtil<Procedure> collectXMLObjectUtil;

    @Inject
    ProcedureServiceImpl(final FileCollector fileCollector,
                         final JodiProperties jodiProperties,
                         final ProcedureTransformationBuilder procTransformationBuilder,
                         final ProcedureServiceProvider procServiceProvider,
                         final ErrorWarningMessageJodi errorWarningMessageJodi) {
        this.fileCollector = fileCollector;
        this.projectCode = jodiProperties.getProjectCode();
        this.procTransformationBuilder = procTransformationBuilder;
        this.procServiceProvider = procServiceProvider;
        this.errorWarningMessageJodi = errorWarningMessageJodi;
        this.collectXMLObjectUtil = new CollectXmlObjectsUtilImpl<>(
                ObjectFactory.class,
                JodiConstants.XSD_FILE_PROCEDURE,
                errorWarningMessageJodi);
    }

    /**
     * Turns an Optional<T> into a Stream<T> of length zero or one depending upon
     * whether a value is present.
     */
    private static <T> Stream<T> toStream(final Optional<T> opt) {
        if (opt.isPresent()) {
            return Stream.of(opt.get());
        } else {
            return Stream.empty();
        }
    }

    // for test purposes only
    protected void setCollectXmlObjectUtil(
            final CollectXmlObjectsUtil<Procedure> collectXMLObjectUtil) {
        this.collectXMLObjectUtil = collectXMLObjectUtil;
    }

    private List<ProcedureInternal> enrichAndValidate(
            final Map<Path, Procedure> procedures) {
        return procedures.entrySet()
                .stream()
                .flatMap(e -> toStream(this.procTransformationBuilder
                        .build(e.getValue(),
                                e.getKey().toString())))
                .collect(Collectors.toList());
    }

    private void createProcedures(final List<ProcedureInternal> internalProcedures,
                                  final boolean generateScenarios) {
        this.procServiceProvider.createProcedures(internalProcedures, generateScenarios,
                this.projectCode);
    }

    private String getProcedureNames(final List<ProcedureHeader> procedures) {
        return procedures.stream()
                .map(p -> p.getName() + " -> " +
                        String.join("/", p.getFolderNames()))
                .collect(Collectors.joining(", "));
    }

    @Override
    public void create(final String metadataDirectory, final boolean generateScenarios) {
        Path path = Paths.get(metadataDirectory);
        final List<Path> files =
                fileCollector.collectInPath(path, PROCEDURE_FILE_NAME + "-", ".xml",
                        PROCEDURE_FILE_NAME + ".xml");
        Map<Path, Procedure> procedures = this.collectXMLObjectUtil
                .collectObjectsFromFiles(files);
        // this list will contain only those procedure definitions that are valid
        List<ProcedureInternal> validProcedures = enrichAndValidate(procedures);
        if (!validProcedures.isEmpty()) {
            try {
                createProcedures(validProcedures, generateScenarios);
            } catch (ProcedureException pe) {
                String msg = this.errorWarningMessageJodi
                        .formatMessage(61010, ERROR_MESSAGE_61010,
                                this.getClass(), "creating",
                                pe.getMessage() != null
                                        ? pe.getMessage() : "",
                                getProcedureNames(pe.getProcedures()));
                errorWarningMessageJodi.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                        .ERRORS);
                logger.error(msg, pe);
            }
        }
    }

    private List<ProcedureHeader> enrichAndValidateHeader(
            final Map<Path, Procedure> procedures) {
        return procedures.entrySet()
                .stream()
                .flatMap(e -> toStream(this.procTransformationBuilder
                        .buildHeader(e.getValue(),
                                e.getKey().toString())))
                .collect(Collectors.toList());
    }

    private void deleteProcedures(final List<ProcedureHeader> internalProcedures) {
        this.procServiceProvider.deleteProcedures(internalProcedures, this.projectCode);
    }

    @Override
    public void delete(final String metadataDirectory) {
        Path path = Paths.get(metadataDirectory);
        final List<Path> files =
                fileCollector.collectInPath(path, PROCEDURE_FILE_NAME + "-", ".xml",
                        PROCEDURE_FILE_NAME + ".xml");
        Map<Path, Procedure> procedures =
                collectXMLObjectUtil.collectObjectsFromFiles(files);
        // this list will contain only those procedure definitions that are valid
        List<ProcedureHeader> validProcedures = enrichAndValidateHeader(procedures);
        if (!validProcedures.isEmpty()) {
            try {
                deleteProcedures(validProcedures);
            } catch (ProcedureException pe) {
                String msg = this.errorWarningMessageJodi
                        .formatMessage(61010, ERROR_MESSAGE_61010,
                                this.getClass(), "deleting",
                                pe.getMessage() != null
                                        ? pe.getMessage() : "",
                                getProcedureNames(pe.getProcedures()));
                errorWarningMessageJodi.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                        .ERRORS);
                logger.error(msg, pe);
            }
        }
    }

}
