package one.jodi.core.context.packages;

import one.jodi.etl.internalmodel.Transformation;

import java.util.List;
import java.util.Set;

/**
 * The PackageCache Interface defines the API available for interacting with the
 * Package metadata. The cache maintains the set of defined packages and their
 * relationship to other packages and interfaces.
 */
public interface PackageCache {

    /**
     * Registre package cache with package list association names from package XML.
     *
     * @param packageName the packages metadata object
     * @param association association
     * @param order       integer representing the order
     */
    void addPackageAssociation(String packageName, String association, int order);


    public List<PackageCacheItem> getPackageNamesForAssociation(String packageListItem);

    /**
     * Get package list association names as defined from package XML, registered
     * from calls to {@link PackageCache#addPackageAssociation(String, String, int)}
     *
     * @return set of packagelist items
     */
    public Set<String> getPackageAssociations();

    /**
     * Adds a relationship between the specified transformation and packages
     * based on the package list values for the transformation and the known
     * packages.
     *
     * @param transformation the transformation
     */
    void addTransformationToPackages(Transformation transformation);

    /**
     * Returns a list of transformation names that have been associated with the
     * specified package via the addTransformationToPackages method. The
     * transformations are listed in the order that they were added.
     *
     * @param packageName the package name
     * @param folderName  the name of the folder
     * @return the transformations associated with the specified package
     */
    List<TransformationCacheItem> getTransformationsForPackage(String packageName, String folderName);

}