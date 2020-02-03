package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.PackageService;
import one.jodi.core.validation.packages.PackageValidationResult;
import one.jodi.core.validation.packages.PackageValidator;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.service.packages.PackageServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link PackageService} interface.
 */
public class PackageServiceImpl implements PackageService {

    private final static Logger logger = LogManager.getLogger(PackageServiceImpl.class);

    private final static String ERROR_MESSAGE_00090 = "Validation Error: %s";
    private final static String ERROR_MESSAGE_05030 = "Validation Warning: %s";

    private final PackageServiceProvider packageService;
    private final JodiProperties properties;
    private final PackageValidator packageValidator;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new PackageServiceImpl instance.
     *
     * @param packageService
     * @param properties
     */
    @Inject
    public PackageServiceImpl(final PackageServiceProvider packageService,
                              final JodiProperties properties,
                              final PackageValidator packageValidator,
                              final ErrorWarningMessageJodi errorWarningMessages) {
        this.packageService = packageService;
        this.properties = properties;
        this.packageValidator = packageValidator;
        this.errorWarningMessages = errorWarningMessages;
    }

    //   @SuppressWarnings("unchecked")
    @Override
    public void createPackages(final List<ETLPackage> packages,
                               final boolean raiseErrorOnFailure) {
        List<ETLPackage> validPackages = validatePackages(packages);
//      if (properties.isUpdateable()) {
//         packageService.truncatePackages((List<ETLPackageHeader>)(List<?>)validPackages,
//                                         properties.getProjectCode(),
//                                         raiseErrorOnFailure);
//      }
        packageService.createPackages(validPackages, properties.getProjectCode(),
                raiseErrorOnFailure);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createPackage(final ETLPackage jodiPackage,
                              final boolean raiseErrorOnFailure) {
        packageService.createPackage(jodiPackage, properties.getProjectCode(),
                raiseErrorOnFailure);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deletePackage(final String packageName, final String folder,
                              final boolean raiseErrorOnFailure) {
        try {
            packageService.removePackage(packageName, folder);
        } catch (RuntimeException ex) {
            if (raiseErrorOnFailure)
                throw ex;
            else logger.info(String.format("Removing package '%1$s' in folder '%2$s'failed.", packageName, folder));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Deleted package: " + packageName + " in folder  :" + folder);
        }
    }

    @Override
    public void deletePackages(final List<ETLPackageHeader> headers,
                               final boolean raiseErrorOnFailure) {
        if (properties.isUpdateable()) {
            packageService.truncatePackages(headers, properties.getProjectCode(),
                    raiseErrorOnFailure);
        } else {
            packageService.removePackages(headers, raiseErrorOnFailure);
        }
    }

    private List<ETLPackage> validatePackages(final List<ETLPackage> packages) {
        final List<ETLPackage> result = new ArrayList<>(packages.size());
        for (PackageValidationResult vr : packageValidator.validatePackages(packages)) {
            logger.debug("validatedPackage result:" + vr.isValid());
            if (vr.isValid()) {
                result.add(vr.getTargetPackage());
                for (String msg : vr.getValidationMessages()) {
                    String message =
                            errorWarningMessages.formatMessage(5030, ERROR_MESSAGE_05030,
                                    this.getClass(), msg);
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                            message, MESSAGE_TYPE.WARNINGS);
                }
            } else {
                for (String msg : vr.getValidationMessages()) {
                    String message =
                            errorWarningMessages.formatMessage(90, ERROR_MESSAGE_00090,
                                    this.getClass(), msg);
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                            message, MESSAGE_TYPE.ERRORS);
                }
            }
        }
        logger.debug("validatedPackages result:" + result.size());
        return result;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void truncatePackage(final String packageName, final String folderName,
                                final boolean raiseErrorOnFailure) {
        packageService.truncatePackage(packageName, folderName, properties.getProjectCode(),
                raiseErrorOnFailure);
    }

}
