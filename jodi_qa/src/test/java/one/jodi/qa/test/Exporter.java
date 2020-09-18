package one.jodi.qa.test;

import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.bootstrap.JodiController;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Exporter {

    @Test
    public void test190Export() {
        final JodiController controller = new JodiController(true);
        List<String> argList = new ArrayList<String>();
        argList.add("-a");
        argList.add("oex");
        argList.add("-c");
        argList.add("src/test/resources/FunctionalTest/Properties_12c/FunctionalTest.properties");
        argList.add("-p");
        argList.add("Init");
        argList.add("-m");
        argList.add("src/test/resources/FunctionalTest/xml");
        argList.add("-dapwd");
        argList.add(new PasswordConfigImpl().getDeploymentArchivePassword());
        argList.add("-da_type");
        argList.add("DA_INITIAL");
        argList.add("-pw");
        argList.add(new PasswordConfigImpl().getOdiUserPassword());
        argList.add("-mpw");
        argList.add(new PasswordConfigImpl().getOdiMasterRepoPassword());
        argList.add("-devmode");
        controller.run(argList.toArray(new String[0]));
        System.out.println(controller.getErrorReport());
    }
}
