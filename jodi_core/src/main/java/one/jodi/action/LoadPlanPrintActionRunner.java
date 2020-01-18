package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.etl.service.loadplan.LoadPlanExportService;

import java.io.File;

public class LoadPlanPrintActionRunner implements ActionRunner {

    private final LoadPlanExportService loadPlanImportService;

    @Inject
    public LoadPlanPrintActionRunner(final LoadPlanExportService loadPlanImportService) {
        this.loadPlanImportService = loadPlanImportService;
    }

    @Override
    public void run(RunConfig config) {
        validateRunConfig(config);
        final boolean useDefaultNames = true;
        this.loadPlanImportService.printLoadPlans(useDefaultNames);
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