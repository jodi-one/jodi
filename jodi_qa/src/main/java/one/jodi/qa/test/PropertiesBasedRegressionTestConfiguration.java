package one.jodi.qa.test;

//import static org.mockito.Mockito.mock;

import one.jodi.base.config.PasswordConfigImpl;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Assert;

public class PropertiesBasedRegressionTestConfiguration implements RegressionConfiguration {
    private final String sysdbaUser;
    private final String sysdbaPassword;
    private final String jdbcUrlMasterRepository;
    private final String masterRepositoryJdbcUser;
    private final String masterRepositoryJdbcPassword;
    private final String workRepositoryJdbcUsername;
    private final String workRepositoryJdbcPassword;
    private final String odiSupervisorUser;
    private final String odiSupervisorPassword;
    private final String masterRepositoryJdbcUrl;
    private final String masterRepositoryJdbcDriver;
    private final String workRepositoryJdbcUrl;
    private final String workRepositoryJdbcDriver;
    private final int workRepositoryId;
    private final String masterRepositoryTechnology;
    private final Configuration config;
    private final String odiContext;
    private final String odiWorkRepositoryName;
    private final boolean install;
    private final String smartExport;
    private final String projectCode;

    public PropertiesBasedRegressionTestConfiguration(final String propertiesFile,
                                                      final String password, final String masterPassword) {
        if (propertiesFile == null || propertiesFile.startsWith("null"))
            throw new RuntimeException("Please set TEST_PROPERTIES_BASE_DIRECTORY system property.");

        //	this.config = new PropertiesConfiguration(propertiesFile).interpolatedConfiguration();
        Parameters params = new Parameters();//mock(Parameters.class);
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased()
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                                .setFileName(propertiesFile));
        builder.setAutoSave(true);
        PropertiesConfiguration pConfig = null;
        try {
            pConfig = builder.getConfiguration();
        } catch (ConfigurationException e) {
// update!
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.config = pConfig.interpolatedConfiguration();

        this.sysdbaUser = config.getString("rt.db.sysdbaUser");
        this.sysdbaPassword = new PasswordConfigImpl().getOracleTestDBPassword();
        this.jdbcUrlMasterRepository = config.getString("odi.master.repo.url");
        // Naming convention: M<3 digits>_ODI_REPO
        this.masterRepositoryJdbcUser = config.getString("odi.master.repo.username");
        this.masterRepositoryJdbcPassword = masterPassword;
        // Naming convention: #<3 digits>_ODI_REPO
        this.workRepositoryJdbcUsername = config.getString("odi.work.repo");
        this.workRepositoryJdbcPassword = masterPassword;
        this.odiSupervisorUser = config.getString("odi.login.username");
        this.odiSupervisorPassword = password;
        this.masterRepositoryJdbcUrl = config.getString("odi.master.repo.url");
        this.masterRepositoryJdbcDriver = config.getString("odi.repo.db.driver");
        this.workRepositoryJdbcUrl = config.getString("odi.master.repo.url");
        this.workRepositoryJdbcDriver = config.getString("odi.repo.db.driver");
        this.workRepositoryId = Integer.parseInt(config.getString("odi.repository.index"));
        this.masterRepositoryTechnology = config.getString("odi.repo.db.technology");
        this.install = Boolean.valueOf(config.getString("rt.install"));
        this.odiContext = config.getString("odi.context");
        this.odiWorkRepositoryName = config.getString("odi.work.repo");
        this.smartExport = config.getString("rt.file.smartexport");
        this.projectCode = config.getString("odi.project.code");
        Assert.assertNotNull(this.sysdbaUser);
        Assert.assertNotNull(this.sysdbaPassword);
        Assert.assertNotNull(this.jdbcUrlMasterRepository);
        Assert.assertNotNull(this.masterRepositoryJdbcUser);
        Assert.assertNotNull(this.masterRepositoryJdbcPassword);
        Assert.assertNotNull(this.workRepositoryJdbcUsername);
        Assert.assertNotNull(this.workRepositoryJdbcPassword);
        Assert.assertNotNull(this.odiSupervisorUser);
        Assert.assertNotNull(this.odiSupervisorPassword);
        Assert.assertNotNull(this.masterRepositoryJdbcUrl);
        Assert.assertNotNull(this.masterRepositoryJdbcDriver);
        Assert.assertNotNull(this.odiContext);
        Assert.assertNotNull(this.odiWorkRepositoryName);
        Assert.assertNotNull(this.masterRepositoryTechnology);
        Assert.assertNotNull(this.smartExport);
        Assert.assertNotNull(this.projectCode);
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getSysdbaUser()
     */
    @Override
    public String getSysdbaUser() {
        return sysdbaUser;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getSysdbaPassword()
     */
    @Override
    public String getSysdbaPassword() {
        return sysdbaPassword;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getJdbcUrlMasterRepository()
     */
    @Override
    public String getJdbcUrlMasterRepository() {
        return jdbcUrlMasterRepository;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getMasterRepositoryJdbcUser()
     */
    @Override
    public String getMasterRepositoryJdbcUser() {
        return masterRepositoryJdbcUser;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getMasterRepositoryJdbcPassword()
     */
    @Override
    public String getMasterRepositoryJdbcPassword() {
        return masterRepositoryJdbcPassword;
    }

    public String getWorkRepositoryJdbcUsername() {
        return workRepositoryJdbcUsername;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getWorkRepositoryJdbcPassword()
     */
    @Override
    public String getWorkRepositoryJdbcPassword() {
        return workRepositoryJdbcPassword;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getOdiSupervisorUser()
     */
    @Override
    public String getOdiSupervisorUser() {
        return odiSupervisorUser;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getOdiSupervisorPassword()
     */
    @Override
    public String getOdiSupervisorPassword() {
        return odiSupervisorPassword;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getMasterRepositoryJdbcUrl()
     */
    @Override
    public String getMasterRepositoryJdbcUrl() {
        return masterRepositoryJdbcUrl;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getMasterRepositoryJdbcDriver()
     */
    @Override
    public String getMasterRepositoryJdbcDriver() {
        return masterRepositoryJdbcDriver;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getWorkRepositoryJdbcUrl()
     */
    @Override
    public String getWorkRepositoryJdbcUrl() {
        return workRepositoryJdbcUrl;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getWorkRepositoryJdbcDriver()
     */
    @Override
    public String getWorkRepositoryJdbcDriver() {
        return workRepositoryJdbcDriver;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getWorkRepositoryId()
     */
    @Override
    public int getWorkRepositoryId() {
        return workRepositoryId;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getMasterRepositoryTechnology()
     */
    @Override
    public String getMasterRepositoryTechnology() {
        return masterRepositoryTechnology;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getConfig()
     */
    @Override
    public Configuration getConfig() {
        return config;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getOdiContext()
     */
    @Override
    public String getOdiContext() {
        return odiContext;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#getOdiWorkRepositoryName()
     */
    @Override
    public String getOdiWorkRepositoryName() {
        return odiWorkRepositoryName;
    }

    /* (non-Javadoc)
     * @see one.jodi.qa.test.RegressionConfiguration#isInstall()
     */
    @Override
    public boolean isInstall() {
        return install;
    }

    @Override
    public String getSmartExport() {
        return smartExport;
    }

    @Override
    public String getProjectCode() {
        return projectCode;
    }
}