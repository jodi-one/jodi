package one.jodi.qa.test;

import org.apache.commons.configuration2.Configuration;

public interface RegressionConfiguration {

    public String getSysdbaUser();

    public String getSysdbaPassword();

    public String getJdbcUrlMasterRepository();

    public String getMasterRepositoryJdbcUser();

    public String getMasterRepositoryJdbcPassword();

    public String getWorkRepositoryJdbcPassword();

    public String getOdiSupervisorUser();

    public String getOdiSupervisorPassword();

    public String getMasterRepositoryJdbcUrl();

    public String getMasterRepositoryJdbcDriver();

    public String getWorkRepositoryJdbcUrl();

    public String getWorkRepositoryJdbcDriver();

    public int getWorkRepositoryId();

    public String getMasterRepositoryTechnology();

    public Configuration getConfig();

    public String getOdiContext();

    public String getOdiWorkRepositoryName();

    public boolean isInstall();

    public String getSmartExport();

    public String getWorkRepositoryJdbcUsername();

    public String getProjectCode();

}