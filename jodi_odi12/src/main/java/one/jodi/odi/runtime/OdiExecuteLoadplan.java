package one.jodi.odi.runtime;

import oracle.odi.runtime.agent.invocation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OdiExecuteLoadplan  {
    private final static Logger logger = LogManager.getLogger(OdiExecuteLoadplan.class);

    public OdiExecuteLoadplan() {
        super();
    }

    public static void main(String[] args) {
        if (args.length == 7) {
            OdiExecuteLoadplan OdiExecuteLoadplan = new OdiExecuteLoadplan();
            OdiExecuteLoadplan.startLoadplan(args[0], args[1], args[2], args[3], args[4], args[5]);
        } else {
            logger.info(OdiExecuteLoadplan.getUsage());
        }
    }

    public static StringBuffer getUsage() {
        StringBuffer Usage = new StringBuffer("Usage:\n");
        Usage.append(
                "one.jodi.odi.runtime.OdiExecuteLoadplan pAgentUrl pUser pPassword pContextCode pWorkRepName pLoadplanName\n");
        Usage.append("java -Dlog4j.configuration=./conf/log4j.properties -classpath ./lib/*:$ODI_LIB_PATH one.jodi.odi.runtime.OdiExecute http://jodi:20910/oraclediagent SUPERVISOR ODI_USER_PASSWORD GLOBAL 5 WORKREP DROP_TEMP_INTERFACE_DATASTORES");
        Usage.append("\n");
        return Usage;
    }

    public void startLoadplan(String agentUrl, String user, String password, String contextCode,
                              String workRepName, String loadplanName) {
        if (agentUrl.startsWith("null"))
            throw new RuntimeException("Please set defaultAgent system properties.");
        String pKeywords = loadplanName;
        boolean pSynchronous = true;
        try {
            RemoteRuntimeAgentInvoker rraInvoker = new RemoteRuntimeAgentInvoker(agentUrl, user, password.toCharArray());
            LoadPlanExecutionInfo statusLP = rraInvoker.invokeStartLoadPlan(loadplanName, contextCode, null, pKeywords, workRepName);
            logger.info("Started: "+ loadplanName + " with: "+ statusLP.getLoadPlanInstanceId() +" with runcount: "+ statusLP.getRunCount());
        } catch (InvocationException e) {
            logger.fatal(e);
            throw new RuntimeException(String.format("Loadplan '%1$s'  not executed.", loadplanName), e);
        }
    }
}
