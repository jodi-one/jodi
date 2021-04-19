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
    private static final Logger logger = LogManager.getLogger(Version.class);
    private static final String ERROR_MESSAGE_83000 = "The Version property file has not been found.";
    private static final String ERROR_MESSAGE_83010 =
            "Fatal error while attempting to retrieve the Version property file.";
    private static final Properties PROPERTIES = new Properties();
    private static final ErrorWarningMessageJodi ERROR_WARNING_MESSAGES = ErrorWarningMessageJodiImpl.getInstance();

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
                PROPERTIES.load(inputStream);
            } catch (Exception e) {
                String msg = ERROR_WARNING_MESSAGES.formatMessage(83000, ERROR_MESSAGE_83000, Version.class);
                logger.warn(msg);
                ERROR_WARNING_MESSAGES.addMessage(ERROR_WARNING_MESSAGES.assignSequenceNumber(), msg,
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
            String msg = ERROR_WARNING_MESSAGES.formatMessage(83010, ERROR_MESSAGE_83010, Version.class);
            ERROR_WARNING_MESSAGES.addMessage(ERROR_WARNING_MESSAGES.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }
    }

    public static String getProductVersion() {
        return PROPERTIES.getProperty(PRODUCT_VERSION);
    }

    @SuppressWarnings("unused")
    public static String getMajorVersion() {
        return PROPERTIES.getProperty(PRODUCT_MAJOR_VERSION);
    }

    @SuppressWarnings("unused")
    public static String getMinorVersion() {
        return PROPERTIES.getProperty(PRODUCT_MINOR_VERSION);
    }

    @SuppressWarnings("unused")
    public static String getPatchVersion() {
        return PROPERTIES.getProperty(PRODUCT_PATCH_VERSION);
    }

    @SuppressWarnings("unused")
    public static String getPatchVersion2() {
        return PROPERTIES.getProperty(PRODUCT_PATCH_VERSION2);
    }

    @SuppressWarnings("unused")
    public static String getPatchVersion3() {
        return PROPERTIES.getProperty(PRODUCT_PATCH_VERSION3);
    }

    @SuppressWarnings("unused")
    public static String getBuildNumber() {
        return PROPERTIES.getProperty(BUILD_NUMBER);
    }

    public static String getBuildDate() {
        return PROPERTIES.getProperty(BUILD_DATE);
    }

    public static String getBuildTime() {
        return PROPERTIES.getProperty(BUILD_TIME);
    }

    @SuppressWarnings("unused")
    public static Boolean isReleaseVersion() {
        return Boolean.valueOf(PROPERTIES.getProperty(BUILD_IS_RELEASE));
    }
}
