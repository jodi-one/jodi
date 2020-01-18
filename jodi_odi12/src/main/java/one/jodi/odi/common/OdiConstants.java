package one.jodi.odi.common;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains constants that are directly related to ODI only.
 *
 */
public class OdiConstants {

    // ODI repository properties
    // must remain public to be properly introspected (see below)
    public static final String ODI_MASTER_REPO_URL = "odi.master.repo.url";
    public static final String ODI_REPO_DB_DRIVER = "odi.repo.db.driver";
    public static final String ODI_MASTER_REPO_USERNAME = "odi.master.repo.username";
    public static final String ODI_LOGIN_USERNAME = "odi.login.username";
    public static final String ODI_WORK_REPO = "odi.work.repo";
    // context
    public static final String ODI_CONTEXT = "odi.context";
    public static final String ODI_SMART_EXPORTFILE = "SmartExport.xml";
    public static final String ODI_SMART_IMPORT_RESPONSE_FILE = "SmartImportResponse.xml";
    private static final String ERROR_MESSAGE_00310 =
            "Unexpected exception during introspection of '%1$s'.";
    private static ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();

    public static List<String> getStaticFieldValues() {

        List<String> staticFieldValues = new ArrayList<>();
        Field[] declaredFields = OdiConstants.class.getDeclaredFields();
        try {
            //collect final fields of type String
            for (Field field : declaredFields) {
                if ((java.lang.reflect.Modifier.isStatic(field.getModifiers())) &&
                        (java.lang.reflect.Modifier.isPublic(field.getModifiers())) &&
                        (field.getType() == java.lang.String.class)) {
                    // (field.getType().equals(java.lang.String.class))) {
                    String value = ((String) field.get(null));
                    staticFieldValues.add(value);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            String msg = errorWarningMessages.formatMessage(310,
                    ERROR_MESSAGE_00310, Class.class.getClass(),
                    OdiConstants.class.toString());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg, e);
        }
        return staticFieldValues;
    }

}
