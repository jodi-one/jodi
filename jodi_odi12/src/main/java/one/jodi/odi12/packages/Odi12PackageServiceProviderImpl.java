package one.jodi.odi12.packages;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.service.procedure.ProcedureServiceProvider;
import one.jodi.odi.packages.OdiBasePackageServiceProvider;
import one.jodi.odi.packages.OdiPackageAccessStrategy;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.Step;
import oracle.odi.domain.project.StepMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Odi12PackageServiceProviderImpl
        extends OdiBasePackageServiceProvider<Mapping, StepMapping> {

    private final static Logger logger =
            LogManager.getLogger(Odi12PackageServiceProviderImpl.class);
    private final OdiPackageAccessStrategy<Mapping, StepMapping> packageAccessStrategy;

    @Inject
    protected Odi12PackageServiceProviderImpl(
            final OdiInstance odiInstance,
            final OdiPackageAccessStrategy<Mapping, StepMapping> packageAccessStrategy,
            final ProcedureServiceProvider procedureService,
            final OdiVariableAccessStrategy odiVariableService,
            final JodiProperties properties,
            final JournalizingContext journalizingContext,
            final ErrorWarningMessageJodi errorWarningMessages,
            final EtlSubSystemVersion etlSubSystemVersion,
            final DatabaseMetadataService databaseMetadataService) {
        super(odiInstance, packageAccessStrategy, procedureService, odiVariableService,
                properties, errorWarningMessages, etlSubSystemVersion, journalizingContext,
                databaseMetadataService);
        this.packageAccessStrategy = packageAccessStrategy;
    }

    @Override
    public void truncatePackages(final List<ETLPackageHeader> packages,
                                 final String projectCode,
                                 final boolean raiseErrorOnFailure) {
        for (ETLPackageHeader etlPackage : packages) {
            truncate(etlPackage, projectCode, raiseErrorOnFailure);
        }
    }

    public void truncate(final OdiPackage odiPackage) {
        if (odiPackage == null || odiPackage.getSteps() == null ||
                odiPackage.getSteps().size() == 0) {
            return;
        }
        Step[] array = new Step[odiPackage.getSteps().size()];
        Step[] arrays = odiPackage.getSteps().toArray(array);
        for (int i = 0; i < odiPackage.getSteps().size(); i++) {
            odiPackage.removeStep(arrays[i]);
        }
        // fix;
        // In odi it seems possible to have multiple "flows"
        // where only 1 is connected; hence we loop it through
        // till steps size is 0
        logger.debug("After truncate steps size is :" + odiPackage.getSteps().size());
        while (odiPackage.getSteps().size() != 0) {
            truncate(odiPackage);
        }
        logger.info(String.format("Truncating package %s in folder %s.", odiPackage.getName(), odiPackage.getParentFolder().getName()));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void truncate(final ETLPackageHeader jodiPackage, final String projectCode,
                         final boolean raiseErrorOnFailure) {
        OdiPackage odiPackage =
                packageAccessStrategy.findPackage(jodiPackage.getPackageName().toUpperCase(),
                        jodiPackage.getFolderCode(), projectCode);
        truncate(odiPackage);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void truncatePackage(final String packageName, final String folderName,
                                final String projectCode, final boolean raiseErrorOnFailure) {
        OdiPackage odiPackage = packageAccessStrategy.findPackage(packageName.toUpperCase(),
                folderName, projectCode);
        truncate(odiPackage);
    }

}
