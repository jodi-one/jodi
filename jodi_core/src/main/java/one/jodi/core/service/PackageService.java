package one.jodi.core.service;

import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;

import java.util.List;


/**
 * Service that provides Package management functionality.
 */
public interface PackageService {

    /**
     * Creates all packages associated with the Packages instance.
     *
     * @param jodiPackages        the Packages instance
     * @param raiseErrorOnFailure the raise error on failure
     */
    void createPackages(List<ETLPackage> jodiPackages, boolean raiseErrorOnFailure);

    /**
     * Creates a package.
     *
     * @param jodiPackage         the Package instance
     * @param raiseErrorOnFailure the raise error on failure
     */
    void createPackage(ETLPackage jodiPackage, boolean raiseErrorOnFailure);

    /**
     * Delete a package matching the provided name.
     *
     * @param packageName         Odi package name to be deleted.
     * @param raiseErrorOnFailure throw new runtime exception when errors occur
     */
    void deletePackage(String packageName, String folderPath, boolean raiseErrorOnFailure);

    /**
     * Delete all packages that are indicated by the list of package names.
     *
     * @param packageNames        List of names of packages to be deleted
     * @param raiseErrorOnFailure throw new runtime exception when errors occur
     */
    void deletePackages(List<ETLPackageHeader> packeHeasders, boolean raiseErrorOnFailure);

    /**
     * Delete a package matching the provided name.
     *
     * @param packageName         Odi package name to be deleted.
     * @param raiseErrorOnFailure throw new runtime exception when errors occur
     */
    void truncatePackage(String packageName, String folderName, boolean raiseErrorOnFailure);

}
