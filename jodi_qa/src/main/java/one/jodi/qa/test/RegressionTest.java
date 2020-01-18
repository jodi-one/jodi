package one.jodi.qa.test;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.db.SQLHelper;
import oracle.odi.setup.RepositorySetupException;

public interface RegressionTest {
    public boolean repositoryIsSetup() throws RepositorySetupException;

    // Use isInstall to check whether installation is required,
    // is so call createEnvironment.
    public boolean createEnvironment();

    public void startAgent(String odiAgentName);

    //
    public void stopAgent(String odiAgentName);

    public void test010Install();

    public String runController(String action,
                                String configFile, String... additionalConfig);

    public void close();

    SQLHelper getSqlHelper();


    ErrorWarningMessageJodi getErrorWarningMessages();
}
