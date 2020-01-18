package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.StringUtils;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.service.PackageService;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteAllPackagesActionRunner implements ActionRunner {

    private final static Logger logger =
            LogManager.getLogger(DeleteAllPackagesActionRunner.class);

    private final static String ERROR_MESSAGE_01053 =
            "The configuration property metadata directory is required to run the command" +
                    "'Delete All Packages'.";
    private final static String ERROR_MESSAGE_01504 =
            "The Jodi properties file sets jodi.update to true. " +
                    "Packages will be truncated and not deleted.";
    private final static String ERROR_MESSAGE_00250 = "Parse error in file 0.xml %s";
    private final static String ERROR_MESSAGE_00260 =
            "Unknown error processing file 0.xml %s";

    private final PackageService packageService;
    private final MetadataServiceProvider metadataProvider;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final JodiProperties properties;

    /**
     * Creates a new DeleteTransformationsActionRunner instance.
     *
     * @param packageService
     */
    @Inject
    protected DeleteAllPackagesActionRunner(
            final PackageService packageService,
            final MetadataServiceProvider metadataProvider,
            final ErrorWarningMessageJodi errorWarningMessages,
            final JodiProperties properties) {
        this.packageService = packageService;
        this.metadataProvider = metadataProvider;
        this.errorWarningMessages = errorWarningMessages;
        this.properties = properties;
    }

    @Override
    public void run(final RunConfig config) {
        final EtlRunConfig etlConfig = (EtlRunConfig) config;
        final List<ETLPackageHeader> packageHeaders;
        try {
            packageHeaders = metadataProvider.getPackageHeaders(etlConfig.isJournalized());
            // clone before reversing header list
            // delete root packages before child folder packages
            List<ETLPackageHeader> revPackageHeaders = new ArrayList<>(packageHeaders);
            Collections.reverse(revPackageHeaders);
            if (properties.isUpdateable()) {
                String msg = errorWarningMessages.formatMessage(1054, ERROR_MESSAGE_01504,
                        this.getClass());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.WARNINGS);
            }
            packageService.deletePackages(revPackageHeaders, true);
        } catch (RuntimeException re) {
            String msg = "";
            if ((re.getCause() != null) && (re.getCause() instanceof JAXBException)) {
                msg = errorWarningMessages.formatMessage(250, ERROR_MESSAGE_00250,
                        this.getClass(), re.getCause());
            } else {
                msg = errorWarningMessages.formatMessage(260, ERROR_MESSAGE_00260,
                        this.getClass(), re);
            }
            logger.error(msg, re);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
        }
    }

    /* (non-Javadoc)
     * @see one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getMetadataDirectory())) {
            String msg = errorWarningMessages.formatMessage(1053,
                    ERROR_MESSAGE_01053, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

}
