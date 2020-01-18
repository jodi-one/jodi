package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.etl.service.repository.OdiRepositoryExportImportException;
import one.jodi.etl.service.repository.OdiRepositoryExportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static one.jodi.action.ImportServiceActionRunner.EXPORTMPORTDIR;

public class ExportServiceActionRunner implements ActionRunner {

    private final OdiRepositoryExportService exportService;
    private final Logger logger = LogManager.getLogger(ExportServiceActionRunner.class);

    @Inject
    protected ExportServiceActionRunner(final OdiRepositoryExportService exportService) {
        this.exportService = exportService;
    }

    @Override
    public void run(final RunConfig config) {
        try {
            String metaDataDir = config.getMetadataDirectory();
            validate(metaDataDir);
            final File parent;
            if (new File(metaDataDir).getParent() == null) {
                parent = new File("..");
            } else {
                parent = new File(metaDataDir).getParentFile();
            }
            validate(parent.getAbsolutePath());
            File exportImport = new File(parent.getAbsolutePath(), EXPORTMPORTDIR);
            if (!exportImport.exists()) {
                if (!exportImport.mkdir()) {
                    String message = "Can't create directory " + exportImport.getAbsolutePath();
                    logger.error(message);
                    throw new OdiRepositoryExportImportException(message);
                }
            }
            exportService.doExport(exportImport.getAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void validate(String metaDataDir) {
        if (!new File(metaDataDir).exists()) {
            String msg = "MetaDataDir (-m) not found.";
            throw new UsageException(msg);
        }
        if (!new File(metaDataDir).isDirectory()) {
            String msg = "MetaDataDir (-m) does not exist.";
            throw new UsageException(msg);
        }
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = "Properties file not found.";
            throw new UsageException(msg);
        }
//	if (!StringUtils.hasLength( ((EtlRunConfig) config).getDeployementArchivePassword())) {
//        String msg = "DeploymentArchivePassword (-dapwd) must be specified on the command line.";
//        throw new UsageException(msg);
//    }
    }

}