package one.jodi.qa.test;


import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.bootstrap.JodiController;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Ec {

    @Test
    public void test011Install() {
        String sourceDir = "D:/git/jodi/jodi_qa/src/test/resources/Ec/";
        final String action = "oim";
        final String configFile = sourceDir + "conf/Ec.properties";
        final String metadataDir = sourceDir + "xml";
        final String prefix = "BACKUP";
        final String odiSchemaPwd = new PasswordConfigImpl().getOdiMasterRepoPassword();
        final String odiUserPwd = new PasswordConfigImpl().getOdiUserPassword();
        final String dapwd = new PasswordConfigImpl().getDeploymentArchivePassword();
        final List<String> argList = new ArrayList<>();
        argList.add("-a");
        argList.add(action);
        argList.add("-c");
        argList.add(configFile);
        argList.add("-pw");
        argList.add(odiUserPwd);
        argList.add("-mpw");
        argList.add(odiSchemaPwd);
        argList.add("-devmode");
        argList.add("-m");
        argList.add(metadataDir);
        argList.add("-p");
        argList.add(prefix);
        argList.add("-dapwd");
        argList.add(new PasswordConfigImpl().getDeploymentArchivePassword());
        final JodiController controller = new JodiController(true);
        controller.run(argList.toArray(new String[0]));
        final String errorReport = controller.getErrorReport();
        System.out.print(errorReport);
    }
}
