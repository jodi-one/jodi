package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.Version;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.service.MetadataServiceProvider.TransformationMetadataHandler;
import one.jodi.core.service.ValidationService;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.logging.ErrorReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationServiceImpl implements ValidationService {
    private static final String newLine = System.getProperty("line.separator");
    private static final String ERROR_MESSAGE_03270 = JodiConstants.VERSION_HEADER;
    private static final String ERROR_MESSAGE_03310 = JodiConstants.ERROR_FOOTER;
    private static final Logger logger = LogManager.getLogger(ValidationServiceImpl.class);
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final EnrichingBuilder enrichingBuilder;
    private final MetadataServiceProvider metadataProvider;

    @Inject
    public ValidationServiceImpl(final ErrorWarningMessageJodi errorWarningMessage,
                                 final EnrichingBuilder enrichingBuilder,
                                 final MetadataServiceProvider metadataProvider) {
        this.errorWarningMessages = errorWarningMessage;
        this.enrichingBuilder = enrichingBuilder;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public void validate(final boolean journalized) {
        logger.debug("Journalized: " + journalized);
//		validateProjectExists();
//		validateJKM();
        ErrorReport.reset();
        metadataProvider.provideTransformationMetadata(new TransformationMetadataHandler() {
            @Override
            public void handleTransformationASC(final Transformation transformation, final int packageSequence) {
                logger.debug("Journalized: " + journalized);
                enrichingBuilder.enrich(transformation, journalized);

            }

            @Override
            public void handleTransformationDESC(final Transformation transformation) {

            }

            @Override
            public void preDESC() {
            }

            @Override
            public void postDESC() {
            }

            @Override
            public void preASC() {
                logger.info("Validation of transformations: started");
            }

            @Override
            public void postASC() {
                logger.info("Validation of transformations: completed");
            }

            @Override
            public void pre() {
            }

            @Override
            public void post() {
            }

            @Override
            public void handleTransformation(final Transformation transformation) {
            }
        });

        if (ErrorReport.getErrorReport()
                       .length() > 1) {
            String msg = errorWarningMessages.formatMessage(3270, ERROR_MESSAGE_03270, this.getClass(),
                                                            Version.getProductVersion());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);

            String errorReport = ErrorReport.getErrorReport()
                                            .toString();
            logger.debug("------- ErrorReport:" + newLine + errorReport + newLine + "------- End ErrorReport");
            ErrorReport.reset();

            throw new UnRecoverableException(
                    errorWarningMessages.formatMessage(3310, ERROR_MESSAGE_03310 + "\n" + errorReport,
                                                       this.getClass()));
        }
        ErrorReport.reset();
    }

}
