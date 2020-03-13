package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.ProjectService;
import one.jodi.core.service.ScenarioService;
import one.jodi.etl.service.loadplan.LoadPlanService;
import one.jodi.etl.service.repository.OdiRepositoryExportImportException;
import one.jodi.etl.service.repository.OdiRepositoryImportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class ImportServiceActionRunner implements ActionRunner {

    public final static String EXPORTMPORTDIR = "DeploymentService";
    private final OdiRepositoryImportService importService;
    private final Logger logger = LogManager.getLogger(ImportServiceActionRunner.class);
    private final ProjectService projectService;
    private final ScenarioService scenarioService;
    private final JodiProperties jodiProperties;
    private final LoadPlanService loadPlanService;

    @Inject
    protected ImportServiceActionRunner(final OdiRepositoryImportService importService,
                                        final ProjectService projectService,
                                        final ScenarioService scenarioService,
                                        final JodiProperties jodiProperties,
                                        final LoadPlanService loadPlanService
    ) {
        this.importService = importService;
        this.projectService = projectService;
        this.scenarioService = scenarioService;
        this.jodiProperties = jodiProperties;
        this.loadPlanService = loadPlanService;
    }

    @Override
    public void run(final RunConfig config) {
        try {
            File xmlDir = new File(config.getMetadataDirectory());
            if (xmlDir == null || !xmlDir.exists()) {
                logger.error("Config directory not found");
            }
            File parentFile = xmlDir.getParentFile();
            if (parentFile == null || !parentFile.exists()) {
                parentFile = new File(".");
                logger.error("Parent of Config directory not found");
            }
            String parentXmlDir = parentFile.getAbsolutePath();
            if (parentXmlDir == null) {
                logger.error("Parent of Config directory not found");
            }
            File exportImport = new File(parentXmlDir, EXPORTMPORTDIR);
            if (!exportImport.exists()) {
                if (!exportImport.mkdir()) {
                    String message = "Can't create directory " + exportImport.getAbsolutePath();
                    logger.error(message);
                    throw new OdiRepositoryExportImportException(message);
                }
            }
            final OdiRepositoryImportService.DA_TYPE pDAType;
            java.lang.String pFilename = exportImport.getAbsolutePath() + File.separator +
                    jodiProperties
                            .getProjectCode() + ".zip";
            if (pFilename.contains("PATCH_ER")) {
                pDAType = OdiRepositoryImportService.DA_TYPE.DA_PATCH_EXEC_REPOS;
            } else if (pFilename.contains("PATCH_DR")) {
                pDAType = OdiRepositoryImportService.DA_TYPE.DA_PATCH_DEV_REPOS;
            } else if (pFilename.contains("INITIAL")) {
                pDAType = OdiRepositoryImportService.DA_TYPE.DA_INITIAL;
            } else {
                throw new RuntimeException("Can't determine type of import");
            }
            if (pDAType.equals(OdiRepositoryImportService.DA_TYPE.DA_PATCH_EXEC_REPOS)) {
                scenarioService.deleteScenarios();
                loadPlanService.deleteLoadPlans();
            } else if (pDAType.equals(OdiRepositoryImportService.DA_TYPE.DA_PATCH_DEV_REPOS)) {
                scenarioService.deleteScenarios();
                loadPlanService.deleteLoadPlans();
                // TOODO move into own access strategy
                projectService.deleteProject();
            }
            importService.doImport(exportImport.getAbsolutePath(), pDAType);
        } catch (Exception ex) {
            logger.error(ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = "Properties file not found.";
            throw new UsageException(msg);
        }
//        if (!StringUtils.hasLength( ((EtlRunConfig) config).getDeployementArchivePassword())) {
//            String msg = "DeploymentArchivePassword (-dapwd) must be specified on the command line.";
//            throw new UsageException(msg);
//        }
    }

}
