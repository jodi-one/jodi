package one.jodi.qa.test;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.bootstrap.JodiController;
import one.jodi.db.SQLHelper;
import one.jodi.odi.runtime.OdiConnection;
import one.jodi.odi.runtime.OdiConnectionFactory;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.config.MasterRepositoryDbInfo;
import oracle.odi.core.config.OdiInstanceConfig;
import oracle.odi.core.config.PoolingAttributes;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.security.Authentication;
import oracle.odi.impexp.OdiImportException;
import oracle.odi.impexp.smartie.ISmartImportService;
import oracle.odi.impexp.smartie.impl.SmartImportServiceImpl;
import oracle.odi.setup.AuthenticationConfiguration;
import oracle.odi.setup.IMasterRepositorySetup;
import oracle.odi.setup.IWorkRepositorySetup;
import oracle.odi.setup.RepositorySetupException;
import oracle.odi.setup.support.MasterRepositorySetupImpl;
import oracle.odi.setup.support.WorkRepositorySetupImpl;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.Assert;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class RegressionTestImpl implements RegressionTest {
    protected final RegressionConfiguration regressionConfiguration;
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(RegressionTestImpl.class);
    private final File smartExport;
    private final SQLHelper sqlHelper = new SQLHelper();
    private final ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiImpl.getInstance();
    private final String propertiesFile;
    private final JodiController controller = new JodiController(true);
    private OdiInstance masterOdiInstance;
    private OdiConnection workOdiConnection;

    public RegressionTestImpl(final String propertiesFile,
                              final String password, final String masterPassword)
            throws ConfigurationException {
        regressionConfiguration = new PropertiesBasedRegressionTestConfiguration(
                propertiesFile, password, masterPassword);

        smartExport = new File(regressionConfiguration.getSmartExport());
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        // set this level to debug for debugging
        rootLogger.setLevel(Level.INFO);

//		Enumeration<?> appenders = rootLogger.getAllAppenders();
//		while (appenders.hasMoreElements()) {
//			Appender appender = (Appender) appenders.nextElement();
//			String PATTERN = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
//			appender.setLayout(new PatternLayout(PATTERN));
//		}
//		//
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.INFO);
        //
        this.propertiesFile = propertiesFile;
        // do this only once per test implementation
//      if(cacheString.equals(this.propertiesFile))
//            return;
//		try {
//		   byte[] encoded = Files.readAllBytes(Paths.get("src/test/resources/create_profile.sql"));
//			String sql = new String(encoded, "UTF-8");
//			if (sql.trim().endsWith("/"))
//				sql = sql.substring(0, sql.length() - 1);
//			Assert.assertTrue(this.sqlHelper.executedSQLSuccesfully(
//					regressionConfiguration.getSysdbaUser(),
//					regressionConfiguration.getSysdbaPassword(),
//					regressionConfiguration.getMasterRepositoryJdbcUrl()
//					,sql ));
//		} catch (IOException e) {
//			throw new RuntimeException("Can't read path to apply create_profile to prevent errors.");
//		}
//      cacheString = this.propertiesFile;
    }


    @Override
    public SQLHelper getSqlHelper() {
        return sqlHelper;
    }


    @Override
    public void test010Install() {

    }

    public abstract void test020Generation();

    public abstract void test030ing();


    /**
     * This function relies on environment prpoperty ODI_ORACLE_HOME and a
     * precise directory structure with ODI Companion libraries installed.
     * <p>
     * To run add the following JVM arguments (note trailing slash!)
     * -Djbo.debugoutput=console -Xss1024k -Xmx500M -Dlog4j.debug
     * -DODI_ORACLE_HOME=<user>\odi_scripts\ Create the following directory
     * structure in your <user> directory <user> oracle.odi.common
     * oracledi.common odi scripts xml (copied from
     * <ODI_COMPANION_HOME>\sdk\oracledi.sdk\lib\scripts\xml) ORACLE (copied
     * recursively from
     * <ODI_COMPANION_HOME>\sdk\oracledi.sdk\lib\scripts\ORACLE) odi_scripts
     * oracledi.sdk lib scripts xml ORACLE This function relies on: An
     * environmental property: ODI_ORACLE_HOME Repository scripts located at:
     * $ODI_ORACLE_HOME/../oracle.odi.common/oracledi.common/odi/scripts/xml,
     * The original scripts may be found in the ODI Companion CD:
     * sdk/oracledi.sdk/lib/scripts/xml/
     *
     * @throws RepositorySetupException
     */

    @Override
    @SuppressWarnings("deprecation")
    public boolean repositoryIsSetup() throws RepositorySetupException {
        String workRepositoryName = RegressionTestUtilities.getWorkReposName(regressionConfiguration.getWorkRepositoryJdbcUsername());
        int masterRepositoryId = RegressionTestUtilities.getWorkReposId(regressionConfiguration.getWorkRepositoryJdbcUsername());
        logger.info("Workrepid:" + workRepositoryName + " Id:"
                + masterRepositoryId);

        int[] forbiddenIds = new int[]{0, 353, 366, 500, 541, 542, 651, 1,
                36, 74, 91, 129, 268, 353, 366, 452, 541, 542, 600, 651, 656,
                667, 720, 789, 801, 888};
        for (int forbiddenId : forbiddenIds) {
            if (forbiddenId == regressionConfiguration.getWorkRepositoryId()) {
                throw new RuntimeException(
                        "Unsupported workRepositoryId: "
                                + regressionConfiguration.getWorkRepositoryId()
                                + " change the masterRepositoryJdbcUser ( "
                                + regressionConfiguration
                                .getMasterRepositoryJdbcUser()
                                + " is not supported.). See support.oracle.com: 1504665.1");
            }
        }

        logger.info("======================================================");
        logger.info("Repository Creation Started....");
        logger.info("======================================================");
        logger.info("Master Repository Creation Started....");
        IMasterRepositorySetup masterRepositorySetup = new MasterRepositorySetupImpl();
        AuthenticationConfiguration authConf = AuthenticationConfiguration
                .createStandaloneAuthenticationConfiguration(regressionConfiguration.getOdiSupervisorPassword()
                        .toCharArray());
        try {
            masterRepositorySetup.createMasterRepository(regressionConfiguration.getMasterRepositoryJdbcUrl(),
                    regressionConfiguration.getMasterRepositoryJdbcDriver(), regressionConfiguration.getMasterRepositoryJdbcUser(),
                    regressionConfiguration.getMasterRepositoryJdbcPassword(), masterRepositoryId,
                    RegressionTestUtilities.getTechnologyNameFromString(regressionConfiguration.getMasterRepositoryTechnology()), true, authConf, null, null);
        } catch (Exception e) {
            String message = "ODI_LIB_PATH not found, please run from gradlew.";
            logger.fatal(message);
            throw new RuntimeException(message, e);
        }
        logger.info("Master Repository Creation Completed.");
        MasterRepositoryDbInfo masterInfo = new MasterRepositoryDbInfo(
                regressionConfiguration.getMasterRepositoryJdbcUrl(), regressionConfiguration.getMasterRepositoryJdbcDriver(),
                regressionConfiguration.getMasterRepositoryJdbcUser(),
                regressionConfiguration.getMasterRepositoryJdbcPassword().toCharArray(),
                new PoolingAttributes());
        OdiInstance odiInstance = OdiInstance
                .createInstance(new OdiInstanceConfig(masterInfo, null));
        // odiSupervisorUser = "SUPERVISOR";
        logger.info("Logging in with: " + regressionConfiguration.getOdiSupervisorUser());
        Authentication auth = odiInstance.getSecurityManager()
                .createAuthentication(regressionConfiguration.getOdiSupervisorUser(),
                        regressionConfiguration.getOdiSupervisorPassword().toCharArray());
        odiInstance.getSecurityManager().setCurrentThreadAuthentication(auth);
        logger.info("Work Repository Creation Started.");
        // AuthenticationConfiguration authConf2 = AuthenticationConfiguration
        // .createStandaloneAuthenticationConfiguration(odiSupervisorPassword
        // .toCharArray());
        IWorkRepositorySetup workRepositorySetup = new WorkRepositorySetupImpl(
                odiInstance);
        workRepositorySetup.createWorkDevRepository(regressionConfiguration.getWorkRepositoryJdbcUrl(),
                regressionConfiguration.getWorkRepositoryJdbcDriver(), regressionConfiguration.getWorkRepositoryJdbcUsername(),
                regressionConfiguration.getWorkRepositoryJdbcPassword(), regressionConfiguration.getWorkRepositoryId(),
                workRepositoryName, RegressionTestUtilities.getTechnologyNameFromString(regressionConfiguration.getMasterRepositoryTechnology()), true);
        logger.info("Work Repository Creation Completed.");
        logger.info("======================================================");
        logger.info("Repository Creation Completed Successfully");
        logger.info("======================================================");
        createMasterOdiInstance(
                regressionConfiguration.getMasterRepositoryJdbcUrl(), regressionConfiguration.getMasterRepositoryJdbcDriver(),
                regressionConfiguration.getMasterRepositoryJdbcUser(), regressionConfiguration.getMasterRepositoryJdbcPassword(),
                regressionConfiguration.getOdiSupervisorUser(), regressionConfiguration.getOdiSupervisorPassword());
        createWorkOdiInstance(regressionConfiguration.getMasterRepositoryJdbcUrl(),
                regressionConfiguration.getMasterRepositoryJdbcDriver(), regressionConfiguration.getMasterRepositoryJdbcUser(),
                regressionConfiguration.getMasterRepositoryJdbcPassword(), regressionConfiguration.getOdiSupervisorUser(),
                regressionConfiguration.getOdiSupervisorPassword(), workRepositoryName);
        odiInstance.close();
        odiInstance = null;
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean smartImport(File xmlFile) throws MalformedURLException, ClassNotFoundException {
        Properties p = new Properties();
        String key = "javax.xml.parsers.SAXParserFactory";
        String value = "oracle.xml.jaxp.JXSAXParserFactory";
        p.setProperty(key, value);

        ITransactionManager tm1;
        tm1 = getWorkOdiInstance().getOdiInstance().getTransactionManager();
        ISmartImportService importService = new SmartImportServiceImpl(getWorkOdiInstance().getOdiInstance());
        boolean result = false;
        if (xmlFile.exists()) {
            try {
                logger.info("Smart import of: " + xmlFile.getAbsolutePath());
                importService.importFromXml(xmlFile.getAbsolutePath());
                result = true;
            } catch (OdiImportException e) {
                logger.fatal(e.getMessage());
                e.printStackTrace();
                result = false;
            } catch (IOException e) {
                logger.fatal(e.getMessage());
                e.printStackTrace();
                result = false;
            }
        } else {
            logger.info("File: " + xmlFile.getAbsolutePath() + " does not exist.");
        }
        tm1.commit(getWorkOdiInstance().getTransactionStatus());
        return result;
    }

    private void createMasterOdiInstance(String odiRepoUrl,
                                         String odiRepoDbDriver, String pJdbcUsername, String pJdbcPassword,
                                         String pOdiUser, String pOdiPassword) {
        if (masterOdiInstance == null || masterOdiInstance.isClosed()) {
            MasterRepositoryDbInfo masterInfo = new MasterRepositoryDbInfo(
                    odiRepoUrl, odiRepoDbDriver, pJdbcUsername,
                    pJdbcPassword.toCharArray(), new PoolingAttributes());
            masterOdiInstance = OdiInstance
                    .createInstance(new OdiInstanceConfig(masterInfo, null));
            logger.debug("created ODI instance " + masterOdiInstance);
            Authentication auth = masterOdiInstance.getSecurityManager()
                    .createAuthentication(pOdiUser, pOdiPassword.toCharArray());
            masterOdiInstance.getSecurityManager().setCurrentThreadAuthentication(auth);
        }
    }

    private void createWorkOdiInstance(String odiRepoUrl,
                                       String odiRepoDbDriver, String pJdbcUsername, String pJdbcPassword,
                                       String pOdiUser, String pOdiPassword, String odiWorkRepo) {
        int positionOf_ = odiWorkRepo.indexOf("_");
        if (positionOf_ != -1)
            odiWorkRepo = odiWorkRepo.substring(0, positionOf_);
        if (workOdiConnection.getOdiInstance() == null || workOdiConnection.getOdiInstance().isClosed()) {
            workOdiConnection = OdiConnectionFactory
                    .getOdiConnection(odiRepoUrl, pJdbcUsername, pJdbcPassword, pOdiUser, pOdiPassword, odiRepoDbDriver, odiWorkRepo);
        }
    }


    @Override
    public boolean createEnvironment() {

        int masterRepositoryId;
        logger.info("--->" + regressionConfiguration.getWorkRepositoryJdbcUsername());
        String workRepositoryName = RegressionTestUtilities.getWorkReposName(regressionConfiguration.getWorkRepositoryJdbcUsername());
        masterRepositoryId = RegressionTestUtilities.getWorkReposId(regressionConfiguration.getWorkRepositoryJdbcUsername());
        logger.info("Workrepid:" + workRepositoryName + " Id:"
                + masterRepositoryId);

        Assert.assertTrue(sqlHelper.deleteUser(RegressionTestUtilities.getTechnologyNameFromString(regressionConfiguration.getMasterRepositoryTechnology()), regressionConfiguration.getSysdbaUser(),
                regressionConfiguration.getSysdbaPassword(), regressionConfiguration.getMasterRepositoryJdbcUrl(), regressionConfiguration.getMasterRepositoryJdbcUser()));
        Assert.assertTrue(sqlHelper.deleteUser(RegressionTestUtilities.getTechnologyNameFromString(regressionConfiguration.getMasterRepositoryTechnology()), regressionConfiguration.getSysdbaUser(),
                regressionConfiguration.getSysdbaPassword(), regressionConfiguration.getMasterRepositoryJdbcUrl(), regressionConfiguration.getWorkRepositoryJdbcUsername()));
        Assert.assertTrue(sqlHelper.masterRepostitoryUserIsCreated(
                RegressionTestUtilities.getTechnologyNameFromString(regressionConfiguration.getMasterRepositoryTechnology()), regressionConfiguration.getSysdbaUser(), regressionConfiguration.getSysdbaPassword(),
                regressionConfiguration.getMasterRepositoryJdbcUrl(), regressionConfiguration.getMasterRepositoryJdbcUser(), regressionConfiguration.getMasterRepositoryJdbcPassword()));
        logger.info("Master Repository user is created.");
        Assert.assertTrue(sqlHelper.workRepostitoryUserIsCreated(
                RegressionTestUtilities.getTechnologyNameFromString(regressionConfiguration.getMasterRepositoryTechnology()), regressionConfiguration.getSysdbaUser(), regressionConfiguration.getSysdbaPassword(),
                regressionConfiguration.getMasterRepositoryJdbcUrl(), regressionConfiguration.getWorkRepositoryJdbcUsername(),
                regressionConfiguration.getWorkRepositoryJdbcPassword()));
        logger.info("Work Repository user is created.");

        //
        try {
            Assert.assertTrue(repositoryIsSetup());
        } catch (RepositorySetupException e) {
            if (System.getProperty("ODI_ORACLE_HOME") == null) {
                String path = System.getProperty("ODI_ORACLE_HOME") + ".."
                        + System.getProperty("file.separator")
                        + "oracle.odi.common"
                        + System.getProperty("file.separator")
                        + "oracledi.common"
                        + System.getProperty("file.separator") + "odi"
                        + System.getProperty("file.separator") + "scripts";
                logger.fatal("Define a vm Argument ODI_ORACLE_HOME (-DODI_ORACLE_HOME=value.).");
                logger.fatal("The path:" + path + " should exist.");
                logger.fatal(e.getMessage());
                e.printStackTrace();
                Assert.assertTrue(false);
            } else if (e.getMessage().contains("UNIQUE")) {
                logger.warn("Repository exists.");
                Assert.assertTrue(true);
            } else {
                e.printStackTrace();
                logger.fatal(e.getMessage());
                Assert.assertTrue(false);
            }
        }
        createMasterOdiInstance(
                regressionConfiguration.getMasterRepositoryJdbcUrl(), regressionConfiguration.getMasterRepositoryJdbcDriver(),
                regressionConfiguration.getMasterRepositoryJdbcUser(), regressionConfiguration.getMasterRepositoryJdbcPassword(),
                regressionConfiguration.getOdiSupervisorUser(), regressionConfiguration.getOdiSupervisorPassword());
        createWorkOdiInstance(regressionConfiguration.getMasterRepositoryJdbcUrl(),
                regressionConfiguration.getMasterRepositoryJdbcDriver(), regressionConfiguration.getMasterRepositoryJdbcUser(),
                regressionConfiguration.getMasterRepositoryJdbcPassword(), regressionConfiguration.getOdiSupervisorUser(),
                regressionConfiguration.getOdiSupervisorPassword(), workRepositoryName);
        logger.info("Master and work repository are created");

        boolean result = false;
        try {
            result = smartImport(smartExport);
        } catch (Exception e) {
            logger.fatal(e);
        }
        Assert.assertTrue(result);
        return true;
    }


    @Override
    public void startAgent(String odiAgentName) {

    }

    @Override
    public void stopAgent(String odiAgentName) {

    }

    private OdiInstance getMasterOdiInstance() {
        return masterOdiInstance;
    }

    public OdiConnection getWorkOdiInstance() {
        if (workOdiConnection == null || workOdiConnection.getOdiInstance() == null || workOdiConnection.getOdiInstance().isClosed()) {
            String odiMasterRepoUrl = regressionConfiguration.getMasterRepositoryJdbcUrl();
            String odiMasterUser = regressionConfiguration.getMasterRepositoryJdbcUser();
            String odiMasterRepoPassword = regressionConfiguration.getMasterRepositoryJdbcPassword();
            String odiLoginUsername = regressionConfiguration.getOdiSupervisorUser();
            String odiLoginPassword = regressionConfiguration.getOdiSupervisorPassword();
            String odiRepoDbDriver = regressionConfiguration.getMasterRepositoryJdbcDriver();
            String odiWorkRepo = regressionConfiguration.getOdiWorkRepositoryName();
            workOdiConnection = OdiConnectionFactory
                    .getOdiConnection(
                            odiMasterRepoUrl, odiMasterUser, odiMasterRepoPassword,
                            odiLoginUsername, odiLoginPassword, odiRepoDbDriver,
                            odiWorkRepo);
        }
        return workOdiConnection;
    }


    public String runController(final String action, final String configFile,
                                final String... additionalConfig) {

        assert regressionConfiguration.getOdiSupervisorPassword() != null;
        assert regressionConfiguration.getMasterRepositoryJdbcPassword() != null;

        List<String> argList = new ArrayList<String>();

        argList.add("-a");
        argList.add(action);
        argList.add("-c");
        argList.add(configFile);

        // add default passwords in here - may need to be externalized
        argList.add("-pw");
        argList.add(regressionConfiguration.getOdiSupervisorPassword());
        argList.add("-mpw");
        argList.add(regressionConfiguration.getMasterRepositoryJdbcPassword());
        argList.add("-devmode");
        for (String item : additionalConfig) {
            argList.add(item);
        }

        controller.run(argList.toArray(new String[0]));
        return controller.getErrorReport();
    }

    public String runControllerExcpectError(final String action, final String configFile,
                                            final String... additionalConfig) {
        List<String> argList = new ArrayList<String>();
        JodiController controllerError = new JodiController();
        argList.add("-a");
        argList.add(action);
        argList.add("-c");
        argList.add(configFile);

        // add default passwords in here - may need to be externalized
        argList.add("-pw");
        argList.add(regressionConfiguration.getOdiSupervisorPassword());
        argList.add("-mpw");
        argList.add(regressionConfiguration.getMasterRepositoryJdbcPassword());
        argList.add("-devmode");
        for (String item : additionalConfig) {
            argList.add(item);
        }

        controllerError.run(argList.toArray(new String[0]));
        return controllerError.getErrorReport();
    }

    @Override
    public void close() {
        if (getMasterOdiInstance() != null && !getMasterOdiInstance().isClosed())
            getMasterOdiInstance().close();
        if (getWorkOdiInstance() != null && getWorkOdiInstance().getOdiInstance() != null && !getWorkOdiInstance().getOdiInstance().isClosed())
            getWorkOdiInstance().getOdiInstance().close();
    }


    @Override
    public ErrorWarningMessageJodi getErrorWarningMessages() {
        return errorWarningMessages;
    }


    public RegressionConfiguration getRegressionConfiguration() {
        return regressionConfiguration;
    }

    public String getPropertiesFile() {
        return propertiesFile;
    }


    public JodiController getController() {
        return controller;
    }

}
