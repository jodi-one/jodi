package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.service.ExtractionTables;


/**
 * An {@link ActionRunner} implementation that invokes the {@link
 * TableServiceProvider#alterSCDTables()} method.
 */
public class ExtractTablesActionRunner implements ActionRunner {
    private final static String ERROR_MESSAGE_01010 = "Could not delete interfaces,\n"
            + "This could be due to incorrect jodi.properties where the jodi.properties are not in line with ODI,\n"
            + "e.g. check that the MODEL_CODE in ODI corresponds to the responding jodi.properties model.code,\n"
            + "or this could be due to an invalid XML file.";
    private final static String ERROR_MESSAGE_01063 = "Package Sequence is required as an integer to run Extract Tables";
    private final static String ERROR_MESSAGE_01064 = "The package sequence configuration is required to run Extract Tables";
    private final static String ERROR_MESSAGE_01065 = "The metadata directory is required to run Extract Tables";
    private final static String ERROR_MESSAGE_01066 = "The configuration property file is required to run Extract Tables";
    private final static String ERROR_MESSAGE_01067 = "The source Model is required to run Extract Tables";
    private final ExtractionTables extractTables;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new DeleteTransformationsActionRunner instance.
     *
     * @param extractTables the extract tables
     */
    @Inject
    protected ExtractTablesActionRunner(
            final ExtractionTables extractTables,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.extractTables = extractTables;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * @see one.jodi.bootstrap.RunConfig$ActionRunner#run(RunConfig)
     */
    @Override
    public void run(final RunConfig config) {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        try {
            extractTables.genExtractTables(config.getSourceModel(), config.getTargetModel(), Integer.parseInt(etlConfig.getPackageSequence()), config.getMetadataDirectory());
        } catch (Exception ex) {
            throw new UnRecoverableException(
                    errorWarningMessages.formatMessage(1010,
                            ERROR_MESSAGE_01010, this.getClass()), ex);
        }
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        if (!StringUtils.hasLength(etlConfig.getPackageSequence())) {
            try {
                Integer.parseInt(etlConfig.getPackageSequence());
            } catch (NumberFormatException nfe) {
                String msg = errorWarningMessages.formatMessage(1063,
                        ERROR_MESSAGE_01063, this.getClass());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new UsageException(msg);
            }
            String msg = errorWarningMessages.formatMessage(1064,
                    ERROR_MESSAGE_01064, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getMetadataDirectory())) {
            String msg = errorWarningMessages.formatMessage(1065,
                    ERROR_MESSAGE_01065, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(1066,
                    ERROR_MESSAGE_01066, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getSourceModel())) {
            String msg = errorWarningMessages.formatMessage(1067,
                    ERROR_MESSAGE_01067, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }
}
