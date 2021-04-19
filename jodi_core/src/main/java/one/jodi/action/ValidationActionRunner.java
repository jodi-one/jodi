package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.service.ValidationService;

public class ValidationActionRunner implements ActionRunner {

    private static final String ERROR_MESSAGE_02050 = "The metadata directory is required to run Transformation creation";
    private static final String ERROR_MESSAGE_02051 = "The configuration property file is required to run Transformation creation";
    private static final String ERROR_MESSAGE_02052 = "The prefix configuration is required to run Transformation creation";
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final ValidationService validationService;

    @Inject
    public ValidationActionRunner(final ErrorWarningMessageJodi errorWarningMessages,
                                  final ValidationService validationService) {
        this.errorWarningMessages = errorWarningMessages;
        this.validationService = validationService;
    }

    @Override
    public void run(RunConfig config) {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        this.validationService.validate(etlConfig.isJournalized());
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        if (!StringUtils.hasLength(etlConfig.getPrefix())) {
            String msg = errorWarningMessages.formatMessage(2052, ERROR_MESSAGE_02052, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getMetadataDirectory())) {
            String msg = errorWarningMessages.formatMessage(2050, ERROR_MESSAGE_02050, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(2051, ERROR_MESSAGE_02051, this.getClass());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }

    }

}
