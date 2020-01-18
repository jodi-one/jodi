package one.jodi.odi.runtime;

import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiDataServer;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.OdiPhysicalSchema;
import oracle.odi.domain.topology.finder.IOdiDataServerFinder;
import oracle.odi.domain.util.ObfuscatedString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OdiUpdateDataserver {
    private final static Logger logger = LogManager.getLogger(OdiUpdateDataserver.class);
    private OdiDataServer oracleDataServer;
    private OdiPhysicalSchema oraclePhysicalSchema;
    private OdiLogicalSchema oracleLogicalSchema;
    private OdiContext context;
    private String BI_PROJECT_CODE;

    public OdiUpdateDataserver() {
        super();
    }

    public static void main(String[] args) {
        OdiUpdateDataserver odiUpdateDataServer = new OdiUpdateDataserver();
        if (args.length != 13) {
            logger.info(odiUpdateDataServer.getUsage().toString());
            return;
        }
        String odiMasterRepoUrl = args[0].trim();
        String odiMasterRepoUser = args[1].trim();
        String odiMasterRepoPassword = args[2].trim();
        String odiWorkRepo = args[3].trim();
        String odiLoginUsername = args[4].trim();
        String odiLoginPassword = args[5].trim();
        String pDataServerName = args[6].trim();
        String pJdbcString = args[7].trim();
        String pUsername = args[8].trim();
        String pPassword = args[9].trim();
        String pServerInstanceName = args[10].trim();
        String pJdbcDriverNameRepository = args[11].trim();
        String pJdbcDriverNameDataServer = args[12].trim();
        odiUpdateDataServer.updateDataServer(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo,
                odiLoginUsername, odiLoginPassword, pDataServerName, pJdbcString, pUsername, pPassword,
                pServerInstanceName, pJdbcDriverNameRepository, pJdbcDriverNameDataServer);
    }

    public void updateDataServer(String odiMasterRepoUrl, String odiMasterRepoUser, String odiMasterRepoPassword,
                                 String odiWorkRepo, String odiLoginUsername, String odiLoginPassword, String dataServerName,
                                 String jdbcString, String username, String password, String serverInstanceName,
                                 String jdbcDriverRepository, String jdbcDriverServer) {
        // Create a Data Server , Physical Schema, Logical Schema
        OdiConnection odiConnection = OdiConnectionFactory.getOdiConnection(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword,
                odiLoginUsername, odiLoginPassword, jdbcDriverRepository, odiWorkRepo);
        OdiDataServer oracleDataServer = ((IOdiDataServerFinder) odiConnection.getOdiInstance()
                .getTransactionalEntityManager().getFinder(OdiDataServer.class)).findByName(dataServerName);
        oracleDataServer.setConnectionSettings(new OdiDataServer.JdbcSettings(jdbcString, jdbcDriverServer));
        oracleDataServer.setUsername(username);
        oracleDataServer.setPassword(ObfuscatedString.obfuscate(password.toCharArray(), odiConnection.getOdiInstance()));
        oracleDataServer.setServerInstanceName(serverInstanceName);
        odiConnection.getOdiInstance().getTransactionalEntityManager().persist(oracleDataServer);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
        odiConnection.getOdiInstance().close();
    }

    public StringBuffer getUsage() {
        StringBuffer usage = new StringBuffer("Usage:\n");
        usage.append(
                "one.jodi.odi.OdiUpdateDataserver odiMasterRepoUrl odiMasterRepoUser odiMasterRepoPassword odiWorkRepo odiLoginUsername odiLoginPassword pDataServerName pJdbcString pUsername pPassword pServerInstanceName jdbcDriverRepository  jdbcDriverServer\n");
        usage.append("java -Dlog4j.configuration=./conf/log4j.properties -classpath ./lib/*:$ODI_LIB_PATH   one.jodi.odi.runtime.OdiUpdateDataserver jdbc:oracle:thin:@jodi:1521/samplec DEV_ODI_REPO ODI_MASTER_REPO_PASSWORD WORKREP SUPERVISOR ODI_USER_PASSWORD ORACLE_DWH_CON_CHINOOK_DEV jdbc:oracle:thin:@jodi:1521/SAMPLEC NEW_DB_USER PASSWORD_IS_IGRNORED_IN12C NEW_TNSNAME oracle.jdbc.driver.OracleDriver oracle.jdbc.driver.OracleDriver\n");

        usage.append("\n");
        return usage;
    }

    public OdiDataServer getOracleDataServer() {
        return oracleDataServer;
    }

    public OdiPhysicalSchema getOraclePhysicalSchema() {
        return oraclePhysicalSchema;
    }

    public OdiLogicalSchema getOracleLogicalSchema() {
        return oracleLogicalSchema;
    }

    public OdiContext getContext() {
        return context;
    }

    public String getBI_PROJECT_CODE() {
        return BI_PROJECT_CODE;
    }

}
