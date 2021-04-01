package one.jodi.qa.test;

import org.apache.commons.configuration2.Configuration;

public interface RegressionConfiguration {

    String getSysdbaUser();

    String getSysdbaPassword();

    String getJdbcUrlMasterRepository();

    String getMasterRepositoryJdbcUser();

    String getMasterRepositoryJdbcPassword();

    String getWorkRepositoryJdbcPassword();

    String getOdiSupervisorUser();

    String getOdiSupervisorPassword();

    String getMasterRepositoryJdbcUrl();

    String getMasterRepositoryJdbcDriver();

    String getWorkRepositoryJdbcUrl();

    String getWorkRepositoryJdbcDriver();

    int getWorkRepositoryId();

    String getMasterRepositoryTechnology();

    Configuration getConfig();

    String getOdiContext();

    String getOdiWorkRepositoryName();

    boolean isInstall();

    String getSmartExport();

    String getWorkRepositoryJdbcUsername();

    String getProjectCode();

}