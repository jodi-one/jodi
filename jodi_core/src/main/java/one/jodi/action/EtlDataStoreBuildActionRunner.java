package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.util.CollectXmlObjectsUtil;
import one.jodi.base.util.CollectXmlObjectsUtilImpl;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.ObjectFactory;
import one.jodi.core.model.Transformation;
import one.jodi.core.service.impl.TransformationFileVisitor;
import one.jodi.etl.service.EtlDataStoreBuildService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class EtlDataStoreBuildActionRunner implements ActionRunner {

    private final static String ERROR_MESSAGE_01050 = "The metadata directory is required to run Transformation creation";
    private final static String ERROR_MESSAGE_01051 = "The configuration property file is required to run Transformation creation";
    private final static String ERROR_MESSAGE_01052 = "The prefix configuration is required to run Transformation creation";
    private final EtlDataStoreBuildService dataStoreBuildService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final FileCollector fileCollector;
    private CollectXmlObjectsUtil<Transformation> transCollectUtil;

    @Inject
    public EtlDataStoreBuildActionRunner(final EtlDataStoreBuildService dataStoreBuildService,
                                         final JodiProperties jodiProperties, final FileCollector fileCollector,
                                         final ErrorWarningMessageJodi errorWarningMessages
    ) {
        this.errorWarningMessages = errorWarningMessages;
        this.dataStoreBuildService = dataStoreBuildService;
        this.transCollectUtil = new CollectXmlObjectsUtilImpl<Transformation, ObjectFactory>(
                ObjectFactory.class, jodiProperties.getInputSchemaLocation(),
                errorWarningMessages);
        this.fileCollector = fileCollector;
    }

    @Override
    public void run(RunConfig config) {
        validateRunConfig(config);
        List<Path> paths = this.fileCollector.collectInPath(Paths.get(config.getMetadataDirectory()),
                new TransformationFileVisitor(this.errorWarningMessages));
        Map<Path, Transformation> transformations = transCollectUtil.collectObjectsFromFiles(paths);
        transformations.values().forEach(t -> dataStoreBuildService.build(t));
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        if (!StringUtils.hasLength(etlConfig.getPrefix())) {
            String msg = errorWarningMessages.formatMessage(1052,
                    ERROR_MESSAGE_01052, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getMetadataDirectory())) {
            String msg = errorWarningMessages.formatMessage(1050,
                    ERROR_MESSAGE_01050, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(1051,
                    ERROR_MESSAGE_01051, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

}
