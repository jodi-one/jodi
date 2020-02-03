package one.jodi.odi.runtime;

import oracle.odi.runtime.agent.invocation.ExecutionInfo;
import oracle.odi.runtime.agent.invocation.ExecutionInfo.SessionStatus;
import oracle.odi.runtime.agent.invocation.InvocationException;
import oracle.odi.runtime.agent.invocation.RemoteRuntimeAgentInvoker;
import oracle.odi.runtime.agent.invocation.StartupParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OdiExecuteScenario {
    private final static Logger logger = LogManager.getLogger(OdiExecuteScenario.class);

    public OdiExecuteScenario() {
        super();
    }

    public static void main(String[] args) {
        if (args.length == 7) {
            OdiExecuteScenario odiExecuteScenario = new OdiExecuteScenario();
            odiExecuteScenario.startScenario(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]), args[5], args[6]);
        } else {
            logger.info(OdiExecuteScenario.getUsage());
        }
    }

    public static StringBuffer getUsage() {
        StringBuffer Usage = new StringBuffer("Usage:\n");
        Usage.append(
                "one.jodi.odi.runtime.OdiExecuteScenario pAgentUrl pUser pPassword pContextCode pLogLevel pWorkRepName pScenarioName\n");
        Usage.append("java -Dlog4j.configuration=./conf/log4j.properties -classpath ./lib/*:$ODI_LIB_PATH one.jodi.odi.runtime.OdiExecute http://jodi:20910/oraclediagent SUPERVISOR ODI_USER_PASSWORD GLOBAL 5 WORKREP DROP_TEMP_INTERFACE_DATASTORES");
        Usage.append("\n");
        return Usage;
    }

    public void startScenario(String agentUrl, String user, String password, String contextCode, int logLevel,
                              String workRepName, String scenarioName) {
        if (agentUrl.startsWith("null"))
            throw new RuntimeException("Please set defaultAgent system properties.");
        RemoteRuntimeAgentInvoker rraInvoker = new RemoteRuntimeAgentInvoker(agentUrl, user, password.toCharArray());
        String pScenVersion = "001";
        StartupParams pVariables = new StartupParams();
        String pKeywords = scenarioName;
        String pSessionName = scenarioName;
        boolean pSynchronous = true;
        ExecutionInfo status = null;
        try {
            status = rraInvoker.invokeStartScenario(scenarioName.toUpperCase(), pScenVersion, pVariables, pKeywords,
                    contextCode, logLevel, pSessionName, pSynchronous, workRepName);
            logger.info("return code for " + scenarioName.toUpperCase() + " is: " + status.getReturnCode());
            if (SessionStatus.ERROR == status.getSessionStatus()) {
                throw new RuntimeException(String.format("Scenario '%1$s'  not executed, status message '%2$s'",
                        scenarioName, status.getStatusMessage()));
            }
            logger.info("Status: " + status.getStatusMessage());
        } catch (InvocationException e) {
            logger.fatal(e);
            throw new RuntimeException(String.format("Scenario '%1$s'  not executed.", scenarioName), e);
        }
    }


    public void startScenario(String agentUrl, String user, String password, String contextCode, String logLevel,
                              String workRepName, String scenarioName) {
        // backwards compatibility
        this.startScenario(agentUrl, user, password, contextCode, Integer.parseInt(logLevel),
                workRepName, scenarioName);
    }
}
