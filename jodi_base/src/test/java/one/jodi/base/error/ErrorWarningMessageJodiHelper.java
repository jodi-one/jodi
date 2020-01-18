package one.jodi.base.error;

public class ErrorWarningMessageJodiHelper {

    private static ErrorWarningMessageJodi errorWarningMessages;

    public static ErrorWarningMessageJodi getTestErrorWarningMessages() {
        if (ErrorWarningMessageJodiHelper.errorWarningMessages == null) {
            errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();
        }
        return errorWarningMessages;
    }

}