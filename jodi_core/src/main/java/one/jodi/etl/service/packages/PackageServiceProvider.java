package one.jodi.etl.service.packages;

import com.google.inject.Inject;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.journalizng.JournalizingConfiguration;

import java.util.Collection;
import java.util.List;

public abstract class PackageServiceProvider {
    private final JournalizingContext journalizingContext;

    @Inject
    public PackageServiceProvider(final JournalizingContext journalizingContext) {
        this.journalizingContext = journalizingContext;
    }

    public abstract void removePackages(List<ETLPackageHeader> headers, boolean raiseErrorOnFailure);

    public abstract void removePackage(String packageName, String folderPath);

    public abstract void createPackages(List<ETLPackage> jodiPackages, String projectCode, boolean raiseErrorOnFailure);

    public abstract void createPackage(ETLPackage jodiPackage, String projectCode, boolean raiseErrorOnFailure);

    /**
     * @param packageName name of the package to be found
     * @param folderCode  optional folder name; if defined the package must be found in this folder
     * @return <code>true</code> if package is found in folder if folder is not <code>null</code> or
     * empty string or is found in project if folder is null or empty string;
     * <code>true</code> otherwise
     */
    public abstract boolean packageExists(String packageName, String folderCode);

    public abstract Collection<String> getProcedureParameterNames(String projectCode,
                                                                  String procedureName)
            throws ProcedureNotFoundException;

    public abstract void truncatePackages(List<ETLPackageHeader> packages, String projectCode,
                                          boolean raiseErrorOnFailure);

    public abstract void truncatePackage(String packageName, String folderName, final String projectCode, final boolean raiseErrorOnFailure);

    public List<JournalizingConfiguration> getJournalizingConfigurationFromContext() {
        return journalizingContext.getJournalizingConfiguration();
    }

    public boolean projectVariableExists(String projectCode, String variable) {
        // TODO Auto-generated method stub
        return false;
    }
}
