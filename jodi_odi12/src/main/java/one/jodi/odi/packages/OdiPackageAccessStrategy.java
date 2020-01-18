package one.jodi.odi.packages;

import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.Step;

import java.util.Collection;
import java.util.Optional;

public interface OdiPackageAccessStrategy<T extends IOdiEntity, S extends Step> {

    S newStep(OdiPackage odiPackage, T odiInterface, String label);

    Optional<OdiPackage> findPackageByName(String packageName, String folderPath,
                                           String projectCode);

    OdiPackage findPackage(String packageName, String folderPath, String projectCode);

    OdiPackage createPackage(String packageName, String folderPath, String projectCode);

    void removePackage(String packageName, String folderPath, String projectCode);

    @Deprecated
        // use for
    Collection<OdiPackage> findPackage(String packageName, String projectCode);

}
