package one.jodi.qa.test;

import one.jodi.base.ListAppender;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.factory.ServiceFactory;
import one.jodi.base.os.OsHelper;
import one.jodi.base.util.StringUtils;
import one.jodi.base.util.XMLParserUtil;
import one.jodi.bootstrap.EtlRunConfig;
import one.jodi.bootstrap.JodiController;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.etlmodel.Package;
import one.jodi.core.etlmodel.*;
import one.jodi.core.service.TransformationService;
import one.jodi.db.DBUnitHelper;
import one.jodi.etl.internalmodel.procedure.ProcedureInternal;
import one.jodi.etl.service.ResourceFoundAmbiguouslyException;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.interfaces.TransformationAccessStrategyException;
import one.jodi.odi.common.OdiVersion;
import one.jodi.odi.constraints.OdiConstraintAccessStrategy;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import one.jodi.odi.packages.OdiPackageAccessStrategy;
import one.jodi.odi.runtime.OdiExecuteScenario;
import one.jodi.odi.runtime.OdiUpdateSchema;
import one.jodi.odi.sequences.OdiSequenceAccessStrategy;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import one.jodi.odi12.folder.Odi12FolderHelper;
import one.jodi.odi12.procedure.Odi12ProcedureServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.IRepositoryEntity;
import oracle.odi.domain.adapter.project.IMapping;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import oracle.odi.setup.TechnologyName;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @param <W>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FunctionalTest<T extends IOdiEntity, U extends IRepositoryEntity,
        V extends IRepositoryEntity, W extends Object,
        X extends Object, A extends Step, Y extends Object,
        Z extends Object>
        extends RegressionTestImpl {
    private static final String[] IGNORED_METHODS = {"Test010Install", "Test020Generation", "Test030ing",
            "Test99999Destructor"};
    private final static String ODI_USER_PASSWORD = FunctionalTestHelper.getOdiPass();
    private final static String functionalTestDir = "FunctionalTest";
    private final static String propertiesDir = FunctionalTestHelper.getPropertiesDir();
    @Rule
    public final TestName testMethodName = new TestName();
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(FunctionalTest.class + "1");
    // Chinook SRC DB
    private final String srcUser;
    private final String srcUserJDBC;
    private final String srcUserJDBCDriver;
    // DWH_CON_CHINOOK
    private final String stgUser;
    private final String stgUserJDBC;
    // DWH_DMT
    private final String dmtUser;
    private final String dmtUserJDBC;
    private final String dmtUserJDBCDriver;
    //
    private final String refUser;
    private final String refUserJDBC;
    private final String refUserJDBCDriver;
    //
    private final String TEST_PROPERTIES_BASE_DIRECTORY;
    private final String TEST_XML_BASE_DIRECTORY;
    private final String defaultProperties;
    private final String defaultAgent = FunctionalTestHelper.getDefaultAgent("src/test/resources/FunctionalTest/" + FunctionalTestHelper.getPropertiesDir() + "/FunctionalTest.properties");
    private final ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();
    private final OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z> odiAccessStrategy;
    private final OdiPackageAccessStrategy<T, A> odiPackageAccessStrategy;
    private final OdiSequenceAccessStrategy odi12SequenceAccessStrategy;
    private final OdiVariableAccessStrategy odi12VariableAccessStrategy;
    private final OdiConstraintAccessStrategy odi12ConstraintsAccessStrategy;
    private final Odi12ProcedureServiceProvider getOdiProcedureService;
    private String stgUserJDBCDriver;
    private OdiExecuteScenario odiExecuteScenario;
    private String testName = null;
    private String metadataDirectory = null;
    private String tempDir;

    @SuppressWarnings("unchecked")
    public FunctionalTest() throws ConfigurationException {
        // ODI 12
        super("src/test/resources/" + functionalTestDir + "/" + propertiesDir + "/FunctionalTest.properties", new PasswordConfigImpl().getOdiUserPassword(),
                new PasswordConfigImpl().getOdiMasterRepoPassword());
        TEST_PROPERTIES_BASE_DIRECTORY = "src/test/resources/" + functionalTestDir + "/" + propertiesDir;
        TEST_XML_BASE_DIRECTORY = "src/test/resources/" + functionalTestDir;
        defaultProperties = TEST_PROPERTIES_BASE_DIRECTORY + "/FunctionalTest.properties";
        refUser = regressionConfiguration.getConfig().getString("rt.custom.refUser");
        refUserJDBC = regressionConfiguration.getConfig().getString("rt.custom.refUserJDBC");
        refUserJDBCDriver = regressionConfiguration.getConfig().getString("rt.custom.refUserJDBCDriver");
        //
        srcUser = regressionConfiguration.getConfig().getString("rt.custom.srcUser");
        srcUserJDBC = regressionConfiguration.getConfig().getString("rt.custom.srcUserJDBC");
        srcUserJDBCDriver = regressionConfiguration.getConfig().getString("rt.custom.srcUserJDBCDriver");
        //
        stgUser = regressionConfiguration.getConfig().getString("rt.custom.stgUser");
        stgUserJDBC = regressionConfiguration.getConfig().getString("rt.custom.stgUserJDBC");
        stgUserJDBCDriver = regressionConfiguration.getConfig().getString("rt.custom.stgUserJDBCDriver");
        //
        dmtUser = regressionConfiguration.getConfig().getString("rt.custom.dmtUser");
        dmtUserJDBC = regressionConfiguration.getConfig().getString("rt.custom.dmtUserJDBC");
        dmtUserJDBCDriver = regressionConfiguration.getConfig().getString("rt.custom.dmtUserJDBCDriver");

        //
        Assert.assertNotNull(refUser);
        Assert.assertNotNull(refUserJDBC);
        Assert.assertNotNull(refUserJDBCDriver);
        //
        Assert.assertNotNull(srcUser);
        Assert.assertNotNull(srcUserJDBC);
        Assert.assertNotNull(srcUserJDBCDriver);
        //
        Assert.assertNotNull(stgUser);
        Assert.assertNotNull(stgUserJDBC);
        Assert.assertNotNull(stgUserJDBCDriver);
        //
        Assert.assertNotNull(dmtUser);
        Assert.assertNotNull(dmtUserJDBC);
        Assert.assertNotNull(dmtUserJDBCDriver);
        createOdiExecute(new OdiExecuteScenario());
        final EtlRunConfig runConfig = new EtlRunConfig() {
            @Override
            public boolean isJournalized() {
                return false;
            }

            @Override
            public boolean isDevMode() {
                return false;
            }

            @Override
            public String getTargetModel() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getSourceModel() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getScenario() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getPropertyFile() {
                return "src/test/resources/" + functionalTestDir + "/" + propertiesDir + "/FunctionalTest.properties";
            }

            @Override
            public String getPrefix() {
                return "Init ";
            }

            @Override
            public boolean isIncludeVariables() {
                return false;
            }

            @Override
            public String getPassword() {
                return regressionConfiguration.getOdiSupervisorPassword();
            }

            @Override
            public String getPackageSequence() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getPackage() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<String> getModuleClasses() {
                // TODO Auto-generated method stub
                return Collections.emptyList();
            }

            @Override
            public String getModelCode() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getMetadataDirectory() {
                return TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
            }

            @Override
            public String getMasterPassword() {
                return regressionConfiguration.getMasterRepositoryJdbcPassword();
            }

            @Override
            public String getDeploymentArchiveType() {
                return null;
            }

            @Override
            public boolean isUsingDefaultscenarioNames() {
                return false;
            }

            @Override
            public String getFolder() {
                // TODO Auto-generated method stub
                logger.info("RETURNING NULL FOR FOLDER");
                return null;
            }

            @Override
            public boolean isExportingDBConstraints() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isIncludingConstraints() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public String getDeployementArchivePassword() {
                return new PasswordConfigImpl().getDeploymentArchivePassword();
            }
        };

        if (OsHelper.isMac()) {
            tempDir = "/tmp";
        } else {
            tempDir = System.getProperty("java.io.tmpdir");
        }

        this.odiAccessStrategy = (OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z>)
                FunctionalTestHelper.getOdiAccessStrategy(runConfig, getController());
        this.odiPackageAccessStrategy = (OdiPackageAccessStrategy<T, A>)
                FunctionalTestHelper.getOdiPackageAccessStrategy(runConfig, getController());
        this.odi12SequenceAccessStrategy =
                FunctionalTestHelper.getOdiSequenceAccessStrategy(runConfig,
                        getController());
        this.odi12VariableAccessStrategy =
                FunctionalTestHelper.getOdiVariableAccessStrategy(runConfig,
                        getController());
        this.odi12ConstraintsAccessStrategy =
                FunctionalTestHelper.getOdiConstraintsAccessStrategy(runConfig,
                        getController());
        this.getOdiProcedureService =
                FunctionalTestHelper.getOdiProcedureService(runConfig, getController());
    }

    public void createOdiExecute(OdiExecuteScenario aOdiExecuteScenario) {
        this.odiExecuteScenario = aOdiExecuteScenario;
    }

    private void generationInterfaceAssertSuccess(String aaTestName, String execeptionMessage) {
        generationInterfaceAssertSuccess(aaTestName, execeptionMessage, defaultProperties);
    }

    private void removeAppender(ListAppender listAppender) {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(listAppender);
    }

    private ListAppender getListAppender() {
        ListAppender listAppender = new ListAppender(testName);
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(listAppender);
        rootLogger.setLevel(org.apache.logging.log4j.Level.INFO);
        return listAppender;
    }

    private void generationInterfaceAssertSuccess(String aTestName, String execeptionMessage,
                                                  String properties) {
        ListAppender listAppender = getListAppender();
        String prefix = "Init ";
        runController("etls", properties, "-p", prefix, "-m", metadataDirectory);
        logger.info(String.format("Listappender size: %d.", listAppender.getEvents().size()));
        if (listAppender.contains(Level.WARN, false)) {
            String msg = "Generation logged warnings/errors.";
            Assert.fail();
            throw new RuntimeException(msg);
        }
        removeAppender(listAppender);
    }

    private void generationInterfaceAssertFailure(String aTestName, String execeptionMessage) {
        generationInterfaceAssertFailure(aTestName, execeptionMessage, defaultProperties);
    }

    private void copyFile(File source, File dest) {
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.debug(e);
        }
        if ((System.getProperty("user.name") != null
                && (OsHelper.isMac() || OsHelper.isUnix() || OsHelper.isSolaris()))
                || hostname.endsWith("linux") // linux OBI image
        ) {
            if (new OdiVersion().isVersion11()) {
                executeCommand("scp ./src/test/resources/FunctionalTest/countrylist.csv jodiuser@ct:/tmp");
                executeCommand("ssh jodiuser@ct 'chown jodiuser:dba /tmp/countrylist.csv'");
                executeCommand("ssh jodiuser@ct 'chmod 777 /tmp/countrylist.csv'");
            } else {
                executeCommand("scp ./src/test/resources/FunctionalTest/countrylist.csv oracle@ct:/tmp");
                executeCommand("ssh oracle@ct 'chown oracle:dba /tmp/countrylist.csv'");
                executeCommand("ssh oracle@ct 'chmod 777 /tmp/countrylist.csv'");
            }
        } else {
            if (dest.exists()) {
                dest.delete();
            }
            if (!dest.exists()) {
                try {
                    dest.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(dest);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            if (!OsHelper.isWindows()) {
                executeCommand("chmod 777 " + dest.getAbsolutePath());
                executeCommand("chown jodiuser:dba " + dest.getAbsolutePath());
            }
            dest.setWritable(true, false);
            dest.setReadable(true, false);
            dest.setExecutable(true, false);
        }
        if ((System.getProperty("user.name") != null)
                || hostname.endsWith("linux") // OBI image
        ) {
            logger.info("file copied with scp");
        } else
            assert (dest.exists()) : "Copy failed from : " + source.getAbsolutePath() + " to: " + dest.getAbsolutePath();
    }

    @SuppressWarnings("deprecation")
    private void generationInterfaceAssertFailure(String testName, String execeptionMessage,
                                                  String properties) {
        String prefix = "Init ";
        try {
            String report = runController("etls", properties, "-p", prefix, "-m", metadataDirectory);
            if (report.isEmpty()) {
                Assert.fail("This test did not throw an exception or report an error - it should.");
            }
            // Assert.fail("This test did not threw an exception, it should.");
        } catch (Exception ex) {
            logger.fatal(ex);
        }
    }

    //
    // The following methods cleanup package and scenarios using
    // methods that are annotated with @Before and @After.
    // Key design driver is separation of concern.
    // The critical information used for this implementation is the
    // name of the test method currently executed that is made available
    // by JUnit through the Rule TestName.
    //

    private boolean isIgnored(String methodName) {
        boolean ignore = false;

        for (String n : IGNORED_METHODS) {
            if (methodName.equalsIgnoreCase(n)) {
                ignore = true;
            }
        }
        return ignore;
    }

    /*
     * Find all methods associated with this test method that are public, are
     * annotated with @Test, start with test and are not part of an exclusion
     * list. Delete packages with the names of the test case when prefix
     * consisting of test and 5 digit number is stripped.
     */
    private void deleteAllPackagesAndScenarios() {
        Method[] methods = FunctionalTest.class.getDeclaredMethods();
        int i = 1;
        for (Method m : methods) {
            Test annotation = m.getAnnotation(Test.class);
            boolean isPublic = java.lang.reflect.Modifier.isPublic(m.getModifiers());
            if ((annotation != null) && (isPublic) && (m.getName().toLowerCase().startsWith("test"))
                    && (!isIgnored(m.getName()))) {
                String packageName = m.getName().substring(9, m.getName().length());
                try {
                    deletePackageAndScenario(defaultProperties, packageName, "BulkLoadORACLE_DWH_STG");
                } catch (Exception ex) {
                    try {
                        deletePackageAndScenario(defaultProperties, packageName, "BulkLoadORACLE_DWH_DMT");
                    } catch (Exception ex1) {
                        logger.debug(ex1.getMessage());
                    }
                    logger.debug(ex.getMessage());
                }
                logger.debug(i++ + ": package " + packageName + " deleted.");
            }
        }
        close();
    }

    @After
    public void close() {
        super.close();
    }

//	@After public void verifyLogs() {
//		if(getListAppender().contains(Level.ERROR, false)){
//			assert(!getListAppender().contains(Level.ERROR, false));
//			throw new RuntimeException("Logs contained errors.");
//		}
//	}

    @Before
    public void setContextBeforeTestCase() {
        String name = testMethodName.getMethodName();
        logger.info("testName -->" + name);
        if (!isIgnored(name)) {
            testName = name.substring(9, name.length());
        } else {
            testName = null;
        }
        metadataDirectory = TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
    }

    @Before
    public void cleanupAllPackages() {
        // the first test case triggers overall cleanup of packages and
        // scenarios.
        // this is just in case that packages were previously not removed.
        if (testMethodName.getMethodName().equalsIgnoreCase(IGNORED_METHODS[1])) {
            deleteAllPackagesAndScenarios();
        }
    }

    @After
    public void cleanupPackageAfterTestCase() {
        // Assumes that testName is set correctly in an method with @Before
        // Annotation
        if (testName != null) {
            try {
                deletePackageAndScenario(defaultProperties, testName, "BulkLoadORACLE_DWH_STG");
                logger.debug("package " + testName + " deleted after execution test case.");
            } catch (RuntimeException ex) {
                logger.info(ex.getMessage());
            }
        }
    }

    /**
     * This tests basic repository creation, importing of ODI zip files,
     * Database Server start, updating of agent odiparams.sh / odiparams.bat,
     * starting of the odiagent. Deploying the basic tables using the ODI
     * package "INIT"
     * <p>
     * Add the @Test annotation to run this test.
     *
     * @category Install
     */
    @Test
    public void test010Install() {

    }

    private void deletePackageAndScenario(String config, String scenarioName, String folderName) {
        deleteScenario(config, scenarioName.toUpperCase());
        deletePackage(config, scenarioName.toUpperCase(), folderName);

        // comment next line if you want to debug,
        // but don't forget to uncomment it again,
        // or tests will fail.
        deleteTransformations(config, scenarioName);
    }

    private void deletePackage(String config, String scenarioName, String folderName) {
        // delete package
        logger.info(String.format("Deleting package '%s' in folder '%s'.", scenarioName, folderName));
        runController("dp", config, "--package", scenarioName, "-f", folderName);
    }

    private void deleteScenario(String config, String scenarioName) {
        // delete scenario
        logger.info("deleting: " + scenarioName);
        runController("ds", config, "--scenario", scenarioName.toUpperCase());
    }

    private void deleteProcedures(String config) {
        // delete scenario
        logger.info("deleting: procedures");
        runController("delproc", config);
    }

    private void deleteTransformations(String config, String testName) {
        String testNameCaseInsensitve = testName.toUpperCase();
        //Test16010IKM_1S_Default_Success
        if (testNameCaseInsensitve.equals(("IKM_1S_Default_Success".toUpperCase()))) {
            return;
        }
        if (testNameCaseInsensitve.equals(("FlowStrategy_1S_Distinct_Success".toUpperCase()))) {
            return;
        }
        if (testNameCaseInsensitve.equals(("FlowStrategy_use_expressions".toUpperCase()))) {
            return;
        }
        if (testNameCaseInsensitve.equals(("PackageCreation_ExecProcWithParams".toUpperCase()))) {
            return;
        }
        // delete transformation
        logger.info(String.format("Deleting transformations '%s' in metadataDirectory '%s'.", testName, metadataDirectory));
        runController("dt", config, "-p", "Init ", "-m", metadataDirectory);
    }

    /**
     * @category success
     * <p>
     * This test that invalid xml raises an exception. At present this
     * test fails.
     * <p>
     * Add the @Test annotation to run this test.
     */
    @Test
    // success
    public void test020Generation() {
        // Generate interfaces
        try {
            runController("ct", defaultProperties, "-p", "Inf", "-m", TEST_XML_BASE_DIRECTORY + "/xml/Generation");
            // an exception was not thrown up the stack
            Assert.fail("An incorrect xml file definition did not raise an error, it should.");
        } catch (AssertionError ae) {
            // an exception was thrown which is desired behavior,
            // since the xml definition is not valid.
            Assert.assertTrue(true);
        } catch (Exception e) {
            // an exception was thrown which is desired behavior,
            // since the xml definition is not valid.
            Assert.assertTrue(true);
        }
    }

    /**
     * @category success
     * <p>
     * This tests that an exception is thrown when 0.xml is not
     * present, since the action "etls" relies on 0.xml
     * <p>
     * Add the @Test annotation to run this test.
     */
    @Test
    // success
    public void test030ing() {
        String testName = "ing";
        // Generate interfaces
        generationInterfaceAssertFailure(testName, "This test threw an exception it should not.");
    }

    @Test
    // success
    public void test031testEnableTestBehavior() {

        try {
            // Generate interfaces
            List<String> argList = new ArrayList<String>();

            argList.add("-a");
            argList.add("etls");
            argList.add("-c");
            argList.add("missing"); // should cause an ErrorReport and hence throw exception.
            // add default passwords in here - may need to be externalized
            argList.add("-pw");
            argList.add(regressionConfiguration.getOdiSupervisorPassword());
            argList.add("-mpw");
            argList.add(regressionConfiguration.getMasterRepositoryJdbcPassword());
            argList.add("-devmode");
            getController().run(argList.toArray(new String[0]));
            Assert.fail("This test should have an error report and hance throw exception; it did not.");
        } catch (RuntimeException rte) {
            logger.info("test031testEnableTestBehavior passed.");
        }
    }

    /**
     * @throws TransformationAccessStrategyException
     * @category success
     * <p>
     * Add the @Test annotation to run this test.
     */
    @Test
    // success
    public void test10001Trans_1S_DefaultName_Success() throws Exception {
        trans_1S_Common(true, "Init S_DA_O", "BulkLoadORACLE_DWH_STG");
    }

    /**
     * @throws TransformationAccessStrategyException
     * @category success
     */
    @Test
    // success
    public void test10002Trans_1S_ExplicitName_Success() throws Exception {
        trans_1S_Common(false, "Init Test110Trans_1S_ExplicitName_Success",
                "BulkLoadExplicit/TargetFolder");
    }

    /**
     * @throws TransformationAccessStrategyException
     * @category success
     * <p>
     * Add the @Test annotation to run this test.
     */
    @Test
    // success
    public void test10003Trans_1S_TempDefaultName_Success() throws Exception {
        trans_1S_Common(false, "I_S_DA_O_S01", "BulkLoadORACLE_DWH_STG");
    }

    /**
     * @category removed -17654: Parameter Data Store must be not null.
     * <p>
     * Explicit names with Temporary Interfaces are not supported.
     */
    /// @Test//removed
    public void test10004Trans_1S_TempExplicitName_Success() throws Exception {
        String prefix = "Init ";
        // Generate interfaces
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        String interfaceName = prefix + "Test110Trans_1S_ExplicitName_Success TruncateInsert";
        Collection<T> interfaces = this.odiAccessStrategy
                .findMappingsByProject(getRegressionConfiguration().getProjectCode());
        boolean found = false;
        for (T odiInterface : interfaces) {
            if (odiInterface.getName().equals(interfaceName)) {
                found = true;
                break;
            }
        }
        assertTrue(found); // : "The interface " + interfaceName +
        // " was not found.";
    }

    /**
     * @category success Changed to AssertFailure
     */
    @Test
    // success
    public void test11001Set_1D_illegalSetOps_Warning() {
        // Generate interfaces
        generationInterfaceAssertFailure(testName, "This test did not threw an exception it should.");
    }

    /**
     * @category success
     * <p>
     * Does not report warning for first dataset UNION ALL operator,
     * since this is the default.
     */
    @Test
    // success
    public void test11002Set_2D_IllegalSetOps_Warning() {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception it should.");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11003Set_2D_Default_Union_All_Success() {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/Set_2D_Default_Union_All_Success.xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = stgUser;
        String jdbcDBPassword = regressionConfiguration.getMasterRepositoryJdbcPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser, jdbcDBPassword);

        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
        dbUnit = null;
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11004Set_2D_Explicit_UNION_ALL_SUCCESS() {
        set_2D_Explicit_Common(TEST_XML_BASE_DIRECTORY + "/Set_2D_Default_Union_All_Success.xml");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11005Set_2D_Explicit_UNION_SUCCESS() {
        set_2D_Explicit_Common(TEST_XML_BASE_DIRECTORY + "/Set_2D_Explicit_Union_Success.xml");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11006Set_2D_Explicit_MINUS_SUCCESS() {
        set_2D_Explicit_Common(TEST_XML_BASE_DIRECTORY + "/Set_2D_Explicit_Minus_Success.xml");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11007Set_2D_Explicit_INTERSECT_SUCCESS() {
        set_2D_Explicit_Common(TEST_XML_BASE_DIRECTORY + "/Set_2D_Explicit_intersect_Success.xml");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11008Set_3D_MinusAndUnion_SUCCESS() {
        set_2D_Explicit_Common(TEST_XML_BASE_DIRECTORY + "/Set_3D_MinusAndUnion_SUCCESS.xml");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11009Source_2S_Default_Success() {
        source_2S_Common(true, false);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test11010Source_2S_OneIntf_Success() {
        source_2S_Common(false, false);
    }

    /**
     * @category waiting for updated properties
     */
    @Test
    // success
    public void test11011Source_2S_OneTempIntf_Success() {
        // explicitly delete package used for called test case and alter
        // testName
        // to reflect context of called test case.
        testName = "Source_2S_OneIntf_Success";
        metadataDirectory = TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
        if (new OdiVersion().isVersion1213()) {
            // test if the folder exist meaning that the 11g and 12c xml files
            // are different. Update to the new filename with "_12c" suffix.
            File testDirectory = new File(
                    TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName);
            if (testDirectory.exists()) {
                metadataDirectory = TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
            }
        }
        try {
            deletePackageAndScenario(defaultProperties, testName, "BulkLoadORACLE_DWH_STG");
            getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                    stgUserJDBC, "truncate table DWH_STG.S_SOURCE_2S_ONEINTF_I");

        } catch (RuntimeException ex) {
            logger.info(ex.getMessage());
        }
        test11010Source_2S_OneIntf_Success();
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/Source_2S_Default_success.xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            assertTrue(dbUnit.areEqual(new File(dumpFile), "REF.S_DA_O", "DWH_STG.S_SOURCE_2S_ONEINTF_I"));
        } catch (Exception e) {
            logger.fatal(e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * @throws TransformationAccessStrategyException
     * @category success
     */
    @Test
    // success
    public void test11012Source_2S_SubSelectTempIntf_Success() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        try {
            getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                    stgUserJDBC, "drop table DWH_STG.I_S_SOURCE_2S_SUBSELECT_I_S01");
        } catch (Exception ex) {
            logger.debug("Temp table I_S_SOURCE_2S_SUBSELECT_I_S01 not dropped.");
        }
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));

        T odiInterface = this.odiAccessStrategy.findMappingsByName("Init SOURCE_2S_SUBSELECTTEMPINTF",
                getRegressionConfiguration().getProjectCode());
        boolean result = false;
        try {
            result = odiAccessStrategy.isOneOfTheSourcesDerived(odiInterface);
        } catch (Exception e) {
            logger.fatal(e);
        }
        assertTrue(result);

        dbUnit = null;
    }

    @Test
    // success
    public void test11017Source_2S_SubSelectLookup() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        try {
            getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                    stgUserJDBC, "drop table DWH_STG.I_S_11017_I_S01");
        } catch (Exception ex) {
            logger.debug("Temp table DWH_STG.I_S_11017_I_S01 not dropped.");
        }
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));

        T odiInterface = this.odiAccessStrategy.findMappingsByName("Init SOURCE_2S_SUBSELECTLOOKUP",
                regressionConfiguration.getProjectCode());
        boolean result = false;
        try {
            result = this.odiAccessStrategy.isOneOfTheSourcesDerived(odiInterface);
        } catch (Exception e) {
            logger.fatal(e);
        }

        assertTrue(result);

        dbUnit = null;
    }

    /**
     * @category success
     */
    // @Test
    // success
    public void test11014Source_2S_SubSelectIntf_Warning() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test did not report warning.");
    }

    /**
     * @throws one.jodi.core.extensions.strategies.NoModelFoundException : Unable to determine a model for data store 'NO_EXISTS'. It
     *                                                                   does not exist in any model. Check name of the specified data
     *                                                                   store. Allowed for failure instead of error.
     * @category success
     */
    @Test
    // success
    public void test11015Source_1S_Undefined_Error() {
        generationInterfaceAssertFailure(testName, "This test threw an exception it should not.");

    }

    // ///////////////////////////////////////////////////////////////////
    // Model
    // ///////////////////////////////////////////////////////////////////

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11110Model_2S_Default_Success() throws Exception {
        model_2S_Common(stgUser, stgUserJDBC, "DWH_STG");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11120Model_2S_Model2_Success() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DB_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
        //
        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, stgUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";
        //
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DB_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        dbUnit = null;
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11130Model_2S_OverrideS2_Model2_Success() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = dmtUserJDBCDriver;
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }
        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, stgUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        dbUnit = null;
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11140Model_2S_OverrideS1_Success() throws Exception {
        model_2S_Common(refUser, refUserJDBC, "REF");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11150Model_2D_Default_Success() throws Exception {
        model_2D_Common();
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11160Model_2D_Override_All_Success() throws Exception {
        model_2D_Common();
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11170Model_2D_Override2D_Success() throws Exception {
        model_2D_Common();
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11180Model_1S_UndefinedModel_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11190Model_1S_UndefinedCode_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11200Model_1S_1L_OverrideLookup_Success() throws Exception {
        model_1S_Override_Common(true);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test11210Model_1S_OverrideTarget_Success() throws Exception {
        model_1S_Override_Common(false);
    }

    // ///////////////////////////////////////////////////////////////////
    // Filters
    // ///////////////////////////////////////////////////////////////////

    /**
     * @category success
     */
    @Test
    // success
    public void test12000Filter_1S_Alias_Success() {
        filterJoinLookupTestCommon(false);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12010Filter_1S_No_Alias_Success() {
        filterJoinLookupTestCommon(false);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12020Filter_1S_UndefinedAlias_Failed() {
        generationInterfaceAssertFailure(testName, "This did not threw an exception it should.");
    }

    /**
     * @category failed
     */
    // @Test//failed
    public void test12030Filter_1S_MalformedFilter_Failed() {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception it should.");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12040Filter_2S_TwoFilters_Success() {
        filterJoinLookupTestCommon(false);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12050Filter_2S_AndFilters_Success() {
        filterJoinLookupTestCommon(false);
    }

    /**
     * @category success
     * <p>
     * Allowed for warning instead of failure
     */
    @Test
    // success
    public void test12060Filter_2S_AndFilterUndefinedAlias_Failure() {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception, it should.");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12070Filter_2S_AndFilterIncorrectAliasReference_Failure() {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception it should.");
    }

    /**
     * @category success
     * <p>
     * Allowed for reporting warning instead of failure.
     */
    @Test
    // success
    public void test12080Filter_2S_AndFilterIncorrect2ndAliasReference_Failure() {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception it should.");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12090Filter_2D_OneFilter_Success() {
        filterJoinLookupTestCommon(false);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12100Filter_2D_TwoFilter_Success() {
        filterJoinLookupTestCommon(false);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test12110Filter_2D_OneFilterIncorrectAliasReference_Failure() {
        generationInterfaceAssertFailure(testName, "This test did not threw exception it should.");
    }

    @Test
    // success
    public void test12120Filter_1S_Explicit_ExecutionLocation_Success() throws Exception {
        // filterJoinLookupTestCommon(false);

        if (!new OdiVersion().isVersion11()) {
            generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
            T m = odiAccessStrategy.findMappingsByName("Init FILTER_1S_EXPLICIT_EXECUTIONLOCATION_SUCCESS",
                    getRegressionConfiguration().getProjectCode());

            Map<String, String> filterExecutionLocations = odiAccessStrategy.getFilterExecutionLocations(m);
            for (String component : filterExecutionLocations.keySet()) {
                assertEquals("SOURCE", filterExecutionLocations.get(component));
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////
    // Joins
    // ///////////////////////////////////////////////////////////////////

    /**
     * @category success
     */
    @Test
    // success
    public void test13010Join_1S_Inner_Failure() {
        // alter testName to reflect desired context.
        // explicitly delete package since not automatically handled
        testName = "Filter_2D_OneFilterIncorrectAliasReference_Failure";
        try {
            deletePackageAndScenario(defaultProperties, testName, "BulkLoadORACLE_DWH_STG");
        } catch (RuntimeException ex) {
            logger.info(ex.getMessage());
        }
        generationInterfaceAssertFailure(testName, "This test did not threw an exception it should.");
    }

    /**
     *
     */
    @Test
    // success
    public void test13020Join_1S_CROSS_Failure() {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception it should.");
    }

    /**
     * @category succes
     */
    @Test
    // success
    public void test13025Join_1S_NATURAL_Failure() {
        generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test13030Join_2S_Default_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test13040Join_2S_SelfReference_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test13050Join_2S_LEFT_OUTER_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category removed
     * <p>
     * -17601: Ordered right outer join is not supported.
     */
    // @Test//removed
    public void test13052Join_2S_RIGHT_OUTER_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test13054Join_2S_INNER_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * This test fails while FULL outer joins are not supported on HYPERSONIC
     * SQL.
     *
     * @category removed Scenarios failed com.sunopsis.dwg.codeinterpretor.
     * SnpGeneratorSQLCIT$SnpGeneratorException: ODI-15037: The
     * interface Init JOIN_2S_FULL_SUCCESS TruncateInsert has fatal
     * errors.
     */
    // @Test//removed
    public void test13056Join_2S_FULL_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test13060Join_2S_CROSS_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @throws TransformationAccessStrategyException
     * @category success comments above.
     */
    // @Test
    public void test13065Join_2S_NATURAL_Success() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception which it should not.");

        T odiInterface = this.odiAccessStrategy.findMappingsByName("Init JOIN_2S_NATURAL_SUCCESS",
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        try {
            found = this.odiAccessStrategy.areAllDatastoresJoinedNaturally(odiInterface);
        } catch (Exception e) {
            logger.debug(e);
        }
        assertTrue(found);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test13070Join_2S_CROSS_WithIncorrectCondition_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test13070Join_2S_NATURAL_WithIncorrectCondition_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test13080Join_2S_MissingJoinCondition_Failure() {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception which it should.");
    }

    /**
     * @category success test cannot be performed: Cross Join does not have join
     * condition.
     */
    @Test
    // success
    public void test13090Join_2S_Wrong_CROSS_Reference_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception which it should.");
    }

    /**
     * @category removed test cannot be performed on HSQLDB: Natural Join not
     * supported
     */
    // @Test//removed
    public void test13095Join_2S_Wrong_NATURAL_Reference_Failure() throws Exception {
        throw new RuntimeException("This test cannot be performed on HSQLDB: Natural Join not supported.");
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test13100Join_3S_InnerAndOuter_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @throws Exception
     * @category Not implemented
     */
    // @Test//fail
    public void test13110Join_3S_XXXX_Success() throws Exception {
        throw new RuntimeException("This test is not yet implemented.");
    }

    @Test
    // success
    public void test13120Join_2S_Explicit_ExecutionLocation_Success() throws Exception {
        // filterJoinLookupTestCommon(true);

        if (!new OdiVersion().isVersion11()) {
            generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");

            T m = odiAccessStrategy.findMappingsByName("Init JOIN_2S_EXPLICIT_EXECUTIONLOCATION_SUCCESS",
                    getRegressionConfiguration().getProjectCode());

            Map<String, String> joinExecutionLocations = odiAccessStrategy.getJoinExecutionLocations(m);
            for (String component : joinExecutionLocations.keySet()) {
                assertEquals("SOURCE", joinExecutionLocations.get(component));
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Lookups
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @category success
     */
    @Test
    // success
    public void test14010Lookup_1S_1L_Default_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test14020Lookup_1S_1L_LeftOuter_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category success
     */
    @Test
    // success no equivalent exists in ODI 12
    public void test14025Lookup_1S_1L_Scalar_Success() {
        if (new OdiVersion().isVersion11()) {
            filterJoinLookupTestCommon(true);
        }
    }

    @Test
    // success
    public void test14026Lookup_1S_1L_DefaultRow_Success() {
        filterJoinLookupTestCommon(true);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test14030Lookup_1S_1L_Incorrect_Alias_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception which it should.");
    }

    /**
     * @category fail
     */
    // @Test//fail
    public void test14040Lookup_1S_1L_MalformedJoin_Failed() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception which it should.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test14050Lookup_1S_2L_Default_Success() throws Exception {
        mappingLookup_Common("DWH_STG.S_DA_O", true, defaultProperties);
        T mapping = odiAccessStrategy.findMappingsByName("Init LOOKUP_1S_2L_DEFAULT_SUCCESS",
                "BulkLoadORACLE_DWH_STG",
                getRegressionConfiguration().getProjectCode());
        List<IMapComponent> sources = ((MapRootContainer) mapping).getSources();
        Optional<IMapComponent> source = sources.stream().filter(s -> s.getName().equalsIgnoreCase("D1DA")).findFirst();
        if (!source.isPresent()) {
            throw new RuntimeException("Can't find source");
        }
        Optional<IMapComponent> lookup = source.get().getDownstreamConnectedLeafComponents().stream().findFirst();
        if (!lookup.isPresent()) {
            throw new RuntimeException("Can't find lookup");
        }
        Optional<IMapComponent> datastore = lookup.get().getUpstreamConnectedLeafComponents().stream().filter(d -> d.getName().equals("D1DB"))
                .findFirst();
        if (!datastore.isPresent()) {
            throw new RuntimeException("Lookups need to be ordered and the first lookup should be S_DB_I with alias D1DB.");
        }
        if (!datastore.get().getName().equalsIgnoreCase("D1DB")) {
            throw new RuntimeException(
                    String.format("Lookups need to be ordered and the first lookup should be S_DB_I with alias D1DB it is %s.",
                            datastore.get().getName()));
        }

    }

    /**
     * @category success
     */
    @Test
    // success
    public void test14060Lookup_1S_2ndUndefinedAlias_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");

    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    public void test14070Lookup_2S_1L_Default_Success() throws Exception {
        mappingLookup_Common("DWH_STG.S_DA_O", true, defaultProperties);
    }

    /**
     * @category success
     */
    @Test
    // success
    public void test14080Lookup_2S_1L_IncorrectAliasReference_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test14090Lookup_2D_1L_Default_Success() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception which it should not.");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");

        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");

    }

    /**
     * @category success
     */
    @Test
    // success
    public void test14100Lookup_2D_1L_IncorrectAliasReference_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");
    }

    // ///////////////////////////////////////////////////////////////////
    // Mapping
    // ///////////////////////////////////////////////////////////////////

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15010Mapping_1S_FullAuto_Success() throws Exception {
        mappingLookup_Common("DWH_STG.S_DA_O", true, defaultProperties);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15020Mapping_1S_MissingColumn_Error() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not report error.");
    }

    /**
     * @throws Exception Allowed failure as results
     * @category success
     */
    @Test
    // success
    public void test15030Mapping_1S_UndefinedColumn_Warning() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not report a warning.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15040Mapping_1S_Distinct_Success() throws Exception {
        mappingLookup_Common("DWH_STG.S_DA_O", true, defaultProperties);
        T m = odiAccessStrategy.findMappingsByName("Init MAPPING_1S_DISTINCT_SUCCESS",
                getRegressionConfiguration().getProjectCode());
        assertTrue(odiAccessStrategy.isDistinctMapping(m));
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15050Mapping_1S_AutoLargerTypeLength_Success() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DC_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DC_I");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        logger.info(dumpFile);
        assertTrue(dbUnit.areEqual("REF.S_DA_I", "DWH_STG.S_DA_I"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DC_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DC_I");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15060Mapping_1S_AutoSmallerTypeLength_Warning() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.");
    }

    /**
     * Allowed for warning instead of error report
     *
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15070Mapping_1S_AutoDifferentTypes_Error() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test did not report error.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15080Mapping_1S_AutoAndManual_Success() throws Exception {
        mappingLookup_Common("DWH_STG.S_DA_O", true, defaultProperties);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    public void test15081Mapping_1S_Temp_Success() throws Exception {
        mappingLookup_Common("DWH_STG.S_MAPPING_1S_TEMP_O", true, defaultProperties);
    }

    /**
     * @throws Exception
     * @category succes
     */
    @Test
    // success
    public void test15082Mapping_1S_TempSmallerTypeLength_Warning() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test did not report warning.");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15083Mapping_1S_Magic_Success() throws Exception {
        generationInterfaceAssertSuccess(testName, "this test did not report an error",
                TEST_PROPERTIES_BASE_DIRECTORY + "/15083_FunctionalTest.properties");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DC_D");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_DMT.W_DC_D");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());

        assertTrue(dbUnit.areEqual("REF.W_DC_D", "DWH_DMT.W_DC_D"));
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DC_D");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_DMT.W_DC_D");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15084Mapping_2S_AutoMixed_Success() throws Exception {
        mapping_2S_Auto_Common(true);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15085Mapping_2S_AutoPrecedence_Success() throws Exception {
        mapping_2S_Auto_Common(true);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    public void test15086Mapping_2S_AutoOverridePrecedence_Success() throws Exception {
        mapping_2S_Auto_Common(true);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    public void test15087Mapping_2D_FullAuto_Success() throws Exception {
        mapping_2D_Common(true);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15088Mapping_2D_PartialAuto_Success() throws Exception {
        mapping_2D_Common(false);

    }

    @Test
    public void test15089Mapping_1S_TempNum_Success() throws Exception {
        // generationInterfaceAssertSuccess(testName, "Mapping failed");
        // deleteTemporaryInterface("I_S_MAPPING_1S_TEMP_O_S03");
        mappingLookup_Common("DWH_STG.S_MAPPING_1S_TEMP_O", true, defaultProperties);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15090Mapping_2D_Manual_Success() throws Exception {
        mappingLookup_Common("DWH_STG.S_DA_O", true, defaultProperties);
    }

    /**
     * @throws Exception
     * @category successs
     */
    @Test
    public void test15100Mapping_2D_MissingExpression_Error() throws Exception {
        generationInterfaceAssertFailure(testName, "");
    }

    /**
     * @throws Exception Allowed warning instead of error.
     * @category success
     */
    // @Test // Disabled check b/c it interferes with function calls in expressions
    public void test15110Mapping_2D_PartialMissingExpression_Error() throws Exception {
        generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");

    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test15120Mapping_2D_TooManyExpressions_Warning() throws Exception {
        generationInterfaceAssertFailure(testName, "");
    }

    @Test
    // success
    public void test15130Mapping_1S_ExplicitMandatory_Success() throws Exception {
        // mappingLookup_Common("DWH_STG.S_DA_O", true);
        logger.info("---->" + new OdiVersion().getVersion());
        if (!new OdiVersion().isVersion11()) {
            generationInterfaceAssertSuccess((this.testName), "This test threw an exception which it should not.");
            // T m =
            // odiAccessStrategy.findMappingsByName(getRegressionConfiguration().getProjectCode(),
            // "Init MAPPING_1S_EXPLICITMANDATORY_SUCCESS", new HashMap<String,
            // T>());
            Map<String, Boolean> flags = odiAccessStrategy.getFlags(getRegressionConfiguration().getProjectCode(),
                    "Init MAPPING_1S_EXPLICITMANDATORY_SUCCESS", "VALUE");
            assertTrue(flags.get(OdiTransformationAccessStrategy.MANDATORY));
        }

    }

    @Test
    // success
    public void test15140Mapping_1S_ExplicitKey_Success() throws Exception {
        if (!new OdiVersion().isVersion11()) {
            // Needs to be failure due to error setting update key for IKM as
            // key isnt defined on datstore.
            generationInterfaceAssertFailure(testName, "This test threw an exception which it should not.");
            // T m =
            // odiAccessStrategy.findMappingsByName(getRegressionConfiguration().getProjectCode(),
            // "Init MAPPING_1S_EXPLICITKEY_SUCCESS", new HashMap<String, T>());
            // assertTrue(odiAccessStrategy.isKey(m, "KEY"));

            Map<String, Boolean> flags = odiAccessStrategy.getFlags(getRegressionConfiguration().getProjectCode(),
                    "Init MAPPING_1S_EXPLICITKEY_SUCCESS", "KEY");
            logger.info("KEY = " + flags.get(OdiTransformationAccessStrategy.KEY));
            assertTrue(flags.get(OdiTransformationAccessStrategy.KEY));
        }

    }

    // ///////////////////////////////////////////////////////////////////
    // KM / IKM / LKM / CKM
    // ///////////////////////////////////////////////////////////////////

    /**
     * Since JUnit relies in ordering of the resultset, e.g. the select * from
     * table returns rows ordered, which is not the case with oracle heap
     * tables. The table type of W_FA_F is changed to be a index organized
     * table.
     *
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16010IKM_1S_Default_Success() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception which it should not.");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_FA_F");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }
        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_FA_F");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");

        try {
            logger.info(testName);
            T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                    regressionConfiguration.getProjectCode());
            assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM Oracle Control Append"));
        } catch (ResourceNotFoundException e) {
            if (!new OdiVersion().isVersion11())
                throw e;
        }

    }

    /**
     * @category success
     */
    @Test
    // success - bad properties value
    public void test16020IKM_1S_UnknownDefault_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception.",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16020_FunctionalTest.properties");
    }

    /**
     * @throws Exception This test succeeds but that is due to the failed test of
     *                   16010.
     * @category success
     */
    @Test
    // success
    public void test16030IKM_1S_DefaultParameter_Success() throws Exception {
        iKM_1S_Common(false);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16040IKM_1S_ExplicitWithDefault_Parameters_Success() throws Exception {
        iKM_1S_Common(false);
        T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                regressionConfiguration.getProjectCode());
        assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL Incremental Update"));

    }

    /**
     * @category success
     */
    @Test
    // success
    public void test16050IKM_1S_ExplicitUnknown_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not threw an exception.",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16020_FunctionalTest.properties");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16060IKM_1S_ExplicitWithExplicitParameters_Success() throws Exception {
        iKM_1S_Common(true);
        T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                regressionConfiguration.getProjectCode());
        assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM Oracle Incremental Update"));
    }

    /**
     * @throws Exception Allowed failure instead of error
     * @category succcess
     */
    @Test
    public void test16070IKM_1S_ExplicitUnknown_Parameter_Error() throws Exception {
        generationInterfaceAssertFailure(testName, "");
    }

    /**
     * @throws Exception
     * @category fail
     */
    @Test
    public void test16080IKM_1S_Unknown_ParameterValue_Error() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not report error.");
    }


    //TODO enable this test by copying file to linux
    //@Test
    public void test16085IKM_SQL_TO_FILE_APPEND_1S() throws Exception {
        updateFileDataServer(testName);
        generationInterfaceAssert(Level.WARN, testName, "this test did not report an error",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16086_FunctionalTest.properties");

        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        logger.info("Starting scenario");
        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        logger.info("Starting dbunit");
        // assertTrue(dbUnit.areEqual("DWH_STG.S_DA_I", "DWH_SRC.S_DA"));

        logger.info("Starting truncate"); // "The data is not equal with
        // reference data.";

        T odiInterface = this.odiAccessStrategy.findMappingsByName("Init IKM_SQL_TO_FILE_APPEND_1S",
                regressionConfiguration.getProjectCode());
        if (new OdiVersion().isVersion11()) {
            assertEquals("ORACLE_DWH.DWH_STG", this.odiAccessStrategy
                    .findStagingAreas(odiInterface, regressionConfiguration.getOdiContext()).iterator().next());
        } else {
            assertEquals("FILE_SRC_DWH_UNIT", this.odiAccessStrategy
                    .findStagingAreas(odiInterface, regressionConfiguration.getOdiContext()).iterator().next());
        }
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        T mapping = odiAccessStrategy.findMappingsByName("Init " + testName, regressionConfiguration.getProjectCode());
        assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL to File Append"));
    }

    //TODO enable this test by copying file to linux
    //@Test
    public void test16086IKM_SQL_TO_FILE_APPEND_2S() throws Exception {
        updateFileDataServer(testName);
        generationInterfaceAssert(Level.WARN, testName, "this test did not report an error",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16086_FunctionalTest.properties");

        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        logger.info("Starting scenario");
        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        logger.info("Starting dbunit");
        // assertTrue(dbUnit.areEqual("DWH_STG.S_DA_I", "DWH_SRC.S_DA"));
        logger.info("Starting truncate"); // "The data is not equal with
        // reference data.";

        T odiInterface = this.odiAccessStrategy.findMappingsByName("Init IKM_SQL_TO_FILE_APPEND_2S",
                regressionConfiguration.getProjectCode());
        if (new OdiVersion().isVersion11()) {
            assertEquals("ORACLE_DWH.DWH_STG", this.odiAccessStrategy
                    .findStagingAreas(odiInterface, regressionConfiguration.getOdiContext()).iterator().next());
        } else {
            assertEquals("ORACLE_DWH_STG_UNIT", this.odiAccessStrategy
                    .findStagingAreas(odiInterface, regressionConfiguration.getOdiContext()).iterator().next());
        }
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        T mapping = odiAccessStrategy.findMappingsByName("Init " + testName, regressionConfiguration.getProjectCode());
        assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL to File Append"));
    }

    //TODO enable this test by copying file to linux
    //@Test
    public void test16087IKM_SQL_TO_FILE_APPEND_2S_Explicit() throws Exception {
        updateFileDataServer(testName);
        generationInterfaceAssert(Level.WARN, testName, "this test did not report an error",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16087_FunctionalTest.properties");

        T odiInterface = this.odiAccessStrategy.findMappingsByName("Init IKM_SQL_TO_FILE_APPEND_2S_Explicit",
                regressionConfiguration.getProjectCode());
        Set<String> stagingAreaNames = this.odiAccessStrategy.findStagingAreas(odiInterface,
                regressionConfiguration.getOdiContext());
        if (new OdiVersion().isVersion11()) {
            assertEquals("ORACLE_DWH_SRC.DWH_SRC", stagingAreaNames.iterator().next());
        } else {
            assertEquals("ORACLE_DWH_STG_UNIT", stagingAreaNames.iterator().next());
        }

        logger.info("Starting scenario");
        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        logger.info("Starting dbunit");
        // TODO 12
        // assertTrue(dbUnit.areEqual("DWH_STG.S_DA_I", "DWH_SRC.S_DA"));
        logger.info("Starting truncate");
        T mapping = odiAccessStrategy.findMappingsByName("Init " + testName, regressionConfiguration.getProjectCode());
        assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL to File Append"));
    }

    /**
     * Test for oracle 2 file.
     *
     * @throws IOException
     * @category throws Exception
     */
    //@Test
    public void test16085IKM_AUTOCOMPLEX_TOPOLOGY() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16087_FunctionalTest.properties");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_FA_F");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
        String pAgentUrl = defaultAgent;
        String pUser = regressionConfiguration.getOdiSupervisorUser();
        String pPassword = regressionConfiguration.getOdiSupervisorPassword();
        String pContextCode = regressionConfiguration.getOdiContext();
        String pLogLevel = "5";
        String pWorkRepName = regressionConfiguration.getOdiWorkRepositoryName();
        String pScenarioName = testName;
        odiExecuteScenario.startScenario(pAgentUrl, pUser, pPassword, pContextCode, pLogLevel, pWorkRepName, pScenarioName);

        assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
        // "The data is not equal with reference data.";

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_FA_F");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
    }

    private void updateFileDataServer(String testName2) throws IOException {
        if (RegressionTestUtilities.getTechnologyNameFromString(regressionConfiguration.getMasterRepositoryTechnology())
                .equals(TechnologyName.ORACLE)) {
            // If we are on oracle master repository
            // set the dataserver to be that of the master repository.
            OdiUpdateSchema updateSchema = new OdiUpdateSchema();
            String pDataServerName = "FILE_SRC_DWH";
            String pUsername = tempDir;
            String pPassword = regressionConfiguration.getWorkRepositoryJdbcPassword();

            String pOdiMasterRepoUrl = regressionConfiguration.getJdbcUrlMasterRepository();
            String odiMasterRepoUser = regressionConfiguration.getMasterRepositoryJdbcUser();
            String pOdiMasterRepoPassword = regressionConfiguration.getMasterRepositoryJdbcPassword();
            String odiWorkRepo = regressionConfiguration.getWorkRepositoryJdbcUsername();
            String odiLoginUsername = regressionConfiguration.getOdiSupervisorUser();
            String odiLoginPassword = regressionConfiguration.getOdiSupervisorPassword();
            String pServerInstanceName = null;
            String jdbcDriverRepository = regressionConfiguration.getMasterRepositoryJdbcDriver();
            String pSchemaName = tempDir;

            updateSchema.updateSchema(pOdiMasterRepoUrl, odiMasterRepoUser, pOdiMasterRepoPassword,
                    odiWorkRepo, odiLoginUsername, odiLoginPassword, pDataServerName, pUsername, pPassword, pServerInstanceName,
                    jdbcDriverRepository, pSchemaName);

//			updateSchema.updateDS(regressionConfiguration.getJdbcUrlMasterRepository(),
//					regressionConfiguration.getMasterRepositoryJdbcUser(),
//					regressionConfiguration.getMasterRepositoryJdbcPassword(),
//					regressionConfiguration.getWorkRepositoryJdbcUsername().substring(0, 4).replace("_", ""),
//					regressionConfiguration.getOdiSupervisorUser(), regressionConfiguration.getOdiSupervisorPassword(),
//					pDataServerName, regressionConfiguration.getJdbcUrlMasterRepository(), pUsername, pPassword,
//					null /* pServerInstanceName */, regressionConfiguration.getMasterRepositoryJdbcDriver(),
//					"oracle.jdbc.OracleDriver", tempDir);
        }
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16090CKM_1S_Default_Success() throws Exception {
        cKM_1S_Common(TEST_PROPERTIES_BASE_DIRECTORY + "/16090_FunctionalTest.properties");

    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16100CKM_1S_NotActive_Success() throws Exception {
        cKM_1S_Common(TEST_PROPERTIES_BASE_DIRECTORY + "/16100_FunctionalTest.properties");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    public void test16110CKM_1S_Manual_Success() throws Exception {
        cKM_1S_Common(null);
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16130CKM_1S_ManualAndIKM_Success() throws Exception {
        cKM_1S_Common(TEST_PROPERTIES_BASE_DIRECTORY + "/16130_FunctionalTest.properties");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16140CKM_1S_Undefined_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not throw error fatal or warning messages.",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16140_FunctionalTest.properties");
    }

    /**
     * @throws Exception
     * @category success
     */
    @Test
    // success
    public void test16150CKM_1S_Unknown_Failure() throws Exception {
        generationInterfaceAssertFailure(testName, "This test did not throw error fatal or warning messages.",
                TEST_PROPERTIES_BASE_DIRECTORY + "/16150_FunctionalTest.properties");
    }

    /**
     * Requires file in /tmp
     *
     * @throws Exception
     * @category success
     */
    //@Test
    //@Ignore // for local test only
    // TODO fix copying to linux
    public void test16160LKM_1S_AutoComplexTopology_Success() throws Exception {
        File tempFile = new File(tempDir, "countrylist.csv");
        try {
            updateFileDataServer(testName);
            logger.info("tempdir is :" + tempDir);
            copyFile(new File(TEST_XML_BASE_DIRECTORY + "/countrylist.csv"), tempFile);
            generationInterfaceAssert(Level.WARN, testName, "This test did not throw error fatal or warning messages.",
                    TEST_PROPERTIES_BASE_DIRECTORY + "/16160_FunctionalTest.properties");
            getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                    refUserJDBC, "truncate table REF.S_DA_O");
            getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                    stgUserJDBC, "truncate table DWH_STG.S_DA_O");
            String dir = ".";
            String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
            String driverDBClass = stgUserJDBCDriver;
            String jdbcDBConnection = stgUserJDBC;
            String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
            String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
            DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                    jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                    regressionConfiguration.getMasterRepositoryJdbcPassword());
            dbUnit.fullDatabaseImport();
            logger.info("Starting s");
            RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);
            logger.info("Starting dbunit");
            assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
            logger.info("Starting truncate"); // "The data is not equal with
            // reference data.";
            getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                    refUserJDBC, "truncate table REF.S_DA_O");
            getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                    stgUserJDBC, "truncate table DWH_STG.S_DA_O");

            T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                    regressionConfiguration.getProjectCode());
            assertTrue(odiAccessStrategy.checkThatAllTargetsHaveLKMName(mapping, "LKM File to SQL"));

        } finally {
            if (tempFile.exists())
                tempFile.delete();
        }
    }

    /**
     * @throws Exception
     * @category waiting for updated properties
     */
    // @Test
    public void test16170LKM_1S_ManualComplexTopology_Success() throws Exception {
        throw new Exception("This test is not yet implemented.");
        // MockLog4jAppender mockLog4jAppender =
        // generationInterfaceAssertFailure(
        // testName, "This test threw an exception which it should not.");
    }

    /**
     * @category
     */
    @Test
    //
    public void test17000Dirs_2D_PartialAuto_Success() {
        generationInterfaceAssert(Level.WARN, testName, "This test did not report error.");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
    }

    @Test

    public void test17100Dataset_relation() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test did not report error.");
        // DATASET_RELATION
        T odiInterface = this.odiAccessStrategy.findMappingsByName("Init DATASET_RELATION",
                getRegressionConfiguration().getProjectCode());
        boolean result = false;
        try {
            result = odiAccessStrategy.validateDataSetRelation(odiInterface);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertTrue(result);
    }

    @Test
    // success
    public void test11020Pivot_1S_Success() throws Exception {
        if (new OdiVersion().isVersion11())
            return;

        String interfaceName = "Pivot_1S_Success";
        String prefix = "Init ";
        interfaceName = prefix + interfaceName;
        String config = defaultProperties;

        // Generate interfaces
        deleteScenario(defaultProperties, testName);
        logger.info("Starting to create PIVOT");
        ListAppender listAppender = getListAppender();
        runController("etls", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Creation of interface logged errors.");
            throw new RuntimeException("Creation of interface logged errors.");
        }
        removeAppender(listAppender);

        assertTrue(this.odiAccessStrategy != null);
        T mapping = this.odiAccessStrategy.findMappingsByName(interfaceName,
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        if (mapping.getName().equals(interfaceName)) {
            found = true;
        }
        Assert.assertTrue(found);

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_VP_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_VP_O");


        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_VP_O", "DWH_STG.S_VP_O"));
/*
		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				refUserJDBC, "truncate table REF.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				refUserJDBC, "truncate table REF.S_PV_O");
		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_O");

*/
    }

    @Test
    public void test11030UnPivot_1S_Success() throws Exception {
        if (new OdiVersion().isVersion11())
            return;

        String interfaceName = "UnPivot_1S_Success";
        String prefix = "Init ";
        interfaceName = prefix + interfaceName;
        String config = defaultProperties;

        // Generate interfaces
        deleteScenario(defaultProperties, testName);
        logger.info("Starting to create UNPIVOT");
        ListAppender listAppender = getListAppender();
        runController("etls", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            String msg = "Creation of interface logged errors.";
            Assert.fail(msg);
            throw new RuntimeException(msg);
        }
        removeAppender(listAppender);
        assertTrue(this.odiAccessStrategy != null);
        T mapping = this.odiAccessStrategy.findMappingsByName(interfaceName,
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        if (mapping.getName().equals(interfaceName)) {
            found = true;
        }
        Assert.assertTrue(found);


        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_VP_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_VP_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_O");


        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_PV_O", "DWH_STG.S_PV_O"));
		/*

		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				refUserJDBC, "truncate table REF.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				refUserJDBC, "truncate table REF.S_PV_O");
		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_O");
				*/

    }

    @Test
    public void test11022Pivot_2S_Success() throws Exception {
        this.testPivot("Pivot_2S_Success", defaultProperties);
    }

    @Test
    // Ensure that pivot is connected to TargetExpression when configured.
    public void test11023Pivot_1S_TgtExp_Success() throws Exception {
        this.testPivot("Pivot_1S_TgtExp_Success", TEST_PROPERTIES_BASE_DIRECTORY + "/11023.properties");
    }

    @Test
    public void test11024Pivot_1S_Filter_Success() throws Exception {
        this.testPivot("Pivot_1S_Filter_Success", defaultProperties);
    }

    @Test
    public void test11025Pivot_1S_1L_Success() throws Exception {
        this.testPivot("Pivot_1S_1L_Success", defaultProperties);
    }

    @Test
    public void test11026Pivot_1S_Distinct_Success() throws Exception {
        this.testPivot("Pivot_1S_Distinct_Success", defaultProperties);
    }

    @Test
    public void test11027Pivot_1S_Aggregate_Success() throws Exception {
        this.testPivot("Pivot_1S_Aggregate_Success", defaultProperties);
    }

    @Test
    public void test11021Pivot_2S_2D_Success() throws Exception {
        this.testPivot("Pivot_2S_2D_Success", defaultProperties);
    }

    public void testPivot(String interfaceName, String config) throws Exception {
        if (new OdiVersion().isVersion11())
            return;

        String prefix = "Init ";
        interfaceName = prefix + interfaceName;

        // Generate interfaces
        deleteScenario(defaultProperties, testName);
        logger.info("Starting to create PIVOT");
        ListAppender listAppender = getListAppender();
        runController("etls", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            String msg = "Creation of interface logged errors.";
            Assert.fail(msg);
            throw new RuntimeException(msg);
        }
        removeAppender(listAppender);
        assertTrue(this.odiAccessStrategy != null);
        T mapping = this.odiAccessStrategy.findMappingsByName(interfaceName,
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        if (mapping.getName().equals(interfaceName)) {
            found = true;
        }
        Assert.assertTrue(found);
    }

    @Test
    public void test11040PivotUnPivot_1S_Success() throws Exception {
        if (new OdiVersion().isVersion11())
            return;

        String interfaceName = "PivotUnPivot_1S_Success";
        String prefix = "Init ";
        interfaceName = prefix + interfaceName;
        String config = defaultProperties;

        // Generate interfaces
        deleteScenario(defaultProperties, testName);
        logger.info("Starting to create UNPIVOT");
        ListAppender listAppender = getListAppender();
        runController("etls", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Creation of interface logged errors.");
            throw new RuntimeException("Creation of interface logged errors.");
        }
        removeAppender(listAppender);
        assertTrue(this.odiAccessStrategy != null);
        T mapping = this.odiAccessStrategy.findMappingsByName(interfaceName,
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        if (mapping.getName().equals(interfaceName)) {
            found = true;
        }
        Assert.assertTrue(found);


        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_O");


        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_PV_O", "DWH_STG.S_PV_O"));

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_O");

    }

//
//	@Test
//	// success
//	public void test11050SUBQUERY_1S_Success() throws Exception {
//		if(OdiVersion.isVersion11())
//			return;
//
//		String interfaceName = "SubQuery_1S_Success";
//		String prefix = "Init ";
//		interfaceName = prefix + interfaceName;
//		String config = defaultProperties;
//
//		// Generate interfaces
//		deleteScenario(defaultProperties, testName);
//		Logger rootLogger = Logger.getRootLogger();
//		MockLog4jAppender mockAppender = new MockLog4jAppender();
//		rootLogger.addAppender(mockAppender);
//		runController("etls", config, "-p", prefix, "-m", metadataDirectory);
//		if (mockAppender.contains(Level.FATAL, "") || mockAppender.contains(Level.WARN, "")) {
//			Assert.fail("The creation of the interface " + prefix + testName
//					+ " TruncateInsert logged a fatal or warn message.");
//		}
//		assertTrue(this.odiAccessStrategy != null);
//		T mapping = this.odiAccessStrategy.findMappingsByName(getRegressionConfiguration().getProjectCode(),
//				interfaceName, new HashMap<String, T>());
//		boolean found = false;
//		if (mapping.getName().equals(interfaceName)) {
//			found = true;
//		}
//		Assert.assertTrue(found);
//
//		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				refUserJDBC, "truncate table REF.S_PV_I");
//		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				refUserJDBC, "truncate table REF.S_PV_O");
//		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				refUserJDBC, "truncate table REF.S_VP_I");
//		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				refUserJDBC, "truncate table REF.S_VP_O");
//		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
//		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				stgUserJDBC, "truncate table DWH_STG.S_PV_O");
//		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				stgUserJDBC, "truncate table DWH_STG.S_VP_O");
//		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				stgUserJDBC, "truncate table DWH_STG.S_VP_O");
//
//
//		String dir = ".";
//		String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
//		String driverDBClass = stgUserJDBCDriver;
//		String jdbcDBConnection = stgUserJDBC;
//		String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
//		String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
//		DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
//				jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
//				regressionConfiguration.getMasterRepositoryJdbcPassword());
//		try {
//			dbUnit.fullDatabaseImport();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			Assert.fail(ex.getMessage());
//		}
//
//		RegressionTestUtilities.startScenario(odiExecute, "5", testName, regressionConfiguration, defaultAgent);
//
//		assertTrue(dbUnit.areEqual("REF.S_PV_O", "DWH_STG.S_PV_O"));
///*
//		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				refUserJDBC, "truncate table REF.S_PV_I");
//		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
//		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				refUserJDBC, "truncate table REF.S_PV_O");
//		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
//				stgUserJDBC, "truncate table DWH_STG.S_PV_O");
//
//*/
//	}

    private void testSubQuery(String interfaceName, String refTable, String table) throws ResourceNotFoundException, ResourceFoundAmbiguouslyException {
        if (new OdiVersion().isVersion11())
            return;

        String prefix = "Init ";
        interfaceName = prefix + interfaceName;
        String config = defaultProperties;

        // Generate interfaces
        deleteScenario(defaultProperties, testName);
        ListAppender listAppender = getListAppender();
        runController("etls", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            String msg = "Creation of interface logged errors.";
            Assert.fail(msg);
            throw new RuntimeException(msg);
        }
        removeAppender(listAppender);
        assertTrue(this.odiAccessStrategy != null);
        T mapping = this.odiAccessStrategy.findMappingsByName(interfaceName,
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        if (mapping.getName().equals(interfaceName)) {
            found = true;
        }
        Assert.assertTrue(found);

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_PV_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_VP_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_VP_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_PV_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_VP_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_VP_O");


        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual(refTable, table));
/*
		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				refUserJDBC, "truncate table REF.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				refUserJDBC, "truncate table REF.S_PV_O");
		getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_O");

*/
    }


    @Test
    // success
    public void test11050SubQuery_1S_Success() throws Exception {
        testSubQuery("SubQuery_1S_Success", "REF.S_PV_O", "DWH_STG.S_PV_O");
    }

    @Test
    // success
    public void test11051SubQuery_1S_NOTIN_Success() throws Exception {
        testSubQuery("SubQuery_1S_NOTIN_Success", "REF.S_PV_O", "DWH_STG.S_PV_O");
    }

    @Test
    // success
    public void test11052SubQuery_1S_GroupCmp_Success() throws Exception {
        testSubQuery("SubQuery_1S_GroupCmp_Success", "REF.S_PV_O", "DWH_STG.S_PV_O");
    }

    @Test
    // success
    public void test11053SubQuery_1S_EXISTS_Success() throws Exception {
        testSubQuery("SubQuery_1S_EXISTS_Success", "REF.S_PV_O", "DWH_STG.S_PV_O");
    }

    @Test
    // success
    public void test11056SubQuery_1S_Temp_Success() throws Exception {
        testSubQuery("SubQuery_1S_Temp_Success", "REF.S_PV_O", "DWH_STG.S_PV_O");
    }


    @Test
    // success
    public void test11054SubQuery_1S_ExecLoc_Success() throws Exception {
        if (new OdiVersion().isVersion11())
            return;

        String prefix = "Init ";
        String interfaceName = prefix + "SubQuery_1S_ExecLoc_Success";
        String config = defaultProperties;

        // Generate interfaces
        deleteScenario(defaultProperties, testName);
        ListAppender listAppender = getListAppender();
        runController("etls", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Creation of interface logged errors.");
            throw new RuntimeException("Creation of interface logged errors.");
        }
        removeAppender(listAppender);
        assertTrue(odiAccessStrategy != null);
        T mapping = odiAccessStrategy.findMappingsByName(interfaceName,
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        if (mapping.getName().equals(interfaceName)) {
            found = true;
        }

        Assert.assertTrue(found);
        odiAccessStrategy.getSubQueryExecutionLocation(mapping).forEach((k, v) -> {
            assert ("STAGING".equals(v)) : "Mapping " + k + " erroneously set execution location to " + v;
        });
    }


    @Test
    // success

    public void test21010Keys_1S_Default_Success() throws Exception {
        String interfaceName = "Keys_1S_Default_Success";
        String prefix = "Init ";
        interfaceName = prefix + interfaceName;
        String config = defaultProperties;
        runController("dt", config, "-p", prefix, "-m", metadataDirectory);
        // Generate interfaces
        ListAppender listAppender = getListAppender();
        runController("ct", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            String msg = "Creation of interface logged errors.";
            Assert.fail(msg);
            new RuntimeException(msg);
        }
        assertTrue(this.odiAccessStrategy != null);
        T mapping = this.odiAccessStrategy.findMappingsByName(interfaceName,
                getRegressionConfiguration().getProjectCode());
        boolean found = false;
        if (mapping.getName().equals(interfaceName)) {
            found = true;
        }
        Assert.assertTrue(found);
        FunctionalTestHelper.checkThatKeysAreSet(mapping, "KEY");
    }

    @Test
    // success
    public void test30010FlowStrategy_TargetComponent() throws Exception {
        String interfaceName = "FlowStrategy_TargetComponent";
        String prefix = "Init ";
        interfaceName = prefix + interfaceName;
        String config = TEST_PROPERTIES_BASE_DIRECTORY + "/30010.properties";
        runController("dt", config, "-p", prefix, "-m", metadataDirectory);
        // Generate interfaces
        ListAppender listAppender = getListAppender();
        runController("ct", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            Assert.fail("Creation of interface logged errors.");
            throw new RuntimeException("Creation of interface logged errors.");
        }
        removeAppender(listAppender);
    }

    @Test
    // success
    public void test30020FlowStrategy_1S_Distinct_Success() throws Exception {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30020.properties";
        mappingLookup_Common("DWH_STG.S_DA_O", true, properties);
        T m = odiAccessStrategy.findMappingsByName("Init FLOWSTRATEGY_1S_DISTINCT_SUCCESS",
                getRegressionConfiguration().getProjectCode());
        assertTrue(odiAccessStrategy.isDistinctMapping(m));
    }

    @Test
    // success
    public void test30030FlowStrategy_use_expressions() throws Exception {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30030.properties";
        mappingLookup_Common("DWH_STG.S_DA_O", true, properties);
        T m = odiAccessStrategy.findMappingsByName("Init FLOWSTRATEGY_USE_EXPRESSIONS",
                getRegressionConfiguration().getProjectCode());
        assertTrue(odiAccessStrategy.isDistinctMapping(m));
    }

    //@TODO upload db and enable
    //@Test
    // success
    public void test30303Thirty_char_limit() throws Exception {
        String targetTable = "DWH_STG.S_DA_I";
        //, boolean checkLogger, String properties;
        logger.info("Comparing " + targetTable + " from testname: " + testName);
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.", defaultProperties);
        logger.info("Comparing " + targetTable + " from testname: " + testName);
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_34567890123456789012345678_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table " + targetTable);
        logger.info("Comparing " + targetTable + " from testname: " + testName);
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
        logger.info("Comparing " + targetTable + " from testname: " + testName);
        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        // Reusable mappings can't be executed.
        logger.info("Comparing " + targetTable + " from testname: " + testName);
        assertTrue(dbUnit.areEqual("REF.S_DA_I", targetTable));

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_34567890123456789012345678_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table " + targetTable);
    }

    @Test
    // success
    public void test30040FlowStrategy_2D_LEFT_OUTER_Success() {
        filterJoinLookupTestCommon(true, defaultProperties);
    }

    @Test
    // success
    public void test30050FlowStrategy_no_expressions() {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30050.properties";
        filterJoinLookupTestCommon(true, properties);
    }

    @Test
    // success
    public void test30060FlowStrategy_aggregate() {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30050.properties";
        filterJoinLookupTestCommon(true, properties);
    }

    @Test
    // success
    public void test30070FlowStrategy_filter_set() {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30050.properties";
        filterJoinLookupTestCommon(true, properties);
    }

    @Test
    // success
    public void test30080FlowStrategy_TargetExpressions() throws Exception {
        String interfaceName = "FlowStrategy_TargetExpressions";
        String prefix = "Init ";
        interfaceName = prefix + interfaceName;
        String config = TEST_PROPERTIES_BASE_DIRECTORY + "/30080.properties";
        runController("dt", config, "-p", prefix, "-m", metadataDirectory);
        // Generate interfaces
        ListAppender listAppender = getListAppender();
        runController("ct", config, "-p", prefix, "-m", metadataDirectory);
        if (listAppender.contains(Level.ERROR, false)) {
            String msg = "Creation of interface logged errors.";
            Assert.fail(msg);
            throw new RuntimeException(msg);
        }
        removeAppender(listAppender);
    }

    @Test
    // success
    public void test30090FlowStrategy_aggregate_expression() {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
        filterJoinLookupTestCommon(true, properties);
    }


    @Test
    // success
    public void test30100FlowStrategy_aggregate_set_expression() {
        if (!new OdiVersion().isVersion11()) {
            String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
            filterJoinLookupTestCommon(true, properties);
        }
    }

    @Test
    // success
    public void test30110FlowStrategy_aggregate_set_expression_single_source() {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
        filterJoinLookupTestCommon(true, properties);
    }

    @Test
    // success
    public void test30120FlowStrategy_aggregate_set_expression_ne() {
        if (!new OdiVersion().isVersion11()) {
            String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30120.properties";
            filterJoinLookupTestCommon(true, properties);
        }
    }

    @Test
    // success
    public void test30130FlowStrategy_aggregate_set_expression_single_source_ne() {
        String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30120.properties";
        filterJoinLookupTestCommon(true, properties);
    }

    @Test
    // success
    public void test30140FlowStrategy_double_aggregate_set_expression() {
        if (!new OdiVersion().isVersion11()) {
            String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
            filterJoinLookupTestCommon(true, properties);
            // in odi 11 the filter is pushed outside the left outer,
            // which essentialy makes it an inner join.
        }
    }

    @Test
    public void test40010VariableTestAll() throws JAXBException, IOException {
        String xmlDir = TEST_XML_BASE_DIRECTORY + "/xml/variables";
        File file = new File(xmlDir + "/expvar", "Variables.xml");
        file.delete();
        runControllerExcpectError("delvar", defaultProperties, "-p", "Init ", "-m", xmlDir);
        runController("crtvar", defaultProperties, "-p", "Init ", "-m", xmlDir);
        assertEquals(17, odi12VariableAccessStrategy.findAllVariables().size());
        runControllerExcpectError("expvar", defaultProperties, "-p", "Init ", "-m", xmlDir + "/expvar");
        file.delete();
        runController("delvar", defaultProperties, "-p", "Init ", "-m", xmlDir);
        logger.info(odi12VariableAccessStrategy.findAllVariables().size());
        assertEquals(4, odi12VariableAccessStrategy.findAllVariables().size());
    }

    @Test
    public void test50010SequencesTestAll() throws JAXBException, IOException {
        String xmlDir = TEST_XML_BASE_DIRECTORY + "/xml/sequences";
        runController("delseq", defaultProperties, "-p", "Init ", "-m", xmlDir);

        runController("crtseq", defaultProperties, "-p", "Init ", "-m", xmlDir);
        assertEquals(4, odi12SequenceAccessStrategy.findAll().size());

        runController("expseq", defaultProperties, "-p", "Init ", "-m", xmlDir + "/exp");
        File file = new File(xmlDir + "/exp", "Sequences.xml");
        System.err.println(file.getAbsolutePath().toString());
        assertTrue(file.exists());
        file.delete();

        runController("delseq", defaultProperties, "-p", "Init ", "-m", xmlDir);
        assertEquals(0, odi12SequenceAccessStrategy.findAll().size());
    }

    @Test
    public void test60010ConstraintsAll() {
        String xmlDir = TEST_XML_BASE_DIRECTORY + "/xml/Constraints";
        runController("delcon", defaultProperties, "-p", "Init ", "-m", xmlDir);

        runController("crtcon", defaultProperties, "-p", "Init ", "-m", xmlDir);

        assertEquals(10, this.odi12ConstraintsAccessStrategy.findAllConditions().size());
        assertEquals(12, this.odi12ConstraintsAccessStrategy.findAllKeys().size());
        assertEquals(13, this.odi12ConstraintsAccessStrategy.findAllReferences().size());

        runController("expcon", defaultProperties, "-p", "Init ", "-m", xmlDir + "/exp");

        File file = new File(xmlDir + "/exp", "Constraints.xml");
        file.delete();
        runController("delcon", defaultProperties, "-p", "Init ", "-m", xmlDir);

        logger.info(this.odi12ConstraintsAccessStrategy.findAllConditions().size());
        logger.info(this.odi12ConstraintsAccessStrategy.findAllKeys().size());
        logger.info(this.odi12ConstraintsAccessStrategy.findAllReferences().size());

        assertEquals(0, this.odi12ConstraintsAccessStrategy.findAllConditions().size());
        assertEquals(5, this.odi12ConstraintsAccessStrategy.findAllKeys().size());
        assertEquals(0, this.odi12ConstraintsAccessStrategy.findAllReferences().size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test70010Asynchronous() throws ResourceNotFoundException, ResourceFoundAmbiguouslyException {
        String prefix = "Init ";
        generationInterfaceAssert(Level.WARN, testName, "This test shouldn't throw an exception.");
        OdiPackage asynchronouspck = this.odiPackageAccessStrategy.findPackage("ASYNCHRONOUS", "BulkLoadORACLE_DWH_DMT", regressionConfiguration.getProjectCode());
        int asynchronouspckCount = 0;
        for (Step s : asynchronouspck.getSteps()) {
            if (s instanceof StepOdiCommand) {
                if (((StepOdiCommand) s).getCommandExpression().getAsString().contains("-SYNC_MODE=2")) {
                    asynchronouspckCount++;
                }
            }
        }
        logger.info("asynchronouspckCount:" + asynchronouspckCount);
        assert (asynchronouspckCount == 3) : "There should be 3 asynchronous scenarios in package ASYNCHRONOUS";
    }

    // @Test // Disabled this test because it interferes with function calls
    //          that are not parsed at all but must be supported
    public void test80000Validation_Error() {
        try {
            runController("vldt", defaultProperties, "-p", "Init ", "-m", TEST_XML_BASE_DIRECTORY + "/xml/Validation_Error");
        } catch (UnRecoverableException e) {
            // it should throw UnRecoverableException
            return;
        }
        throw new RuntimeException("This method should throw exception.");
    }

    //@Test
    public void test20010Stream() {

        File[] metadatas = null;
        metadatas = new File(TEST_XML_BASE_DIRECTORY + "/xml/" + testName).listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().equals("0.xml") ? false : true;
            }
        });

        if (metadatas == null || metadatas.length < 1) {
            throw new RuntimeException("Test metadata files not configured properly");
        }

        File metadata = metadatas[0];
        logger.info(metadata.getName());
        Pattern pattern = Pattern.compile("\\d*");
        Matcher match = pattern.matcher(metadata.getName().replace(".xml", ""));
        match.find();
        String ps = match.group();
        final Integer packageSequence = Integer.parseInt(ps);

        // InputStream is = new FileInputStream(metadata);
        JodiController controller = new JodiController(true);
        controller.init(new RunConfig() {
            public String getMetadataDirectory() {
                return "";
            }

            public List<String> getModuleClasses() {
                List<String> module = new ArrayList<String>();
                module.add("one.jodi.odi.factory.OdiModuleProvider");
                return module;
            }

            /*			public String getPrefix() {
				return "Init ";
			}
*/
            public String getPropertyFile() {
                return defaultProperties;
            }

            /*			public String getPackage() {
				return null;
			}

			public String getScenario() {
				return null;
			}
*/
            public boolean isDevMode() {
                return true;
            }

            /*			public boolean isJournalized() {
				return false;
			}
*/
            public String getSourceModel() {
                return null;
            }

            public String getTargetModel() {
                return null;
            }

/*			public String getPackageSequence() {
				return packageSequence + "";
			}*/

            public String getModelCode() {
                return null;
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public String getMasterPassword() {
                return null;
            }

            @Override
            public String getDeploymentArchiveType() {
                return null;
            }
        });

        TransformationService ts = ServiceFactory.getInstance().getServiceInstance(TransformationService.class);
        FileInputStream fos1 = null;
        FileInputStream fos2 = null;
        try {
            fos1 = new FileInputStream(metadata);
            fos2 = new FileInputStream(metadata);
            ts.deleteTransformation(fos1);
            ts.createTransformation(fos2, packageSequence, false);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (fos1 != null) {
                try {
                    fos1.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (fos2 != null) {
                try {
                    fos2.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        // TODO Below is commented out as the running the scenario will hang.
        // Does package info need to be created?
        // runController("cp",
        // defaultProperties,
        // "-p", "Init ",
        // "-m", TEST_XML_BASE_DIRECTORY + "/xml/" + testName);
        //
        // getSqlHelper().executedSQLSuccesfully(refUser,
        // getMasterRepositoryJdbcPassword(), refUserJDBC,
        // "truncate table REF.S_DA_O");
        // getSqlHelper().executedSQLSuccesfully(stgUser,
        // getMasterRepositoryJdbcPassword(), stgUserJDBC,
        // "truncate table DWH_STG.S_DA_I");
        // getSqlHelper().executedSQLSuccesfully(stgUser,
        // getMasterRepositoryJdbcPassword(), stgUserJDBC,
        // "truncate table DWH_STG.S_DA_O");
        // String dir = ".";
        // String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        // String driverDBClass = stgUserJDBCDriver;
        // String jdbcDBConnection = stgUserJDBC;
        // String jdbcDBUsername = getSysdbaUser();
        // String jdbcDBPassword = getSysdbaPassword();
        // DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass,
        // jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
        // refUserJDBCDriver, refUserJDBC, refUser,
        // getMasterRepositoryJdbcPassword());
        // try {
        // dbUnit.fullDatabaseImport();
        // } catch (Exception ex) {
        // Assert.fail(ex.getMessage());
        // }
        // String pAgentUrl = defaultAgent;
        // String pUser = getOdiSupervisorUser();
        // String pPassword = getOdiSupervisorPassword();
        // String pContextCode = getOdiContext();
        // String pLogLevel = "5";
        // String pWorkRepName = getOdiWorkRepositoryName();
        // String pScenarioName = testName;
        // odiExecute.startScenario(pAgentUrl, pUser, pPassword, pContextCode,
        // pLogLevel, pWorkRepName, pScenarioName);
        //
        //
        // assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
        //
        // getSqlHelper().executedSQLSuccesfully(refUser,
        // getMasterRepositoryJdbcPassword(), refUserJDBC,
        // "truncate table REF.S_DA_O");
        // getSqlHelper().executedSQLSuccesfully(stgUser,
        // getMasterRepositoryJdbcPassword(), stgUserJDBC,
        // "truncate table DWH_STG.S_DA_I");
        // getSqlHelper().executedSQLSuccesfully(stgUser,
        // getMasterRepositoryJdbcPassword(), stgUserJDBC,
        // "truncate table DWH_STG.S_DA_O");
    }

    // ///////////////////////////////////////////////////////////////////
    // 2009-01-01

    /**
     * Close the HSQLDB databases.
     * <p>
     * On Unix / Linux is the build server, on the build server stop the
     * databases, every run.
     * <p>
     * On Windows / Mac are the developers, keep the DB's running there.
     * <p>
     * Add the @Test annotation to close the databases after this test.
     */
    @Test
    public void test99999Destructor() {
        if (!OsHelper.isWindows() && new OdiVersion().isVersion11()) {
            // stopAgent("FUNCTIONAL_TEST");
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void cKM_1S_Common(String properties) throws Exception {
        if (properties != null) {
            generationInterfaceAssertSuccess(testName,
                    "This test did not pass without error fatal or warning messages.", properties);
        } else {
            generationInterfaceAssertSuccess(testName,
                    "This test did not pass without error fatal or warning messages.");
        }
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
        if (testName.equals("CKM_1S_Default_Success")
                || testName.equals("CKM_1S_NotActive_Success")
                || testName.equals("CKM_1S_Manual_Success")
                || testName.equals("CKM_1S_ManualAndIKM_Success")
        ) {
            testName = testName.toUpperCase();
        }
        T mapping = odiAccessStrategy.findMappingsByName("Init " + testName,
                regressionConfiguration.getProjectCode());
        assertTrue(odiAccessStrategy.checkThatAllTargetsHaveCKMName(mapping, "CKM Oracle"));
    }

    private void model_2D_Common() throws Exception {
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
        //
        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());

        assertTrue(dbUnit.areEqual("REF.W_DA_D", "DWH_DMT.W_DA_D"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";
        //

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        dbUnit = null;
    }

    private void filterJoinLookupTestCommon(boolean joinLookup) {
        filterJoinLookupTestCommon(joinLookup, defaultProperties);
    }

    private void filterJoinLookupTestCommon(boolean joinLookup, String properties) {
        generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.", properties);
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");

        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName.toUpperCase(), regressionConfiguration,
                defaultAgent);

        if (joinLookup) {
            assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
            // "The data is not equal with reference data.";

        } else {
            try {
                assertTrue(dbUnit.areEqual(new File(dumpFile), "REF.S_DA_O", "DWH_STG.S_DA_O",
                        new String[]{"KEY", "VALUE", "LAST_CHANGED_DT"}));// :
                // "The data is not equal with reference data.";
            } catch (Exception e) {
                logger.fatal(e);
                Assert.fail(e.getMessage());
            }
        }
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
    }

    /// this test is not working.
    @Test
    public void test89981PackageCreation_ExecProc() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        Packages packages = loadPackagesMetaData();

        assertNotNull(packages);
        assertNotNull(packages.getPackage());

        for (Package p : packages.getPackage()) {
            validatePackage(p);
            deletePackage(defaultProperties, p.getPackageName(), "BulkLoadExplicit/TargetFolder");
        }

        ProcedureInternal procedure =
                this.getOdiProcedureService
                        .extractProcedures(getRegressionConfiguration().getProjectCode())
                        .stream()
                        .filter(p -> p.getName().equals("PROC_DUAL_TEST"))
                        .findFirst()
                        .orElse(null);

        Assert.assertNotNull(procedure);
        Assert.assertEquals("wrong folder", "BulkLoadExplicit/TargetFolder",
                procedure.getFolderPath());
        Assert.assertEquals("wrong line count", 2, procedure.getTasks().size());
        Assert.assertFalse("expected no source command", procedure.getTasks()
                .get(0)
                .getSourceCommand()
                .isPresent());
        Assert.assertTrue("expected target command", procedure.getTasks()
                .get(0)
                .getTargetCommand()
                .isPresent());
        Assert.assertTrue("expected source command", procedure.getTasks()
                .get(1)
                .getSourceCommand()
                .isPresent());
        Assert.assertTrue("expected target command", procedure.getTasks()
                .get(1)
                .getTargetCommand()
                .isPresent());

        deleteProcedures(defaultProperties);
        Optional<ProcedureInternal> delProcedure =
                this.getOdiProcedureService
                        .extractProcedures(getRegressionConfiguration().getProjectCode())
                        .stream()
                        .filter(p -> p.getName().equals("PROC_DUAL_TEST"))
                        .findFirst();
        Assert.assertFalse(delProcedure.isPresent());
    }

    @Test
    public void test89982PackageCreation_ExecProcWithParams() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        Packages packages = loadPackagesMetaData();

        assertNotNull(packages);
        assertNotNull(packages.getPackage());

        for (Package p : packages.getPackage()) {
            validatePackage(p);
            deletePackage(defaultProperties, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
        }
    }

    @Test
    public void test89983PackageCreation_ExecPackageAsync() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        Packages packages = loadPackagesMetaData();

        assertNotNull(packages);
        assertNotNull(packages.getPackage());

        for (Package p : packages.getPackage()) {
            validatePackage(p);
            deletePackage(defaultProperties, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
        }
    }

    @Test
    public void test89984PackageCreation_ExecPackageNoAsync() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        Packages packages = loadPackagesMetaData();

        assertNotNull(packages);
        assertNotNull(packages.getPackage());

        for (Package p : packages.getPackage()) {
            validatePackage(p);
            deletePackage(defaultProperties, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
        }
    }

    @Test
    public void test89985PackageCreation_SuccessStep() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        Packages packages = loadPackagesMetaData();

        assertNotNull(packages);
        assertNotNull(packages.getPackage());

        for (Package p : packages.getPackage()) {
            validatePackage(p);
            deletePackage(defaultProperties, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
        }
    }

    @Test
    public void test89986PackageCreation_FailureStep() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        Packages packages = loadPackagesMetaData();

        assertNotNull(packages);
        assertNotNull(packages.getPackage());

        for (Package p : packages.getPackage()) {
            validatePackage(p);
            deletePackage(defaultProperties, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
        }
    }

    @Test
    public void test89987PackageCreation_WrongFolder() throws Exception {
        deleteAllPackagesAndScenarios();
        deletePackage(defaultProperties, "PACKAGE_CREATION_TEST1_WRONG_FOLDER", "BulkLoadORACLE_DWH_STG");
        deletePackage(defaultProperties, "PACKAGE_CREATION_TEST2_WRONG_FOLDER", "BulkLoadORACLE_DWH_STG");
        deletePackage(defaultProperties, "PACKAGE_CREATION_TEST3_WRONG_FOLDER", "BulkLoadORACLE_DWH_STG");

        T mapping = null;
        try {
            mapping = odiAccessStrategy.findMappingsByName("PC_Interface3_wrong_folder",
                    regressionConfiguration.getProjectCode());
        } catch (Exception e) {

        }

        if (mapping != null) {
            ITransactionManager tm = getWorkOdiInstance().getOdiInstance().getTransactionManager();
            getWorkOdiInstance().getOdiInstance().getTransactionalEntityManager().remove(mapping);
            tm.commit(getWorkOdiInstance().getTransactionStatus());
        }
        generationInterfaceAssertFailure(testName, "This test threw an exception it should not.");

        String wrongFolder = "BulkLoadORACLE_DWH_STG";
        String packageName = "PACKAGE_CREATION_TEST1_WRONG_FOLDER";
        OdiPackage odiPackage = odiPackageAccessStrategy.findPackage(packageName,
                wrongFolder, regressionConfiguration.getProjectCode());
        Collection<Step> steps = null;
        if (odiPackage != null && odiPackage.getSteps() != null) {
            steps = odiPackage.getSteps();
        }
        if (steps != null) {
            int nextStepIsNullCounter = 0;
            for (Step step : steps) {
                logger.info("step:" + step.getName());
                logger.info("Next step: " + step.getNextStepAfterSuccess());
                if (step.getNextStepAfterSuccess() == null) {
                    nextStepIsNullCounter++;
                }
            }
            if (nextStepIsNullCounter > 1) {
                throw new RuntimeException("Multipleflows; that is not allowed although ODI doesn't report error.");
            }
        }
    }

    @Test
    public void test89999PackageCreation() throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        Packages packages = loadPackagesMetaData();

        assertNotNull(packages);
        assertNotNull(packages.getPackage());

        for (Package p : packages.getPackage()) {
            validatePackage(p);
            deletePackage(defaultProperties, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
        }
    }

    private Packages loadPackagesMetaData() throws Exception {
        Properties p = new Properties();
        FileInputStream fis = null;
        InputStream is = null;
        Packages packages = null;
        try {
            fis = new FileInputStream(new File(defaultProperties));
            p.load(fis);

            XMLParserUtil<Packages, one.jodi.core.etlmodel.ObjectFactory> etlParser = new XMLParserUtil<Packages, one.jodi.core.etlmodel.ObjectFactory>(
                    one.jodi.core.etlmodel.ObjectFactory.class, JodiConstants.getEmbeddedXSDFileNames(), errorWarningMessages);

            File etlFile = new File(TEST_XML_BASE_DIRECTORY + "/xml/" + testName, "0.xml");

            is = new FileInputStream(etlFile);
            packages = etlParser.loadObjectFromXMLAndValidate(is, p.getProperty("xml.xsd.packages"), etlFile.getPath());
        } catch (FileNotFoundException e) {
            String friendlyError = "FATAL: xml file  not found.";
            throw new RuntimeException(friendlyError, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (fis != null) {
                fis.close();
            }
        }

        return packages;
    }

    /*
     * Please note this validation doesn't work on odi12; The steps in the xml
     * do not add up to the mappings; reusable mappings are no longer included,
     * as autonomous steps.
     */
    private void validatePackage(Package jodiPackage) {
        assertNotNull("Package instance was null", jodiPackage);
        String packageName = jodiPackage.getPackageName();
        assertNotNull("Package name was null", packageName);
        OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
        IOdiPackageFinder finder =
                ((IOdiPackageFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiPackage.class));

        // consider nested folders
        String[] folderPath = jodiPackage.getFolderCode().split("/");
        String containingFolder = folderPath[folderPath.length - 1];
        Collection<OdiPackage> pList =
                finder.findByName(packageName, regressionConfiguration.getProjectCode(),
                        containingFolder);

        assertNotNull("No ODI Package found with name " + packageName + " in folder " +
                jodiPackage.getFolderCode(), pList);
        assertFalse("Multiple ODI Packages found with name " + packageName +
                " in folder " + jodiPackage.getFolderCode(), pList.size() > 1);
        assertEquals("No ODI Package found with name " + packageName + " in folder " +
                jodiPackage.getFolderCode(), 1, pList.size());

        OdiPackage odiPackage = pList.iterator().next();
        Collection<Step> odiSteps = odiPackage.getSteps();
        Steps beforeSteps = jodiPackage.getBefore();
        int beforeSize = (beforeSteps != null && beforeSteps.getStep() != null ? beforeSteps.getStep().size() : 0);
        Steps afterSteps = jodiPackage.getAfter();
        int afterSize = (afterSteps != null && afterSteps.getStep() != null ? afterSteps.getStep().size() : 0);
        Steps failureSteps = jodiPackage.getFailure();
        int failureSize = (failureSteps != null && failureSteps.getStep() != null ? failureSteps.getStep().size() : 0);

        int interfaceSize = odiSteps.size() - beforeSize - afterSize - failureSize;
        assertTrue("Not enough steps in the " + packageName
                + " ODI package to account for all of the steps defined in the metadata.", interfaceSize >= 0);

        assertTrue("Number of steps in metadata does not match number of steps in ODI package",
                (beforeSize + interfaceSize + afterSize + failureSize) == odiSteps.size());

        Step currentOdiStep = odiPackage.getFirstStep();

        if (beforeSize > 0) {
            logger.info("validating package: " + jodiPackage.getPackageName());
            currentOdiStep = validateSteps(currentOdiStep, beforeSteps);
        }

        /**
         * deprecated since validation relies on instanceof stepinterface, which
         * on odi12 is stepmapping.
         */
        if (interfaceSize > 0) {
            currentOdiStep = validateInterfaceSteps(interfaceSize, currentOdiStep);
        }

        if (afterSize > 0) {
            currentOdiStep = validateSteps(currentOdiStep, afterSteps);
        }

        if (jodiPackage.getGotoOnFinalSuccess() != null) {
            assertNotNull("Expected step on final success but none found.", currentOdiStep);

            StepType jodiStep = findLabeledStep(jodiPackage.getGotoOnFinalSuccess(), beforeSteps);
            if (jodiStep == null) {

            }

            jodiStep = findLabeledStep(jodiPackage.getGotoOnFinalSuccess(), beforeSteps);

            assertNotNull("Could not find the GoToOnFinalSuccess step.", jodiStep);
            validateStep(currentOdiStep, jodiStep);
        } else {
            assertNull("Expected final step but addition ODI steps exists.", currentOdiStep);
        }
    }

    private Step validateSteps(Step currentStep, Steps steps) {
        Step activeStep = currentStep;

        for (JAXBElement<? extends StepType> element : steps.getStep()) {
            StepType st = element.getValue();
            validateStep(activeStep, st);
            activeStep = activeStep.getNextStepAfterSuccess();
        }
        return activeStep;
    }

    private void validateStep(Step odiStep, StepType jodiStep) {
        logger.info(String.format("Odi '%2$s' Jodi '%1$s'", getStepLabel(jodiStep), odiStep.getName()));
        assertTrue("Metadata step name does not match ODI step name",
                StringUtils.equals(getStepLabel(jodiStep), odiStep.getName()));
        if (jodiStep instanceof VariableType) {
            assertTrue("Metada step is a variable but ODI step was not", odiStep instanceof StepVariable);
        } else if (jodiStep instanceof ExecProcedureType) {
            assertTrue("Metada step is a procedure but ODI step was not", odiStep instanceof StepProcedure);
        } else if (jodiStep instanceof ExecPackageType) {
            assertTrue("Metada step is a package but ODI step was not", odiStep instanceof StepOdiCommand);
            StepOdiCommand commandStep = (StepOdiCommand) odiStep;
            assertNotNull("Command expression should not be null", commandStep.getCommandExpression());

            if (((ExecPackageType) jodiStep).isAsynchronous() != null && ((ExecPackageType) jodiStep).isAsynchronous()) {
                assertTrue("ODI command should be asynchronous",
                        commandStep.getCommandExpression().getAsString().contains("SYNC_MODE=2"));
            }
        } else if (jodiStep instanceof ExecCommandType) {

        } else {
            fail("Unrecognized StepType '" + jodiStep.getClass().getName() + "'");
        }
    }

    /**
     * This method is deprecated, since on odi 12 it is not instance of
     * stepinterface.
     *
     * @param count
     * @param currentStep
     * @return
     */
    private Step validateInterfaceSteps(int count, Step currentStep) {
        return new FunctionalTestHelper().validateInterfaceStep(count, currentStep);
    }

    private StepType findLabeledStep(String label, Steps... steps) {
        StepType result = null;

        for (Steps cSteps : steps) {
            if (cSteps.getStep() != null) {
                for (JAXBElement<? extends StepType> element : cSteps.getStep()) {
                    StepType st = element.getValue();
                    if (StringUtils.equals(label, st.getLabel())) {
                        result = st;
                        break;
                    }
                }
            }

            if (result != null) {
                break;
            }
        }

        return result;
    }

    private void set_2D_Explicit_Common(String dumpFile) {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        String dir = ".";
        // String dumpFile = TEST_DATA_BASE_DIRECTORY +
        // "/Set_2D_Explicit_Union_Success.xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = stgUser;
        String jdbcDBPassword = regressionConfiguration.getMasterRepositoryJdbcPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        Assert.assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O")); // :
        // "The
        // data
        // is
        // not
        // equal
        // with
        // reference
        // data.";
        dbUnit = null;
    }

    private void iKM_1S_Common(boolean is16060) throws Exception {
        if (is16060) {
            generationInterfaceAssertSuccess(testName,
                    "This test did not pass without error fatal or warning messages.",
                    TEST_PROPERTIES_BASE_DIRECTORY + "/16060_FunctionalTest.properties");
        } else {
            generationInterfaceAssertSuccess(testName, "This test threw an exception which it should not.");
        }
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_FA_F");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_FA_F");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_FA_I");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
    }

    private void model_1S_Override_Common(boolean lookup) throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
        //
        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        if (lookup) {
            dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
                    refUserJDBCDriver, refUserJDBC, refUser, regressionConfiguration.getMasterRepositoryJdbcPassword());
            assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
            // "The
            // data
            // is
            // not
            // equal
            // with
            // reference
            // data.";
            //
        } else {
            // Target
            assertTrue(dbUnit.areEqual("REF.W_DA_D", "DWH_DMT.W_DA_D"));// :
        }
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        dbUnit = null;

    }

    private void trans_1S_Common(final boolean checkLogger, final String interfaceName,
                                 final String folderPath) throws Exception {
        String prefix = "Init ";
        String config = defaultProperties;

        runController("dt", config, "-p", prefix, "-m", metadataDirectory);
        // Generate interfaces
        ListAppender listAppender = getListAppender();
        runController("ct", config, "-p", prefix, "-m", metadataDirectory);
        if (checkLogger) {
            if (listAppender.contains(Level.WARN, false)) {
                String msg = "Creation of interface logged warning.";
                Assert.fail(msg);
                throw new RuntimeException(msg);
            }
        }
        removeAppender(listAppender);
        assertTrue(this.odiAccessStrategy != null);
        IMapping odiInterface;
        try {
            odiInterface = (IMapping) this.odiAccessStrategy.findMappingsByName(interfaceName,
                    getRegressionConfiguration().getProjectCode());
            boolean found = false;
            if (odiInterface.getName().equals(interfaceName)) {
                found = true;
            }
            Assert.assertTrue(found);
        } catch (ResourceNotFoundException e) {
            fail("Mapping " + interfaceName + " not found in project " +
                    getRegressionConfiguration().getProjectCode() + ": " + e.getMessage());
            throw e;
        } catch (ResourceFoundAmbiguouslyException e) {
            fail("Multiple Mapping fopund with name " + interfaceName + " in project " +
                    getRegressionConfiguration().getProjectCode() + ": " + e.getMessage());
            throw e;
        }

        if (folderPath != null) {
            OdiFolder folder = (OdiFolder) odiInterface.getFolder();
            Assert.assertNotNull(folder);
            Assert.assertEquals(folderPath, Odi12FolderHelper.getFolderPath(folder));
        }
    }

    @SuppressWarnings("deprecation")
    private void generationInterfaceAssert(Level level, String testName, String execeptionMessage,
                                           String properties) {
        String levelStr = level == Level.ERROR ? "error" : "warning";
        String prefix = "Init ";
        String report = null;
        ListAppender listAppender = getListAppender();
        try {
            report = runController("etls", properties, "-p", prefix, "-m", metadataDirectory);
        } catch (Exception ex) {
            Assert.fail(testName + String.format(": did throw exception it should report %s.", levelStr)
                    + execeptionMessage);
        }

        if (!listAppender.contains(level, false) && !report.isEmpty()) {
            String msg = testName + ":" + execeptionMessage + String.format(": did not report %s.", levelStr);
            Assert.fail(msg);
            throw new RuntimeException(msg);
        }
        removeAppender(listAppender);
    }

    private void generationInterfaceAssert(Level level, String testName, String execeptionMessage) {
        generationInterfaceAssert(level, testName, execeptionMessage, defaultProperties);
    }

    private void mappingLookup_Common(String targetTable, boolean checkLogger, String properties) throws Exception {

        if (checkLogger)
            generationInterfaceAssertSuccess(testName, "This test threw an exception which it should not.", properties);
        else
            generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.",
                    properties);

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        // Reusable mappings can't be executed.
        logger.info("Comparing " + targetTable + " from testname: " + testName);
        assertTrue(dbUnit.areEqual("REF.S_DA_O", targetTable));

        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
    }

    private void mapping_2S_Auto_Common(boolean checkLogger) throws Exception {
        if (checkLogger)
            generationInterfaceAssertSuccess(testName, "This test did not report error.");
        else
            generationInterfaceAssert(Level.WARN, testName, "This test did not report error.");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
    }

    private void source_2S_Common(boolean isDefault, boolean checkLogger) {
        deleteScenario(defaultProperties, testName);
        if (!checkLogger)
            generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        else
            generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/Source_2S_Default_success.xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());

        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }
        getSqlHelper().executedSQLSuccesfully(jdbcDBUsername, jdbcDBPassword,
                jdbcDBConnection, "truncate table DWH_STG.S_SOURCE_2S_ONEINTF_I");
        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        if (isDefault) {
            assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
            // "The data is not equal with reference data.";
        } else {
            // S_Source_2S_OneIntf_I_S02
            // can't execute reusablemappings
            assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_SOURCE_2S_ONEINTF_I"));
            //

            // "The data is not equal with reference data.";

        }
        dbUnit = null;
    }

    private void model_2S_Common(String jdbcUser, String jdbcUrl, String dbSchema) throws Exception {
        generationInterfaceAssertSuccess(testName, "This test threw an exception it should not.");
        // export for some data
        getSqlHelper().executedSQLSuccesfully(jdbcUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                jdbcUrl, String.format("truncate table %s.S_DA_I", dbSchema));
        getSqlHelper().executedSQLSuccesfully(jdbcUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                jdbcUrl, String.format("truncate table %s.S_DB_I", dbSchema));
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        //
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
        //
        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword(), refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDMTDatabaseImport();
        } catch (Exception ex) {
            logger.fatal(ex);
            Assert.fail(ex.getMessage());
        }
        //

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
                refUserJDBCDriver, refUserJDBC, refUser, regressionConfiguration.getMasterRepositoryJdbcPassword());

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
        // "The data
        // is not
        // equal
        // with
        // reference
        // data.";
        //
        getSqlHelper().executedSQLSuccesfully(jdbcUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                jdbcUrl, String.format("truncate table %s.S_DA_I", dbSchema));
        getSqlHelper().executedSQLSuccesfully(jdbcUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                jdbcUrl, String.format("truncate table %s.S_DB_I", dbSchema));
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
        getSqlHelper().executedSQLSuccesfully(dmtUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        dbUnit = null;
    }

    private void mapping_2D_Common(boolean checkLog) throws Exception {
        if (checkLog)
            generationInterfaceAssertSuccess(testName, "This test did not report error.");
        else
            generationInterfaceAssert(Level.WARN, testName, "This test did not report error.");
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");
        String dir = ".";
        String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
        String driverDBClass = stgUserJDBCDriver;
        String jdbcDBConnection = stgUserJDBC;
        String jdbcDBUsername = regressionConfiguration.getSysdbaUser();
        String jdbcDBPassword = regressionConfiguration.getSysdbaPassword();
        DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername,
                jdbcDBPassword, refUserJDBCDriver, refUserJDBC, refUser,
                regressionConfiguration.getMasterRepositoryJdbcPassword());
        try {
            dbUnit.fullDatabaseImport();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, regressionConfiguration, defaultAgent);

        assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
        getSqlHelper().executedSQLSuccesfully(refUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                refUserJDBC, "truncate table REF.S_DA_O");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DB_I");
        getSqlHelper().executedSQLSuccesfully(stgUser, regressionConfiguration.getMasterRepositoryJdbcPassword(),
                stgUserJDBC, "truncate table DWH_STG.S_DA_O");

    }

    private String getStepLabel(StepType jodiStep) {
        return (StringUtils.hasLength(jodiStep.getLabel()) ? jodiStep.getLabel() : jodiStep.getName());
    }

    private String executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p = null;
        InputStreamReader inputStream = null;
        BufferedReader reader = null;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            inputStream = new InputStreamReader(p.getInputStream());
            reader = new BufferedReader(inputStream);
            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return output.toString();
    }

//	@Test
//	public void test40000UpdateAgent(){
//		OdiUpdateAgent updateAgent = new OdiUpdateAgent();
//		String odiMasterRepoUrl = regressionConfiguration.getMasterRepositoryJdbcUrl();
//		String odiMasterRepoUser = regressionConfiguration.getMasterRepositoryJdbcUser();
//		String odiMasterRepoPassword = regressionConfiguration.getMasterRepositoryJdbcPassword();
//		String odiWorkRepo = regressionConfiguration.getOdiWorkRepositoryName();
//		String odiLoginUsername = regressionConfiguration.getOdiSupervisorUser();
//		String odiLoginPassword = regressionConfiguration.getOdiSupervisorPassword();
//		String agentUrl = "http://test:20910/test";
//		String jdbcDriver = regressionConfiguration.getMasterRepositoryJdbcDriver();
//		String agentName = "TEST";
//		updateAgent.updateAgent(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo, odiLoginUsername, odiLoginPassword, agentUrl, jdbcDriver, agentName);
//	}
//	
//	@Test
//	public void test40010UpdateDataServer(){
//		OdiUpdateDataserver updateDataServer = new OdiUpdateDataserver();
//		String odiMasterRepoUrl = regressionConfiguration.getMasterRepositoryJdbcUrl();
//		String odiMasterRepoUser = regressionConfiguration.getMasterRepositoryJdbcUser();
//		String odiMasterRepoPassword = regressionConfiguration.getMasterRepositoryJdbcPassword();
//		String odiWorkRepo = regressionConfiguration.getOdiWorkRepositoryName(); 
//		String odiLoginUsername = regressionConfiguration.getOdiSupervisorUser();
//		String odiLoginPassword = regressionConfiguration.getOdiSupervisorPassword();
//		String dataServerName = "";
//		String jdbcString = "";
//		String username = "";
//		String password = "";
//		String serverInstanceName = "";
//		String jdbcDriverRepository =  regressionConfiguration.getMasterRepositoryJdbcDriver();
//		String jdbcDriverServer = regressionConfiguration.getMasterRepositoryJdbcDriver();
//		updateDataServer.updateDataServer(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo, odiLoginUsername, odiLoginPassword, dataServerName, jdbcString, username, password, serverInstanceName, jdbcDriverRepository, jdbcDriverServer);
//	}
//	
//	@Test
//	public void test4020UpdateUser(){
//		OdiUpdateUser updateUser = new OdiUpdateUser();
//		String odiMasterRepoUrl = regressionConfiguration.getMasterRepositoryJdbcUrl();
//		String odiMasterRepoUser = regressionConfiguration.getMasterRepositoryJdbcUser();
//		String odiMasterRepoPassword = regressionConfiguration.getMasterRepositoryJdbcPassword();
//		String odiWorkRepo = regressionConfiguration.getOdiWorkRepositoryName();
//		String odiLoginUsername = regressionConfiguration.getOdiSupervisorUser();
//		String odiLoginPassword = regressionConfiguration.getOdiSupervisorPassword();
//		String password  = "test";
//		String jdbcDriverMasterRepo = regressionConfiguration.getMasterRepositoryJdbcDriver();
//		updateUser.updateUser(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo, odiLoginUsername, odiLoginPassword, password, jdbcDriverMasterRepo);
//	}
}
