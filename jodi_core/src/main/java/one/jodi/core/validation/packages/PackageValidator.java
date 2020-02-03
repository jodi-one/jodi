package one.jodi.core.validation.packages;

import one.jodi.etl.internalmodel.ETLPackage;

import java.util.List;

/**
 * The Interface that defines the Package validator API.
 */
public interface PackageValidator {

    /**
     * Validate a collection of Package instances.
     *
     * @param packages the packages to validate
     * @return the list of validation results for all validated packages
     */
    public abstract List<PackageValidationResult> validatePackages(
            List<ETLPackage> packages);

}