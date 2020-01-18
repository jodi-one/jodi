package one.jodi.odi.runtime;

import oracle.odi.domain.topology.OdiPhysicalAgent;
import oracle.odi.domain.topology.finder.IOdiPhysicalAgentFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OdiUpdateAgent {
    private final static Logger logger = LogManager.getLogger(OdiUpdateAgent.class);

    public OdiUpdateAgent() {
        super();
    }

    public static void main(String[] args) {
        OdiUpdateAgent odiUpdateAgent = new OdiUpdateAgent();
        if (args.length != 9) {
            logger.info(odiUpdateAgent.getUsage().toString());
            return;
        }
        String odiMasterRepoUrl = args[0].trim();
        String odiMasterRepoUser = args[1].trim();
        String odiMasterRepoPassword = args[2].trim();
        String odiWorkRepo = args[3].trim();
        String odiLoginUsername = args[4].trim();
        String odiLoginPassword = args[5].trim();
        String agentUrl = args[6].trim();
        String pJdbcDriver = args[7].trim();
        String agentName = args[8].trim();
        odiUpdateAgent.updateAgent(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo,
                odiLoginUsername, odiLoginPassword, agentUrl, pJdbcDriver, agentName);
    }

    public void updateAgent(String odiMasterRepoUrl, String odiMasterRepoUser, String odiMasterRepoPassword,
                            String odiWorkRepo, String odiLoginUsername, String odiLoginPassword, String agentUrl,
                            String jdbcDriver, String agentName) {
        // Create a Data Server , Physical Schema, Logical Schema
        OdiConnection odiConnection = OdiConnectionFactory.getOdiConnection(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword,
                odiLoginUsername, odiLoginPassword, jdbcDriver, odiWorkRepo);
        OdiPhysicalAgent oraclePhysicalAgent = ((IOdiPhysicalAgentFinder) odiConnection.getOdiInstance()
                .getTransactionalEntityManager().getFinder(OdiPhysicalAgent.class)).findByName(agentName);
        oraclePhysicalAgent.setHostName(getHostName(agentUrl));
        oraclePhysicalAgent.setHostPort(Integer.parseInt(getHostPort(agentUrl)));
        oraclePhysicalAgent.setApplicationName(getApplicationName(agentUrl));
        odiConnection.getOdiInstance().getTransactionalEntityManager().persist(oraclePhysicalAgent);
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
                "one.jodi.odi.runtime.OdiUpdateAgent odiMasterRepoUrl odiMasterRepoUser odiMasterRepoPassword odiWorkRepo odiLoginUsername odiLoginPassword agentUrl, pJdbcDriver, agentName\n");
        usage.append("java -Dlog4j.configuration=./conf/log4j.properties -classpath ./lib/*:$ODI_LIB_PATH   one.jodi.odi.runtime.OdiUpdateAgent jdbc:oracle:thin:@jodi:1521/samplec DEV_ODI_REPO ODI_MASTER_DB_PASSWORD WORKREP SUPERVISOR ODI_USER_PASSWORD \"http://newlocation:9090/oraclediagent\" oracle.jdbc.driver.OracleDriver OracleDIAgent1");
        usage.append("\n");
        return usage;
    }
}
