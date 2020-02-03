package one.jodi.core.service.impl;

import junit.framework.TestCase;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.validation.packages.PackageValidator;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.service.packages.PackageServiceProvider;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/**
 * DOCUMENT ME!
 */
public class PackageServiceProviderTest extends TestCase {

    private PackageServiceImpl packageService;
    private PackageServiceProvider etlProvider;
    private PackageValidator packageValidator;

    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Creates a new PackageServiceProviderTest instance.
     *
     * @param name DOCUMENT ME!
     */
    public PackageServiceProviderTest(final String name) {
        super(name);
    }

    @Override
    @Before
    protected void setUp() throws Exception {
        super.setUp();
        etlProvider = mock(PackageServiceProvider.class);

        JodiProperties properties = mock(JodiProperties.class);
        packageValidator = mock(PackageValidator.class);
        packageService = new PackageServiceImpl(etlProvider, properties,
                packageValidator, errorWarningMessages);
    }

    /**
     * DOCUMENT ME!
     */
    @SuppressWarnings("unchecked")
    public void testCreateMultiplePackagesFromFolder() {
        ETLPackage pack1 = mock(ETLPackage.class);
        ETLPackage pack2 = mock(ETLPackage.class);
        List<ETLPackage> plist = new ArrayList<ETLPackage>(2);
        plist.add(pack1);
        plist.add(pack2);

        boolean raiseErrorOnFailure = true;
        packageService.createPackages(plist, raiseErrorOnFailure);

        verify(etlProvider).createPackages(anyList(), anyString(), eq(raiseErrorOnFailure));
    }


    /**
     * DOCUMENT ME!
     */
    public void testCreatePackageFromFolder() {
        ETLPackage pack = mock(ETLPackage.class);

        boolean raiseErrorOnFailure = true;
        packageService.createPackage(pack, raiseErrorOnFailure);

        verify(etlProvider).createPackage(eq(pack), anyString(), eq(raiseErrorOnFailure));
    }

    /**
     * DOCUMENT ME!
     */
    public void testDeleteListOfPackages() {
        List<ETLPackageHeader> plist = new ArrayList<>(2);
        plist.add(mock(ETLPackageHeader.class));
        plist.add(mock(ETLPackageHeader.class));
        boolean throwErrorOnFailure = true;
        packageService.deletePackages(plist, throwErrorOnFailure);

        verify(etlProvider).removePackages(plist, throwErrorOnFailure);
    }

    /**
     * DOCUMENT ME!
     */
    public void testDeletePackage() {
        String pack = "testPAckage";
        boolean throwErrorOnFailure = true;
        packageService.deletePackage(pack, "folder", throwErrorOnFailure);

        verify(etlProvider).removePackage(pack, "folder");
    }

}
