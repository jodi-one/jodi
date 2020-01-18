package one.jodi.base.util;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.exception.UnRecoverableException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

    private static final String FILE_NAME = "version.properties";

    private static final String PRODUCT_VERSION = "product.version";
    private static final String PRODUCT_MAJOR_VERSION = "product.version.major";
    private static final String PRODUCT_MINOR_VERSION = "product.version.minor";
    private static final String PRODUCT_PATCH_VERSION = "product.version.patch";
    private static final String PRODUCT_PATCH_VERSION2 = "product.version.patch2";
    private static final String PRODUCT_PATCH_VERSION3 = "product.version.patch3";
    private static final String BUILD_NUMBER = "build.number";
    private static final String BUILD_DATE = "build.date";
    private static final String BUILD_TIME = "build.time";
    private static final String BUILD_IS_RELEASE = "build.isRelease";
    private final static Logger logger = LogManager.getLogger(Version.class);
    private static final String ERROR_MESSAGE_83000 =
            "The Version property file has not been found.";
    private static final String ERROR_MESSAGE_83010 =
            "Fatal error while attempting to retrieve the Version property file.";
    private static Properties properties = new Properties();
    private static ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiImpl.getInstance();

    /*
     * The init method looks for the version.properties file in the root of the jar.
     * on windows the File file = file new File(...); method does not work with
     * JDeveloper version 11.1.2.3, the getResourceAsStream method does.
     * If changes are to be made to this init method, it should be tested on
     * Windows 7, with JDeveloper, since this is the only platform where File file = new File(...)
     * is not working. Java version at which this occurs is 1.6.0.24
     */

    public static void init() {
        InputStream inputStream = null;
        ClassLoader cl = Version.class.getClassLoader();
        if (cl != null) {
            try {
                inputStream = cl.getResourceAsStream(FILE_NAME);
                properties.load(inputStream);
            } catch (Exception e) {
                String msg = errorWarningMessages.formatMessage(83000,
                        ERROR_MESSAGE_83000, Version.class);
                logger.warn(msg);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new IllegalArgumentException(msg, e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        System.err.println("Cannot close Jodi Version file.");
                    }
                }
            }
        } else {
            String msg = errorWarningMessages.formatMessage(83010,
                    ERROR_MESSAGE_83010, Version.class);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }
    }

    public static String getProductVersion() {
        return properties.getProperty(PRODUCT_VERSION);
    }

    public static String getMajorVersion() {
        return properties.getProperty(PRODUCT_MAJOR_VERSION);
    }

    public static String getMinorVersion() {
        return properties.getProperty(PRODUCT_MINOR_VERSION);
    }

    public static String getPatchVersion() {
        return properties.getProperty(PRODUCT_PATCH_VERSION);
    }

    public static String getPatchVersion2() {
        return properties.getProperty(PRODUCT_PATCH_VERSION2);
    }

    public static String getPatchVersion3() {
        return properties.getProperty(PRODUCT_PATCH_VERSION3);
    }


    public static String getBuildNumber() {
        return properties.getProperty(BUILD_NUMBER);
    }

    public static String getBuildDate() {
        return properties.getProperty(BUILD_DATE);
    }

    public static String getBuildTime() {
        return properties.getProperty(BUILD_TIME);
    }

    public static Boolean isReleaseVersion() {
        return Boolean.valueOf(properties.getProperty(BUILD_IS_RELEASE));
    }
}
