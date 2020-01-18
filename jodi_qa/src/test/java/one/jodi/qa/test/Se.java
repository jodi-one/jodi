package one.jodi.qa.test;


import one.jodi.base.config.PasswordConfigImpl;
import one.jodi.bootstrap.JodiController;
import org.apache.logging.log4j.LogManager;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Se {

    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(Se.class);

    // @Test
    public void testGenerate() {
        JodiController controller = new JodiController(true);
        List<String> argList = new ArrayList<String>();
        argList.add("--action");
        argList.add("etls");
        argList.add("--config");
        argList.add("../../Se/conf/Se.properties");
        argList.add("--password");
        argList.add("welcome1");
        argList.add("--masterpassword");
        argList.add("welcome1");
        argList.add("--prefix");
        argList.add("Init ");
        argList.add("--metadata");
        argList.add("../../Se/xml/");
        controller.run(argList.toArray(new String[0]));
    }

    //@Test
    public void testExport() {
        JodiController controller = new JodiController(true);
        List<String> argList = new ArrayList<String>();
        argList.add("--action");
        argList.add("oex");
        argList.add("--config");
        argList.add("../../Se/conf/Se.properties");
        argList.add("--password");
        argList.add("welcome1");
        argList.add("--masterpassword");
        argList.add("welcome1");
        argList.add("--prefix");
        argList.add("Init ");
        argList.add("--metadata");
        argList.add("../../Se/xml/");
        argList.add("-dapwd");
        argList.add(new PasswordConfigImpl().getDeploymentArchivePassword());
        controller.run(argList.toArray(new String[0]));
    }

}
