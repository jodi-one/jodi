package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.CreateEtlsImpl;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.core.annotations.JournalizedData;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.service.ScenarioService;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.logging.ErrorReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import java.util.List;

public class CreateScenariosActionRunner implements ActionRunner {

    private final static Logger logger =
            LogManager.getLogger(CreateScenariosActionRunner.class);

    private final ScenarioService scenarioService;
    private final MetadataServiceProvider metadataProvider;
    private final Boolean journalized;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public CreateScenariosActionRunner(final ScenarioService scenarioService,
                                       final MetadataServiceProvider metadataProvider,
                                       final @JournalizedData String journalized,
                                       final ErrorWarningMessageJodi errorWarningMessages) {
        this.scenarioService = scenarioService;
        this.metadataProvider = metadataProvider;
        this.journalized = Boolean.valueOf(journalized);
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public void run(final RunConfig config) {
        List<ETLPackageHeader> packageHeaders;
        try {
            packageHeaders = metadataProvider.getPackageHeaders(journalized);
        } catch (RuntimeException re) {
            String msg = "";
            if ((re.getCause() != null) && (re.getCause() instanceof JAXBException)) {
                msg = errorWarningMessages.formatMessage(250,
                        CreateEtlsImpl.ERROR_MESSAGE_00250, this.getClass(), re.getCause());
            } else {
                msg = errorWarningMessages.formatMessage(260,
                        CreateEtlsImpl.ERROR_MESSAGE_00260, this.getClass(), re);
            }
            ErrorReport.addErrorLine(0, msg);
            logger.error(msg, re);
            throw new UnRecoverableException(msg, re);
        }
        this.scenarioService.deleteScenario(packageHeaders);
        this.scenarioService.generateAllScenarios(packageHeaders, metadataProvider.getInternaTransformations());
    }

    @Override
    public void validateRunConfig(final RunConfig config) throws UsageException {
        // purposely empty
    }

}
