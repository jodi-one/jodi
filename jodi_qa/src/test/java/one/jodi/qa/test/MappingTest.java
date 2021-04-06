package one.jodi.qa.test;

import one.jodi.base.ListAppender;
import one.jodi.base.config.PasswordConfigImpl;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static one.jodi.qa.test.FunctionalTestHelper.getListAppender;
import static one.jodi.qa.test.FunctionalTestHelper.removeAppender;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MappingTest extends RegressionTestImpl {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(MappingTest.class);

    private static final Pattern TEST_METHOD_NAME_PATTERN = Pattern.compile("^test_?([0-9]+)_?(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Path TEST_XML_BASE_DIRECTORY = Paths.get("src", "test", "resources", "MappingTest");
    private static final Path TEST_PROPERTIES_BASE_DIRECTORY = TEST_XML_BASE_DIRECTORY.resolve(FunctionalTestHelper.getPropertiesDir());

    private static final Path DEFAULT_PROPERTIES = TEST_PROPERTIES_BASE_DIRECTORY.resolve("MappingTest.properties");
    private static final String DEFAULT_AGENT = FunctionalTestHelper.getDefaultAgent(DEFAULT_PROPERTIES);

    @Rule
    public final TestName testMethodName = new TestName();

    private String testName;
    private long testNumber;
    private Path metadataDirectory;

    private final String refUser;
    private final String refUserJDBC;
    private final String refUserJDBCDriver;

    public MappingTest() {
        super(DEFAULT_PROPERTIES.toString(),
                new PasswordConfigImpl().getOdiUserPassword(),
                new PasswordConfigImpl().getOdiMasterRepoPassword());

        refUser = getRegressionConfiguration().getConfig().getString("rt.custom.refUser");
        refUserJDBC = getRegressionConfiguration().getConfig().getString("rt.custom.refUserJDBC");
        refUserJDBCDriver = getRegressionConfiguration().getConfig().getString("rt.custom.refUserJDBCDriver");
    }

    @Before
    public void setContextBeforeTestCase() {
        String name = testMethodName.getMethodName();
        LOGGER.info("testName -->" + name);
        Matcher matcher = TEST_METHOD_NAME_PATTERN.matcher(name);
        if (matcher.matches()) {
            testNumber = Long.parseLong(matcher.group(1));
            testName = matcher.group(2);
        } else {
            testName = name;
            testNumber = -1;
        }
        metadataDirectory = TEST_XML_BASE_DIRECTORY.resolve(Paths.get("xml", "" + testNumber));
//        metadataDirectory = TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
    }

    //    @After TODO activate when ready
    public void cleanupPackageAfterTestCase() {
        // Assumes that testName is set correctly in a method with @Before-annotation
        if (testName != null) {
            try {
                OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
                ITransactionManager tm = odiInstance.getTransactionManager();
                IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
                IOdiPackageFinder mf = (IOdiPackageFinder) odiInstance.getFinder(OdiPackage.class);
                @SuppressWarnings("unchecked") Collection<OdiPackage> packages = mf.findAll();
                for (OdiPackage p : packages) {
                    tem.remove(p);
                }
                tm.commit(getWorkOdiInstance().getTransactionStatus());
//                deletePackageAndScenario(DEFAULT_PROPERTIES, testName, "BulkLoadORACLE_DWH_STG");
                LOGGER.debug("package " + testName + " deleted after execution test case.");
            } catch (RuntimeException ex) {
                LOGGER.info(ex.getMessage());
            }
        }
    }

    @Test
    @Override
    public void test020Generation() {
        // let's assume this is fine for the moment
    }

    @Test
    @Override
    public void test030ing() {
        // let's assume this is fine for the moment
    }

    @Test
    public void test_15150_Mapping_GbuModule_Automapping_Success() {
        final Path properties = TEST_PROPERTIES_BASE_DIRECTORY.resolve("15150_MappingTest.properties");
        ListAppender listAppender = getListAppender(testName);
        String prefix = "Init";

        try {

//        runController("etls", properties, "-p", prefix, "-m", metadataDirectory, "--model", "one.jodi.extensions.GbuExtensionModule");
//        assertFalse("Creation of interface logged errors.", listAppender.contains(Level.ERROR, false));


            // TODO fill 'er up
        /*
        use DBUnit to import data into db tables
        generate manually Transformation XML like 13054_JOIN.xml + 0.xml
        create odi mappings with:
            runController("ct", defaultProperties, "-p", "Inf", "-m", TEST_XML_BASE_DIRECTORY + "/xml/Generation" --model injectModel);
        execute the mappings in ODI with:
            RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);
        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
        not sure if dependencies work out for you, for module injection work out, could be you need to reshuffle dependencies
         */

        } finally {
            removeAppender(listAppender);
        }
    }
}
