package one.jodi.qa.test;

import one.jodi.odi.runtime.OdiExecuteScenario;
import oracle.odi.setup.TechnologyName;
import oracle.odi.setup.support.TechnologyNameUtils;

public class RegressionTestUtilities {

    public static TechnologyName getTechnologyNameFromString(String technologyName) {
        technologyName = technologyName.toUpperCase();
        return TechnologyNameUtils.getTechnologyName(technologyName);

    }

    public static String getWorkReposName(String workRepositoryJdbcUsername2) {
        String name = "";
        if (workRepositoryJdbcUsername2.contains("_"))
            name = workRepositoryJdbcUsername2.substring(0, workRepositoryJdbcUsername2.indexOf("_"));
        else
            name = workRepositoryJdbcUsername2;
        return name;
    }

    public static int getWorkReposId(String name) {
        int workRepositoryId = -1;
        String clean_name = name.replace("W", "").replace("_ODI_REPO", "");
        workRepositoryId = Integer.parseInt(clean_name.replace("W", "").replace("_ODI_REPO", ""));
        return workRepositoryId;
    }

    public static void startScenario(OdiExecuteScenario odiExecuteScenario, String logLevel,
                                     String scenarioName, RegressionConfiguration regressionConfiguration, String agentUrl) {
        String pUser = regressionConfiguration.getOdiSupervisorUser();
        String pPassword = regressionConfiguration.getOdiSupervisorPassword();
        String pContextCode = regressionConfiguration.getOdiContext();
        String pWorkRepName = regressionConfiguration.getOdiWorkRepositoryName();
        odiExecuteScenario.startScenario(agentUrl, pUser, pPassword, pContextCode,
                logLevel, pWorkRepName, scenarioName);
    }

}
