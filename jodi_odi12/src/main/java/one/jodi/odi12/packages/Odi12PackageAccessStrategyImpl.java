package one.jodi.odi12.packages;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.odi.packages.OdiPackageAccessStrategy;
import one.jodi.odi12.folder.Odi12FolderServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.StepMapping;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static one.jodi.odi12.folder.Odi12FolderHelper.getFolderPath;

public class Odi12PackageAccessStrategyImpl implements
        OdiPackageAccessStrategy<Mapping, StepMapping> {
    private static final Logger logger =
            LogManager.getLogger(Odi12PackageAccessStrategyImpl.class);
    private final OdiInstance odiInstance;
    private final Odi12FolderServiceProvider folderService;

    @Inject
    Odi12PackageAccessStrategyImpl(final OdiInstance odiInstance,
                                   final Odi12FolderServiceProvider folderService,
                                   final ErrorWarningMessageJodi errorWarningMessages) {
        this.odiInstance = odiInstance;
        this.folderService = folderService;
    }

    @Override
    public StepMapping newStep(final OdiPackage odiPackage, final Mapping mapping,
                               final String label) {
        return new StepMapping(odiPackage, mapping, label);
    }

    public Optional<OdiPackage> findPackageByName(final String packageName,
                                                  final String folderPath,
                                                  final String projectCode) {
        assert (folderPath != null && !folderPath.isEmpty() && !folderPath.contains("//"));

        String[] folderNames = folderPath.split("/");
        String folderName = folderNames[folderNames.length - 1];

        // search for all packages that have the desired name and parent folder name
        Collection<OdiPackage> allPackages =
                ((IOdiPackageFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiPackage.class))
                        .findByName(packageName, projectCode,
                                folderName);
        // narrow search down to the desired full folder path
        return allPackages.stream()
                .filter(p -> getFolderPath(p.getParentFolder()).equals(folderPath))
                .findFirst();
    }

    @Override
    public OdiPackage findPackage(final String packageName, final String folderPath,
                                  final String projectCode) {
        Optional<OdiPackage> odiPackage = findPackageByName(packageName, folderPath,
                projectCode);
        if (!odiPackage.isPresent()) {
            return null;
        }
        return odiPackage.get();
    }

    @Override
    @Deprecated
    public Collection<OdiPackage> findPackage(final String packageName,
                                              final String projectCode) {

        IOdiPackageFinder finder = ((IOdiPackageFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiPackage.class));
        Collection<OdiPackage> packages = finder.findByName(packageName, projectCode);
        return packages;
    }

    @Override
    public OdiPackage createPackage(final String packageName, final String folderPath,
                                    final String projectCode) {
        Map<String, OdiFolder> folders =
                folderService.findOrCreateFolders(Arrays.asList(folderPath), projectCode);
        if (folders.get(folderPath) != null) {
            logger.debug(String.format("found folder '%s'.", folderPath));
        }
        OdiFolder folder = folders.get(folderPath);
        OdiPackage odiPackage = new OdiPackage(folders.get(folderPath), packageName);
        odiInstance.getTransactionalEntityManager().persist(odiPackage);
        logger.info("New Package created: " + packageName + " in folder " + folder.getQualifiedName() + ".");
        return odiPackage;
    }

    @SuppressWarnings("unused") // TODO consider integration
    private void deleteProcedureScenario(final OdiPackage odiPackage) {
        ((IOdiScenarioFinder)
                odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiScenario.class))
                .findBySourcePackage(odiPackage.getPackageId())
                .stream()
                .peek(s -> logger.info(String.format("Removed scenario: '%s'.",
                        s.getName())))
                .forEach(s -> odiInstance.getTransactionalEntityManager()
                        .remove(s));
    }

    public void removePackage(final String packageName, final String folderPath,
                              final String projectCode) {
        OdiPackage odiPackage = findPackage(packageName, folderPath, projectCode);
        if (odiPackage == null) {
            return;
        }
        logger.info(String.format("Removing package %s in folder %s.", odiPackage.getName(), odiPackage.getParentFolder().getName()));
        odiInstance.getTransactionalEntityManager().remove(odiPackage);
    }

}
