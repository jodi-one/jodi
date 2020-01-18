package one.jodi.qa.test;

import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.bootstrap.JodiController;

import java.util.ArrayList;
import java.util.List;

public class Argenta {

    //@Test
    public void testImport() {

        final JodiController controller = new JodiController(true);

        List<String> argList = new ArrayList<String>();

        argList.add("-a");
        argList.add("oim");
        argList.add("-c");
        argList.add("src/test/resources/argenta/conf/argenta.properties");
        argList.add("-pw");
        argList.add(new PasswordConfigImpl().getOdiUserPassword());
        argList.add("-mpw");
        argList.add(new PasswordConfigImpl().getOdiMasterRepoPassword());
        argList.add("-m");
        argList.add("src/test/resources/argenta/xml");
        argList.add("-dapwd");
        argList.add("");
        argList.add("-devmode");
        controller.run(argList.toArray(new String[0]));
        controller.getErrorReport();
    }

}
