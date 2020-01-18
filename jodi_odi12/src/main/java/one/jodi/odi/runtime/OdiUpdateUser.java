package one.jodi.odi.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OdiUpdateUser {

    private static final Logger logger = LogManager.getLogger(OdiUpdateUser.class);

    public OdiUpdateUser() {
        super();
    }

    public static void main(String[] args) {
        OdiUpdateUser odiUpdateUser = new OdiUpdateUser();
        if (args.length != 8) {
            logger.info(odiUpdateUser.getUsage().toString());
            return;
        }
        String odiMasterRepoUrl = args[0].trim();
        String odiMasterRepoUser = args[1].trim();
        String odiMasterRepoPassword = args[2].trim();
        String odiWorkRepo = args[3].trim();
        String odiLoginUsername = args[4].trim();
        String odiLoginPassword = args[5].trim();
        String pPassword = args[6].trim();
        String pJdbcDriver = args[7].trim();
        odiUpdateUser.updateUser(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo,
                odiLoginUsername, odiLoginPassword, pPassword, pJdbcDriver);
    }

    public void updateUser(String odiMasterRepoUrl, String odiMasterRepoUser, String odiMasterRepoPassword,
                           String odiWorkRepo, String odiLoginUsername, String odiLoginPassword, String password,
                           String jdbcDriverMasterRepo) {
        // Create a Data Server , Physical Schema, Logical Schema
        OdiConnection odiConnection = OdiConnectionFactory.getOdiConnection(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword,
                odiLoginUsername, odiLoginPassword, jdbcDriverMasterRepo, odiWorkRepo);
        odiConnection.getOdiInstance().getSecurityManager().setAuthenticatedUserPassword(password.toCharArray());
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
        odiConnection.getOdiInstance().close();
    }

    public String getHostName(String pAgentName) {
        String hostname = "";
        hostname = pAgentName.substring(pAgentName.indexOf("/") + 2, pAgentName.lastIndexOf(":"));
        return hostname;
    }

    public String getHostPort(String pAgentName) {
        String portname = "";
        portname = pAgentName.substring(pAgentName.lastIndexOf(":") + 1, pAgentName.lastIndexOf("/"));
        return portname;
    }

    public String getApplicationName(String pAgentName) {
        String applicationName = "";
        applicationName = pAgentName.substring(pAgentName.lastIndexOf("/") + 1, pAgentName.length());
        return applicationName;
    }

    public StringBuffer getUsage() {
        StringBuffer usage = new StringBuffer("Usage:\n");
        usage.append(
                "one.jodi.odi.runtime.OdiUpdateUser odiMasterRepoUrl odiMasterRepoUser odiMasterRepoPassword odiWorkRepo odiLoginUsername odiLoginPassword pNewPassword pJDBCDriver\n");
        usage.append("java -Dlog4j.configuration=./conf/log4j.properties -classpath ./lib/*:$ODI_LIB_PATH  one.jodi.odi.runtime.OdiUpdateUser jdbc:oracle:thin:@jodi:1521/samplec DEV_ODI_REPO DB_PASS WORKREP SUPERVISOR ODI_USER_PASSWORD NEW_PASS oracle.jdbc.driver.OracleDriver\n");
        usage.append("\n");
        return usage;
    }
}
