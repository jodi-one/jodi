package one.jodi.qa.test;

import one.jodi.bootstrap.JodiController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseTest {

    protected final String propertiesLocation;
    protected final String password;
    protected final String masterPassword;
    protected final String folderName;
    private final String metaDataDirectory;
    protected boolean vboxSetup;
    protected String serverName = "";
    protected String serviceName = "undetermined";

    public BaseTest(final String metaDataDirectory, final String propertiesFile,
                    final String password, final String masterPassword,
                    final String folderName) {
        this.metaDataDirectory = metaDataDirectory;
        this.propertiesLocation = propertiesFile;
        this.password = password;
        this.masterPassword = (masterPassword == null) ? "" : masterPassword;
        this.folderName = folderName;

        Pattern p = Pattern.compile("\\s*odb.star.url\\s*=\\s*jdbc:oracle:thin:@([\\w\\.]+):1521/(\\w+)");
        try {
            Files.lines(Paths.get(propertiesFile))
                    .map(p::matcher)
                    .filter(Matcher::matches)
                    .findFirst()
                    .ifPresent(matcher -> serverName = matcher.group(1));
            this.vboxSetup = serverName.equalsIgnoreCase("ct");

        } catch (IOException e) {
            this.vboxSetup = false;
        }
        try {
            Files.lines(Paths.get(propertiesFile))
                    .map(p::matcher)
                    .filter(Matcher::matches)
                    .findFirst()
                    .ifPresent(matcher -> serviceName = matcher.group(2));
        } catch (IOException e) {
            this.vboxSetup = false;
        }
    }

    public boolean isVboxSetup() {
        return vboxSetup;
    }

    public void test00000(String cmd, String... fileName) {
        String[] params = {
                "-a",
                cmd,
                "-c",
                propertiesLocation,
                "-pw",
                password,
                "-mpw",
                masterPassword,
                "-m", this.metaDataDirectory,
                "--prefix",
                "Int_",
                "-f",
                this.folderName,
                "-devmode",
        };
        // removed parameters
        JodiController controller = new JodiController(true);
        controller.run(params, fileName);
        //controller.getErrorReport();
    }

}