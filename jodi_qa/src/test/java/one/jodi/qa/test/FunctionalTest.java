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
import one.jodi.core.etlmodel.ExecCommandType;
import one.jodi.core.etlmodel.ExecPackageType;
import one.jodi.core.etlmodel.ExecProcedureType;
import one.jodi.core.etlmodel.Package;
import one.jodi.core.etlmodel.Packages;
import one.jodi.core.etlmodel.StepType;
import one.jodi.core.etlmodel.Steps;
import one.jodi.core.etlmodel.VariableType;
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
import one.jodi.odi.runtime.OdiUpdateAgent;
import one.jodi.odi.runtime.OdiUpdateDataserver;
import one.jodi.odi.runtime.OdiUpdateSchema;
import one.jodi.odi.runtime.OdiUpdateUser;
import one.jodi.odi.sequences.OdiSequenceAccessStrategy;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import one.jodi.odi12.folder.Odi12FolderHelper;
import one.jodi.odi12.procedure.Odi12ProcedureServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.IRepositoryEntity;
import oracle.odi.domain.adapter.project.IMapping;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.Step;
import oracle.odi.domain.project.StepOdiCommand;
import oracle.odi.domain.project.StepProcedure;
import oracle.odi.domain.project.StepVariable;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import oracle.odi.setup.TechnologyName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import javax.xml.bind.JAXBElement;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static one.jodi.qa.test.FunctionalTestHelper.getListAppender;
import static one.jodi.qa.test.FunctionalTestHelper.removeAppender;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("JavaDoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FunctionalTest<T extends IOdiEntity, U extends IRepositoryEntity, V extends IRepositoryEntity, W, X, A extends Step, Y, Z>
        extends RegressionTestImpl {
   private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(FunctionalTest.class + "1");

   private static final String[] IGNORED_METHODS =
           {"Test010Install", "Test020Generation", "Test030ing", "Test99999Destructor"};

   private static final String TEST_XML_BASE_DIRECTORY = "src/test/resources/FunctionalTest";
   private static final String TEST_PROPERTIES_BASE_DIRECTORY =
           TEST_XML_BASE_DIRECTORY + "/" + FunctionalTestHelper.getPropertiesDir();

   private static final String DEFAULT_PROPERTIES = TEST_PROPERTIES_BASE_DIRECTORY + "/FunctionalTest.properties";
   private static final String DEFAULT_AGENT = FunctionalTestHelper.getDefaultAgent(DEFAULT_PROPERTIES);

   @Rule
   public final TestName testMethodName = new TestName();

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
   private String tempDir = "/tmp";

   private final ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();
   private final OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z> odiAccessStrategy;
   private final OdiPackageAccessStrategy<T, A> odiPackageAccessStrategy;
   private final OdiSequenceAccessStrategy odi12SequenceAccessStrategy;
   private final OdiVariableAccessStrategy odi12VariableAccessStrategy;
   private final OdiConstraintAccessStrategy odi12ConstraintsAccessStrategy;
   private final Odi12ProcedureServiceProvider getOdiProcedureService;
   private final String stgUserJDBCDriver;

   private OdiExecuteScenario odiExecuteScenario;
   private String testName = null;
   private String metadataDirectory = null;

   public FunctionalTest() {
      // ODI 12
      super(DEFAULT_PROPERTIES, new PasswordConfigImpl().getOdiUserPassword(),
            new PasswordConfigImpl().getOdiMasterRepoPassword());
      refUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.refUser");
      refUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.refUserJDBC");
      refUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.refUserJDBCDriver");
      //
      srcUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.srcUser");
      srcUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.srcUserJDBC");
      srcUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.srcUserJDBCDriver");
      //
      stgUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.stgUser");
      stgUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.stgUserJDBC");
      stgUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.stgUserJDBCDriver");
      //
      dmtUser = getRegressionConfiguration().getConfig()
                                            .getString("rt.custom.dmtUser");
      dmtUserJDBC = getRegressionConfiguration().getConfig()
                                                .getString("rt.custom.dmtUserJDBC");
      dmtUserJDBCDriver = getRegressionConfiguration().getConfig()
                                                      .getString("rt.custom.dmtUserJDBCDriver");

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
            return null;
         }

         @Override
         public String getSourceModel() {
            return null;
         }

         @Override
         public String getScenario() {
            return null;
         }

         @Override
         public String getPropertyFile() {
            return DEFAULT_PROPERTIES;
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
            return getRegressionConfiguration().getOdiSupervisorPassword();
         }

         @Override
         public String getPackageSequence() {
            return null;
         }

         @Override
         public String getPackage() {
            return null;
         }

         @Override
         public List<String> getModuleClasses() {
            return Collections.emptyList();
         }

         @Override
         public String getModelCode() {
            return null;
         }

         @Override
         public String getMetadataDirectory() {
            return TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
         }

         @Override
         public String getMasterPassword() {
            return getRegressionConfiguration().getMasterRepositoryJdbcPassword();
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
            LOGGER.info("RETURNING NULL FOR FOLDER");
            return null;
         }

         @Override
         public boolean isExportingDBConstraints() {
            return false;
         }

         @Override
         public boolean isIncludingConstraints() {
            return false;
         }

         @Override
         public String getDeployementArchivePassword() {
            return new PasswordConfigImpl().getDeploymentArchivePassword();
         }
      };

      if (OsHelper.isMac() || OsHelper.isUnix()) {
         tempDir = "/tmp";
      } else {
         tempDir = System.getProperty("java.io.tmpdir");
      }

      //noinspection unchecked
      this.odiAccessStrategy =
              (OdiTransformationAccessStrategy<T, U, V, W, X, Y, Z>) FunctionalTestHelper.getOdiAccessStrategy(
                      runConfig, getController());
      //noinspection unchecked
      this.odiPackageAccessStrategy =
              (OdiPackageAccessStrategy<T, A>) FunctionalTestHelper.getOdiPackageAccessStrategy(runConfig,
                                                                                                getController());
      this.odi12SequenceAccessStrategy = FunctionalTestHelper.getOdiSequenceAccessStrategy(runConfig, getController());
      this.odi12VariableAccessStrategy = FunctionalTestHelper.getOdiVariableAccessStrategy(runConfig, getController());
      this.odi12ConstraintsAccessStrategy =
              FunctionalTestHelper.getOdiConstraintsAccessStrategy(runConfig, getController());
      this.getOdiProcedureService = FunctionalTestHelper.getOdiProcedureService(runConfig, getController());
   }

   public void createOdiExecute(final OdiExecuteScenario aOdiExecuteScenario) {
      this.odiExecuteScenario = aOdiExecuteScenario;
   }

   private void generationInterfaceAssertSuccess() {
      generationInterfaceAssertSuccess(DEFAULT_PROPERTIES);
   }

   private void generationInterfaceAssertSuccess(final String properties) {
      final ListAppender listAppender = getListAppender(testName);
      final String prefix = "Init ";
      runController("etls", properties, "-p", prefix, "-m", metadataDirectory);
      LOGGER.info(String.format("Listappender size: %d.", listAppender.getEvents()
                                                                      .size()));
      if (listAppender.contains(Level.WARN, false)) {
         final String msg = "Generation logged warnings/errors.";
         Assert.fail();
         throw new RuntimeException(msg);
      }
      removeAppender(listAppender);
   }

   private void generationInterfaceAssertFailure() {
      generationInterfaceAssertFailure(DEFAULT_PROPERTIES);
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   private void copyFile(final File source, final File dest) {
      executeCommand("cp ./src/test/resources/FunctionalTest/countrylist.csv /tmp");
/*
      String hostname = "";
      try {
         hostname = InetAddress.getLocalHost()
                               .getHostName();
      } catch (final Exception e) {
         LOGGER.debug(e);
      }
      if ((System.getProperty("user.name") != null &&
              (OsHelper.isMac() || OsHelper.isUnix() || OsHelper.isSolaris())) ||
              hostname.endsWith("linux") // linux OBI image
      ) {
         if (new OdiVersion().isVersion11()) {
            executeCommand("scp ./src/test/resources/FunctionalTest/countrylist.csv jodiuser@ct:/tmp");
            executeCommand("ssh jodiuser@ct 'chown jodiuser:dba /tmp/countrylist.csv'");
            executeCommand("ssh jodiuser@ct 'chmod 777 /tmp/countrylist.csv'");
         } else {
            executeCommand("cp ./src/test/resources/FunctionalTest/countrylist.csv /tmp");
         }
      } else {
         System.out.println("didn't use cp.");
         if (dest.exists()) {
            dest.delete();
         }
         if (!dest.exists()) {
            try {
               dest.createNewFile();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
         InputStream in = null;
         OutputStream out = null;
         try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);
            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
               out.write(buf, 0, len);
            }
         } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
         } finally {
            if (in != null) {
               try {
                  in.close();
               } catch (final IOException e) {
                  e.printStackTrace();
               }
            }
            if (out != null) {
               try {
                  out.close();
               } catch (final IOException e) {
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
      if ((System.getProperty("user.name") != null) || hostname.endsWith("linux") // OBI image
      ) {
         LOGGER.info("file copied with cp");
      } else {
         assert (dest.exists()) : "Copy failed from : " + source.getAbsolutePath() + " to: " + dest.getAbsolutePath();
      }
      */
   }

   private void generationInterfaceAssertFailure(final String properties) {
      final String prefix = "Init ";
      try {
         final String report = runController("etls", properties, "-p", prefix, "-m", metadataDirectory);
         if (report.isEmpty()) {
            Assert.fail("This test did not throw an exception or report an error - it should.");
         }
         // Assert.fail("This test did not threw an exception, it should.");
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
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

   private boolean isIgnored(final String methodName) {
      boolean ignore = false;

      for (final String n : IGNORED_METHODS) {
         if (methodName.equalsIgnoreCase(n)) {
            ignore = true;
            break;
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
      final Method[] methods = FunctionalTest.class.getDeclaredMethods();
      int i = 1;
      for (final Method m : methods) {
         final Test annotation = m.getAnnotation(Test.class);
         final boolean isPublic = java.lang.reflect.Modifier.isPublic(m.getModifiers());
         if ((annotation != null) && (isPublic) && (m.getName()
                                                     .toLowerCase()
                                                     .startsWith("test")) && (!isIgnored(m.getName()))) {
            final String packageName = m.getName()
                                        .substring(9);
            try {
               deletePackageAndScenario(DEFAULT_PROPERTIES, packageName, "BulkLoadORACLE_DWH_STG");
            } catch (final Exception ex) {
               try {
                  deletePackageAndScenario(DEFAULT_PROPERTIES, packageName, "BulkLoadORACLE_DWH_DMT");
               } catch (final Exception ex1) {
                  LOGGER.debug(ex1.getMessage());
               }
               LOGGER.debug(ex.getMessage());
            }
            LOGGER.debug(i++ + ": package " + packageName + " deleted.");
         }
      }
      close();
   }

   @Override
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
      final String name = testMethodName.getMethodName();
      LOGGER.info("testName -->" + name);
      if (!isIgnored(name)) {
         testName = name.substring(9);
      } else {
         testName = name;
      }
      metadataDirectory = TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
   }

   @Before
   public void cleanupAllPackages() {
      // the first test case triggers overall cleanup of packages and scenarios.
      // this is just in case that packages were previously not removed.
      if (testMethodName.getMethodName()
                        .equalsIgnoreCase(IGNORED_METHODS[1])) {
         deleteAllPackagesAndScenarios();
      }
   }

   @After
   public void cleanupPackageAfterTestCase() {
      // Assumes that testName is set correctly in an method with @Before
      // Annotation
      if (testName != null) {
         try {
            final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
            final ITransactionManager tm = odiInstance.getTransactionManager();
            final IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
            final IOdiPackageFinder mf = (IOdiPackageFinder) odiInstance.getFinder(OdiPackage.class);
            @SuppressWarnings("unchecked") final Collection<OdiPackage> packages = mf.findAll();
            for (final OdiPackage p : packages) {
               tem.remove(p);
            }
            tm.commit(getWorkOdiInstance().getTransactionStatus());
            deletePackageAndScenario(DEFAULT_PROPERTIES, testName, "BulkLoadORACLE_DWH_STG");
            LOGGER.debug("package " + testName + " deleted after execution test case.");
         } catch (final RuntimeException ex) {
            LOGGER.info(ex.getMessage());
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
   @Override
   @Test
   public void test010Install() {

   }

   private void deletePackageAndScenario(final String config, final String scenarioName, final String folderName) {
//      deleteScenario(config, scenarioName.toUpperCase());
//      deletePackage(config, scenarioName.toUpperCase(), folderName);
//
//      // comment next line if you want to debug,
//      // but don't forget to uncomment it again,
//      // or tests will fail.
//      deleteTransformations(config, scenarioName);
   }

   private void deletePackage(final String config, final String scenarioName, final String folderName) {
      // delete package
      LOGGER.info(String.format("Deleting package '%s' in folder '%s'.", scenarioName, folderName));
      runController("dp", config, "--package", scenarioName, "-f", folderName);
   }

   private void deleteScenario(final String config, final String scenarioName) {
      // delete scenario
      LOGGER.info("deleting: " + scenarioName);
      runController("ds", config, "--scenario", scenarioName.toUpperCase());
   }

   private void deleteProcedures(final String config) {
      // delete scenario
      LOGGER.info("deleting: procedures");
      runController("delproc", config);
   }

   private void deleteTransformations(final String config, final String testName) {
      final String testNameCaseInsensitve = testName.toUpperCase();
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
      LOGGER.info(
              String.format("Deleting transformations '%s' in metadataDirectory '%s'.", testName, metadataDirectory));
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
   @Override
   @Test
   // success
   public void test020Generation() {
      // Generate interfaces
      try {
         runController("ct", DEFAULT_PROPERTIES, "-p", "Inf", "-m", TEST_XML_BASE_DIRECTORY + "/xml/Generation");
         // an exception was not thrown up the stack
         Assert.fail("An incorrect xml file definition did not raise an error, it should.");
      } catch (final AssertionError | Exception ae) {
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
   @Override
   @Test
   // success
   public void test030ing() {
      // Generate interfaces
      generationInterfaceAssertFailure();
   }

   @Test
   // success
   public void test031testEnableTestBehavior() {

      try {
         // Generate interfaces
         final List<String> argList = new ArrayList<>();

         argList.add("-a");
         argList.add("etls");
         argList.add("-c");
         argList.add("missing"); // should cause an ErrorReport and hence throw exception.
         // add default passwords in here - may need to be externalized
         argList.add("-pw");
         argList.add(getRegressionConfiguration().getOdiSupervisorPassword());
         argList.add("-mpw");
         argList.add(getRegressionConfiguration().getMasterRepositoryJdbcPassword());
         argList.add("-devmode");
         getController().run(argList.toArray(new String[0]));
         Assert.fail("This test should have an error report and hance throw exception; it did not.");
      } catch (final RuntimeException rte) {
         LOGGER.info("test031testEnableTestBehavior passed.");
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
      trans_1S_Common(false, "Init Test110Trans_1S_ExplicitName_Success", "BulkLoadExplicit/TargetFolder");
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
   @Test
   @Ignore
   public void test10004Trans_1S_TempExplicitName_Success() throws Exception {
      final String prefix = "Init ";
      // Generate interfaces
      generationInterfaceAssertSuccess();
      final String interfaceName = prefix + "Test110Trans_1S_ExplicitName_Success TruncateInsert";
      final Collection<T> interfaces =
              this.odiAccessStrategy.findMappingsByProject(getRegressionConfiguration().getProjectCode());
      boolean found = false;
      for (final T odiInterface : interfaces) {
         if (odiInterface.getName()
                         .equals(interfaceName)) {
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
      generationInterfaceAssertFailure();
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
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11003Set_2D_Default_Union_All_Success() {
      generationInterfaceAssertSuccess();
      // export for some data
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/Set_2D_Default_Union_All_Success.xml";
      final String jdbcDBPassword = getRegressionConfiguration().getOracleTestDBPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, stgUser, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser, jdbcDBPassword);

      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
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
         final File testDirectory =
                 new File(TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName);
         if (testDirectory.exists()) {
            metadataDirectory = TEST_XML_BASE_DIRECTORY + File.separator + "xml" + File.separator + testName;
         }
      }
      try {
         deletePackageAndScenario(DEFAULT_PROPERTIES, testName, "BulkLoadORACLE_DWH_STG");
         getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                               stgUserJDBC, "truncate table DWH_STG.S_SOURCE_2S_ONEINTF_I");

      } catch (final RuntimeException ex) {
         LOGGER.info(ex.getMessage());
      }
      test11010Source_2S_OneIntf_Success();
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/Source_2S_Default_success.xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         assertTrue(dbUnit.areEqual(new File(dumpFile), "REF.S_DA_O", "DWH_STG.S_SOURCE_2S_ONEINTF_I"));
      } catch (final Exception e) {
         LOGGER.fatal(e);
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
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      try {
         getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                               stgUserJDBC, "drop table DWH_STG.I_S_SOURCE_2S_SUBSELECT_I_S01");
      } catch (final Exception ex) {
         LOGGER.debug("Temp table I_S_SOURCE_2S_SUBSELECT_I_S01 not dropped.");
      }
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));

      final T odiInterface = this.odiAccessStrategy.findMappingsByName("Init SOURCE_2S_SUBSELECTTEMPINTF",
                                                                       getRegressionConfiguration().getProjectCode());
      boolean result = false;
      try {
         result = odiAccessStrategy.isOneOfTheSourcesDerived(odiInterface);
      } catch (final Exception e) {
         LOGGER.fatal(e);
      }
      assertTrue(result);
   }

   @Test
   // success
   public void test11017Source_2S_SubSelectLookup() throws Exception {
      generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.");
      // export for some data
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      try {
         getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                               stgUserJDBC, "drop table DWH_STG.I_S_11017_I_S01");
      } catch (final Exception ex) {
         LOGGER.debug("Temp table DWH_STG.I_S_11017_I_S01 not dropped.");
      }
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));

      final T odiInterface = this.odiAccessStrategy.findMappingsByName("Init SOURCE_2S_SUBSELECTLOOKUP",
                                                                       getRegressionConfiguration().getProjectCode());
      boolean result = false;
      try {
         result = this.odiAccessStrategy.isOneOfTheSourcesDerived(odiInterface);
      } catch (final Exception e) {
         LOGGER.fatal(e);
      }

      assertTrue(result);
   }

   /**
    * @category success
    */
   @Test
   public void test11014Source_2S_SubSelectIntf_Warning() {
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
      generationInterfaceAssertFailure();

   }

   // ///////////////////////////////////////////////////////////////////
   // Model
   // ///////////////////////////////////////////////////////////////////

   /**
    * @category success
    */
   @Test
   // success
   public void test11110Model_2S_Default_Success() {
      model_2S_Common(stgUser, stgUserJDBC, "DWH_STG");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11120Model_2S_Model2_Success() {
      generationInterfaceAssertSuccess();
      // export for some data
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DB_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                             refUserJDBCDriver, refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }
      //
      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, stgUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";
      //
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DB_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11130Model_2S_OverrideS2_Model2_Success() {
      generationInterfaceAssertSuccess();
      // export for some data
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = dmtUserJDBCDriver;
      //
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                             getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver,
                                             refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }
      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, stgUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11140Model_2S_OverrideS1_Success() {
      model_2S_Common(refUser, refUserJDBC, "REF");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11150Model_2D_Default_Success() {
      model_2D_Common();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11160Model_2D_Override_All_Success() {
      model_2D_Common();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11170Model_2D_Override2D_Success() {
      model_2D_Common();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11180Model_1S_UndefinedModel_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11190Model_1S_UndefinedCode_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11200Model_1S_1L_OverrideLookup_Success() {
      model_1S_Override_Common(true);
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test11210Model_1S_OverrideTarget_Success() {
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
      generationInterfaceAssertFailure();
   }

   /**
    * @category failed
    */
   @Test
   @Ignore
   public void test12030Filter_1S_MalformedFilter_Failed() {
      generationInterfaceAssertFailure();
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
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test12070Filter_2S_AndFilterIncorrectAliasReference_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    * <p>
    * Allowed for reporting warning instead of failure.
    */
   @Test
   // success
   public void test12080Filter_2S_AndFilterIncorrect2ndAliasReference_Failure() {
      generationInterfaceAssertFailure();
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
      generationInterfaceAssertFailure();
   }

   @Test
   // success
   public void test12120Filter_1S_Explicit_ExecutionLocation_Success() throws Exception {
      // filterJoinLookupTestCommon(false);

      if (!new OdiVersion().isVersion11()) {
         generationInterfaceAssertSuccess();
         final T m = odiAccessStrategy.findMappingsByName("Init FILTER_1S_EXPLICIT_EXECUTIONLOCATION_SUCCESS",
                                                          getRegressionConfiguration().getProjectCode());

         final Map<String, String> filterExecutionLocations = odiAccessStrategy.getFilterExecutionLocations(m);
         for (final String component : filterExecutionLocations.keySet()) {
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
         deletePackageAndScenario(DEFAULT_PROPERTIES, testName, "BulkLoadORACLE_DWH_STG");
      } catch (final RuntimeException ex) {
         LOGGER.info(ex.getMessage());
      }
      generationInterfaceAssertFailure();
   }

   /**
    *
    */
   @Test
   // success
   public void test13020Join_1S_CROSS_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category succes
    */
   @Test
   // success
   public void test13025Join_1S_NATURAL_Failure() {
      generationInterfaceAssertFailure();
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
   @Test
   @Ignore
   public void test13052Join_2S_RIGHT_OUTER_Success() {
      filterJoinLookupTestCommon(true);
   }

   /**
    * @category success
    */
   @Test
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
   @Test
   @Ignore
   public void test13056Join_2S_FULL_Success() {
      filterJoinLookupTestCommon(true);
   }

   /**
    * @category success
    */
   @Test
   public void test13060Join_2S_CROSS_Success() {
      filterJoinLookupTestCommon(true);
   }

   /**
    * @throws TransformationAccessStrategyException
    * @category success comments above.
    */
   @Test
   @Ignore
   public void test13065Join_2S_NATURAL_Success() throws Exception {
      generationInterfaceAssertSuccess();

      final T odiInterface = this.odiAccessStrategy.findMappingsByName("Init JOIN_2S_NATURAL_SUCCESS",
                                                                       getRegressionConfiguration().getProjectCode());
      boolean found = false;
      try {
         found = this.odiAccessStrategy.areAllDatastoresJoinedNaturally(odiInterface);
      } catch (final Exception e) {
         LOGGER.debug(e);
      }
      assertTrue(found);
   }

   /**
    * @category success
    */
   @Test
   public void test13070Join_2S_CROSS_WithIncorrectCondition_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   public void test13070Join_2S_NATURAL_WithIncorrectCondition_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test13080Join_2S_MissingJoinCondition_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success test cannot be performed: Cross Join does not have join
    * condition.
    */
   @Test
   public void test13090Join_2S_Wrong_CROSS_Reference_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category removed test cannot be performed on HSQLDB: Natural Join not
    * supported
    */
   @Test
   @Ignore
   public void test13095Join_2S_Wrong_NATURAL_Reference_Failure() {
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
    * @category Not implemented
    */
   @Test
   @Ignore
   public void test13110Join_3S_XXXX_Success() {
      throw new RuntimeException("This test is not yet implemented.");
   }

   @Test
   // success
   public void test13120Join_2S_Explicit_ExecutionLocation_Success() throws Exception {
      // filterJoinLookupTestCommon(true);

      if (!new OdiVersion().isVersion11()) {
         generationInterfaceAssertSuccess();

         final T m = odiAccessStrategy.findMappingsByName("Init JOIN_2S_EXPLICIT_EXECUTIONLOCATION_SUCCESS",
                                                          getRegressionConfiguration().getProjectCode());

         final Map<String, String> joinExecutionLocations = odiAccessStrategy.getJoinExecutionLocations(m);
         for (final String component : joinExecutionLocations.keySet()) {
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
   public void test14030Lookup_1S_1L_Incorrect_Alias_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category fail
    */
   @Test
   @Ignore
   public void test14040Lookup_1S_1L_MalformedJoin_Failed() {
      generationInterfaceAssertFailure();
   }

   /**
    * @throws Exception
    * @category success
    */
   @Test
   // success
   public void test14050Lookup_1S_2L_Default_Success() throws Exception {
      mappingLookup_Common("DWH_STG.S_DA_O", true, DEFAULT_PROPERTIES);
      final T mapping =
              odiAccessStrategy.findMappingsByName("Init LOOKUP_1S_2L_DEFAULT_SUCCESS", "BulkLoadORACLE_DWH_STG",
                                                   getRegressionConfiguration().getProjectCode());
      final List<IMapComponent> sources = ((MapRootContainer) mapping).getSources();
      final Optional<IMapComponent> source = sources.stream()
                                                    .filter(s -> s.getName()
                                                                  .equalsIgnoreCase("D1DA"))
                                                    .findFirst();
      if (!source.isPresent()) {
         throw new RuntimeException("Can't find source");
      }
      final Optional<IMapComponent> lookup = source.get()
                                                   .getDownstreamConnectedLeafComponents()
                                                   .stream()
                                                   .findFirst();
      if (!lookup.isPresent()) {
         throw new RuntimeException("Can't find lookup");
      }
      final Optional<IMapComponent> datastore = lookup.get()
                                                      .getUpstreamConnectedLeafComponents()
                                                      .stream()
                                                      .filter(d -> d.getName()
                                                                    .equals("D1DB"))
                                                      .findFirst();
      if (!datastore.isPresent()) {
         throw new RuntimeException(
                 "Lookups need to be ordered and the first lookup should be S_DB_I with alias D1DB.");
      }
      if (!datastore.get()
                    .getName()
                    .equalsIgnoreCase("D1DB")) {
         throw new RuntimeException(String.format(
                 "Lookups need to be ordered and the first lookup should be S_DB_I with alias D1DB it is %s.",
                 datastore.get()
                          .getName()));
      }

   }

   /**
    * @category success
    */
   @Test
   // success
   public void test14060Lookup_1S_2ndUndefinedAlias_Failure() {
      generationInterfaceAssertFailure();

   }

   /**
    * @category success
    */
   @Test
   public void test14070Lookup_2S_1L_Default_Success() {
      mappingLookup_Common("DWH_STG.S_DA_O", true, DEFAULT_PROPERTIES);
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test14080Lookup_2S_1L_IncorrectAliasReference_Failure() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test14090Lookup_2D_1L_Default_Success() {
      generationInterfaceAssertSuccess();
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");

      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");

   }

   /**
    * @category success
    */
   @Test
   // success
   public void test14100Lookup_2D_1L_IncorrectAliasReference_Failure() {
      generationInterfaceAssertFailure();
   }

   // ///////////////////////////////////////////////////////////////////
   // Mapping
   // ///////////////////////////////////////////////////////////////////

   /**
    * @category success
    */
   @Test
   // success
   public void test15010Mapping_1S_FullAuto_Success() {
      mappingLookup_Common("DWH_STG.S_DA_O", true, DEFAULT_PROPERTIES);
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15020Mapping_1S_MissingColumn_Error() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15030Mapping_1S_UndefinedColumn_Warning() {
      generationInterfaceAssertFailure();
   }

   /**
    * @throws Exception
    * @category success
    */
   @Test
   // success
   public void test15040Mapping_1S_Distinct_Success() throws Exception {
      mappingLookup_Common("DWH_STG.S_DA_O", true, DEFAULT_PROPERTIES);
      final T m = odiAccessStrategy.findMappingsByName("Init MAPPING_1S_DISTINCT_SUCCESS",
                                                       getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.isDistinctMapping(m));
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15050Mapping_1S_AutoLargerTypeLength_Success() {
      generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DC_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DC_I");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      LOGGER.info(dumpFile);
      assertTrue(dbUnit.areEqual("REF.S_DA_I", "DWH_STG.S_DA_I"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DC_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DC_I");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15060Mapping_1S_AutoSmallerTypeLength_Warning() {
      generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.");
   }

   /**
    * Allowed for warning instead of error report
    *
    * @category success
    */
   @Test
   // success
   public void test15070Mapping_1S_AutoDifferentTypes_Error() {
      generationInterfaceAssert(Level.WARN, testName, "This test did not report error.");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15080Mapping_1S_AutoAndManual_Success() {
      mappingLookup_Common("DWH_STG.S_DA_O", true, DEFAULT_PROPERTIES);
   }

   /**
    * @category success
    */
   @Test
   public void test15081Mapping_1S_Temp_Success() {
      mappingLookup_Common("DWH_STG.S_MAPPING_1S_TEMP_O", true, DEFAULT_PROPERTIES);
   }

   /**
    * @category succes
    */
   @Test
   // success
   public void test15082Mapping_1S_TempSmallerTypeLength_Warning() {
      generationInterfaceAssert(Level.WARN, testName, "This test did not report warning.");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15083Mapping_1S_Magic_Success() {
      generationInterfaceAssert(Level.WARN, "test15083Mapping_1S_Magic_Success", " ",
                                TEST_PROPERTIES_BASE_DIRECTORY + "/15083_FunctionalTest.properties");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DC_D");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_DMT.W_DC_D");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                             refUserJDBCDriver, refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());

      assertTrue(dbUnit.areEqual("REF.W_DC_D", "DWH_DMT.W_DC_D"));
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DC_D");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_DMT.W_DC_D");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15084Mapping_2S_AutoMixed_Success() {
      mapping_2S_Auto_Common(true);
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15085Mapping_2S_AutoPrecedence_Success() {
      mapping_2S_Auto_Common(true);
   }

   /**
    * @category success
    */
   @Test
   public void test15086Mapping_2S_AutoOverridePrecedence_Success() {
      mapping_2S_Auto_Common(true);
   }

   /**
    * @category success
    */
   @Test
   public void test15087Mapping_2D_FullAuto_Success() {
      mapping_2D_Common(true);
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15088Mapping_2D_PartialAuto_Success() {
      mapping_2D_Common(false);

   }

   @Test
   public void test15089Mapping_1S_TempNum_Success() {
      // generationInterfaceAssertSuccess(testName, "Mapping failed");
      // deleteTemporaryInterface("I_S_MAPPING_1S_TEMP_O_S03");
      mappingLookup_Common("DWH_STG.S_MAPPING_1S_TEMP_O", true, DEFAULT_PROPERTIES);
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15090Mapping_2D_Manual_Success() {
      mappingLookup_Common("DWH_STG.S_DA_O", true, DEFAULT_PROPERTIES);
   }

   /**
    * @category successs
    */
   @Test
   public void test15100Mapping_2D_MissingExpression_Error() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category success
    */
   @Test
   @Ignore("Disabled check b/c it interferes with function calls in expressions")
   public void test15110Mapping_2D_PartialMissingExpression_Error() {
      generationInterfaceAssertFailure();

   }

   /**
    * @category success
    */
   @Test
   // success
   public void test15120Mapping_2D_TooManyExpressions_Warning() {
      generationInterfaceAssertFailure();
   }

   @Test
   // success
   public void test15130Mapping_1S_ExplicitMandatory_Success() throws Exception {
      // mappingLookup_Common("DWH_STG.S_DA_O", true);
      LOGGER.info("---->" + new OdiVersion().getVersion());
      if (!new OdiVersion().isVersion11()) {
         generationInterfaceAssertSuccess();
         // T m =
         // odiAccessStrategy.findMappingsByName(getRegressionConfiguration().getProjectCode(),
         // "Init MAPPING_1S_EXPLICITMANDATORY_SUCCESS", new HashMap<String,
         // T>());
         final Map<String, Boolean> flags = odiAccessStrategy.getFlags(getRegressionConfiguration().getProjectCode(),
                                                                       "Init MAPPING_1S_EXPLICITMANDATORY_SUCCESS",
                                                                       "VALUE");
         assertTrue(flags.get(OdiTransformationAccessStrategy.MANDATORY));
      }

   }

   @Test
   // success
   public void test15140Mapping_1S_ExplicitKey_Success() throws Exception {
      if (!new OdiVersion().isVersion11()) {
         // Needs to be failure due to error setting update key for IKM as
         // key isnt defined on datstore.
         generationInterfaceAssertFailure();
         // T m = odiAccessStrategy.findMappingsByName(getRegressionConfiguration().getProjectCode(),
         // "Init MAPPING_1S_EXPLICITKEY_SUCCESS", new HashMap<String, T>());
         // assertTrue(odiAccessStrategy.isKey(m, "KEY"));

         final Map<String, Boolean> flags = odiAccessStrategy.getFlags(getRegressionConfiguration().getProjectCode(),
                                                                       "Init MAPPING_1S_EXPLICITKEY_SUCCESS", "KEY");
         LOGGER.info("KEY = " + flags.get(OdiTransformationAccessStrategy.KEY));
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
      generationInterfaceAssertSuccess();
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_FA_F");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                             refUserJDBCDriver, refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }
      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
      // "The data is not equal with reference data.";

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_FA_F");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");

      try {
         LOGGER.info(testName);
         final T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                                                                getRegressionConfiguration().getProjectCode());
         assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM Oracle Control Append"));
      } catch (final ResourceNotFoundException e) {
         if (!new OdiVersion().isVersion11()) {
            throw e;
         }
      }

   }

   /**
    * @category success
    */
   @Test
   // success - bad properties value
   public void test16020IKM_1S_UnknownDefault_Failure() {
      generationInterfaceAssertFailure(TEST_PROPERTIES_BASE_DIRECTORY + "/16020_FunctionalTest.properties");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test16030IKM_1S_DefaultParameter_Success() {
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
      final T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                                                             getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL Incremental Update"));

   }

   /**
    * @category success
    */
   @Test
   // success
   public void test16050IKM_1S_ExplicitUnknown_Failure() {
      generationInterfaceAssertFailure(TEST_PROPERTIES_BASE_DIRECTORY + "/16020_FunctionalTest.properties");
   }

   /**
    * @throws Exception
    * @category success
    */
   @Test
   // success
   public void test16060IKM_1S_ExplicitWithExplicitParameters_Success() throws Exception {
      iKM_1S_Common(true);
      final T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                                                             getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM Oracle Incremental Update"));
   }

   /**
    * @category succcess
    */
   @Test
   public void test16070IKM_1S_ExplicitUnknown_Parameter_Error() {
      generationInterfaceAssertFailure();
   }

   /**
    * @category fail
    */
   @Test
   public void test16080IKM_1S_Unknown_ParameterValue_Error() {
      generationInterfaceAssertFailure();
   }


   @Test
   public void test16085IKM_SQL_TO_FILE_APPEND_1S() throws Exception {
      updateFileDataServer();
      generationInterfaceAssert(Level.WARN, testName, "this test did not report an error",
                                TEST_PROPERTIES_BASE_DIRECTORY + "/16086_FunctionalTest.properties");

      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      LOGGER.info("Starting scenario");
      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      LOGGER.info("Starting dbunit");
      // assertTrue(dbUnit.areEqual("DWH_STG.S_DA_I", "DWH_SRC.S_DA"));

      LOGGER.info("Starting truncate"); // "The data is not equal with
      // reference data.";

      final T odiInterface = this.odiAccessStrategy.findMappingsByName("Init IKM_SQL_TO_FILE_APPEND_1S",
                                                                       getRegressionConfiguration().getProjectCode());
      if (new OdiVersion().isVersion11()) {
         assertEquals("ORACLE_DWH.DWH_STG", this.odiAccessStrategy.findStagingAreas(odiInterface,
                                                                                    getRegressionConfiguration().getOdiContext())
                                                                  .iterator()
                                                                  .next());
      } else {
         assertEquals("FILE_SRC_DWH_UNIT", this.odiAccessStrategy.findStagingAreas(odiInterface,
                                                                                   getRegressionConfiguration().getOdiContext())
                                                                 .iterator()
                                                                 .next());
      }
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      final T mapping =
              odiAccessStrategy.findMappingsByName("Init " + testName, getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL to File Append"));
   }

   @Test
   public void test16086IKM_SQL_TO_FILE_APPEND_2S() throws Exception {
      updateFileDataServer();
      generationInterfaceAssert(Level.WARN, testName, "this test did not report an error",
                                TEST_PROPERTIES_BASE_DIRECTORY + "/16086_FunctionalTest.properties");

      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      LOGGER.info("Starting scenario");
      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      LOGGER.info("Starting dbunit");
      // assertTrue(dbUnit.areEqual("DWH_STG.S_DA_I", "DWH_SRC.S_DA"));
      LOGGER.info("Starting truncate"); // "The data is not equal with
      // reference data.";

      final T odiInterface = this.odiAccessStrategy.findMappingsByName("Init IKM_SQL_TO_FILE_APPEND_2S",
                                                                       getRegressionConfiguration().getProjectCode());
      if (new OdiVersion().isVersion11()) {
         assertEquals("ORACLE_DWH.DWH_STG", this.odiAccessStrategy.findStagingAreas(odiInterface,
                                                                                    getRegressionConfiguration().getOdiContext())
                                                                  .iterator()
                                                                  .next());
      } else {
         assertEquals("ORACLE_DWH_STG_UNIT", this.odiAccessStrategy.findStagingAreas(odiInterface,
                                                                                     getRegressionConfiguration().getOdiContext())
                                                                   .iterator()
                                                                   .next());
      }
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      final T mapping =
              odiAccessStrategy.findMappingsByName("Init " + testName, getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL to File Append"));
   }

   @Test
   public void test16087IKM_SQL_TO_FILE_APPEND_2S_Explicit() throws Exception {
      updateFileDataServer();
      generationInterfaceAssert(Level.WARN, testName, "this test did not report an error",
                                TEST_PROPERTIES_BASE_DIRECTORY + "/16087_FunctionalTest.properties");

      final T odiInterface = this.odiAccessStrategy.findMappingsByName("Init IKM_SQL_TO_FILE_APPEND_2S_Explicit",
                                                                       getRegressionConfiguration().getProjectCode());
      final Set<String> stagingAreaNames =
              this.odiAccessStrategy.findStagingAreas(odiInterface, getRegressionConfiguration().getOdiContext());
      if (new OdiVersion().isVersion11()) {
         assertEquals("ORACLE_DWH_SRC.DWH_SRC", stagingAreaNames.iterator()
                                                                .next());
      } else {
         assertEquals("ORACLE_DWH_STG_UNIT", stagingAreaNames.iterator()
                                                             .next());
      }

      LOGGER.info("Starting scenario");
      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      LOGGER.info("Starting dbunit");
      // TODO 12
      // assertTrue(dbUnit.areEqual("DWH_STG.S_DA_I", "DWH_SRC.S_DA"));
      LOGGER.info("Starting truncate");
      final T mapping =
              odiAccessStrategy.findMappingsByName("Init " + testName, getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.checkThatAllTargetsHaveIKMName(mapping, "IKM SQL to File Append"));
   }

   /**
    * Test for oracle 2 file.
    *
    * @category throws Exception
    */
   @Test
   public void test16085IKM_AUTOCOMPLEX_TOPOLOGY() {
      generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.",
                                TEST_PROPERTIES_BASE_DIRECTORY + "/16087_FunctionalTest.properties");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_FA_F");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                             refUserJDBCDriver, refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         ex.printStackTrace();
         Assert.fail(ex.getMessage());
      }
      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         ex.printStackTrace();
         Assert.fail(ex.getMessage());
      }
      final String pUser = getRegressionConfiguration().getOdiSupervisorUser();
      final String pPassword = getRegressionConfiguration().getOdiSupervisorPassword();
      final String pContextCode = getRegressionConfiguration().getOdiContext();
      final String pLogLevel = "5";
      final String pWorkRepName = getRegressionConfiguration().getOdiWorkRepositoryName();
      final String pScenarioName = testName;
      odiExecuteScenario.startScenario(DEFAULT_AGENT, pUser, pPassword, pContextCode, pLogLevel, pWorkRepName,
                                       pScenarioName);

      assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
      // "The data is not equal with reference data.";

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_FA_F");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
   }

   private void updateFileDataServer() {
      if (RegressionTestUtilities.getTechnologyNameFromString(
              getRegressionConfiguration().getMasterRepositoryTechnology())
                                 .equals(TechnologyName.ORACLE)) {
         // If we are on oracle master repository
         // set the dataserver to be that of the master repository.
         final OdiUpdateSchema updateSchema = new OdiUpdateSchema();
         final String pDataServerName = "FILE_SRC_DWH";
         final String pPassword = getRegressionConfiguration().getWorkRepositoryJdbcPassword();

         final String pOdiMasterRepoUrl = getRegressionConfiguration().getJdbcUrlMasterRepository();
         final String odiMasterRepoUser = getRegressionConfiguration().getMasterRepositoryJdbcUser();
         final String pOdiMasterRepoPassword = getRegressionConfiguration().getMasterRepositoryJdbcPassword();
         final String odiWorkRepo = getRegressionConfiguration().getWorkRepositoryJdbcUsername();
         final String odiLoginUsername = getRegressionConfiguration().getOdiSupervisorUser();
         final String odiLoginPassword = getRegressionConfiguration().getOdiSupervisorPassword();
         final String pServerInstanceName = null;
         final String jdbcDriverRepository = getRegressionConfiguration().getMasterRepositoryJdbcDriver();

         updateSchema.updateSchema(pOdiMasterRepoUrl, odiMasterRepoUser, pOdiMasterRepoPassword, odiWorkRepo,
                                   odiLoginUsername, odiLoginPassword, pDataServerName, tempDir, pPassword,
                                   pServerInstanceName, jdbcDriverRepository, tempDir);
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
    * @category success
    */
   @Test
   // success
   public void test16140CKM_1S_Undefined_Failure() {
      generationInterfaceAssertFailure(TEST_PROPERTIES_BASE_DIRECTORY + "/16140_FunctionalTest.properties");
   }

   /**
    * @category success
    */
   @Test
   // success
   public void test16150CKM_1S_Unknown_Failure() {
      generationInterfaceAssertFailure(TEST_PROPERTIES_BASE_DIRECTORY + "/16150_FunctionalTest.properties");
   }

   /**
    * Requires file in /tmp
    *
    * @throws Exception
    * @category success
    */
   @SuppressWarnings("ResultOfMethodCallIgnored")
   @Test
   @Ignore
   public void test16160LKM_1S_AutoComplexTopology_Success() throws Exception {
      final File tempFile = new File(tempDir, "countrylist.csv");
      try {
         updateFileDataServer();
         LOGGER.info("tempdir is :" + tempDir);
         copyFile(new File(TEST_XML_BASE_DIRECTORY + "/countrylist.csv"), tempFile);
         generationInterfaceAssert(Level.WARN, testName, "This test did not throw error fatal or warning messages.",
                                   TEST_PROPERTIES_BASE_DIRECTORY + "/16160_FunctionalTest.properties");
         getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                               refUserJDBC, "truncate table REF.S_DA_O");
         getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                               stgUserJDBC, "truncate table DWH_STG.S_DA_O");
         final String dir = ".";
         final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
         final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
         final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
         final DBUnitHelper dbUnit =
                 new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                  refUserJDBCDriver, refUserJDBC, refUser,
                                  getRegressionConfiguration().getOracleTestDBPassword());
         dbUnit.fullDatabaseImport();
         LOGGER.info("Starting s");
         RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                               DEFAULT_AGENT);
         LOGGER.info("Starting dbunit");
         assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
         LOGGER.info("Starting truncate"); // "The data is not equal with
         // reference data.";
         getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                               refUserJDBC, "truncate table REF.S_DA_O");
         getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                               stgUserJDBC, "truncate table DWH_STG.S_DA_O");

         final T mapping = odiAccessStrategy.findMappingsByName("Init " + (testName.toUpperCase()),
                                                                getRegressionConfiguration().getProjectCode());
         assertTrue(odiAccessStrategy.checkThatAllTargetsHaveLKMName(mapping, "LKM File to SQL"));

      } finally {
         if (tempFile.exists()) {
            tempFile.delete();
         }
      }
   }

   /**
    * @throws Exception
    * @category waiting for updated properties
    */
   @Test
   @Ignore
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
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
   }

   @Test

   public void test17100Dataset_relation() throws Exception {
      generationInterfaceAssertSuccess();
      // DATASET_RELATION
      final T odiInterface = this.odiAccessStrategy.findMappingsByName("Init DATASET_RELATION",
                                                                       getRegressionConfiguration().getProjectCode());
      boolean result = false;
      try {
         result = odiAccessStrategy.validateDataSetRelation(odiInterface);
      } catch (final Exception e) {
         e.printStackTrace();
      }
      assertTrue(result);
   }

   @Test
   // success
   public void test11020Pivot_1S_Success() throws Exception {
      if (new OdiVersion().isVersion11()) {
         return;
      }

      String interfaceName = "Pivot_1S_Success";
      final String prefix = "Init ";
      interfaceName = prefix + interfaceName;

      // Generate interfaces
      deleteScenario(DEFAULT_PROPERTIES, testName);
      LOGGER.info("Starting to create PIVOT");
      final ListAppender listAppender = getListAppender(testName);
      runController("etls", DEFAULT_PROPERTIES, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Creation of interface logged errors.");
         throw new RuntimeException("Creation of interface logged errors.");
      }
      removeAppender(listAppender);

      assertNotNull(this.odiAccessStrategy);
      final T mapping =
              this.odiAccessStrategy.findMappingsByName(interfaceName, getRegressionConfiguration().getProjectCode());
      final boolean found = mapping.getName()
                                   .equals(interfaceName);
      Assert.assertTrue(found);

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_VP_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_VP_O");


      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         ex.printStackTrace();
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_VP_O", "DWH_STG.S_VP_O"));
/*
		getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getSchemaPassword(),
				refUserJDBC, "truncate table REF.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getSchemaPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getSchemaPassword(),
				refUserJDBC, "truncate table REF.S_PV_O");
		getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getSchemaPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_O");

*/
   }

   @Test
   public void test11030UnPivot_1S_Success() throws Exception {
      if (new OdiVersion().isVersion11()) {
         return;
      }

      String interfaceName = "UnPivot_1S_Success";
      final String prefix = "Init ";
      interfaceName = prefix + interfaceName;

      // Generate interfaces
      deleteScenario(DEFAULT_PROPERTIES, testName);
      LOGGER.info("Starting to create UNPIVOT");
      final ListAppender listAppender = getListAppender(testName);
      runController("etls", DEFAULT_PROPERTIES, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         final String msg = "Creation of interface logged errors.";
         Assert.fail(msg);
         throw new RuntimeException(msg);
      }
      removeAppender(listAppender);
      assertNotNull(this.odiAccessStrategy);
      final T mapping =
              this.odiAccessStrategy.findMappingsByName(interfaceName, getRegressionConfiguration().getProjectCode());
      final boolean found = mapping.getName()
                                   .equals(interfaceName);
      Assert.assertTrue(found);


      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_VP_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_VP_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_O");


      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         ex.printStackTrace();
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_PV_O", "DWH_STG.S_PV_O"));
		/*

		getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
				refUserJDBC, "truncate table REF.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
				refUserJDBC, "truncate table REF.S_PV_O");
		getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_O");
				*/

   }

   @Test
   public void test11022Pivot_2S_Success() throws Exception {
      this.testPivot("Pivot_2S_Success", DEFAULT_PROPERTIES);
   }

   @Test
   // Ensure that pivot is connected to TargetExpression when configured.
   public void test11023Pivot_1S_TgtExp_Success() throws Exception {
      this.testPivot("Pivot_1S_TgtExp_Success", TEST_PROPERTIES_BASE_DIRECTORY + "/11023.properties");
   }

   @Test
   public void test11024Pivot_1S_Filter_Success() throws Exception {
      this.testPivot("Pivot_1S_Filter_Success", DEFAULT_PROPERTIES);
   }

   @Test
   public void test11025Pivot_1S_1L_Success() throws Exception {
      this.testPivot("Pivot_1S_1L_Success", DEFAULT_PROPERTIES);
   }

   @Test
   public void test11026Pivot_1S_Distinct_Success() throws Exception {
      this.testPivot("Pivot_1S_Distinct_Success", DEFAULT_PROPERTIES);
   }

   @Test
   public void test11027Pivot_1S_Aggregate_Success() throws Exception {
      this.testPivot("Pivot_1S_Aggregate_Success", DEFAULT_PROPERTIES);
   }

   @Test
   public void test11021Pivot_2S_2D_Success() throws Exception {
      this.testPivot("Pivot_2S_2D_Success", DEFAULT_PROPERTIES);
   }

   public void testPivot(String interfaceName, final String config) throws Exception {
      if (new OdiVersion().isVersion11()) {
         return;
      }

      final String prefix = "Init ";
      interfaceName = prefix + interfaceName;

      // Generate interfaces
      deleteScenario(DEFAULT_PROPERTIES, testName);
      LOGGER.info("Starting to create PIVOT");
      final ListAppender listAppender = getListAppender(testName);
      runController("etls", config, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         final String msg = "Creation of interface logged errors.";
         Assert.fail(msg);
         throw new RuntimeException(msg);
      }
      removeAppender(listAppender);
      assertNotNull(this.odiAccessStrategy);
      final T mapping =
              this.odiAccessStrategy.findMappingsByName(interfaceName, getRegressionConfiguration().getProjectCode());
      final boolean found = mapping.getName()
                                   .equals(interfaceName);
      Assert.assertTrue(found);
   }

   @Test
   public void test11040PivotUnPivot_1S_Success() throws Exception {
      if (new OdiVersion().isVersion11()) {
         return;
      }

      String interfaceName = "PivotUnPivot_1S_Success";
      final String prefix = "Init ";
      interfaceName = prefix + interfaceName;

      // Generate interfaces
      deleteScenario(DEFAULT_PROPERTIES, testName);
      LOGGER.info("Starting to create UNPIVOT");
      final ListAppender listAppender = getListAppender(testName);
      runController("etls", DEFAULT_PROPERTIES, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Creation of interface logged errors.");
         throw new RuntimeException("Creation of interface logged errors.");
      }
      removeAppender(listAppender);
      assertNotNull(this.odiAccessStrategy);
      final T mapping =
              this.odiAccessStrategy.findMappingsByName(interfaceName, getRegressionConfiguration().getProjectCode());
      final boolean found = mapping.getName()
                                   .equals(interfaceName);
      Assert.assertTrue(found);


      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_O");


      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         ex.printStackTrace();
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_PV_O", "DWH_STG.S_PV_O"));

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_O");

   }

   private void testSubQuery(String interfaceName, final String refTable, final String table) throws
           ResourceNotFoundException, ResourceFoundAmbiguouslyException {
      if (new OdiVersion().isVersion11()) {
         return;
      }

      final String prefix = "Init ";
      interfaceName = prefix + interfaceName;

      // Generate interfaces
      deleteScenario(DEFAULT_PROPERTIES, testName);
      final ListAppender listAppender = getListAppender(testName);
      runController("etls", DEFAULT_PROPERTIES, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         final String msg = "Creation of interface logged errors.";
         Assert.fail(msg);
         throw new RuntimeException(msg);
      }
      removeAppender(listAppender);
      assertNotNull(this.odiAccessStrategy);
      final T mapping =
              this.odiAccessStrategy.findMappingsByName(interfaceName, getRegressionConfiguration().getProjectCode());
      final boolean found = mapping.getName()
                                   .equals(interfaceName);
      Assert.assertTrue(found);

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_PV_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_VP_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_VP_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_PV_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_VP_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_VP_O");


      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         ex.printStackTrace();
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual(refTable, table));
/*
		getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
				refUserJDBC, "truncate table REF.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
				stgUserJDBC, "truncate table DWH_STG.S_PV_I");
		getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
				refUserJDBC, "truncate table REF.S_PV_O");
		getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
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
      if (new OdiVersion().isVersion11()) {
         return;
      }

      final String prefix = "Init ";
      final String interfaceName = prefix + "SubQuery_1S_ExecLoc_Success";

      // Generate interfaces
      deleteScenario(DEFAULT_PROPERTIES, testName);
      final ListAppender listAppender = getListAppender(testName);
      runController("etls", DEFAULT_PROPERTIES, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         Assert.fail("Creation of interface logged errors.");
         throw new RuntimeException("Creation of interface logged errors.");
      }
      removeAppender(listAppender);
      assertNotNull(odiAccessStrategy);
      final T mapping =
              odiAccessStrategy.findMappingsByName(interfaceName, getRegressionConfiguration().getProjectCode());
      final boolean found = mapping.getName()
                                   .equals(interfaceName);

      Assert.assertTrue(found);
      odiAccessStrategy.getSubQueryExecutionLocation(mapping)
                       .forEach((k, v) -> {
                          assert ("STAGING".equals(v)) : "Mapping " + k + " erroneously set execution location to " + v;
                       });
   }


   @Test
   // success

   public void test21010Keys_1S_Default_Success() throws Exception {
      String interfaceName = "Keys_1S_Default_Success";
      final String prefix = "Init ";
      interfaceName = prefix + interfaceName;
      final String config = DEFAULT_PROPERTIES;
      runController("dt", config, "-p", prefix, "-m", metadataDirectory);
      // Generate interfaces
      final ListAppender listAppender = getListAppender(testName);
      runController("ct", config, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         final String msg = "Creation of interface logged errors.";
         Assert.fail(msg);
      }
      assertNotNull(this.odiAccessStrategy);
      final T mapping =
              this.odiAccessStrategy.findMappingsByName(interfaceName, getRegressionConfiguration().getProjectCode());
      final boolean found = mapping.getName()
                                   .equals(interfaceName);
      Assert.assertTrue(found);
      FunctionalTestHelper.checkThatKeysAreSet(mapping, "KEY");
   }

   @Test
   // success
   public void test30010FlowStrategy_TargetComponent() {
      final String prefix = "Init ";
      final String config = TEST_PROPERTIES_BASE_DIRECTORY + "/30010.properties";
      runController("dt", config, "-p", prefix, "-m", metadataDirectory);
      // Generate interfaces
      final ListAppender listAppender = getListAppender(testName);
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
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30020.properties";
      mappingLookup_Common("DWH_STG.S_DA_O", true, properties);
      final T m = odiAccessStrategy.findMappingsByName("Init FLOWSTRATEGY_1S_DISTINCT_SUCCESS",
                                                       getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.isDistinctMapping(m));
   }

   @Test
   // success
   public void test30030FlowStrategy_use_expressions() throws Exception {
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30030.properties";
      mappingLookup_Common("DWH_STG.S_DA_O", true, properties);
      final T m = odiAccessStrategy.findMappingsByName("Init FLOWSTRATEGY_USE_EXPRESSIONS",
                                                       getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.isDistinctMapping(m));
   }

   @Test
   @Ignore("TODO upload db and enable")
   // success
   public void test30303Thirty_char_limit() {
      final String targetTable = "DWH_STG.S_DA_I";
      //, boolean checkLogger, String properties;
      LOGGER.info("Comparing " + targetTable + " from testname: " + testName);
      generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.",
                                DEFAULT_PROPERTIES);
      LOGGER.info("Comparing " + targetTable + " from testname: " + testName);
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_34567890123456789012345678_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table " + targetTable);
      LOGGER.info("Comparing " + targetTable + " from testname: " + testName);
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }
      LOGGER.info("Comparing " + targetTable + " from testname: " + testName);
      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      // Reusable mappings can't be executed.
      LOGGER.info("Comparing " + targetTable + " from testname: " + testName);
      assertTrue(dbUnit.areEqual("REF.S_DA_I", targetTable));

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_34567890123456789012345678_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table " + targetTable);
   }

   @Test
   // success
   public void test30040FlowStrategy_2D_LEFT_OUTER_Success() {
      filterJoinLookupTestCommon(true, DEFAULT_PROPERTIES);
   }

   @Test
   // success
   public void test30050FlowStrategy_no_expressions() {
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30050.properties";
      filterJoinLookupTestCommon(true, properties);
   }

   @Test
   // success
   public void test30060FlowStrategy_aggregate() {
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30050.properties";
      filterJoinLookupTestCommon(true, properties);
   }

   @Test
   // success
   public void test30070FlowStrategy_filter_set() {
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30050.properties";
      filterJoinLookupTestCommon(true, properties);
   }

   @Test
   // success
   public void test30080FlowStrategy_TargetExpressions() {
      final String prefix = "Init ";
      final String config = TEST_PROPERTIES_BASE_DIRECTORY + "/30080.properties";
      runController("dt", config, "-p", prefix, "-m", metadataDirectory);
      // Generate interfaces
      final ListAppender listAppender = getListAppender(testName);
      runController("ct", config, "-p", prefix, "-m", metadataDirectory);
      if (listAppender.contains(Level.ERROR, false)) {
         final String msg = "Creation of interface logged errors.";
         Assert.fail(msg);
         throw new RuntimeException(msg);
      }
      removeAppender(listAppender);
   }

   @Test
   // success
   public void test30090FlowStrategy_aggregate_expression() {
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
      filterJoinLookupTestCommon(true, properties);
   }


   @Test
   // success
   public void test30100FlowStrategy_aggregate_set_expression() {
      if (!new OdiVersion().isVersion11()) {
         final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
         filterJoinLookupTestCommon(true, properties);
      }
   }

   @Test
   // success
   public void test30110FlowStrategy_aggregate_set_expression_single_source() {
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
      filterJoinLookupTestCommon(true, properties);
   }

   @Test
   // success
   public void test30120FlowStrategy_aggregate_set_expression_ne() {
      if (!new OdiVersion().isVersion11()) {
         final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30120.properties";
         filterJoinLookupTestCommon(true, properties);
      }
   }

   @Test
   // success
   public void test30130FlowStrategy_aggregate_set_expression_single_source_ne() {
      final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30120.properties";
      filterJoinLookupTestCommon(true, properties);
   }

   @Test
   // success
   public void test30140FlowStrategy_double_aggregate_set_expression() {
      if (!new OdiVersion().isVersion11()) {
         final String properties = TEST_PROPERTIES_BASE_DIRECTORY + "/30090.properties";
         filterJoinLookupTestCommon(true, properties);
         // in odi 11 the filter is pushed outside the left outer,
         // which essentialy makes it an inner join.
      }
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   @Test
   public void test40010VariableTestAll() {
      final String xmlDir = TEST_XML_BASE_DIRECTORY + "/xml/variables";
      final File file = new File(xmlDir + "/expvar", "Variables.xml");
      file.delete();
      runControllerExpectError("delvar", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);
      runController("crtvar", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);
      assertEquals(17, odi12VariableAccessStrategy.findAllVariables()
                                                  .size());
      runControllerExpectError("expvar", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir + "/expvar");
      file.delete();
      runController("delvar", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);
      LOGGER.info(odi12VariableAccessStrategy.findAllVariables()
                                             .size());
      assertEquals(4, odi12VariableAccessStrategy.findAllVariables()
                                                 .size());
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   @Test
   public void test50010SequencesTestAll() {
      final String xmlDir = TEST_XML_BASE_DIRECTORY + "/xml/sequences";
      runController("delseq", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);

      runController("crtseq", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);
      assertEquals(4, odi12SequenceAccessStrategy.findAll()
                                                 .size());

      runController("expseq", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir + "/exp");
      final File file = new File(xmlDir + "/exp", "Sequences.xml");
      System.err.println(file.getAbsolutePath());
      assertTrue(file.exists());
      file.delete();

      runController("delseq", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);
      assertEquals(0, odi12SequenceAccessStrategy.findAll()
                                                 .size());
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   @Test
   public void test60010ConstraintsAll() {
      final String xmlDir = TEST_XML_BASE_DIRECTORY + "/xml/Constraints";
      runController("delcon", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);

      runController("crtcon", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);

      assertEquals(10, this.odi12ConstraintsAccessStrategy.findAllConditions()
                                                          .size());
      assertEquals(48, this.odi12ConstraintsAccessStrategy.findAllKeys()
                                                          .size());
      assertEquals(18, this.odi12ConstraintsAccessStrategy.findAllReferences()
                                                          .size());

      runController("expcon", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir + "/exp");

      final File file = new File(xmlDir + "/exp", "Constraints.xml");
      file.delete();
      runController("delcon", DEFAULT_PROPERTIES, "-p", "Init ", "-m", xmlDir);

      LOGGER.info(this.odi12ConstraintsAccessStrategy.findAllConditions()
                                                     .size());
      LOGGER.info(this.odi12ConstraintsAccessStrategy.findAllKeys()
                                                     .size());
      LOGGER.info(this.odi12ConstraintsAccessStrategy.findAllReferences()
                                                     .size());

      assertEquals(0, this.odi12ConstraintsAccessStrategy.findAllConditions()
                                                         .size());
      assertEquals(41, this.odi12ConstraintsAccessStrategy.findAllKeys()
                                                          .size());
      assertEquals(5, this.odi12ConstraintsAccessStrategy.findAllReferences()
                                                         .size());
   }

   @Test
   public void test70010Asynchronous() {
      generationInterfaceAssert(Level.WARN, testName, "This test shouldn't throw an exception.");
      final OdiPackage asynchronouspck =
              this.odiPackageAccessStrategy.findPackage("ASYNCHRONOUS", "BulkLoadORACLE_DWH_DMT",
                                                        getRegressionConfiguration().getProjectCode());
      int asynchronouspckCount = 0;
      for (final Step s : asynchronouspck.getSteps()) {
         if (s instanceof StepOdiCommand) {
            if (((StepOdiCommand) s).getCommandExpression()
                                    .getAsString()
                                    .contains("-SYNC_MODE=2")) {
               asynchronouspckCount++;
            }
         }
      }
      LOGGER.info("asynchronouspckCount:" + asynchronouspckCount);
      assert (asynchronouspckCount == 3) : "There should be 3 asynchronous scenarios in package ASYNCHRONOUS";
   }

   @Test
   @Ignore("Disabled this test because it interferes with function calls that are not parsed at all but must be supported")
   public void test80000Validation_Error() {
      try {
         runController("vldt", DEFAULT_PROPERTIES, "-p", "Init ", "-m",
                       TEST_XML_BASE_DIRECTORY + "/xml/Validation_Error");
      } catch (final UnRecoverableException e) {
         // it should throw UnRecoverableException
         return;
      }
      throw new RuntimeException("This method should throw exception.");
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   @Test
   @Ignore
   public void test20010Stream() {

      final File[] metadatas = new File(TEST_XML_BASE_DIRECTORY + "/xml/" + testName).listFiles(f -> !f.getName()
                                                                                                       .equals("0.xml"));

      if (metadatas == null || metadatas.length < 1) {
         throw new RuntimeException("Test metadata files not configured properly");
      }

      final File metadata = metadatas[0];
      LOGGER.info(metadata.getName());
      final Pattern pattern = Pattern.compile("\\d*");
      final Matcher match = pattern.matcher(metadata.getName()
                                                    .replace(".xml", ""));
      match.find();
      final String ps = match.group();
      final int packageSequence = Integer.parseInt(ps);

      // InputStream is = new FileInputStream(metadata);
      final JodiController controller = new JodiController(true);
      controller.init(new RunConfig() {
         @Override
         public String getMetadataDirectory() {
            return "";
         }

         @Override
         public List<String> getModuleClasses() {
            final List<String> module = new ArrayList<>();
            module.add("one.jodi.odi.factory.OdiModuleProvider");
            return module;
         }

         @Override
         public String getPropertyFile() {
            return DEFAULT_PROPERTIES;
         }

         @Override
         public boolean isDevMode() {
            return true;
         }

         @Override
         public String getSourceModel() {
            return null;
         }

         @Override
         public String getTargetModel() {
            return null;
         }

         @Override
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

      final TransformationService ts = ServiceFactory.getInstance()
                                                     .getServiceInstance(TransformationService.class);
      FileInputStream fos1 = null;
      FileInputStream fos2 = null;
      try {
         fos1 = new FileInputStream(metadata);
         fos2 = new FileInputStream(metadata);
         ts.deleteTransformation(fos1);
         ts.createTransformation(fos2, packageSequence, false);
      } catch (final FileNotFoundException e) {
         throw new RuntimeException(e);
      } finally {
         if (fos1 != null) {
            try {
               fos1.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
         if (fos2 != null) {
            try {
               fos2.close();
            } catch (final IOException e) {
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
      // getOracleTestDBPassword(), refUserJDBC,
      // "truncate table REF.S_DA_O");
      // getSqlHelper().executedSQLSuccesfully(stgUser,
      // getOracleTestDBPassword(), stgUserJDBC,
      // "truncate table DWH_STG.S_DA_I");
      // getSqlHelper().executedSQLSuccesfully(stgUser,
      // getOracleTestDBPassword(), stgUserJDBC,
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
      // getOracleTestDBPassword());
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
      // getOracleTestDBPassword(), refUserJDBC,
      // "truncate table REF.S_DA_O");
      // getSqlHelper().executedSQLSuccesfully(stgUser,
      // getOracleTestDBPassword(), stgUserJDBC,
      // "truncate table DWH_STG.S_DA_I");
      // getSqlHelper().executedSQLSuccesfully(stgUser,
      // getOracleTestDBPassword(), stgUserJDBC,
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
         } catch (final InterruptedException e) {
            e.printStackTrace();
         }
      }
   }

   private void cKM_1S_Common(final String properties) throws Exception {
      if (properties != null) {
         generationInterfaceAssertSuccess(properties);
      } else {
         generationInterfaceAssertSuccess();
      }
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                             refUserJDBCDriver, refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
      if (testName.equals("CKM_1S_Default_Success") || testName.equals("CKM_1S_NotActive_Success") ||
              testName.equals("CKM_1S_Manual_Success") || testName.equals("CKM_1S_ManualAndIKM_Success")) {
         testName = testName.toUpperCase();
      }
      final T mapping =
              odiAccessStrategy.findMappingsByName("Init " + testName, getRegressionConfiguration().getProjectCode());
      assertTrue(odiAccessStrategy.checkThatAllTargetsHaveCKMName(mapping, "CKM Oracle"));
   }

   private void model_2D_Common() {
      generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.");
      // export for some data
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                             refUserJDBCDriver, refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }
      //
      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());

      assertTrue(dbUnit.areEqual("REF.W_DA_D", "DWH_DMT.W_DA_D"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";
      //

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
   }

   private void filterJoinLookupTestCommon(final boolean joinLookup) {
      filterJoinLookupTestCommon(joinLookup, DEFAULT_PROPERTIES);
   }

   private void filterJoinLookupTestCommon(final boolean joinLookup, final String properties) {
      generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.", properties);
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");

      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName.toUpperCase(),
                                            getRegressionConfiguration(), DEFAULT_AGENT);

      if (joinLookup) {
         assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
         // "The data is not equal with reference data.";

      } else {
         try {
            assertTrue(dbUnit.areEqual(new File(dumpFile), "REF.S_DA_O", "DWH_STG.S_DA_O",
                                       new String[]{"KEY", "VALUE", "LAST_CHANGED_DT"}));// :
            // "The data is not equal with reference data.";
         } catch (final Exception e) {
            LOGGER.fatal(e);
            Assert.fail(e.getMessage());
         }
      }
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
   }

   /// this test is not working.
   @Test
   public void test89981PackageCreation_ExecProc() throws Exception {
      generationInterfaceAssertSuccess();
      final Packages packages = loadPackagesMetaData();

      assertNotNull(packages);
      assertNotNull(packages.getPackage());

      for (final Package p : packages.getPackage()) {
         validatePackage(p);
         deletePackage(DEFAULT_PROPERTIES, p.getPackageName(), "BulkLoadExplicit/TargetFolder");
      }

      final ProcedureInternal procedure =
              this.getOdiProcedureService.extractProcedures(getRegressionConfiguration().getProjectCode())
                                         .stream()
                                         .filter(p -> p.getName()
                                                       .equals("PROC_DUAL_TEST"))
                                         .findFirst()
                                         .orElse(null);

      Assert.assertNotNull(procedure);
      Assert.assertEquals("wrong folder", "BulkLoadExplicit/TargetFolder", procedure.getFolderPath());
      Assert.assertEquals("wrong line count", 2, procedure.getTasks()
                                                          .size());
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

      deleteProcedures(DEFAULT_PROPERTIES);
      final Optional<ProcedureInternal> delProcedure =
              this.getOdiProcedureService.extractProcedures(getRegressionConfiguration().getProjectCode())
                                         .stream()
                                         .filter(p -> p.getName()
                                                       .equals("PROC_DUAL_TEST"))
                                         .findFirst();
      Assert.assertFalse(delProcedure.isPresent());
   }

   @Test
   public void test89982PackageCreation_ExecProcWithParams() throws Exception {
      generationInterfaceAssertSuccess();
      final Packages packages = loadPackagesMetaData();

      assertNotNull(packages);
      assertNotNull(packages.getPackage());

      for (final Package p : packages.getPackage()) {
         validatePackage(p);
         deletePackage(DEFAULT_PROPERTIES, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
      }
   }

   @Test
   public void test89983PackageCreation_ExecPackageAsync() throws Exception {
      generationInterfaceAssertSuccess();
      final Packages packages = loadPackagesMetaData();

      assertNotNull(packages);
      assertNotNull(packages.getPackage());

      for (final Package p : packages.getPackage()) {
         validatePackage(p);
         deletePackage(DEFAULT_PROPERTIES, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
      }
   }

   @Test
   public void test89984PackageCreation_ExecPackageNoAsync() throws Exception {
      generationInterfaceAssertSuccess();
      final Packages packages = loadPackagesMetaData();

      assertNotNull(packages);
      assertNotNull(packages.getPackage());

      for (final Package p : packages.getPackage()) {
         validatePackage(p);
         deletePackage(DEFAULT_PROPERTIES, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
      }
   }

   @Test
   public void test89985PackageCreation_SuccessStep() throws Exception {
      generationInterfaceAssertSuccess();
      final Packages packages = loadPackagesMetaData();

      assertNotNull(packages);
      assertNotNull(packages.getPackage());

      for (final Package p : packages.getPackage()) {
         validatePackage(p);
         deletePackage(DEFAULT_PROPERTIES, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
      }
   }

   @Test
   public void test89986PackageCreation_FailureStep() throws Exception {
      generationInterfaceAssertSuccess();
      final Packages packages = loadPackagesMetaData();

      assertNotNull(packages);
      assertNotNull(packages.getPackage());

      for (final Package p : packages.getPackage()) {
         validatePackage(p);
         deletePackage(DEFAULT_PROPERTIES, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
      }
   }

   @Test
   public void test89987PackageCreation_WrongFolder() {
      deleteAllPackagesAndScenarios();
      deletePackage(DEFAULT_PROPERTIES, "PACKAGE_CREATION_TEST1_WRONG_FOLDER", "BulkLoadORACLE_DWH_STG");
      deletePackage(DEFAULT_PROPERTIES, "PACKAGE_CREATION_TEST2_WRONG_FOLDER", "BulkLoadORACLE_DWH_STG");
      deletePackage(DEFAULT_PROPERTIES, "PACKAGE_CREATION_TEST3_WRONG_FOLDER", "BulkLoadORACLE_DWH_STG");

      T mapping = null;
      try {
         mapping = odiAccessStrategy.findMappingsByName("PC_Interface3_wrong_folder",
                                                        getRegressionConfiguration().getProjectCode());
      } catch (final Exception e) {
         // no-op
      }

      if (mapping != null) {
         final ITransactionManager tm = getWorkOdiInstance().getOdiInstance()
                                                            .getTransactionManager();
         getWorkOdiInstance().getOdiInstance()
                             .getTransactionalEntityManager()
                             .remove(mapping);
         tm.commit(getWorkOdiInstance().getTransactionStatus());
      }
      generationInterfaceAssertFailure();

      final String wrongFolder = "BulkLoadORACLE_DWH_STG";
      final String packageName = "PACKAGE_CREATION_TEST1_WRONG_FOLDER";
      final OdiPackage odiPackage = odiPackageAccessStrategy.findPackage(packageName, wrongFolder,
                                                                         getRegressionConfiguration().getProjectCode());
      Collection<Step> steps = null;
      if (odiPackage != null && odiPackage.getSteps() != null) {
         steps = odiPackage.getSteps();
      }
      if (steps != null) {
         int nextStepIsNullCounter = 0;
         for (final Step step : steps) {
            LOGGER.info("step:" + step.getName());
            LOGGER.info("Next step: " + step.getNextStepAfterSuccess());
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
      generationInterfaceAssertSuccess();
      final Packages packages = loadPackagesMetaData();

      assertNotNull(packages);
      assertNotNull(packages.getPackage());

      for (final Package p : packages.getPackage()) {
         validatePackage(p);
         deletePackage(DEFAULT_PROPERTIES, p.getPackageName(), "BulkLoadORACLE_DWH_STG");
      }
   }

   private Packages loadPackagesMetaData() throws Exception {
      final Properties p = new Properties();
      FileInputStream fis = null;
      InputStream is = null;
      Packages packages;
      try {
         fis = new FileInputStream(DEFAULT_PROPERTIES);
         p.load(fis);

         final XMLParserUtil<Packages, one.jodi.core.etlmodel.ObjectFactory> etlParser =
                 new XMLParserUtil<>(one.jodi.core.etlmodel.ObjectFactory.class,
                                     JodiConstants.getEmbeddedXSDFileNames(), errorWarningMessages);

         final File etlFile = new File(TEST_XML_BASE_DIRECTORY + "/xml/" + testName, "0.xml");

         is = new FileInputStream(etlFile);
         packages = etlParser.loadObjectFromXMLAndValidate(is, p.getProperty("xml.xsd.packages"), etlFile.getPath());
      } catch (final FileNotFoundException e) {
         final String friendlyError = "FATAL: xml file  not found.";
         throw new RuntimeException(friendlyError, e);
      } finally {
         if (is != null) {
            try {
               is.close();
            } catch (final IOException e) {
               // no-op
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
   private void validatePackage(final Package jodiPackage) {
      assertNotNull("Package instance was null", jodiPackage);
      final String packageName = jodiPackage.getPackageName();
      assertNotNull("Package name was null", packageName);
      final OdiInstance odiInstance = getWorkOdiInstance().getOdiInstance();
      final IOdiPackageFinder finder = ((IOdiPackageFinder) odiInstance.getTransactionalEntityManager()
                                                                       .getFinder(OdiPackage.class));

      // consider nested folders
      final String[] folderPath = jodiPackage.getFolderCode()
                                             .split("/");
      final String containingFolder = folderPath[folderPath.length - 1];
      final Collection<OdiPackage> pList =
              finder.findByName(packageName, getRegressionConfiguration().getProjectCode(), containingFolder);

      assertNotNull("No ODI Package found with name " + packageName + " in folder " + jodiPackage.getFolderCode(),
                    pList);
      assertFalse("Multiple ODI Packages found with name " + packageName + " in folder " + jodiPackage.getFolderCode(),
                  pList.size() > 1);
      assertEquals("No ODI Package found with name " + packageName + " in folder " + jodiPackage.getFolderCode(), 1,
                   pList.size());

      final OdiPackage odiPackage = pList.iterator()
                                         .next();
      final Collection<Step> odiSteps = odiPackage.getSteps();
      final Steps beforeSteps = jodiPackage.getBefore();
      final int beforeSize = (beforeSteps != null && beforeSteps.getStep() != null ? beforeSteps.getStep()
                                                                                                .size() : 0);
      final Steps afterSteps = jodiPackage.getAfter();
      final int afterSize = (afterSteps != null && afterSteps.getStep() != null ? afterSteps.getStep()
                                                                                            .size() : 0);
      final Steps failureSteps = jodiPackage.getFailure();
      final int failureSize = (failureSteps != null && failureSteps.getStep() != null ? failureSteps.getStep()
                                                                                                    .size() : 0);

      final int interfaceSize = odiSteps.size() - beforeSize - afterSize - failureSize;
      assertTrue("Not enough steps in the " + packageName +
                         " ODI package to account for all of the steps defined in the metadata.", interfaceSize >= 0);

      assertEquals("Number of steps in metadata does not match number of steps in ODI package",
                   (beforeSize + interfaceSize + afterSize + failureSize), odiSteps.size());

      Step currentOdiStep = odiPackage.getFirstStep();

      if (beforeSize > 0) {
         LOGGER.info("validating package: " + jodiPackage.getPackageName());
         currentOdiStep = validateSteps(currentOdiStep, beforeSteps);
      }

      /*
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

         final StepType jodiStep = findLabeledStep(jodiPackage.getGotoOnFinalSuccess(), beforeSteps);

         assertNotNull("Could not find the GoToOnFinalSuccess step.", jodiStep);
         validateStep(currentOdiStep, jodiStep);
      } else {
         assertNull("Expected final step but addition ODI steps exists.", currentOdiStep);
      }
   }

   private Step validateSteps(final Step currentStep, final Steps steps) {
      Step activeStep = currentStep;

      for (final JAXBElement<? extends StepType> element : steps.getStep()) {
         final StepType st = element.getValue();
         validateStep(activeStep, st);
         activeStep = activeStep.getNextStepAfterSuccess();
      }
      return activeStep;
   }

   private void validateStep(final Step odiStep, final StepType jodiStep) {
      LOGGER.info(String.format("Odi '%2$s' Jodi '%1$s'", getStepLabel(jodiStep), odiStep.getName()));
      assertTrue("Metadata step name does not match ODI step name",
                 StringUtils.equals(getStepLabel(jodiStep), odiStep.getName()));
      if (jodiStep instanceof VariableType) {
         assertTrue("Metada step is a variable but ODI step was not", odiStep instanceof StepVariable);
      } else if (jodiStep instanceof ExecProcedureType) {
         assertTrue("Metada step is a procedure but ODI step was not", odiStep instanceof StepProcedure);
      } else if (jodiStep instanceof ExecPackageType) {
         assertTrue("Metada step is a package but ODI step was not", odiStep instanceof StepOdiCommand);
         final StepOdiCommand commandStep = (StepOdiCommand) odiStep;
         assertNotNull("Command expression should not be null", commandStep.getCommandExpression());

         if (((ExecPackageType) jodiStep).isAsynchronous() != null && ((ExecPackageType) jodiStep).isAsynchronous()) {
            assertTrue("ODI command should be asynchronous", commandStep.getCommandExpression()
                                                                        .getAsString()
                                                                        .contains("SYNC_MODE=2"));
         }
      } else //noinspection StatementWithEmptyBody
         if (jodiStep instanceof ExecCommandType) {
            // rien ne vas plus
         } else {
            fail("Unrecognized StepType '" + jodiStep.getClass()
                                                     .getName() + "'");
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
   private Step validateInterfaceSteps(final int count, final Step currentStep) {
      return new FunctionalTestHelper().validateInterfaceStep(count, currentStep);
   }

   private StepType findLabeledStep(final String label, final Steps... steps) {
      StepType result = null;

      for (final Steps cSteps : steps) {
         if (cSteps.getStep() != null) {
            for (final JAXBElement<? extends StepType> element : cSteps.getStep()) {
               final StepType st = element.getValue();
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

   private void set_2D_Explicit_Common(final String dumpFile) {
      generationInterfaceAssertSuccess();
      // export for some data
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      final String dir = ".";
      // String dumpFile = TEST_DATA_BASE_DIRECTORY +
      // "/Set_2D_Explicit_Union_Success.xml";
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      final DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC,
                                                   getRegressionConfiguration().getSysdbaUser(), jdbcDBPassword,
                                                   refUserJDBCDriver, refUserJDBC, refUser,
                                                   getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      Assert.assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O")); // :
   }

   private void iKM_1S_Common(final boolean is16060) {
      if (is16060) {
         generationInterfaceAssertSuccess(TEST_PROPERTIES_BASE_DIRECTORY + "/16060_FunctionalTest.properties");
      } else {
         generationInterfaceAssertSuccess();
      }
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_FA_F");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      DBUnitHelper dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                                             refUserJDBCDriver, refUserJDBC, refUser,
                                             getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      assertTrue(dbUnit.areEqual("REF.W_FA_F", "DWH_DMT.W_FA_F"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_FA_F");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_FA_I");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_FA_F");
   }

   private void model_1S_Override_Common(final boolean lookup) {
      generationInterfaceAssertSuccess();
      // export for some data
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBConnection = stgUserJDBC;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }
      //
      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      if (lookup) {
         dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
                                   refUserJDBCDriver, refUserJDBC, refUser,
                                   getRegressionConfiguration().getOracleTestDBPassword());
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
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");

   }

   private void trans_1S_Common(final boolean checkLogger, final String interfaceName, final String folderPath) throws
           Exception {
      final String prefix = "Init ";
      final String config = DEFAULT_PROPERTIES;

      runController("dt", config, "-p", prefix, "-m", metadataDirectory);
      // Generate interfaces
      final ListAppender listAppender = getListAppender(testName);
      runController("ct", config, "-p", prefix, "-m", metadataDirectory);
      if (checkLogger) {
         if (listAppender.contains(Level.WARN, false)) {
            final String msg = "Creation of interface logged warning.";
            Assert.fail(msg);
            throw new RuntimeException(msg);
         }
      }
      removeAppender(listAppender);
      assertNotNull(this.odiAccessStrategy);
      final IMapping odiInterface;
      try {
         odiInterface = (IMapping) this.odiAccessStrategy.findMappingsByName(interfaceName,
                                                                             getRegressionConfiguration().getProjectCode());
         final boolean found = odiInterface.getName()
                                           .equals(interfaceName);
         Assert.assertTrue(found);
      } catch (final ResourceNotFoundException e) {
         fail("Mapping " + interfaceName + " not found in project " + getRegressionConfiguration().getProjectCode() +
                      ": " + e.getMessage());
         throw e;
      } catch (final ResourceFoundAmbiguouslyException e) {
         fail("Multiple Mapping fopund with name " + interfaceName + " in project " +
                      getRegressionConfiguration().getProjectCode() + ": " + e.getMessage());
         throw e;
      }

      if (folderPath != null) {
         final OdiFolder folder = (OdiFolder) odiInterface.getFolder();
         Assert.assertNotNull(folder);
         Assert.assertEquals(folderPath, Odi12FolderHelper.getFolderPath(folder));
      }
   }

   private void generationInterfaceAssert(final Level level, final String testName, final String execeptionMessage,
                                          final String properties) {
      final String levelStr = level == Level.ERROR ? "error" : "warning";
      final String prefix = "Init ";
      String report = null;
      final ListAppender listAppender = getListAppender(testName);
      try {
         report = runController("etls", properties, "-p", prefix, "-m", metadataDirectory);
      } catch (final Exception ex) {
         Assert.fail(
                 testName + String.format(": did throw exception it should report %s.", levelStr) + execeptionMessage);
      }

      if (!listAppender.contains(level, false) && !report.isEmpty()) {
         final String msg = testName + ":" + execeptionMessage + String.format(": did not report %s.", levelStr);
         Assert.fail(msg);
         throw new RuntimeException(msg);
      }
      removeAppender(listAppender);
   }

   private void generationInterfaceAssert(final Level level, final String testName, final String execeptionMessage) {
      generationInterfaceAssert(level, testName, execeptionMessage, DEFAULT_PROPERTIES);
   }

   private void mappingLookup_Common(final String targetTable, final boolean checkLogger, final String properties) {

      if (checkLogger) {
         generationInterfaceAssertSuccess(properties);
      } else {
         generationInterfaceAssert(Level.WARN, testName, "This test threw an exception which it should not.",
                                   properties);
      }

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }
      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      // Reusable mappings can't be executed.
      LOGGER.info("Comparing " + targetTable + " from testname: " + testName);
      assertTrue(dbUnit.areEqual("REF.S_DA_O", targetTable));

      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
   }

   private void mapping_2S_Auto_Common(final boolean checkLogger) {
      if (checkLogger) {
         generationInterfaceAssertSuccess();
      } else {
         generationInterfaceAssert(Level.WARN, testName, "This test did not report error.");
      }
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
   }

   private void source_2S_Common(final boolean isDefault, final boolean checkLogger) {
      deleteScenario(DEFAULT_PROPERTIES, testName);
      if (!checkLogger) {
         generationInterfaceAssertSuccess();
      } else {
         generationInterfaceAssert(Level.WARN, testName, "This test threw an exception it should not.");
      }
      // export for some data
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/Source_2S_Default_success.xml";
      final String jdbcDBConnection = stgUserJDBC;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());

      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }
      getSqlHelper().executedSQLSuccesfully(jdbcDBUsername, jdbcDBPassword, jdbcDBConnection,
                                            "truncate table DWH_STG.S_SOURCE_2S_ONEINTF_I");
      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

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
   }

   private void model_2S_Common(final String jdbcUser, final String jdbcUrl, final String dbSchema) {
      generationInterfaceAssertSuccess();
      // export for some data
      getSqlHelper().executedSQLSuccesfully(jdbcUser, getRegressionConfiguration().getOracleTestDBPassword(), jdbcUrl,
                                            String.format("truncate table %s.S_DA_I", dbSchema));
      getSqlHelper().executedSQLSuccesfully(jdbcUser, getRegressionConfiguration().getOracleTestDBPassword(), jdbcUrl,
                                            String.format("truncate table %s.S_DB_I", dbSchema));
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String driverDBClass = stgUserJDBCDriver;
      final String jdbcDBConnection = stgUserJDBC;
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      //
      DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }
      //
      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, dmtUserJDBC, dmtUser,
                                getRegressionConfiguration().getOracleTestDBPassword(), refUserJDBCDriver, refUserJDBC,
                                refUser, getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDMTDatabaseImport();
      } catch (final Exception ex) {
         LOGGER.fatal(ex);
         Assert.fail(ex.getMessage());
      }
      //

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      dbUnit = new DBUnitHelper(dir, dumpFile, driverDBClass, jdbcDBConnection, jdbcDBUsername, jdbcDBPassword,
                                refUserJDBCDriver, refUserJDBC, refUser,
                                getRegressionConfiguration().getOracleTestDBPassword());

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));// :
      // "The data
      // is not
      // equal
      // with
      // reference
      // data.";
      //
      getSqlHelper().executedSQLSuccesfully(jdbcUser, getRegressionConfiguration().getOracleTestDBPassword(), jdbcUrl,
                                            String.format("truncate table %s.S_DA_I", dbSchema));
      getSqlHelper().executedSQLSuccesfully(jdbcUser, getRegressionConfiguration().getOracleTestDBPassword(), jdbcUrl,
                                            String.format("truncate table %s.S_DB_I", dbSchema));
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DA_D");
      getSqlHelper().executedSQLSuccesfully(dmtUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            dmtUserJDBC, "truncate table DWH_DMT.W_DB_D");
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
   }

   private void mapping_2D_Common(final boolean checkLog) {
      if (checkLog) {
         generationInterfaceAssertSuccess();
      } else {
         generationInterfaceAssert(Level.WARN, testName, "This test did not report error.");
      }
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");
      final String dir = ".";
      final String dumpFile = TEST_XML_BASE_DIRECTORY + "/" + testName + ".xml";
      final String jdbcDBUsername = getRegressionConfiguration().getSysdbaUser();
      final String jdbcDBPassword = getRegressionConfiguration().getSysdbaPassword();
      final DBUnitHelper dbUnit =
              new DBUnitHelper(dir, dumpFile, stgUserJDBCDriver, stgUserJDBC, jdbcDBUsername, jdbcDBPassword,
                               refUserJDBCDriver, refUserJDBC, refUser,
                               getRegressionConfiguration().getOracleTestDBPassword());
      try {
         dbUnit.fullDatabaseImport();
      } catch (final Exception ex) {
         Assert.fail(ex.getMessage());
      }

      RegressionTestUtilities.startScenario(odiExecuteScenario, "5", testName, getRegressionConfiguration(),
                                            DEFAULT_AGENT);

      assertTrue(dbUnit.areEqual("REF.S_DA_O", "DWH_STG.S_DA_O"));
      getSqlHelper().executedSQLSuccesfully(refUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            refUserJDBC, "truncate table REF.S_DA_O");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DB_I");
      getSqlHelper().executedSQLSuccesfully(stgUser, getRegressionConfiguration().getOracleTestDBPassword(),
                                            stgUserJDBC, "truncate table DWH_STG.S_DA_O");

   }

   private String getStepLabel(final StepType jodiStep) {
      return (StringUtils.hasLength(jodiStep.getLabel()) ? jodiStep.getLabel() : jodiStep.getName());
   }

   private String executeCommand(final String command) {

      final StringBuilder output = new StringBuilder();

      final Process p;
      InputStreamReader inputStream = null;
      BufferedReader reader = null;
      try {
         p = Runtime.getRuntime()
                    .exec(command);
         p.waitFor();
         inputStream = new InputStreamReader(p.getInputStream());
         reader = new BufferedReader(inputStream);
         String line;
         while ((line = reader.readLine()) != null) {
            output.append(line)
                  .append("\n");
         }
      } catch (final IOException | InterruptedException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      } finally {
         if (inputStream != null) {
            try {
               inputStream.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
         if (reader != null) {
            try {
               reader.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
      return output.toString();
   }

   @Test
   @Ignore
   public void test40000UpdateAgent() {
      final OdiUpdateAgent updateAgent = new OdiUpdateAgent();
      final String odiMasterRepoUrl = getRegressionConfiguration().getMasterRepositoryJdbcUrl();
      final String odiMasterRepoUser = getRegressionConfiguration().getMasterRepositoryJdbcUser();
      final String odiMasterRepoPassword = getRegressionConfiguration().getMasterRepositoryJdbcPassword();
      final String odiWorkRepo = getRegressionConfiguration().getOdiWorkRepositoryName();
      final String odiLoginUsername = getRegressionConfiguration().getOdiSupervisorUser();
      final String odiLoginPassword = getRegressionConfiguration().getOdiSupervisorPassword();
      final String agentUrl = "http://test:20910/test";
      final String jdbcDriver = getRegressionConfiguration().getMasterRepositoryJdbcDriver();
      final String agentName = "TEST";
      updateAgent.updateAgent(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo, odiLoginUsername,
                              odiLoginPassword, agentUrl, jdbcDriver, agentName);
   }

   @Test
   @Ignore
   public void test40010UpdateDataServer() {
      final OdiUpdateDataserver updateDataServer = new OdiUpdateDataserver();
      final String odiMasterRepoUrl = getRegressionConfiguration().getMasterRepositoryJdbcUrl();
      final String odiMasterRepoUser = getRegressionConfiguration().getMasterRepositoryJdbcUser();
      final String odiMasterRepoPassword = getRegressionConfiguration().getMasterRepositoryJdbcPassword();
      final String odiWorkRepo = getRegressionConfiguration().getOdiWorkRepositoryName();
      final String odiLoginUsername = getRegressionConfiguration().getOdiSupervisorUser();
      final String odiLoginPassword = getRegressionConfiguration().getOdiSupervisorPassword();
      final String dataServerName = "";
      final String jdbcString = "";
      final String username = "";
      final String password = "";
      final String serverInstanceName = "";
      final String jdbcDriverRepository = getRegressionConfiguration().getMasterRepositoryJdbcDriver();
      final String jdbcDriverServer = getRegressionConfiguration().getMasterRepositoryJdbcDriver();
      updateDataServer.updateDataServer(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo,
                                        odiLoginUsername, odiLoginPassword, dataServerName, jdbcString, username,
                                        password, serverInstanceName, jdbcDriverRepository, jdbcDriverServer);
   }

   @Test
   @Ignore
   public void test4020UpdateUser() {
      final OdiUpdateUser updateUser = new OdiUpdateUser();
      final String odiMasterRepoUrl = getRegressionConfiguration().getMasterRepositoryJdbcUrl();
      final String odiMasterRepoUser = getRegressionConfiguration().getMasterRepositoryJdbcUser();
      final String odiMasterRepoPassword = getRegressionConfiguration().getMasterRepositoryJdbcPassword();
      final String odiWorkRepo = getRegressionConfiguration().getOdiWorkRepositoryName();
      final String odiLoginUsername = getRegressionConfiguration().getOdiSupervisorUser();
      final String odiLoginPassword = getRegressionConfiguration().getOdiSupervisorPassword();
      final String password = "test";
      final String jdbcDriverMasterRepo = getRegressionConfiguration().getMasterRepositoryJdbcDriver();
      updateUser.updateUser(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo, odiLoginUsername,
                            odiLoginPassword, password, jdbcDriverMasterRepo);
   }
}
