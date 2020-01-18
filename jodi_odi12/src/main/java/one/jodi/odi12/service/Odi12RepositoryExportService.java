package one.jodi.odi12.service;

import com.google.inject.Inject;
import one.jodi.annotations.DeploymentArchivePassword;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.service.repository.OdiRepositoryExportService;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.service.deployment.*;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.runtime.loadplan.OdiLoadPlan;
import oracle.odi.domain.runtime.loadplan.finder.IOdiLoadPlanFinder;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder;
import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.OdiPhysicalSchema;
import oracle.odi.domain.topology.finder.IOdiContextFinder;
import oracle.odi.domain.topology.finder.IOdiLogicalSchemaFinder;
import oracle.odi.domain.topology.finder.IOdiPhysicalSchemaFinder;
import oracle.odi.impexp.EncodingOptions;
import oracle.odi.impexp.support.ExportServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Odi12RepositoryExportService implements OdiRepositoryExportService {

    private final static Logger logger =
            LogManager.getLogger(Odi12RepositoryExportService.class);
    private final OdiInstance odiInstance;
    private final JodiProperties jodiProperties;
    private final String deploymentArchivePassword;


    @Inject
    public Odi12RepositoryExportService(final OdiInstance odiInstance,
                                        final JodiProperties jodiProperties,
                                        final @DeploymentArchivePassword String deploymentArchivePassword) {
        this.odiInstance = odiInstance;
        this.jodiProperties = jodiProperties;
        this.deploymentArchivePassword = deploymentArchivePassword;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void doExport(final String metaDataDirectory) throws IOException {
        boolean pIncludePhysicalTopologyData = true;
        char[] pExportKey = getCipherData().toCharArray();
        boolean pCreateWithoutCipherData = false;
        try {
            {
                final List<OdiObjectId> patchList = new ArrayList<>();

                IOdiScenarioFinder scenarioFinder = (IOdiScenarioFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiScenario.class);
                scenarioFinder.findAll().stream().forEach(s -> {
                    patchList.add(new OdiObjectId(OdiScenario.class, ((OdiScenario) s).getInternalId()));
                });
                IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiLoadPlan.class);
                loadPlanFinder.findAll().stream().forEach(l -> {
                    patchList.add(new OdiObjectId(OdiLoadPlan.class, ((OdiLoadPlan) l).getInternalId()));
                });

                boolean pIncludeSecurityObjects = true;
                boolean regenerateScenarios = false;

                java.lang.String pFilename;
                java.lang.String pName;
                java.lang.String pDescription;
                pFilename = metaDataDirectory + File.separator +
                        "PATCH_ER_" + jodiProperties
                        .getProjectCode() + ".zip";
                pName = "PATCH Execution Repository " + jodiProperties.getProjectCode();
                pDescription = "Patch Export for project " + jodiProperties
                        .getProjectCode() + " with scenarios and loadplans only.";
                DeploymentService.createDeploymentArchiveFromRepo(odiInstance,
                        patchList,
                        pName,
                        pDescription,
                        DeploymentArchiveType.PATCH,
                        pFilename,
                        pIncludePhysicalTopologyData,
                        pExportKey,
                        pCreateWithoutCipherData,
                        pIncludeSecurityObjects,
                        regenerateScenarios);
            }
            {
                final List<OdiObjectId> patchList = new ArrayList<>();

                IOdiScenarioFinder scenarioFinder = (IOdiScenarioFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiScenario.class);
                scenarioFinder.findAll().stream().forEach(s -> {
                    patchList.add(new OdiObjectId(OdiScenario.class, ((OdiScenario) s).getInternalId()));
                });
                IOdiLoadPlanFinder loadPlanFinder = (IOdiLoadPlanFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiLoadPlan.class);
                loadPlanFinder.findAll().stream().forEach(l -> {
                    patchList.add(new OdiObjectId(OdiLoadPlan.class, ((OdiLoadPlan) l).getInternalId()));
                });
                IOdiProjectFinder projectFinder = (IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiProject.class);
                projectFinder.findAll().stream().forEach(p -> {
                    patchList.add(new OdiObjectId(OdiProject.class, ((OdiProject) p).getInternalId()));
                });
                boolean pIncludeSecurityObjects = true;
                boolean regenerateScenarios = false;

                java.lang.String pFilename;
                java.lang.String pName;
                java.lang.String pDescription;
                pFilename = metaDataDirectory + File.separator +
                        "PATCH_DR_" + jodiProperties
                        .getProjectCode() + ".zip";
                pName = "PATCH Development Repository " + jodiProperties.getProjectCode();
                pDescription = "Patch Export for project " + jodiProperties
                        .getProjectCode() + " with scenarios loadplans and projects only.";
                DeploymentService.createDeploymentArchiveFromRepo(odiInstance,
                        patchList,
                        pName,
                        pDescription,
                        DeploymentArchiveType.PATCH,
                        pFilename,
                        pIncludePhysicalTopologyData,
                        pExportKey,
                        pCreateWithoutCipherData,
                        pIncludeSecurityObjects,
                        regenerateScenarios);
            }
            //
            {
                String pFilename = metaDataDirectory + File.separator +
                        "INITIAL_" + jodiProperties
                        .getProjectCode() + ".zip";
                String pName = "INITIAL " + jodiProperties.getProjectCode();
                String pDescription = "Initial Export for project " + jodiProperties
                        .getProjectCode();
                DeploymentService.createDeploymentArchiveFromRepo(odiInstance,
                        pName,
                        pDescription,
                        pFilename,
                        pIncludePhysicalTopologyData,
                        pExportKey,
                        pCreateWithoutCipherData);
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