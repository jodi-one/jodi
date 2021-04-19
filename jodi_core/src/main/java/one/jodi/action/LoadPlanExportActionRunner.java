package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.etl.service.loadplan.LoadPlanExportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Actionrunner to import loadplans from ODI into an external model,
 * textual specification.
 */
public class LoadPlanExportActionRunner implements ActionRunner {

    private static final Logger logger = LogManager.getLogger(LoadPlanExportActionRunner.class);
    private final LoadPlanExportService loadPlanExportService;

    @Inject
    public LoadPlanExportActionRunner(final LoadPlanExportService loadPlanImportService) {
        this.loadPlanExportService = loadPlanImportService;
    }

    @Override
    public void run(RunConfig config) {
        logger.info("Load plan import started.");
        validateRunConfig(config);
        logger.info("Load plan validation finished.");
        this.loadPlanExportService.exportLoadPlans(((EtlRunConfig) config).isUsingDefaultscenarioNames());
        logger.info("Load plan export finished.");
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (config.getPropertyFile() == null || !new File(config.getPropertyFile()).exists()) {
            String msg = "Properties file should be specified and exist.";
            throw new UsageException(msg);
        }
        if (config.getMetadataDirectory() == null || !new File(config.getMetadataDirectory()).exists()) {
            String msg = "Metadata directory should be specified and exist.";
            throw new UsageException(msg);
        }
    }
}
