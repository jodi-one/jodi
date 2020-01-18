package one.jodi.odi12.service;

import com.google.inject.Inject;
import one.jodi.annotations.DeploymentArchivePassword;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.service.repository.OdiRepositoryImportService;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.core.service.deployment.DeploymentService;
import oracle.odi.core.service.deployment.DeploymentServiceException;
import oracle.odi.core.service.deployment.OdiObjectId;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.runtime.loadplan.finder.IOdiLoadPlanFinder;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;

public class Odi12RepositoryImportService implements OdiRepositoryImportService {

    private final static Logger logger =
            LogManager.getLogger(Odi12RepositoryImportService.class);
    private final OdiInstance odiInstance;
    private final JodiProperties jodiProperties;
    private final String deploymentArchivePassword;

    @Inject
    public Odi12RepositoryImportService(final OdiInstance odiInstance,
                                        final JodiProperties jodiProperties,
                                        final @DeploymentArchivePassword String deploymentArchivePassword) {
        this.odiInstance = odiInstance;
        this.jodiProperties = jodiProperties;
        this.deploymentArchivePassword = deploymentArchivePassword;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void doImport(final String metaDataDirectory, final DA_TYPE pDAType) {
        /*
        ImportServiceImpl exportService = new ImportServiceImpl(odiInstance);
        exportService.importExecutionEnvironmentFromFolder(metaDataDirectory,new EncodingOptions());
        exportService.importLogicalTopologyFromFolder(metaDataDirectory,new EncodingOptions());
        exportService.importTopologyFromFolder(metaDataDirectory,new EncodingOptions());
        exportService.importWorkFromFolder(metaDataDirectory,new EncodingOptions());
        exportService.importMasterFromFolder(metaDataDirectory,new EncodingOptions(),true,true);
        exportService.importSecurityFromFolder(metaDataDirectory,new EncodingOptions());
         */
        java.lang.String pFilename = metaDataDirectory + File.separator +
                jodiProperties
                        .getProjectCode()+".zip";
        char[] pExportKey = getCipherData().length() > 0 ?  getCipherData().toCharArray() : null;
        boolean pApplyWithoutCipherData = getCipherData().length() > 0 ? false : true;


            try {
            if( pFilename.contains("PATCH_ER")) {
                boolean pIncludePhysicalTopologyData = false;
                boolean pCreateRollbackDA = false;
                String pRollbackDaFilename = metaDataDirectory + File.separator +
                        "ROLLBACK_" +jodiProperties
                                .getProjectCode()+".zip";
                logger.info("Importing Deployment Archive with scenarios and loadplans only with filename "+pFilename);
                DeploymentService.applyPatchDeploymentArchive
                        ( odiInstance,
                                pFilename,
                             pCreateRollbackDA,
                             pRollbackDaFilename,
                             pIncludePhysicalTopologyData,
                             pExportKey,
                             pApplyWithoutCipherData);
            } else if( pFilename.contains("PATCH_DR")) {
                boolean pCreateRollbackDA = false;
                boolean pIncludePhysicalTopologyData = false;
                String pRollbackDaFilename = metaDataDirectory + File.separator +
                        "ROLLBACK_" +jodiProperties
                        .getProjectCode()+".zip";
                logger.info("Importing Deployment Archive with scenarios and loadplans and projects with filename "+pFilename);
                DeploymentService.applyPatchDeploymentArchive
                        ( odiInstance,
                                pFilename,
                                pCreateRollbackDA,
                                pRollbackDaFilename,
                                pIncludePhysicalTopologyData,
                                pExportKey,
                                pApplyWithoutCipherData);
            }else{
                boolean pIncludePhysicalTopologyData = true;
                logger.info("Importing Initial Deployment Archive with filename "+pFilename);
                DeploymentService.applyFullDeploymentArchive(
                        odiInstance,
                        pFilename,
                        pIncludePhysicalTopologyData,
                        pExportKey, pApplyWithoutCipherData
                );
            }
        } catch (DeploymentServiceException e) {
            logger.error(e);
            throw new Odi12RepositoryException(e);
        }
    }

    @Override
    public String getCipherData() {
        return this.deploymentArchivePassword;
    }
}
