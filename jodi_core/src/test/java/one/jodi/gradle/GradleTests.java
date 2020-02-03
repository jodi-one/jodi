package one.jodi.gradle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleTests {
    private final static Logger logger = LogManager.getLogger(
            GradleTests.class);

    @Test
    public void testGradleStartScript() throws FileNotFoundException {
        final String buildFile = "build.gradle";
        testAllForPath(buildFile);
        testAllForPath("../jodi_odi12/" + buildFile);
        testAllForPath("../jodi_tools/" + buildFile);
    }

    private void testAllForPath(String buildfile2) throws FileNotFoundException {
        testAbsenceOfPattern(buildfile2, "\\${1,1}:{0,}\\s{1,}\\$od{1,1}");
        testPresenceOfString(buildfile2, "writer.println \"$odiLibPathVariable=\\$$odiLibPathVariable\"");
        testPresenceOfString(buildfile2, "writer.println \"set $odiLibPathVariable=%$odiLibPathVariable%\"");
        testLog4j(buildfile2);
    }

    private void testLog4j(final String path) throws FileNotFoundException {
        String windowsCheckString = "writer.println \"set DEFAULT_JVM_OPTS=\\\"-Dlog4j.configurationFile=%~dp0/../conf/log4j2.xml\\\"\"";
        testPresenceOfString(path, windowsCheckString);
        String linuxCheckString = "writer.println \"DEFAULT_JVM_OPTS=\\\"-Dlog4j.configurationFile=\\${0%/*}/../conf/log4j2.xml\\\"\"";
        testPresenceOfString(path, linuxCheckString);
    }

    /**
     * The goals is to find $: $od in the unix starter script since then
     * variables are not found. then it should be cahnged to $$od......
     *
     * @param path
     * @throws FileNotFoundException
     */
    private void testAbsenceOfPattern(final String path, final String pattern) throws FileNotFoundException {
        File buildFilePath = new File(path);
        if (!buildFilePath.exists()) {
            throw new RuntimeException("Can't test file; " + path);
        }
        Scanner scannner = null;
        try {
            scannner = new Scanner(buildFilePath, "UTF-8");
            String content = scannner.useDelimiter("\\Z").next();
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(content);
            if (m.find()) {
                throw new RuntimeException(
                        "Found $: $od in the starter script this should be corrected in '$$od.... etc' in file "
                                + path);
            }
        } finally {
            if (scannner != null)
                scannner.close();
        }
    }

    private void testPresenceOfString(final String path, final String windowsCheckString) throws FileNotFoundException {
        File buildFilePath = new File(path);
        Scanner scannner = null;
        boolean found = false;
        try {
            scannner = new Scanner(buildFilePath, "UTF-8");
            String content = scannner.useDelimiter("\\Z").next();
            logger.info(content);
            if (content.trim().contains(windowsCheckString)) {
                found = true;
            }
        } finally {
            if (scannner != null)
                scannner.close();
        }
        if (!found) {
            throw new RuntimeException(
                    "wrong build script set on file: " + path + " missing string is: " + windowsCheckString);
        }
    }
}
