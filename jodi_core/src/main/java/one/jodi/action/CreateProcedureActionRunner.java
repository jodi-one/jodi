package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.ProcedureService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateProcedureActionRunner implements ActionRunner {
    public final static String ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES =
            "odi12.generateScenariosForProcedures";
    private final static Logger logger =
            LogManager.getLogger(CreateProcedureActionRunner.class);
    private final boolean generateScenarioForProcedures;
    private final ProcedureService service;

    @Inject
    protected CreateProcedureActionRunner(final JodiProperties properties,
                                          final ProcedureService service) {
        this.service = service;
        if (properties.getPropertyKeys()
                .contains(ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES)) {
            this.generateScenarioForProcedures =
                    Boolean.valueOf(properties.getProperty(
                            ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES));
        } else {
            this.generateScenarioForProcedures = false;
        }
    }

    @Override
    public void run(final RunConfig config) {
        service.create(config.getMetadataDirectory(), this.generateScenarioForProcedures);
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = "Properties file not found.";
            logger.fatal(msg);
            throw new UsageException(msg);
        }
    }
}
