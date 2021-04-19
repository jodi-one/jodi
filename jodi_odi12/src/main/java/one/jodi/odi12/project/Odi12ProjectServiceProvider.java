package one.jodi.odi12.project;

import com.google.inject.Inject;
import one.jodi.etl.service.project.ProjectServiceProvider;
import one.jodi.odi.runtime.OdiConnection;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.ReusableMapping;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.mapping.finder.IReusableMappingFinder;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.finder.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class Odi12ProjectServiceProvider implements ProjectServiceProvider {

    private final OdiInstance odiInstance;
    private static final Logger logger = LogManager.getLogger(Odi12ProjectServiceProvider.class);

    @Inject
    public Odi12ProjectServiceProvider(final OdiInstance odiInstance) {
        this.odiInstance = odiInstance;
    }

    @Override
    public void deleteProjects() {

        IOdiFolderFinder folderFinder = (IOdiFolderFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiFolder.class);
        Collection<OdiFolder> odiFolders = folderFinder.findAll();
        logger.info("Found " + odiFolders.size() + " odifolders to delete");

        odiFolders.forEach(f -> {
                    logger.info("Deleting folder : " + f.getName());
                    removeFolder((OdiFolder) f);
                }
        );

        IOdiProjectFinder projectFinder = (IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiProject.class);
        projectFinder.findAll().stream().forEach(s -> {
            odiInstance.removeEntity((OdiProject) s);
        });
    }


    private void deleteModels() {
        IOdiModelFinder projectFinder = (IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiModel.class);
        projectFinder.findAll().stream().forEach(s -> {
            logger.info("Deleting model " + ((OdiModel) s).getName());
            removeModel(((OdiModel) s).getGlobalId());
        });
    }

    private void removeFolder(OdiFolder odiFolder) {
        if (odiFolder.getSubFolders() == null || odiFolder.getSubFolders().size() == 0) {
            logger.info("Deleting folder " + odiFolder.getName());
            removeFolder(odiFolder.getGlobalId());
        }
    }

    private void removeFolder(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IOdiFolderFinder finder = (IOdiFolderFinder) odiConnection.getOdiInstance().getFinder(OdiFolder.class);
        OdiFolder entity = (OdiFolder) finder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(entity);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }

    private void removeMapping(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IMappingFinder finder = (IMappingFinder) odiConnection.getOdiInstance().getFinder(Mapping.class);
        Mapping entity = (Mapping) finder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(entity);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }

    private void removePackage(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IOdiPackageFinder finder = (IOdiPackageFinder) odiConnection.getOdiInstance().getFinder(OdiPackage.class);
        OdiPackage entity = (OdiPackage) finder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(entity);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }

    private void removeInterfaces(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IOdiInterfaceFinder finder = (IOdiInterfaceFinder) odiConnection.getOdiInstance().getFinder(OdiInterface.class);
        OdiInterface entity = (OdiInterface) finder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(entity);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }

    private void removeReusableMapping(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IReusableMappingFinder finder = (IReusableMappingFinder) odiConnection.getOdiInstance().getFinder(ReusableMapping.class);
        ReusableMapping entity = (ReusableMapping) finder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(entity);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }

    private void removeUserProcedures(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IOdiUserProcedureFinder finder = (IOdiUserProcedureFinder) odiConnection.getOdiInstance().getFinder(OdiUserProcedure.class);
        OdiUserProcedure entity = (OdiUserProcedure) finder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(entity);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }

    private void removeModel(String globalId) {
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        OdiConnection odiConnection = new OdiConnection(odiInstance, trans);
        IOdiModelFinder finder = (IOdiModelFinder) odiConnection.getOdiInstance().getFinder(OdiModel.class);
        OdiModel entity = (OdiModel) finder.findByGlobalId(globalId);
        odiConnection.getOdiInstance().removeEntity(entity);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
    }
}
